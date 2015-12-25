package codechicken.translocator;

import codechicken.nei.api.API;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.lib.packet.PacketCustom;
import cpw.mods.fml.common.registry.GameRegistry;

import static codechicken.translocator.Translocator.*;

public class TranslocatorProxy
{
    public void init() {
        blockTranslocator = new BlockTranslocator();
        blockTranslocator.setBlockName("translocator").setCreativeTab(CreativeTabs.tabRedstone);
        blockCraftingGrid = new BlockCraftingGrid();
        blockCraftingGrid.setBlockName("craftingGrid");
        blockCraftingGrid.setBlockTextureName("planks_oak");

        itemDiamondNugget = new Item()
                .setUnlocalizedName("translocator:diamondNugget").setTextureName("translocator:diamondNugget")
                .setCreativeTab(CreativeTabs.tabMaterials);
        GameRegistry.registerItem(itemDiamondNugget, "diamondNugget");
        OreDictionary.registerOre("nuggetDiamond", itemDiamondNugget);

        GameRegistry.registerBlock(blockTranslocator, ItemTranslocator.class, "translocator");
        GameRegistry.registerBlock(blockCraftingGrid, "craftingGrid");
        MinecraftForge.EVENT_BUS.register(blockTranslocator);
        MinecraftForge.EVENT_BUS.register(blockCraftingGrid);

        GameRegistry.registerTileEntity(TileItemTranslocator.class, "itemTranslocator");
        GameRegistry.registerTileEntity(TileLiquidTranslocator.class, "liquidTranslocator");
        GameRegistry.registerTileEntity(TileCraftingGrid.class, "craftingGrid");

        PacketCustom.assignHandler(TranslocatorSPH.channel, new TranslocatorSPH());

        GameRegistry.addRecipe(new ItemStack(blockTranslocator, 2, 0),
                "rer",
                "ipi",
                "rgr",
                'r', Items.redstone,
                'e', Items.ender_pearl,
                'i', Items.iron_ingot,
                'g', Items.gold_ingot,
                'p', Blocks.piston);
        GameRegistry.addRecipe(new ItemStack(blockTranslocator, 2, 1),
                "rer",
                "ipi",
                "rlr",
                'r', Items.redstone,
                'e', Items.ender_pearl,
                'i', Items.iron_ingot,
                'l', new ItemStack(Items.dye, 1, 4),
                'p', Blocks.piston);
        API.hideItem(new ItemStack(blockCraftingGrid));

        GameRegistry.addShapelessRecipe(new ItemStack(Items.diamond),
                itemDiamondNugget, itemDiamondNugget, itemDiamondNugget,
                itemDiamondNugget, itemDiamondNugget, itemDiamondNugget,
                itemDiamondNugget, itemDiamondNugget, itemDiamondNugget);

        GameRegistry.addShapelessRecipe(new ItemStack(itemDiamondNugget, 9), Items.diamond);
    }
}
