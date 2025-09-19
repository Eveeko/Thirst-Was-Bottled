package net.flatdev.coldbottles;

import com.mojang.logging.LogUtils;
import dev.ghen.thirst.Thirst;
import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.content.purity.ContainerWithPurity;
import dev.ghen.thirst.foundation.common.event.ThirstEventFactory;
import dev.ghen.thirst.foundation.gui.appleskin.ThirstValues;
import dev.ghen.thirst.foundation.network.ThirstModPacketHandler;
import net.flatdev.coldbottles.item.ModCreativeModeTabs;
import net.flatdev.coldbottles.item.Moditems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import static dev.ghen.thirst.content.purity.WaterPurity.addContainer;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ColdBottles.MOD_ID)
public class ColdBottles
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "coldbottles";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ColdBottles(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModeTabs.register(modEventBus);
        Moditems.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        ThirstHelper.VALID_DRINKS.put(Moditems.PAPERCUP.get(), new Number[]{1.5, 1});
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ItemProperties.register(
                    Moditems.PAPERCUP.get(),
                    new ResourceLocation(ColdBottles.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> stack.hasTag() && stack.getTag().getBoolean("Filled") ? 1.0F : 0.0F
            );
            // Fluid type (new)
            ItemProperties.register(Moditems.PAPERCUP.get(),
                    new ResourceLocation(ColdBottles.MOD_ID, "fluid"),
                    (stack, level, entity, seed) -> {
                        if (!stack.hasTag() || !stack.getTag().contains("Fluid")) return 0f;
                        String fluidName = stack.getTag().getString("Fluid");
                        if (fluidName.equals("minecraft:water")) return 1f;
                        if (fluidName.equals("minecraft:lava")) return 2f;
                        return 0f;
                    });
        }
    }
}
