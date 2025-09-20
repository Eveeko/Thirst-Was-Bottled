package net.flatdev.thirstwasbottled.foundation.common;

import com.mojang.logging.LogUtils;
import dev.ghen.thirst.api.ThirstHelper;
import net.flatdev.thirstwasbottled.item.ModCreativeModeTabs;
import net.flatdev.thirstwasbottled.item.Moditems;
import net.flatdev.thirstwasbottled.item.PaperCupItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ThirstWasBottled.MOD_ID)
public class ThirstWasBottled
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "thirstwasbottled";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ThirstWasBottled(FMLJavaModLoadingContext context)
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
        ThirstHelper.VALID_DRINKS.put(Moditems.PAPERCUP.get(), new Number[]{((PaperCupItem)Moditems.PAPERCUP.get()).getHydration(), ((PaperCupItem)Moditems.PAPERCUP.get()).getQuench()});
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
                    new ResourceLocation(ThirstWasBottled.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> stack.hasTag() && stack.getTag().getBoolean("Filled") ? 1.0F : 0.0F
            );
            // Fluid type (new)
            ItemProperties.register(Moditems.PAPERCUP.get(),
                    new ResourceLocation(ThirstWasBottled.MOD_ID, "fluid"),
                    (stack, level, entity, seed) -> {
                        if (!stack.hasTag() || !stack.getTag().contains("Fluid")) return 0f;
                        String fluidName = stack.getTag().getString("Fluid");
                        if (fluidName.equals("minecraft:water")) return 1f;
                        if (fluidName.equals("minecraft:lava")) return 2f;
                        return 0f;
                    });
        }
        @SubscribeEvent
        public static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
            event.register(
                    (stack, tintIndex) -> {
                        // tintIndex references the layer number in the item.json file. this whole event gets called once per layer as its getting drawn.
                        if (tintIndex == 1) {
                            CompoundTag tag = stack.getTag();
                            if(tag != null && tag.contains("Purity")){
                                int purity = tag.getInt("Purity");
                                return switch (purity) {
                                    case 1 -> 0x5c809b;
                                    case 2 -> 0x335ed7;
                                    case 3 -> 0x21affc;
                                    default -> 0x917a3b;
                                };
                            }
                            return 0xFFFFFF; // no tint for if the cup is empty as purity will always be present in a filled cup.
                        }
                        return 0xFFFFFF; // no tint (normal colors) for other layers
                    },
                    Moditems.PAPERCUP.get()
            );
        }
    }
}
