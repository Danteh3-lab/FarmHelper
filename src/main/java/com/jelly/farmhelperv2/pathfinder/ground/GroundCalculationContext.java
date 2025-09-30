package com.jelly.farmhelperv2.pathfinder.ground;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class GroundCalculationContext {
    private final Minecraft mc;
    public final World world;
    public final EntityPlayer player;
    public final BlockStateAccessor bsa;
    public final int jumpBoostAmplifier;
    public final ActionCosts cost;
    public final int maxFallHeight = 20;
    
    public GroundCalculationContext(double sprintFactor, double walkFactor, double sneakFactor) {
        this.mc = Minecraft.getMinecraft();
        this.player = mc.thePlayer;
        this.world = mc.theWorld;
        this.bsa = new BlockStateAccessor(world);
        
        PotionEffect jumpBoost = player.getActivePotionEffect(Potion.jump);
        this.jumpBoostAmplifier = jumpBoost != null ? jumpBoost.getAmplifier() : -1;
        this.cost = new ActionCosts(sprintFactor, walkFactor, sneakFactor, jumpBoostAmplifier);
    }
    
    public GroundCalculationContext() {
        this(0.13, 0.1, 0.03);
    }
    
    public IBlockState get(int x, int y, int z) {
        return bsa.get(x, y, z);
    }
}
