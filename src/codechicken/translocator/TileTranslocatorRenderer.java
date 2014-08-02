package codechicken.translocator;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import codechicken.core.ClientUtils;
import codechicken.lib.math.MathHelper;
import codechicken.lib.colour.CustomGradient;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.SwapYZ;
import codechicken.lib.vec.Vector3;
import codechicken.translocator.TileItemTranslocator.MovingItem;
import codechicken.translocator.TileLiquidTranslocator.MovingLiquid;
import codechicken.translocator.TileTranslocator.Attachment;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import static codechicken.lib.vec.Rotation.*;

public class TileTranslocatorRenderer extends TileEntitySpecialRenderer
{
    public static Vector3[] sidePos = new Vector3[]{
        new Vector3(0.5, 0, 0.5),
        new Vector3(0.5, 1, 0.5),
        new Vector3(0.5, 0.5, 0),
        new Vector3(0.5, 0.5, 1),
        new Vector3(0, 0.5, 0.5),
        new Vector3(1, 0.5, 0.5)};
    public static Vector3[] sideVec = new Vector3[]{
        new Vector3( 0,-1, 0),
        new Vector3( 0, 1, 0),
        new Vector3( 0, 0,-1),
        new Vector3( 0, 0, 1),
        new Vector3(-1, 0, 0),
        new Vector3( 1, 0, 0)};
    
    public static CCModel[] plates = new CCModel[6];
    public static CCModel insert;
    
    static
    {
        Map<String, CCModel> models = CCModel.parseObjModels(new ResourceLocation("translocator", "models/model.obj"), new SwapYZ());
        plates[0] = models.get("Plate");
        insert = models.get("Insert");
        CCModel.generateSidedModels(plates, 0, new Vector3());
    }
    
    private CustomGradient gradient = new CustomGradient(new ResourceLocation("translocator", "textures/grad.png"));
    
    public TileTranslocatorRenderer()
    {
    }
    
    @Override
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float f)
    {
        TileTranslocator ttrans = (TileTranslocator)tileentity;
        double time = ClientUtils.getRenderTime();
        
        CCRenderState.reset();
        CCRenderState.changeTexture("translocator:textures/tex.png");
        CCRenderState.pullLightmap();
        CCRenderState.useNormals = true;
        CCRenderState.setColour(-1);
        CCRenderState.startDrawing(4);
        
        for(int i = 0; i < 6; i++)
        {
            Attachment a = ttrans.attachments[i];
            if(a != null)
                renderAttachment(i, ttrans.getBlockMetadata(), 
                        MathHelper.interpolate(a.b_insertpos, a.a_insertpos, f), 
                        a.getIconIndex(), 
                        x, y, z);
        }
        CCRenderState.draw();
        
        if(ttrans instanceof TileItemTranslocator)
        {
            TileItemTranslocator titrans = (TileItemTranslocator)ttrans;
            for(MovingItem m : titrans.movingItems)
            {
                GL11.glPushMatrix();
                double d = MathHelper.interpolate(m.b_progress, m.a_progress, f);
                    Vector3 pos = getPath(m.src, m.dst, d)
                            .add(itemFloat(m.src, m.dst, d));
                    GL11.glTranslated(x+pos.x, y+pos.y-0.06, z+pos.z);
                    GL11.glScaled(0.35, 0.35, 0.35);
                    
                    RenderUtils.renderItemUniform(m.stack);
                GL11.glPopMatrix();
            }
        }
        if(ttrans instanceof TileLiquidTranslocator)
        {
            TileLiquidTranslocator tltrans = (TileLiquidTranslocator)ttrans;
            for(MovingLiquid m : tltrans.movingLiquids())
            {
                double start = MathHelper.interpolate(m.b_start, m.a_start, f);
                double end = MathHelper.interpolate(m.b_end, m.a_end, f);
                
                drawLiquidSpiral(m.src, m.dst, m.liquid, start, end, time, 0, x, y, z);
                if(m.fast)
                    drawLiquidSpiral(m.src, m.dst, m.liquid, start, end, time, 0.5, x, y, z);
            }
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        CCRenderState.changeTexture("translocator:textures/particle.png");
        CCRenderState.startDrawing(7);
        for(int src = 0; src < 6; src++)
        {
            Attachment asrc = ttrans.attachments[src];
            if(asrc == null || !asrc.a_eject)
                continue;
            
            for(int dst = 0; dst < 6; dst++)
            {
                Attachment adst = ttrans.attachments[dst];
                if(adst != null && !adst.a_eject)
                    renderLink(src, dst, time, ttrans.xCoord, ttrans.yCoord, ttrans.zCoord);
            }
        }
        CCRenderState.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }
    
    private void drawLiquidSpiral(int src, int dst, FluidStack stack, double start, double end, double time, double theta0, double x, double y, double z)
    {
        IIcon tex = RenderUtils.prepareFluidRender(stack, 255);
        
        CCRenderState.startDrawing(7);
        Tessellator t = Tessellator.instance;
        t.setTranslation(x, y, z);
        
        Vector3[] last = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3()};
        Vector3[] next = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3()};
        double tess = 0.05;

        Vector3 a = getPerp(src, dst);
        boolean rev = sum(a.copy().crossProduct(getPathNormal(src, dst, 0))) != sum(sideVec[src]);
        
        for(double di = end; di <= start; di+=tess)
        {
            Vector3 b = getPathNormal(src, dst, di);
            Vector3 c = getPath(src, dst, di);
            
            if(rev)
                b.negate();
            
            double r = (2*di-time/10+theta0+dst/6)*2*Math.PI;
            double sz = 0.1;
            Vector3 p = c.add(a.copy().multiply(MathHelper.sin(r)*sz)).add(b.copy().multiply(MathHelper.cos(r)*sz));

            double s1 = 0.02;
            double s2 =-0.02;
            next[0].set(p).add(a.x*s1+b.x*s1, a.y*s1+b.y*s1, a.z*s1+b.z*s1);
            next[1].set(p).add(a.x*s2+b.x*s1, a.y*s2+b.y*s1, a.z*s2+b.z*s1);
            next[2].set(p).add(a.x*s2+b.x*s2, a.y*s2+b.y*s2, a.z*s2+b.z*s2);
            next[3].set(p).add(a.x*s1+b.x*s2, a.y*s1+b.y*s2, a.z*s1+b.z*s2);
            
            if(di > end)
            {
                double u1 = tex.getInterpolatedU(Math.abs(di)*16);
                double u2 = tex.getInterpolatedU(Math.abs(di-tess)*16);
                for(int i = 0; i < 4; i++)
                {
                    int j = (i+1)%4;
                    Vector3 axis = next[j].copy().subtract(next[i]);
                    double v1 = tex.getInterpolatedV(Math.abs(next[i].scalarProject(axis))*16);
                    double v2 = tex.getInterpolatedV(Math.abs(next[j].scalarProject(axis))*16);
                    t.addVertexWithUV(next[i].x, next[i].y, next[i].z, u1, v1);
                    t.addVertexWithUV(next[j].x, next[j].y, next[j].z, u1, v2);
                    t.addVertexWithUV(last[j].x, last[j].y, last[j].z, u2, v2);
                    t.addVertexWithUV(last[i].x, last[i].y, last[i].z, u2, v1);
                }
            }
            
            Vector3[] tmp = last;
            last = next;
            next = tmp;
        }
        
        CCRenderState.draw();
        t.setTranslation(0, 0, 0);
        
        RenderUtils.postFluidRender();
    }

    private double sum(Vector3 v)
    {
        return v.x+v.y+v.z;
    }

    private void renderLink(int src, int dst, double time, int x, int y, int z)
    {
        double d = ((time+src+dst*2)%10)/6;
        //0 is head
        for(int n = 0; n < 20; n++)
        {
            double dn = d-n*0.1;
            int spriteX = (int) (7-n*1.5-d*2);
            if(!MathHelper.between(0, dn, 1) || spriteX < 0)
                continue;
            
            Vector3 pos = getPath(src, dst, dn).add(x, y, z);
            double b = 1;//d*0.6+0.4;
            double s = 1;//d*0.6+0.4;
            
            double u1 = spriteX/8D;
            double u2 = u1+1/8D;
            double v1 = 0;
            double v2 = 1;
            
            RenderParticle.render(pos.x, pos.y, pos.z, gradient.getColour((dn-0.5)*1.2+0.5).multiplyC(b), s*0.12, u1, v1, u2, v2);
        }
    }

    private Vector3 itemFloat(int src, int dst, double d)
    {
        return getPerp(src, dst).multiply(0.01*MathHelper.sin(d*4*Math.PI));
    }

    public static Vector3 getPath(int src, int dst, double d)
    {
        Vector3 v;
        if((src^1) == dst)//opposite
            v = sideVec[src^1].copy().multiply(d);
        else
        {
            Vector3 vsrc = sideVec[src^1];
            Vector3 vdst = sideVec[dst^1];
            Vector3 a = vsrc.copy().multiply(5/16D);
            Vector3 b = vdst.copy().multiply(6/16D);
            double sind = MathHelper.sin(d*Math.PI/2);
            double cosd = MathHelper.cos(d*Math.PI/2);
            v = a.multiply(sind).add(b.multiply(cosd-1)).add(vsrc.copy().multiply(3/16D));
        }
        return v.add(sidePos[src]);
    }
    
    public static Vector3 getPerp(int src, int dst)
    {
        if((src^1) == dst)
            return sideVec[(src+2)%6].copy();

        for(int i = 0; i < 3; i++)
            if(i != src/2 && i != dst/2)
                return sideVec[i*2].copy();
        
        return null;
    }
    
    private static Vector3 getPathNormal(int src, int dst, double d)
    {
        if((src^1) == dst)
            return sideVec[(src+4)%6].copy();
        
        double sind = MathHelper.sin(d*Math.PI/2);
        double cosd = MathHelper.cos(d*Math.PI/2);

        Vector3 vsrc = sideVec[src^1].copy();
        Vector3 vdst = sideVec[dst^1].copy();
        
        return vsrc.multiply(sind).add(vdst.multiply(cosd)).normalize();
    }

    public static void renderAttachment(int i, int type, double insertpos, int field, double x, double y, double z)
    {
        double tx = field/64D;
        double ty = type/2D;
        
        plates[i].render(x+0.5, y+0.5, z+0.5, tx, ty);
        
        Matrix4 matrix = new Matrix4()
            .translate(new Vector3(x+0.5, y+0.5, z+0.5))
            .apply(sideRotations[i])
            .translate(new Vector3(0, -0.5, 0))
            .scale(new Vector3(1, insertpos*2/3+1/3D, 1));
        
        insert.render(matrix, tx, ty);
    }
}
