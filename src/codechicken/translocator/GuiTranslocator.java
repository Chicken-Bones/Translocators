package codechicken.translocator;

import codechicken.nei.guihook.IGuiSlotDraw;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import codechicken.lib.inventory.InventoryUtils;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.FontUtils;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GuiTranslocator extends GuiContainer implements IGuiSlotDraw
{
    public GuiTranslocator(Container par1Container) {
        super(par1Container);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glPushMatrix();
        GL11.glTranslated(guiLeft, guiTop, 0);
        GL11.glColor4f(1, 1, 1, 1);

        CCRenderState.changeTexture("textures/gui/container/dispenser.png");
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize);

        fontRendererObj.drawString(StatCollector.translateToLocal(((ContainerItemTranslocator) inventorySlots).getInvName()), 6, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 6, 72, 0x404040);
        GL11.glPopMatrix();
    }

    @Override
    public void drawSlotItem(Slot par1Slot, ItemStack itemstack, int i, int j, String s) {
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.renderEngine, itemstack, i, j);
        FontUtils.drawItemQuantity(i, j, itemstack, null, 0);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.renderEngine, InventoryUtils.copyStack(itemstack, 1), i, j, null);
    }
}
