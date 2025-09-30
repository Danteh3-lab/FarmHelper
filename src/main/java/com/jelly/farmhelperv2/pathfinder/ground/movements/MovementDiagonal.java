package com.jelly.farmhelperv2.pathfinder.ground.movements;

import com.jelly.farmhelperv2.pathfinder.ground.GroundCalculationContext;
import com.jelly.farmhelperv2.pathfinder.ground.GroundMovementHelper;
import com.jelly.farmhelperv2.pathfinder.ground.MovementResult;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class MovementDiagonal {
    private static final double SQRT_2 = Math.sqrt(2.0);
    
    public static void calculateCost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        res.set(destX, y, destZ);
        cost(ctx, x, y, z, destX, destZ, res);
    }
    
    private static void cost(GroundCalculationContext ctx, int x, int y, int z, int destX, int destZ, MovementResult res) {
        if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)) return;
        
        boolean ascend = false;
        boolean descend = false;
        IBlockState sourceState = ctx.get(x, y, z);
        IBlockState destState = ctx.bsa.get(destX, y, destZ);
        
        if (!GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, destZ)) {
            ascend = true;
            if (!GroundMovementHelper.canWalkThrough(ctx.bsa, x, y + 3, z) ||
                !GroundMovementHelper.canStandOn(ctx.bsa, destX, y + 1, destZ) ||
                !GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)) {
                return;
            }
            destState = ctx.bsa.get(destX, y + 1, destZ);
            res.y = y + 1;
        } else {
            if (!GroundMovementHelper.canStandOn(ctx.bsa, destX, y, destZ, destState)) {
                descend = true;
                if (!GroundMovementHelper.canStandOn(ctx.bsa, destX, y - 1, destZ) ||
                    !GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y, destZ)) {
                    return;
                }
                destState = ctx.bsa.get(destX, y - 1, destZ);
                res.y = y - 1;
            }
        }
        
        double cost = ctx.cost.ONE_BLOCK_WALK_COST;
        
        if (GroundMovementHelper.isLadder(sourceState)) {
            return;
        }
        
        if (GroundMovementHelper.isWater(ctx.get(x, y + 1, z))) {
            if (ascend) return;
            cost = ctx.cost.ONE_BLOCK_WALK_IN_WATER_COST * SQRT_2;
        } else {
            cost *= ctx.cost.SPRINT_MULTIPLIER;
        }
        
        IBlockState ALOWState = ctx.get(x, y + 1, destZ);
        IBlockState BLOWState = ctx.get(destX, y + 1, z);
        
        boolean ATOP = GroundMovementHelper.canWalkThrough(ctx.bsa, x, y + 3, destZ);
        boolean AMID = GroundMovementHelper.canWalkThrough(ctx.bsa, x, y + 2, destZ);
        boolean ALOW = GroundMovementHelper.canWalkThrough(ctx.bsa, x, y + 1, destZ, ALOWState);
        boolean BTOP = GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 3, z);
        boolean BMID = GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, z);
        boolean BLOW = GroundMovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, z, BLOWState);
        
        if (!(ATOP && AMID && ALOW && BTOP && BMID && BLOW)) {
            return;
        }
        
        if (!(ascend || descend)) {
            res.cost = cost * SQRT_2;
            return;
        }
        
        AxisAlignedBB sourceBB = sourceState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(x, y, z), sourceState);
        double sourceMaxY = sourceBB != null ? sourceBB.maxY : y;
        
        if (ascend) {
            AxisAlignedBB destBB = destState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(destX, y + 1, destZ), destState);
            double destMaxY = destBB != null ? destBB.maxY : (y + 1.0);
            
            if (destMaxY - sourceMaxY <= 0.5) {
                res.cost = cost * SQRT_2;
            } else if (destMaxY - sourceMaxY <= 1.125) {
                res.cost = cost * SQRT_2 + ctx.cost.JUMP_ONE_BLOCK_COST;
            } else {
                res.cost = ctx.cost.INF_COST;
            }
            return;
        }
        
        if (descend) {
            AxisAlignedBB destBB = destState.getBlock().getCollisionBoundingBox(ctx.world, new BlockPos(destX, y - 1, destZ), destState);
            double destMaxY = destBB != null ? destBB.maxY : (y + 1.0);
            
            if (sourceMaxY - destMaxY <= 0.5) {
                res.cost = cost * SQRT_2;
            } else if (sourceMaxY - destMaxY <= 1.0) {
                res.cost = ctx.cost.N_BLOCK_FALL_COST[1] + cost * SQRT_2;
            } else {
                res.cost = ctx.cost.INF_COST;
            }
        }
    }
}
