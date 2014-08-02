package codechicken.translocator;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Keyboard;

import codechicken.lib.packet.PacketCustom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;

public class CraftingGridKeyHandler extends KeyBinding
{
    private boolean wasPressed = false;

    public CraftingGridKeyHandler() {
        super("key.craftingGrid", Keyboard.KEY_C, "key.categories.gameplay");
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void tick(ClientTickEvent event) {
        if(event.phase != Phase.END)
            return;

        boolean pressed = getIsKeyPressed();
        if(pressed != wasPressed) {
            wasPressed = pressed;
            if(pressed)
                onKeyPressed();
        }
    }

    private void onKeyPressed() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null)
            return;

        //place the grid
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK)
            return;

        if (mc.theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ) == Translocator.blockCraftingGrid) {
            PacketCustom packet = new PacketCustom(TranslocatorCPH.channel, 2);
            packet.writeCoord(hit.blockX, hit.blockY, hit.blockZ);
            packet.sendToServer();

            mc.thePlayer.swingItem();
        } else if (Translocator.blockCraftingGrid.placeBlock(mc.theWorld, mc.thePlayer, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit)) {
            PacketCustom packet = new PacketCustom(TranslocatorCPH.channel, 1);
            packet.writeCoord(hit.blockX, hit.blockY, hit.blockZ);
            packet.writeByte(hit.sideHit);
            packet.sendToServer();
        }
    }
}
