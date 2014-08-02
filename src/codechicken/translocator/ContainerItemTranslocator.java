package codechicken.translocator;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import codechicken.core.inventory.ContainerExtended;
import codechicken.lib.inventory.InventorySimple;
import codechicken.core.inventory.SlotDummy;
import codechicken.lib.packet.PacketCustom;

public class ContainerItemTranslocator extends ContainerExtended
{
    IInventory inv;

    public ContainerItemTranslocator(InventorySimple inv, InventoryPlayer playerInv) {
        this.inv = inv;

        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                this.addSlotToContainer(new SlotDummy(inv, y + x * 3, 62 + y * 18, 17 + x * 18, inv.limit));

        bindPlayerInventory(playerInv);
    }

    public String getInvName() {
        return inv.getInventoryName();
    }

    @Override
    public void sendLargeStack(ItemStack stack, int slot, List<EntityPlayerMP> players) {
        PacketCustom packet = new PacketCustom(TranslocatorSPH.channel, 5);
        packet.writeByte(slot);
        packet.writeItemStack(stack, true);

        for (EntityPlayerMP player : players)
            packet.sendToPlayer(player);
    }
}
