package codechicken.translocator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.math.MathHelper;
import codechicken.lib.inventory.InventoryUtils;
import codechicken.lib.packet.ICustomPacketTile;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import static codechicken.lib.vec.Rotation.*;
import static codechicken.lib.vec.Vector3.*;

public abstract class TileTranslocator extends TileEntity implements ICustomPacketTile
{
    public class Attachment
    {
        public final int side;
        
        public boolean a_eject;
        public boolean b_eject;
        
        public boolean redstone;
        public boolean invert_redstone;
        public boolean fast;
        
        public double a_insertpos;
        public double b_insertpos;

        public Attachment(int side)
        {
            this.side = side;
            a_eject = b_eject = invert_redstone = true;
            a_insertpos = b_insertpos = 1;
        }
        
        public void read(NBTTagCompound tag)
        {
            invert_redstone = tag.getBoolean("invert_redstone");
            redstone = tag.getBoolean("redstone");
            fast = tag.getBoolean("fast");
        }
        
        public void update(boolean client)
        {
            b_insertpos = a_insertpos;
            a_insertpos = MathHelper.approachExp(a_insertpos, approachInsertPos(), 0.5, 0.1);
                
            if(!client)
            {
                b_eject = a_eject;
                a_eject = (redstone && gettingPowered()) != invert_redstone;
                if(a_eject != b_eject)
                    markUpdate();
            }
        }

        public double approachInsertPos()
        {
            return a_eject ? 1 : 0;
        }

        public void write(PacketCustom packet)
        {
            packet.writeBoolean(a_eject);
            packet.writeBoolean(redstone);
            packet.writeBoolean(fast);
        }
        
        public void read(PacketCustom packet, boolean described)
        {
            a_eject = packet.readBoolean();
            redstone = packet.readBoolean();
            fast = packet.readBoolean();
            
            if(!described)
                a_insertpos = b_insertpos = approachInsertPos();
        }

        public NBTTagCompound write(NBTTagCompound tag)
        {
            tag.setBoolean("invert_redstone", invert_redstone);
            tag.setBoolean("redstone", redstone);
            tag.setBoolean("fast", fast);
            return tag;
        }

        public boolean activate(EntityPlayer player, int subPart)
        {
            ItemStack held = player.inventory.getCurrentItem();
            if(held == null && player.isSneaking())
            {
                stripModifiers();
                markUpdate();
            }
            else if(held == null)
            {
                if(subPart == 1)
                    invert_redstone = !invert_redstone;
                else
                    openGui(player);
            }
            else if(held.getItem() == Items.redstone && !redstone)
            {
                redstone = true;
                if(!player.capabilities.isCreativeMode)
                    held.stackSize--;
                
                if((gettingPowered() != invert_redstone) != a_eject)
                    invert_redstone = !invert_redstone;
                markUpdate();
            }
            else if(held.getItem() == Items.glowstone_dust && !fast)
            {
                fast = true;
                if(!player.capabilities.isCreativeMode)
                    held.stackSize--;
                markUpdate();
            }
            else
                openGui(player);
            
            return true;
        }
        
        public void stripModifiers()
        {
            if(redstone)
            {
                redstone = false;
                dropItem(new ItemStack(Items.redstone));
                
                if(invert_redstone != a_eject)
                    invert_redstone = !invert_redstone;
            }
            if(fast)
            {
                fast = false;
                dropItem(new ItemStack(Items.glowstone_dust));
            }
        }

        public void openGui(EntityPlayer player)
        {
        }

        public void markUpdate()
        {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            markDirty();
        }

        public Collection<ItemStack> getDrops()
        {
            LinkedList<ItemStack> items = new LinkedList<ItemStack>();
            items.add(new ItemStack(getBlockType(), 1, getBlockMetadata()));
            if(redstone)
                items.add(new ItemStack(Items.redstone));
            if(fast)
                items.add(new ItemStack(Items.glowstone_dust));
            return items;
        }

        public int getIconIndex()
        {
            int i = 0;
            if(redstone)
                i|= gettingPowered() ? 2 : 1;
            if(fast)
                i|=4;
            return i;
        }
    }
    
    public Attachment[] attachments = new Attachment[6];
    
    @Override
    public void updateEntity()
    {
        for(Attachment a : attachments)
            if(a != null)
                a.update(worldObj.isRemote);
    }

    @Override
    public Packet getDescriptionPacket()
    {
        PacketCustom packet = new PacketCustom(TranslocatorSPH.channel, 1);
        packet.writeCoord(xCoord, yCoord, zCoord);
        
        int attachmask = 0;
        for(int i = 0; i < 6; i++)
            if(attachments[i] != null)
                attachmask|= 1<<i;
        
        packet.writeByte(attachmask);
        for(Attachment a : attachments)
            if(a != null)
                a.write(packet);
        
        return packet.toPacket();
    }

    @Override
    public void handleDescriptionPacket(PacketCustom packet)
    {
        if(packet.getType() == 1)
        {
            int attachmask = packet.readUByte();
            for(int i = 0; i < 6; i++)
            {
                if((attachmask&1<<i) != 0)
                {
                    boolean described = attachments[i] != null;
                    if(!described)
                        createAttachment(i);
                    attachments[i].read(packet, described);
                }
                else
                    attachments[i] = null;
            }
            
            worldObj.func_147479_m(xCoord, yCoord, zCoord);
        }
    }

    public void createAttachment(int side)
    {
        attachments[side] = new Attachment(side);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        for(int i = 0; i < 6; i++)
            if(attachments[i] != null)
                tag.setTag("atmt" + i, attachments[i].write(new NBTTagCompound()));
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        
        for(int i = 0; i < 6; i++)
            if(tag.hasKey("atmt"+i))
            {
                createAttachment(i);
                attachments[i].read(tag.getCompoundTag("atmt"+i));
            }
    }

    public void addTraceableCuboids(List<IndexedCuboid6> cuboids)
    {
        Vector3 pos = Vector3.fromTileEntity(this);
        Cuboid6 base = new Cuboid6(3/16D, 0, 3/16D, 13/16D, 2/16D, 13/16D);
        
        for(int i = 0; i < 6; i++)
        {
            Attachment a = attachments[i];
            if(a != null)
            {
                cuboids.add(new IndexedCuboid6(i, transformPart(base, pos, i)));
                cuboids.add(new IndexedCuboid6(i+6, transformPart(
                        new Cuboid6(6/16D, 0, 6/16D, 10/16D, a.a_insertpos*2/16D+1/16D, 10/16D), 
                        pos, i)));
            }
        }
    }
    
    private Cuboid6 transformPart(Cuboid6 box, Vector3 pos, int i)
    {
        return box.copy()
            .apply(sideRotations[i].at(center))
            .add(pos);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord+1, yCoord+1, zCoord+1);
    }

    public boolean harvestPart(int i, boolean drop)
    {
        Attachment a = attachments[i];
        if(!worldObj.isRemote && drop)
            for(ItemStack stack : a.getDrops())
                dropItem(stack);
        
        attachments[i] = null;
        
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        for(Attachment a1 : attachments)
            if(a1 != null)
                return false;
        
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        return true;
    }

    public void dropItem(ItemStack stack)
    {
        InventoryUtils.dropItem(stack, worldObj, Vector3.fromTileEntityCenter(this));
    }

    public boolean gettingPowered()
    {
        return worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
    }
    
    public boolean connectRedstone()
    {
        for(Attachment a : attachments)
            if(a != null && a.redstone)
                return true;
        return false;
    }

    public int strongPowerLevel(int side)
    {
        return 0;
    }
}
