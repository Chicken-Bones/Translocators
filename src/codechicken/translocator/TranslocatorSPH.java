package codechicken.translocator;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;

public class TranslocatorSPH implements IServerPacketHandler
{
    public static Object channel = Translocator.instance;

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        switch (packet.getType()) {
            case 1:
                Translocator.blockCraftingGrid.placeBlock(sender.worldObj, sender, packet.readInt(), packet.readInt(), packet.readInt(), packet.readUByte());
                break;
            case 2:
                TileEntity tile = sender.worldObj.getTileEntity(packet.readInt(), packet.readInt(), packet.readInt());
                if (tile instanceof TileCraftingGrid)
                    ((TileCraftingGrid) tile).craft(sender);
                break;
        }
    }
}
