package codechicken.translocator;

import codechicken.lib.colour.Colour;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;

public class RenderParticle
{
    public static void render(double x, double y, double z, Colour colour, double s, double u1, double v1, double u2, double v2)
    {
        x-=EntityFX.interpPosX;
        y-=EntityFX.interpPosY;
        z-=EntityFX.interpPosZ;
        //TODO: check
        
        float par3 = ActiveRenderInfo.rotationX;
        float par4 = ActiveRenderInfo.rotationXZ;
        float par5 = ActiveRenderInfo.rotationZ;
        float par6 = ActiveRenderInfo.rotationYZ;
        float par7 = ActiveRenderInfo.rotationXY;
        
        Tessellator t = Tessellator.instance;
        t.setColorRGBA(colour.r&0xFF, colour.g&0xFF, colour.b&0xFF, colour.a&0xFF);
        t.addVertexWithUV((x - par3 * s - par6 * s), (y - par4 * s), (z - par5 * s - par7 * s), u2, v2);
        t.addVertexWithUV((x - par3 * s + par6 * s), (y + par4 * s), (z - par5 * s + par7 * s), u2, v1);
        t.addVertexWithUV((x + par3 * s + par6 * s), (y + par4 * s), (z + par5 * s + par7 * s), u1, v1);
        t.addVertexWithUV((x + par3 * s - par6 * s), (y - par4 * s), (z + par5 * s - par7 * s), u1, v2);
    }
}
