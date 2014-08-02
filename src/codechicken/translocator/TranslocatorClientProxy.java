package codechicken.translocator;

import codechicken.core.GuiModListScroll;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import codechicken.core.CCUpdateChecker;
import codechicken.lib.packet.PacketCustom;
import cpw.mods.fml.client.registry.ClientRegistry;

import static codechicken.translocator.Translocator.*;

public class TranslocatorClientProxy extends TranslocatorProxy
{
    public void init()
    {
        if(config.getTag("checkUpdates").getBooleanValue(true))
            CCUpdateChecker.updateCheck("Translocator");
        
        super.init();

        GuiModListScroll.register("Translocator");

        ClientRegistry.bindTileEntitySpecialRenderer(TileItemTranslocator.class, new TileTranslocatorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileLiquidTranslocator.class, new TileTranslocatorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileCraftingGrid.class, new TileCraftingGridRenderer());
        
        PacketCustom.assignHandler(TranslocatorCPH.channel, new TranslocatorCPH());
        
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(blockTranslocator), new ItemTranslocatorRenderer());

        FMLCommonHandler.instance().bus().register(new CraftingGridKeyHandler());
        ClientRegistry.registerKeyBinding(new CraftingGridKeyHandler());
    }
}
