package codechicken.translocator;

import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import codechicken.core.ClientUtils;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.TextureUtils;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

public class TileCraftingGridRenderer extends TileEntitySpecialRenderer
{
    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float f)
    {
        TileCraftingGrid tcraft = (TileCraftingGrid)tile;
        
        TextureUtils.bindAtlas(0);
        IIcon icon = Translocator.blockCraftingGrid.gridIcon;
        Tessellator t = Tessellator.instance;
        t.setTranslation(x, y+0.001, z);
        t.startDrawingQuads();
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(1, 0, 0, icon.getMinU(), icon.getMinV());
        t.addVertexWithUV(0, 0, 0, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(0, 0, 1, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(1, 0, 1, icon.getMaxU(), icon.getMinV());
        t.draw();
        t.setTranslation(0, 0, 0);

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glPushMatrix();
        GL11.glTranslated(x+0.5, y, z+0.5);
        Transformation orient = Rotation.quarterRotations[tcraft.rotation];

        for(int i = 0; i < 9; i++)
        {
            ItemStack item = tcraft.items[i];
            if(item == null)
                continue;
            
            int row = i/3;
            int col = i%3;

            Vector3 pos = new Vector3((col-1)*5/16D, 0.1+0.01*Math.sin(i*1.7+ClientUtils.getRenderTime()/5), (row-1)*5/16D).apply(orient);
            GL11.glPushMatrix();
            GL11.glTranslated(pos.x, pos.y, pos.z);
            GL11.glScaled(0.5, 0.5, 0.5);

            RenderUtils.renderItemUniform(item);

            GL11.glPopMatrix();
        }
        
        if(tcraft.result != null)
        {
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0.6 + 0.02 * Math.sin(ClientUtils.getRenderTime() / 10), 0);
            GL11.glScaled(0.8, 0.8, 0.8);

            RenderUtils.renderItemUniform(tcraft.result, ClientUtils.getRenderTime());
            
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }
}
