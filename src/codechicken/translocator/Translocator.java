package codechicken.translocator;

import java.io.File;

import net.minecraft.item.Item;

import codechicken.core.CommonUtils;
import codechicken.core.launch.CodeChickenCorePlugin;
import codechicken.lib.config.ConfigFile;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "Translocator",
        dependencies = "required-after:CodeChickenCore@[" + CodeChickenCorePlugin.version + ",);required-after:NotEnoughItems",
        acceptedMinecraftVersions = CodeChickenCorePlugin.mcVersion)
public class Translocator
{
    @SidedProxy(clientSide = "codechicken.translocator.TranslocatorClientProxy", serverSide = "codechicken.translocator.TranslocatorProxy")
    public static TranslocatorProxy proxy;

    @Instance(value = "Translocator")
    public static Translocator instance;

    public static ConfigFile config;

    public static BlockTranslocator blockTranslocator;
    public static BlockCraftingGrid blockCraftingGrid;
    public static Item itemDiamondNugget;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new ConfigFile(new File(CommonUtils.getMinecraftDir() + "/config", "Translocator.cfg")).setComment("Translocator Configuration File\nDeleting any element will restore it to it's default value\nBlock ID's will be automatically generated the first time it's run");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
