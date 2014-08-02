package codechicken.translocator;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.tileentity.TileEntity;
import codechicken.core.ClientUtils;
import codechicken.lib.inventory.InventorySimple;
import codechicken.lib.packet.ICustomPacketTile;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;

public class TranslocatorCPH implements IClientPacketHandler
{
    public static Object channel = Translocator.instance;

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        switch (packet.getType()) {
            case 1:
            case 2:
            case 3:
                TileEntity tile = mc.theWorld.getTileEntity(packet.readInt(), packet.readInt(), packet.readInt());
                if (tile instanceof ICustomPacketTile)
                    ((ICustomPacketTile) tile).handleDescriptionPacket(packet);
                break;
            case 4:
                int windowId = packet.readUByte();
                GuiTranslocator gui = new GuiTranslocator(new ContainerItemTranslocator(
                        new InventorySimple(9, packet.readUShort(), packet.readString()), mc.thePlayer.inventory));
                ClientUtils.openSMPGui(windowId, gui);
                break;
            case 5:
                mc.thePlayer.openContainer.putStackInSlot(packet.readUByte(), packet.readItemStack(true));
                break;
        }
    }
}
