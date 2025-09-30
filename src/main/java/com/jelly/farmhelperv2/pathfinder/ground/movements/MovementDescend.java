package com.jelly.farmhelperv2.pathfinder.ground.movements;

import com.jelly.farmhelperv2.pathfinder.ground.GroundCalculationContext;
import com.jelly.farmhelperv2.pathfinder.ground.GroundMovementHelper;
import com.jelly.farmhelperv2.pathfinder.ground.MovementResult;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class MovementDescend {
    
    public static void calculateCost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        res.set(destX, y - 1, destZ);
        cost(ctx, x, y, z, destX, destZ, res);
    }
    
    private static void cost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        IBlockState destUpState = ctx.get(destX, y, destZ);
        if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ) ||
            !GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, destZ) ||
            !GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y, destZ, destUpState)) {
            return;
        }
        
        IBlockState sourceState = ctx.get(x, y, z);
        if (GroundMovementHelper.isLadder(sourceState) || GroundMovementHelper.isLadder(destUpState)) {
            return;
        }
        
        IBlockState destState = ctx.get(destX, y - 1, destZ);
        if (!GroundMovementHelper.canStandOn(ctx.bsa, destX, y - 1, destZ, destState) || GroundMovementHelper.isLadder(destState)) {
            freeFallCost(ctx, x, y, z, destX, destZ, destState, res);
            return;
        }
        
        AxisAlignedBB sourceBB = sourceState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(x, y, z), sourceState);
        AxisAlignedBB destBB = destState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(destX, y - 1, destZ), destState);
        
        if (sourceBB == null || destBB == null) {
            return;
        }
        
        double sourceHeight = sourceBB.maxY;
        double destHeight = destBB.maxY;
        double diff = sourceHeight - destHeight;
        
        if (diff <= 0.5) {
            res.cost = ctx.cost.ONE_BLOCK_WALK_COST;
        } else if (diff <= 1.125) {
            res.cost = ctx.cost.WALK_OFF_ONE_BLOCK_COST * ctx.cost.SPRINT_MULTIPLIER + ctx.cost.N_BLOCK_FALL_COST[1];
        } else {
            res.cost = ctx.cost.INF_COST;
        }
    }
    
    private static void freeFallCost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, IBlockState destState, MovementResult res) {
        if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y - 1, destZ, destState)) {
            return;
        }
        
        int effStartHeight = y;
        double cost = 0.0;
        
        for (int fellSoFar = 2; fellSoFar < Integer.MAX_VALUE; fellSoFar++) {
            int newY = y - fellSoFar;
            if (newY < 0) return;
            
            IBlockState blockOnto = ctx.get(destX, newY, destZ);
            int unprotectedFallHeight = fellSoFar - (y - effStartHeight);
            double costUpUntilThisBlock = ctx.cost.WALK_OFF_ONE_BLOCK_COST + ctx.cost.N_BLOCK_FALL_COST[unprotectedFallHeight] + cost;
            
            if (!GroundMovementHelper.canStandOn(ctx.bsa, destX, newY, destZ, blockOnto)) {
                if (GroundMovementHelper.isWater(blockOnto)) {
                    if (GroundMovementHelper.canStandOn(ctx.bsa, destX, newY - 1, destZ)) {
                        res.y = newY - 1;
                        res.cost = costUpUntilThisBlock;
                        return;
                    }
                    return;
                }
                
                if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, newY, destZ, blockOnto)) {
                    return;
                }
                continue;
            }
            
            if (unprotectedFallHeight <= 11 && GroundMovementHelper.isLadder(blockOnto)) {
                cost += ctx.cost.N_BLOCK_FALL_COST[unprotectedFallHeight - 1] + ctx.cost.ONE_DOWN_LADDER_COST;
                effStartHeight = newY;
                continue;
            }
            
            if (fellSoFar <= ctx.maxFallHeight) {
                res.y = newY;
                res.cost = costUpUntilThisBlock;
                return;
            }
            return;
        }
    }
}
