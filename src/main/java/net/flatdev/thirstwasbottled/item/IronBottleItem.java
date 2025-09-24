package net.flatdev.thirstwasbottled.item;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import net.flatdev.thirstwasbottled.foundation.common.item.DrinkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public class IronBottleItem extends DrinkItem {
    public static final int CAPACITY = 1000; // mB (like 1 bucket)
    public static final int SIP_SIZE = 335;  // mB per use
    public IronBottleItem(Item.Properties properties) {
        super(properties.stacksTo(1));
        this.setHydration(6); // 3 drops
        this.setQuench(4); // 2 drops
    }
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY); // CAPACITY = how many mb your item can hold
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hit.getType() == HitResult.Type.BLOCK && stack.getTag() == null) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            FluidStack fluidStack = FluidStack.EMPTY;
            FluidState fluidState = state.getFluidState();
            AtomicReference<Boolean> isWater = new AtomicReference<>(false);
            if(blockEntity != null) {
                blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(fluidHandler -> {
                    FluidStack fluidInBlock = fluidHandler.getFluidInTank(0);
                    if (!fluidInBlock.isEmpty() && fluidInBlock.getFluid().isSame(Fluids.WATER)) {
                        isWater.set(true);
                    }
                });
            }else{
                if(fluidState.is(Fluids.WATER)){
                    isWater.set(true);
                }
            }
            if (isWater.get()) {
                if (!level.isClientSide){
                    FluidType type = fluidState.getFluidType(); // FluidType, not Fluid
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(ForgeRegistries.FLUID_TYPES.get().getKey(type));
                    fluidStack = new FluidStack(fluid, 100); // 100 mB
                    // Pick purity based on source
                    int purity = 0; // default clean
                    purity = WaterPurity.getBlockPurity(level, pos);
                    if(purity == -1){ purity = 0; };
                    // Create a new filled cup
                    WaterPurity.addPurity(stack, purity);
                    stack.getOrCreateTag().putBoolean("Filled", true);
                    stack.getOrCreateTag().putString("Fluid",
                            ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid()).toString());

                    // Grab fluid purity like the bottle does

                    //filledCup.getOrCreateTag().putInt("Purity", purity);

                    // Reduce the original stack size
                    stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
                        int filled = handler.fill(new FluidStack(Fluids.WATER, CAPACITY), IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            player.displayClientMessage(Component.literal("Filled with " + filled + "mb water"), true);
                        }
                    });
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }else {
            if (!player.isCreative() && !player.isSpectator()) {
                int thirst = ThirstBarRenderer.PLAYER_THIRST.getThirst();
                    if(thirst < 20){
                        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
                            ItemUtils.startUsingInstantly(level, player, hand); // makes it hold and play animation
                        });
                    } else {
                        return InteractionResultHolder.pass(stack);
                    }
            };
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Play drinking sound
            if (!player.isCreative() && !player.isSpectator()) {
                System.out.println("Player thirst " + ThirstBarRenderer.PLAYER_THIRST.getThirst());
                int thirst = ThirstBarRenderer.PLAYER_THIRST.getThirst();
                if (thirst < 20) {
                    if (stack.getTag().getBoolean("Filled")) {
                        int purity = WaterPurity.getPurity(stack);
                        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                        player.getCapability(ModCapabilities.PLAYER_THIRST, (Direction)null).ifPresent((cap) -> {
                            if (WaterPurity.givePurityEffects(player, stack)) {
                                cap.drink(player, this.getHydration(), this.getQuench());
                            }

                        });
                        WaterPurity.givePurityEffects(player, purity);
                        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
                            System.out.println("is valid fluid item.");
                            FluidStack containedFluid = handler.getFluidInTank(0);
                            if (!containedFluid.isEmpty()) {
                                FluidStack drained = handler.drain(SIP_SIZE, IFluidHandler.FluidAction.EXECUTE);
                                if (!drained.isEmpty()) {
                                    player.displayClientMessage(Component.literal("You drank " + drained.getAmount() + "mb water!"), true);
                                }
                                // If tank is empty, wipe custom NBT
                                if (handler.getFluidInTank(0).isEmpty()) {
                                    CompoundTag tag = stack.getTag();
                                    if (tag != null) {
                                        tag.remove("Filled");
                                        tag.remove("Fluid");
                                        tag.remove("Purity"); // if you store this
                                        if (tag.isEmpty()) {
                                            stack.setTag(null); // completely clears NBT if nothing left
                                        }
                                    }
                                }
                            }
                        });
                        return (stack);
                    }
                }
            }
        }
        // Return the empty cup
        return (stack);
    }
    @Override
    public boolean isBarVisible(ItemStack stack) {
        // Show the bar only if it contains fluid
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler.getFluidInTank(0).getAmount() > 0)
                .orElse(false);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> {
                    int amount = handler.getFluidInTank(0).getAmount();
                    int capacity = handler.getTankCapacity(0);
                    return Math.round((float) amount / capacity * 13.0F); // bar is 13px wide
                })
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Blue color (0x0000FF). You can change this to any RGB.
        return 0x0000FF;
    }
}
