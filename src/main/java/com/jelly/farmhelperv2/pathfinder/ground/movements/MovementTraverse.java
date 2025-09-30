package com.jelly.farmhelperv2.pathfinder.ground.movements;

import com.jelly.farmhelperv2.pathfinder.ground.GroundCalculationContext;
import com.jelly.farmhelperv2.pathfinder.ground.GroundMovementHelper;
import com.jelly.farmhelperv2.pathfinder.ground.MovementResult;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class MovementTraverse {
    
    public static void calculateCost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        res.set(destX, y, destZ);
        cost(ctx, x, y, z, destX, destZ, res);
    }
    
    private static void cost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        if (!GroundMovementHelper.canStandOn(ctx.bsa, destX, y, destZ)) {
            return;
        }
        
        IBlockState destUpState = ctx.get(destX, y + 1, destZ);
        if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, destZ, destUpState) || 
            !GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)) {
            return;
        }
        
        IBlockState srcUpState = ctx.get(x, y + 1, z);
        
        boolean isSourceTopWalkableLadder = GroundMovementHelper.canWalkIntoLadder(srcUpState, x - destX, z - destZ);
        boolean isDestTopWalkableLadder = GroundMovementHelper.canWalkIntoLadder(destUpState, destX - x, destZ - z);
        
        if (GroundMovementHelper.isLadder(destUpState) && !isDestTopWalkableLadder) {
            res.cost = ctx.cost.INF_COST;
            return;
        }
        if (GroundMovementHelper.isLadder(srcUpState) && !isSourceTopWalkableLadder) {
            res.cost = ctx.cost.INF_COST;
            return;
        }
        
        IBlockState sourceState = ctx.get(x, y, z);
        IBlockState destState = ctx.get(destX, y, destZ);
        
        AxisAlignedBB sourceBB = sourceState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(x, y, z), sourceState);
        AxisAlignedBB destBB = destState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(destX, y, destZ), destState);
        
        if (sourceBB == null || destBB == null) {
            return;
        }
        
        double sourceHeight = sourceBB.maxY;
        double destHeight = destBB.maxY;
        double diff = destHeight - sourceHeight;
        
        if (diff <= 0.5) {
            res.cost = ctx.cost.ONE_BLOCK_SPRINT_COST;
        } else if (diff <= 1) {
            res.cost = ctx.cost.JUMP_ONE_BLOCK_COST;
        } else {
            res.cost = ctx.cost.INF_COST;
        }
    }
}
