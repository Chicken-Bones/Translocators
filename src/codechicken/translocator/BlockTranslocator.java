package codechicken.translocator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import codechicken.lib.math.MathHelper;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.translocator.TileTranslocator.Attachment;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fluids.IFluidHandler;

import static codechicken.lib.vec.Rotation.*;

public class BlockTranslocator extends Block
{
    private RayTracer rayTracer = new RayTracer();

    public BlockTranslocator() {
        super(Material.iron);
        setHardness(1.5F);
        setResistance(10.0F);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return metadata < 2;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        switch (metadata) {
            case 0:
                return new TileItemTranslocator();
            case 1:
                return new TileLiquidTranslocator();
        }

        return null;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    public static boolean canExistOnSide(World world, int x, int y, int z, int side, int meta) {
        BlockCoord pos = new BlockCoord(x, y, z).offset(side);
        switch (meta) {
            case 0:
                return world.getTileEntity(pos.x, pos.y, pos.z) instanceof IInventory;
            case 1:
                return world.getTileEntity(pos.x, pos.y, pos.z) instanceof IFluidHandler;
        }
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        for (int i = 0; i < 6; i++)
            if (ttrans.attachments[i] != null && !canExistOnSide(world, x, y, z, i, meta))
                if (ttrans.harvestPart(i, true))
                    break;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willharvest) {
        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        if (hit == null)
            return false;

        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        return ttrans.harvestPart(hit.subHit % 6, !player.capabilities.isCreativeMode);
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return 0;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int md, int fortune) {
        ArrayList<ItemStack> ai = new ArrayList<ItemStack>();
        if (world.isRemote)
            return ai;

        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        if (ttrans != null)
            for (Attachment a : ttrans.attachments)
                if (a != null)
                    ai.addAll(a.getDrops());

        return ai;
    }

    @Override
    public IIcon getIcon(int par1, int par2) {
        return Blocks.obsidian.getIcon(par1, par2);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        return rayTracer.rayTraceCuboids(new Vector3(start), new Vector3(end), getParts(world, x, y, z), new BlockCoord(x, y, z), this);
    }

    public List<IndexedCuboid6> getParts(World world, int x, int y, int z) {
        TileTranslocator tile = (TileTranslocator) world.getTileEntity(x, y, z);
        if (tile == null)
            return null;

        List<IndexedCuboid6> cuboids = new LinkedList<IndexedCuboid6>();
        tile.addTraceableCuboids(cuboids);
        return cuboids;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB ebb, List list, Entity entity) {
        List<IndexedCuboid6> cuboids = getParts(world, x, y, z);
        for (IndexedCuboid6 cb : cuboids) {
            AxisAlignedBB aabb = cb.toAABB();
            if (aabb.intersectsWith(ebb))
                list.add(aabb);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote)
            return true;

        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);

        if (hit != null) {
            if (hit.subHit < 6) {
                Vector3 vhit = new Vector3(hit.hitVec);
                vhit.add(-x - 0.5, -y - 0.5, -z - 0.5);
                vhit.apply(sideRotations[hit.subHit % 6].inverse());
                if (MathHelper.between(-2 / 16D, vhit.x, 2 / 16D) && MathHelper.between(-2 / 16D, vhit.z, 2 / 16D))
                    hit.subHit += 6;
            }

            return ttrans.attachments[hit.subHit % 6].activate(player, hit.subHit / 6);
        }

        return false;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.target.typeOfHit == MovingObjectType.BLOCK && event.player.worldObj.getBlock(event.target.blockX, event.target.blockY, event.target.blockZ) == this)
            RayTracer.retraceBlock(event.player.worldObj, event.player, event.target.blockX, event.target.blockY, event.target.blockZ);
    }

    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List list) {
        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 1));
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        return ttrans.connectRedstone();
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        return ttrans.strongPowerLevel(side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister) {
    }
}
