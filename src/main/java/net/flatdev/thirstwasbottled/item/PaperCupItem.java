package net.flatdev.thirstwasbottled.item;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import net.flatdev.thirstwasbottled.foundation.common.item.DrinkItem;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class PaperCupItem extends DrinkItem {
    public PaperCupItem(Properties properties) {
        super(properties);
        this.setHydration(1.5);
        this.setQuench(1);
    }
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
                        player.addItem(new ItemStack(Moditems.PAPERCUP_CRUSHED.get()));
                        return (stack);
                    }
                }
            }
        }
        // Return the empty cup
        return new ItemStack(Moditems.PAPERCUP_CRUSHED.get());
    }
}
