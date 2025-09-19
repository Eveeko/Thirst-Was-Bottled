package net.flatdev.coldbottles.item;

import net.flatdev.coldbottles.ColdBottles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColdBottles.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TUTORIAL_TAB = CREATIVE_MODE_TABS.register("coldbottles_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Moditems.PAPERCUP.get()))
            .title(Component.translatable("creativetab.coldbottles_tab"))
            .displayItems((pParameters, pOutput) -> {
                for(RegistryObject<Item> item:Moditems.ITEMS.getEntries()){
                    pOutput.accept(item.get());
                }
            }).build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
