package codechicken.translocator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.math.MathHelper;
import codechicken.core.fluid.FluidUtils;
import codechicken.core.fluid.TankAccess;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileLiquidTranslocator extends TileTranslocator implements IFluidHandler
{
    public class MovingLiquid
    {
        public int src;
        public int dst;
        public FluidStack liquid;
        
        public double a_start;
        public double b_start;
        
        public double a_end;
        public double b_end;
        
        public boolean fast;
        
        public MovingLiquid(int src, int dst, FluidStack add)
        {
            this.src = src;
            this.dst = dst;
            liquid = add;
            fast = attachments[src].fast;
            capLiquid();
        }

        private void capLiquid()
        {
            liquid.amount = Math.min(liquid.amount, fast ? 1000 : 100);
        }

        public boolean update()
        {
            if(a_end == 1)
                return true;
            
            b_start = a_start;
            a_start = MathHelper.approachLinear(a_start, 1, 0.2);
            
            b_end = a_end;
            
            if(liquid.amount > 0)
            {
                liquid.amount = Math.max(liquid.amount-(fast ? 200 : 20), 0);
                return liquid.amount == 0;
            }
            a_end = MathHelper.approachLinear(a_end, 1, 0.2);
            
            return false;
        }
        
        public void addLiquid(int moving)
        {
            if(liquid.amount == 0)
                throw new IllegalArgumentException("Something went wrong!");
            
            liquid.amount += moving;
            if(attachments[src] != null)
                fast = attachments[src].fast;
            capLiquid();
        }

        public void finish()
        {
            liquid.amount = 0;
        }
    }
    
    private class LiquidTransfer
    {
        public LiquidTransfer(int src, int dst, FluidStack liquid)
        {
            key = src<<4|dst;
            this.liquid = liquid;
        }
        
        public LiquidTransfer(int key, FluidStack liquid)
        {
            this.key = key;
            this.liquid = liquid;
        }

        int key;
        FluidStack liquid;
    }

    public LinkedList<MovingLiquid> movingLiquids = new LinkedList<MovingLiquid>();
    public LinkedList<MovingLiquid> exitingLiquids = new LinkedList<MovingLiquid>();
    
    @Override
    public void updateEntity()
    {
        super.updateEntity();
        
        if(worldObj.isRemote)
        {
            for(Iterator<MovingLiquid> iterator = movingLiquids.iterator(); iterator.hasNext();)
            {
                MovingLiquid m = iterator.next();
                if(m.update())
                {
                    iterator.remove();
                    exitingLiquids.add(m);
                }
            }
            
            for(Iterator<MovingLiquid> iterator = exitingLiquids.iterator(); iterator.hasNext();)
                if(iterator.next().update())
                    iterator.remove();
        }
        else
        {
            BlockCoord pos = new BlockCoord(this);
            TankAccess[] attached = new TankAccess[6];
            int[] outputs = null;
            int[] r_outputs = null;
            
            for(int i = 0; i < 6; i++)
            {
                Attachment a = attachments[i];
                if(a == null)
                    continue;
    
                BlockCoord invpos = pos.copy().offset(i);
                TileEntity tile = worldObj.getTileEntity(invpos.x, invpos.y, invpos.z);
                if(!(tile instanceof IFluidHandler))
                {
                    harvestPart(i, true);
                    continue;
                }
                attached[i] = new TankAccess((IFluidHandler)tile, i^1);
            }
            
            ArrayList<LiquidTransfer> transfers = new ArrayList<LiquidTransfer>();
            
            for(int i = 0; i < 6; i++)
            {
                Attachment a = attachments[i];
                if(a == null || !a.a_eject)
                    continue;
                
                TankAccess t = attached[i];
                FluidStack drain = t.drain(a.fast ? 1000 : 100, false);
                if(drain == null || drain.amount == 0)
                    continue;
                
                if(outputs == null)
                {
                    outputs = sortOutputs(false);
                    r_outputs = sortOutputs(true);
                }
                
                FluidStack move = drain.copy();
                spreadOutput(move, i, outputs, attached, transfers);
                spreadOutput(move, i, r_outputs, attached, transfers);
                t.drain(drain.amount-move.amount, true);
            }
            
            if(!transfers.isEmpty())
                sendTransferPacket(transfers);
        }
    }

    private void spreadOutput(FluidStack move, int src, int[] outputs, TankAccess[] attached, ArrayList<LiquidTransfer> transfers)
    {
        for(int k = 0; k < outputs.length && move.amount > 0; k++)
        {
            int dst = outputs[k];
            TankAccess outaccess = attached[dst];
            
            int fit = outaccess.fill(move, false);
            int spread = outputs.length-k;
            fit = Math.min(fit, move.amount/spread + worldObj.rand.nextInt(move.amount%spread+1));
            
            if(fit == 0)
                continue;
            
            FluidStack add = FluidUtils.copy(move, fit);
            outaccess.fill(add, true);
            move.amount-=fit;
            
            transfers.add(new LiquidTransfer(src, dst, add));
        }
    }
    
    public int[] sortOutputs(boolean b)
    {
        int[] str = new int[6];
        int k = 0;
        for(int i = 0; i < 6; i++)
        {
            Attachment a = attachments[i];
            if(a != null && !a.a_eject && a.redstone == b)
                str[k++] = i;
        }
        
        int[] ret = new int[k];
        System.arraycopy(str, 0, ret, 0, k);
        return ret;
    }

    private void sendTransferPacket(ArrayList<LiquidTransfer> transfers)
    {
        PacketCustom packet = new PacketCustom(TranslocatorSPH.channel, 2);
        packet.writeCoord(xCoord, yCoord, zCoord);
        packet.writeByte(transfers.size());
        for(LiquidTransfer t : transfers)
        {
            packet.writeByte(t.key);
            packet.writeFluidStack(t.liquid);
        }
        packet.sendToChunk(worldObj, xCoord>>4, zCoord>>4);
    }
    
    @Override
    public void handleDescriptionPacket(PacketCustom packet)
    {
        if(packet.getType() == 2)
        {
            ArrayList<LiquidTransfer> transfers = new ArrayList<LiquidTransfer>();
            HashSet<Integer> maintainingKeys = new HashSet<Integer>();
            
            int k = packet.readUByte();
            for(int i = 0; i < k; i++)
            {
                LiquidTransfer t = new LiquidTransfer(packet.readUByte(), packet.readFluidStack());
                transfers.add(t);
                maintainingKeys.add(t.key);
            }
            
            for(LiquidTransfer t : transfers)
            {
                int src = t.key>>4;
                int dst = t.key&0xF;
                
                boolean found = false;
                for(Iterator<MovingLiquid> iterator = movingLiquids.iterator(); iterator.hasNext();)
                {
                    MovingLiquid m = iterator.next();
                    if(m.liquid.isFluidEqual(t.liquid) && m.src == src && m.dst == dst)
                    {
                        m.addLiquid(t.liquid.amount);
                        found = true;
                        continue;
                    }
                    if((m.dst == dst || m.src == src) && !maintainingKeys.contains(m.src<<4|m.dst))
                    {
                        iterator.remove();
                        m.finish();
                        exitingLiquids.add(m);
                    }
                }
                
                if(!found && attachments[src] != null)
                    movingLiquids.add(new MovingLiquid(src, dst, t.liquid));
            }
        }
        else
        {
            super.handleDescriptionPacket(packet);
        }
    }

    public Iterable<MovingLiquid> movingLiquids()
    {
        ArrayList<MovingLiquid> comp = new ArrayList<TileLiquidTranslocator.MovingLiquid>();
        comp.addAll(movingLiquids);
        comp.addAll(exitingLiquids);
        return comp;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        return null;
    }
    
    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        return null;
    }
    
    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return false;
    }
    
    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return false;
    }
    
    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        if(from == ForgeDirection.UNKNOWN) {
            List<FluidTankInfo> list = new LinkedList<FluidTankInfo>();
            for(Attachment a : attachments)
                if(a != null)
                    list.add(new FluidTankInfo(null, 0));
            
            return list.toArray(new FluidTankInfo[0]);
        }
        
        if(attachments[from.ordinal()] != null)
            return new FluidTankInfo[]{new FluidTankInfo(null, 0)};
        
        return new FluidTankInfo[0];
    }
}
