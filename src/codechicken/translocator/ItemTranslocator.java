package codechicken.translocator;

import codechicken.lib.vec.BlockCoord;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidHandler;

public class ItemTranslocator extends ItemBlock
{
    public ItemTranslocator(Block block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        Block block = world.getBlock(x, y, z);
        if (block != field_150939_a) {
            if (!world.canPlaceEntityOnSide(field_150939_a, x, y, z, false, side, null, stack))
                return false;

            if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata))
                return false;
        }

        TileTranslocator ttrans = (TileTranslocator) world.getTileEntity(x, y, z);
        if (ttrans.attachments[side ^ 1] != null)
            return false;

        ttrans.createAttachment(side ^ 1);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        world.markBlockForUpdate(x, y, z);
        return true;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(x, y, z);

        if (block == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1) {
            side = 1;
        } else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush && !block.isReplaceable(world, x, y, z)) {
            if (side == 0)
                --y;
            if (side == 1)
                ++y;
            if (side == 2)
                --z;
            if (side == 3)
                ++z;
            if (side == 4)
                --x;
            if (side == 5)
                ++x;
        }

        if (stack.stackSize == 0 || !player.canPlayerEdit(x, y, z, side, stack))
            return false;

        if (placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, stack.getItemDamage())) {
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, field_150939_a.stepSound.func_150496_b(), (field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F, field_150939_a.stepSound.getPitch() * 0.8F);
            --stack.stackSize;
            return true;
        }

        return false;
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */
    public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        BlockCoord pos = new BlockCoord(x, y, z).offset(side);
        if (world.getBlock(pos.x, pos.y, pos.z) == field_150939_a && world.getBlockMetadata(pos.x, pos.y, pos.z) != stack.getItemDamage())
            return false;

        switch (stack.getItemDamage()) {
            case 0:
                return world.getTileEntity(x, y, z) instanceof IInventory;
            case 1:
                return world.getTileEntity(x, y, z) instanceof IFluidHandler;
        }
        return false;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "|" + stack.getItemDamage();
    }
}
