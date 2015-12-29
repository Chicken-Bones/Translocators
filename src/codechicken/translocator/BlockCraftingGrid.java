package codechicken.translocator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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
import net.minecraftforge.common.util.ForgeDirection;

import static codechicken.lib.vec.Vector3.*;
import static codechicken.translocator.Translocator.disableCraftingGridKey;

public class BlockCraftingGrid extends Block
{
    private RayTracer rayTracer = new RayTracer();

    @SideOnly(Side.CLIENT)
    public IIcon gridIcon;

    public BlockCraftingGrid() {
        super(Material.wood);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return meta == 0;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileCraftingGrid();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
        super.harvestBlock(world, player, x, y, z, meta);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> ai = new ArrayList<ItemStack>();
        if (world.isRemote)
            return ai;

        TileCraftingGrid tcraft = (TileCraftingGrid) world.getTileEntity(x, y, z);
        if (tcraft != null)
            for (ItemStack item : tcraft.items)
                if (item != null)
                    ai.add(item.copy());

        return ai;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent event) {
        if (event.target.typeOfHit == MovingObjectType.BLOCK && event.player.worldObj.getBlock(event.target.blockX, event.target.blockY, event.target.blockZ) == this)
            RayTracer.retraceBlock(event.player.worldObj, event.player, event.target.blockX, event.target.blockY, event.target.blockZ);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        return rayTracer.rayTraceCuboids(new Vector3(start), new Vector3(end), getParts(world, x, y, z), new BlockCoord(x, y, z), this);
    }

    public List<IndexedCuboid6> getParts(World world, int x, int y, int z) {
        LinkedList<IndexedCuboid6> parts = new LinkedList<IndexedCuboid6>();
        parts.add(new IndexedCuboid6(0,
                new Cuboid6(0, 0, 0, 1, 0.005, 1)
                        .add(new Vector3(x, y, z))
        ));

        TileCraftingGrid tcraft = (TileCraftingGrid) world.getTileEntity(x, y, z);

        for (int i = 0; i < 9; i++) {
            Cuboid6 box = new Cuboid6(1 / 16D, 0, 1 / 16D, 5 / 16D, 0.01, 5 / 16D)
                    .apply(new Translation((i % 3) * 5 / 16D, 0, (i / 3) * 5 / 16D)
                            .with(Rotation.quarterRotations[tcraft.rotation].at(center))
                            .with(new Translation(x, y, z)));

            parts.add(new IndexedCuboid6(i + 1, box));
        }
        return parts;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote)
            return true;

        MovingObjectPosition hit = RayTracer.retraceBlock(world, player, x, y, z);
        TileCraftingGrid tcraft = (TileCraftingGrid) world.getTileEntity(x, y, z);

        if (hit != null) {
            if (hit.subHit > 0)
                tcraft.activate(hit.subHit - 1, player);
            return true;
        }
        return false;
    }

    public boolean placeBlock(World world, EntityPlayer player, int x, int y, int z, int side) {
        if(disableCraftingGridKey)
            return false;

        Block block = world.getBlock(x, y, z);
        if(side != 1 && block != Blocks.snow_layer)
            return false;

        if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush
                && !block.isReplaceable(world, x, y, z))
            y++;

        if (!world.isSideSolid(x, y-1, z, ForgeDirection.UP))
            return false;

        if (!world.canPlaceEntityOnSide(this, x, y, z, false, 1, null, null))
            return false;

        player.swingItem();
        if(!world.setBlock(x, y, z, this))
            return false;

        onBlockPlacedBy(world, x, y, z, player, null);
        return true;
    }

    ThreadLocal<BlockCoord> replaceCheck = new ThreadLocal<BlockCoord>();

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        replaceCheck.set(new BlockCoord(x, y, z));
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        BlockCoord beneath = new BlockCoord(x, y, z).offset(0);
        if (!world.isSideSolid(beneath.x, beneath.y, beneath.z, ForgeDirection.UP)) {
            dropBlockAsItem(world, x, y, z, 0, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, int i, int j, int k, EntityLivingBase entity, ItemStack item) {
        ((TileCraftingGrid) world.getTileEntity(i, j, k)).onPlaced(entity);
    }

    @Override
    public void onBlockPreDestroy(World world, int x, int y, int z, int meta) {
        BlockCoord c = replaceCheck.get();
        if (!world.isRemote && c != null && c.equals(new BlockCoord(x, y, z)))
            dropBlockAsItem(world, x, y, z, meta, 0);
        replaceCheck.set(null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        gridIcon = register.registerIcon("translocator:craftingGrid");
    }
}
