package net.flatdev.thirstwasbottled.item;

import net.flatdev.thirstwasbottled.ThirstWasBottled;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Moditems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThirstWasBottled.MOD_ID);

    public static final RegistryObject<Item> PAPERCUP = ITEMS.register("papercup",() -> new PaperCupItem(new Item.Properties()));
    public static final RegistryObject<Item> CLUPCUP = ITEMS.register("clupcup", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }


}
