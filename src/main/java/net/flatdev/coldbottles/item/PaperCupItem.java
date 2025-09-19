package net.flatdev.coldbottles.item;

import dev.ghen.thirst.Thirst;
import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.content.thirst.PlayerThirstManager;
import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.event.ThirstEventFactory;
import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import dev.ghen.thirst.foundation.gui.appleskin.ThirstValues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class PaperCupItem extends Item{
    public PaperCupItem(Properties properties){
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hit.getType() == HitResult.Type.BLOCK && stack.getTag() == null) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            FluidStack fluidStack = FluidStack.EMPTY;

            if (state.getBlock() == Blocks.WATER && state.getFluidState().isSource()) {
                if (!level.isClientSide){
                    FluidState fluidState = state.getFluidState();
                    FluidType type = fluidState.getFluidType(); // FluidType, not Fluid
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(ForgeRegistries.FLUID_TYPES.get().getKey(type));
                    fluidStack = new FluidStack(fluid, 100); // 100 mB
                    // Pick purity based on source
                    int purity = 0; // default clean
                    purity = WaterPurity.getBlockPurity(level, pos);
                    if(purity == -1){ purity = 0; };
                    // Create a new filled cup
                    ItemStack filledCup = stack.copy();
                    filledCup.setCount(1);
                    WaterPurity.addPurity(filledCup, purity);
                    filledCup.getOrCreateTag().putBoolean("Filled", true);
                    filledCup.getOrCreateTag().putString("Fluid",
                            ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid()).toString());
                    filledCup.getOrCreateTag().putInt("Amount", 100);
                    // Grab fluid purity like the bottle does

                    //filledCup.getOrCreateTag().putInt("Purity", purity);

                    // Reduce the original stack size
                    stack.shrink(1);

                    // Try to put the filled cup into player’s inventory
                    if (!player.getInventory().add(filledCup)) {
                        // If inventory full, drop it in the world
                        player.drop(filledCup, false);
                    }
                    // Play sound
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }else {
            return ItemUtils.startUsingInstantly(level, player, hand); // makes it hold and play animation
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if(stack.getTag() != null) {
            if (stack.getTag().getBoolean("Filled")) {
                return UseAnim.DRINK; // Makes the player use the drinking animation
            }
        }
        return UseAnim.NONE;
    }
    @Override
    public int getUseDuration(ItemStack stack) {
        return 32; // Default duration (like drinking a potion)
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Play drinking sound
            if (!player.isCreative() && !player.isSpectator()) {
                System.out.println("Player thirst " + ThirstBarRenderer.PLAYER_THIRST.getThirst());
                Integer thirst = ThirstBarRenderer.PLAYER_THIRST.getThirst();
                if (thirst < 20) {
                    System.out.println("Player is able to drink!");
                    ItemStack cup = stack.copy();
                    cup.setCount(1);
                    if (cup.getTag().getBoolean("Filled") && cup.getTag().getString("Fluid").equals("minecraft:water")) {
                        stack.shrink(1);
                        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                        PlayerThirst.drink(cup, player);

                        return (stack);
                    }
                }
            }
        }

        // Return the empty cup
        return new ItemStack(Moditems.PAPERCUP.get());
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("Purity")) {
            int purity = stack.getTag().getInt("Purity");

            // Format like Thirst Was Taken’s bottles
            MutableComponent purityText = Component.literal(WaterPurity.getPurityText(purity))
                    .setStyle(Style.EMPTY.withColor(WaterPurity.getPurityColor(purity)));
            tooltip.add(purityText);
        }
    }
}
