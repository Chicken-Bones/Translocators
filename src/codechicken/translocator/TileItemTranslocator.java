package codechicken.translocator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import codechicken.core.IGuiPacketSender;
import codechicken.core.ServerUtils;
import codechicken.lib.math.MathHelper;
import codechicken.lib.inventory.InventoryRange;
import codechicken.lib.inventory.InventorySimple;
import codechicken.lib.inventory.InventoryUtils;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TileItemTranslocator extends TileTranslocator
{
    public class ItemAttachment extends Attachment
    {
        boolean regulate = false;
        boolean a_powering = false;
        boolean signal = false;
        
        ItemStack[] filters = new ItemStack[9];
        
        public ItemAttachment(int side)
        {
            super(side);
        }

        public void setPowering(boolean b)
        {
            if((signal || !b) && b != a_powering)
            {
                a_powering = b;
                BlockCoord pos = new BlockCoord(TileItemTranslocator.this);
                worldObj.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, Blocks.redstone_wire);
                pos.offset(side);
                worldObj.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, Blocks.redstone_wire);
                markUpdate();
            }
        }
        
        @Override
        public boolean activate(EntityPlayer player, int subPart)
        {
            ItemStack held = player.inventory.getCurrentItem();
            if(held == null)
            {
                return super.activate(player, subPart);
            }
            else if(held.getItem() == Translocator.itemDiamondNugget && !regulate)
            {
                regulate = true;
                
                if(!player.capabilities.isCreativeMode)
                    held.stackSize--;
                markUpdate();
                return true;
            }
            else if(held.getItem() == Items.iron_ingot && !signal)
            {
                signal = true;
                
                if(!player.capabilities.isCreativeMode)
                    held.stackSize--;
                markUpdate();
                return true;
            }
            else
            {
                return super.activate(player, subPart);
            }
        }
        
        @Override
        public void stripModifiers()
        {
            super.stripModifiers();
            if(regulate)
            {
                regulate = false;
                dropItem(new ItemStack(Translocator.itemDiamondNugget));
            }
            if(signal)
            {
                setPowering(false);
                signal = false;
                dropItem(new ItemStack(Items.iron_ingot));
            }
        }
        
        @Override
        public Collection<ItemStack> getDrops()
        {
            Collection<ItemStack> stuff = super.getDrops();
            if(regulate)
                stuff.add(new ItemStack(Translocator.itemDiamondNugget));
            if(signal)
                stuff.add(new ItemStack(Items.iron_ingot));
            return stuff;
        }
        
        @Override
        public int getIconIndex()
        {
            int i = super.getIconIndex();
            if(regulate)
                i|=8;
            if(signal)
                i|= a_powering? 32 : 16;
            return i;
        }
        
        @Override
        public void openGui(EntityPlayer player)
        {
            openItemGui(player, filters, regulate ? "translocator.regulate" : "translocator.filter");
        }

        private void openItemGui(EntityPlayer player, ItemStack[] filters, final String string)
        {
            ServerUtils.openSMPContainer((EntityPlayerMP) player, 
                new ContainerItemTranslocator(new InventorySimple(filters, filterStackLimit())
            {
                @Override
                public void markDirty()
                {
                    markUpdate();
                }
            }, player.inventory), new IGuiPacketSender()
            {
                @Override
                public void sendPacket(EntityPlayerMP player, int windowId)
                {
                    PacketCustom packet = new PacketCustom(TranslocatorSPH.channel, 4);
                    packet.writeByte(windowId);
                    packet.writeShort(filterStackLimit());
                    packet.writeString(string);
                    
                    packet.sendToPlayer(player);
                }
            });
        }
        
        private int filterStackLimit()
        {
            if(regulate)
                return 65535;
            if(fast)
                return 64;
            return 1;
        }

        @Override
        public void read(NBTTagCompound tag)
        {
            super.read(tag);
            regulate = tag.getBoolean("regulate");
            signal = tag.getBoolean("signal");
            a_powering = tag.getBoolean("powering");
            InventoryUtils.readItemStacksFromTag(filters, tag.getTagList("filters", 10));
        }
        
        @Override
        public NBTTagCompound write(NBTTagCompound tag)
        {
            tag.setBoolean("regulate", regulate);
            tag.setBoolean("signal", signal);
            tag.setBoolean("powering", a_powering);
            tag.setTag("filters", InventoryUtils.writeItemStacksToTag(filters, 65536));
            return super.write(tag);
        }
        
        @Override
        public void read(PacketCustom packet, boolean described)
        {
            super.read(packet, described);
            regulate = packet.readBoolean();
            signal = packet.readBoolean();
            a_powering = packet.readBoolean();
        }
        
        @Override
        public void write(PacketCustom packet)
        {
            super.write(packet);
            packet.writeBoolean(regulate);
            packet.writeBoolean(signal);
            packet.writeBoolean(a_powering);
        }
    }
    
    public class MovingItem
    {
        public int src;
        public int dst;
        public ItemStack stack;
        
        public double a_progress;
        public double b_progress;
        
        public MovingItem(PacketCustom packet)
        {
            int b = packet.readUByte();
            src = b>>4;
            dst = b&0xF;
            stack = packet.readItemStack();
        }
        
        public boolean update()
        {
            if(a_progress >= 1)
                return true;
            
            b_progress = a_progress;
            a_progress = MathHelper.approachLinear(a_progress, 1, 0.2);
            return false;
        }
    }
    
    public LinkedList<MovingItem> movingItems = new LinkedList<MovingItem>();
    
    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        if(tag.hasKey("items"))
            for(Attachment a : attachments)
                if(a != null)
                    InventoryUtils.readItemStacksFromTag(((ItemAttachment)a).filters, tag.getTagList("items", 10));
    }
    
    @Override
    public void createAttachment(int side)
    {
        attachments[side] = new ItemAttachment(side);
    }
    
    @Override
    public void updateEntity()
    {
        super.updateEntity();
        if(worldObj.isRemote)
        {
            for(Iterator<MovingItem> iterator = movingItems.iterator(); iterator.hasNext();)
                if(iterator.next().update())
                    iterator.remove();
        }
        else
        {
            //move those items
            BlockCoord pos = new BlockCoord(this);
            InventoryRange[] attached = new InventoryRange[6];
            
            for(int i = 0; i < 6; i++)
            {
                Attachment a = attachments[i];
                if(a == null)
                    continue;
    
                BlockCoord invpos = pos.copy().offset(i);
                IInventory inv = InventoryUtils.getInventory(worldObj, invpos.x, invpos.y, invpos.z);
                if(inv == null)
                {
                    harvestPart(i, true);
                    continue;
                }
                attached[i] = new InventoryRange(inv, i^1);
            }
            
            for(int i = 0; i < 6; i++)
            {
                ItemAttachment ia = (ItemAttachment) attachments[i];
                if(ia == null || !ia.a_eject)
                    continue;
                
                int largestQuantity = 0;
                int largestSlot = 0;
                InventoryRange access = attached[i];
                for(int slot : access.slots)
                {
                    ItemStack stack = access.inv.getStackInSlot(slot);
                    if(stack == null || !access.canExtractItem(slot, stack) || 
                            stack.stackSize == 0)//stack size 0 hack
                        continue;
                    
                    int quantity = ia.fast ? stack.stackSize : 1;
                    if(quantity <= largestQuantity)
                        continue;
    
                    quantity = Math.min(quantity, extractAmount(stack, ia, access));
                    if(quantity <= largestQuantity)
                        continue;
                    
                    quantity = Math.min(quantity, insertAmount(stack, attached));
                    if(quantity <= largestQuantity)
                        continue;
                    
                    largestSlot = slot;
                    largestQuantity = quantity;
                }
                
                if(largestQuantity > 0)
                {
                    ItemStack move = InventoryUtils.copyStack(access.inv.getStackInSlot(largestSlot), largestQuantity);
                    spreadOutput(move, i, false, attached);
                    spreadOutput(move, i, true, attached);
                    
                    InventoryUtils.decrStackSize(access.inv, largestSlot, largestQuantity-move.stackSize);
                }
            }
            
            boolean allSatisfied = true;
            for(int i = 0; i < 6; i++)
            {
                ItemAttachment ia = (ItemAttachment) attachments[i];
                if(ia != null && !ia.a_eject)
                {
                    boolean b = isSatsified(ia, attached[i]);
                    ia.setPowering(b);
                    if(!b)
                        allSatisfied = false;
                }
            }
            
            for(int i = 0; i < 6; i++)
            {
                ItemAttachment ia = (ItemAttachment) attachments[i];
                if(ia != null && ia.signal && ia.a_eject)
                    ia.setPowering(allSatisfied || !canTransferFilter(ia, attached[i], attached));
            }
        }
    }
    
    
    private boolean matches(ItemStack stack, ItemStack filter)
    {
        return stack.getItem() == filter.getItem() &&
            (!filter.getHasSubtypes() || filter.getItemDamage() == stack.getItemDamage()) && 
            ItemStack.areItemStackTagsEqual(filter, stack);
    }

    private boolean canTransferFilter(ItemAttachment ia, InventoryRange access, InventoryRange[] attached)
    {
        boolean filterSet = false;
        for(ItemStack filter : ia.filters)
            if(filter != null)
            {
                filterSet = true;
                if((!ia.regulate || countMatchingStacks(access, filter, false) > filterCount(ia, filter)) && 
                        insertAmount(filter, attached) > 0)
                    return true;
            }
        
        return !filterSet;
    }

    private boolean isSatsified(ItemAttachment ia, InventoryRange access)
    {
        boolean filterSet = false;
        for(ItemStack filter : ia.filters)
            if(filter != null)
            {
                filterSet = true;
                if(ia.regulate)
                {
                    if(countMatchingStacks(access, filter, !ia.a_eject) < filterCount(ia, filter))
                        return false;
                }
                else
                {
                    if(InventoryUtils.getInsertibleQuantity(access, filter) > 0)
                        return false;
                }
            }
        
        return filterSet || !hasEmptySpace(access);
    }

    private boolean hasEmptySpace(InventoryRange inv)
    {
        for(int slot : inv.slots)
        {
            ItemStack stack = inv.inv.getStackInSlot(slot);
            if(inv.canInsertItem(slot, new ItemStack(Items.diamond)) &&
                    (stack == null || 
                    stack.isStackable() && stack.stackSize < Math.min(stack.getMaxStackSize(), inv.inv.getInventoryStackLimit())))
                return true;
        }
        return false;
    }

    private int filterCount(ItemAttachment ia, ItemStack stack)
    {
        boolean filterSet = false;
        int match = 0;
        for(ItemStack filter : ia.filters)
            if(filter != null)
            {
                filterSet = true;
                if(matches(stack, filter))
                    match += filter.stackSize;
            }
        
        return filterSet ? match : -1;
    }

    private void spreadOutput(ItemStack move, int src, boolean rspass, InventoryRange[] attached)
    {
        if(move.stackSize == 0)
            return;
        
        int outputCount = 0;
        int[] outputQuantities = new int[6];
        for(int i = 0; i < 6; i++)
        {
            ItemAttachment ia = (ItemAttachment) attachments[i];
            if(ia != null && !ia.a_eject && ia.redstone == rspass)
            {
                outputQuantities[i] = insertAmount(move, ia, attached[i]);
                if(outputQuantities[i] > 0)
                    outputCount++;
            }
        }
        
        for(int dst = 0; dst < 6 && move.stackSize > 0; dst++)
        {
            int qty = outputQuantities[dst];
            if(qty <= 0)
                continue;
            
            qty = Math.min(qty, move.stackSize/outputCount + worldObj.rand.nextInt(move.stackSize%outputCount+1));
            outputCount--;
            
            if(qty == 0)
                continue;

            InventoryRange range = attached[dst];
            ItemStack add = InventoryUtils.copyStack(move, qty);
            InventoryUtils.insertItem(range, add, false);
            move.stackSize-=qty;
            
            sendTransferPacket(src, dst, add);
        }
    }

    private int countMatchingStacks(InventoryRange inv, ItemStack filter, boolean insertable)
    {
        int c = 0;
        for(int slot : inv.slots)
        {
            ItemStack stack = inv.inv.getStackInSlot(slot);
            if(stack != null && matches(filter, stack) && 
                    (insertable ? inv.canInsertItem(slot, stack) : inv.canExtractItem(slot, stack)))
                c+=stack.stackSize;
        }
        return c;
    }

    private void sendTransferPacket(int i, int j, ItemStack add)
    {
        PacketCustom packet = new PacketCustom(TranslocatorSPH.channel, 2);
        packet.writeCoord(xCoord, yCoord, zCoord);
        packet.writeByte(i << 4 | j);
        packet.writeItemStack(add);
        packet.sendToChunk(worldObj, xCoord>>4, zCoord>>4);
    }

    private int insertAmount(ItemStack stack, InventoryRange[] attached)
    {
        int insertAmount = 0;
        for(int i = 0; i < 6; i++)
        {
            ItemAttachment ia = (ItemAttachment) attachments[i];
            if(ia == null || ia.a_eject)
                continue;
            
            insertAmount+=insertAmount(stack, ia, attached[i]);
        }
        return insertAmount;
    }

    private int insertAmount(ItemStack stack, ItemAttachment ia, InventoryRange range)
    {
        int filter = filterCount(ia, stack);
        if(filter == 0)
            return 0;
        
        int fit = InventoryUtils.getInsertibleQuantity(range, stack);
        if(fit == 0)
            return 0;

        if(ia.regulate && filter > 0)
            fit = Math.min(fit, filter-countMatchingStacks(range, stack, true));
        
        return fit > 0 ? fit : 0;
    }

    private int extractAmount(ItemStack stack, ItemAttachment ia, InventoryRange range)
    {
        int filter = filterCount(ia, stack);
        if(filter == 0)
            return ia.regulate ? stack.getMaxStackSize() : 0;
        
        int qty = filter < 0 ? stack.getMaxStackSize() : filter;
        
        if(ia.regulate && filter > 0)
            qty = Math.min(qty, countMatchingStacks(range, stack, false)-filter);
        
        return qty > 0 ? qty : 0;
    }
    
    @Override
    public void handleDescriptionPacket(PacketCustom packet)
    {
        if(packet.getType() == 2)
            movingItems.add(new MovingItem(packet));
        else
            super.handleDescriptionPacket(packet);
    }
    
    @Override
    public int strongPowerLevel(int side)
    {
        ItemAttachment ia = (ItemAttachment)attachments[side^1];
        if(ia != null && ia.a_powering)
            return 15;
        return 0;
    }
}
