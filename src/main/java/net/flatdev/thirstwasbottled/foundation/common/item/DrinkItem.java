package net.flatdev.thirstwasbottled.foundation.common.item;

import dev.ghen.thirst.content.purity.FillableWithPurity;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import net.flatdev.thirstwasbottled.item.Moditems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DrinkItem extends Item {
    public DrinkItem(Properties properties) {
        super((new Item.Properties()));
    }
    public int dripTracker = 0;
    public int hydration = 1;
    public int quench = 1;

    public int getHydration(){
        return hydration;
    }
    public int getQuench() {
        return quench;
    }
    public void setHydration(int amount){
        hydration = amount;
    }
    public void setQuench(int amount){
        quench = amount;
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
            if (!player.isCreative() && !player.isSpectator()) {
                int thirst = ThirstBarRenderer.PLAYER_THIRST.getThirst();
                if (!player.isCreative() && !player.isSpectator()) {
                    if(thirst < 20){
                        return ItemUtils.startUsingInstantly(level, player, hand); // makes it hold and play animation
                    } else {
                        return InteractionResultHolder.pass(stack);
                    }
                } else{
                    return InteractionResultHolder.pass(stack);
                }
            };
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
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int count) {
        if (level.isClientSide) {
            dripTracker++;
            if(dripTracker % 9 == 0) {
                Vec3 vec3 = living.getLookAngle(); // returns a normalized vector, which is what you want in this case
                double x = living.getX() + vec3.x * 0.15;
                double y = (living.getY() + 1.45) + vec3.y * 0.25;
                double z = living.getZ() + vec3.z * 0.15;
                level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, 100, 0);
                dripTracker = 0;
            }
        }
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Play drinking sound
            if (!player.isCreative() && !player.isSpectator()) {
                System.out.println("Player thirst " + ThirstBarRenderer.PLAYER_THIRST.getThirst());
                int thirst = ThirstBarRenderer.PLAYER_THIRST.getThirst();
                if (thirst < 20) {
                    System.out.println("Player is able to drink!");
                    ItemStack cup = stack.copy();
                    cup.setCount(1);
                    if (cup.getTag().getBoolean("Filled") && cup.getTag().getString("Fluid").equals("minecraft:water")) {
                        int purity = WaterPurity.getPurity(cup);
                        stack.shrink(1);
                        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                        PlayerThirst.drink(cup, player);
                        WaterPurity.givePurityEffects(player, purity);

                        // Returns the modified stack and adds a empty cup to the players inventory. (an empty stack will clear the inventory slot)
                        player.addItem(new ItemStack(Moditems.PAPERCUP.get()));
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
