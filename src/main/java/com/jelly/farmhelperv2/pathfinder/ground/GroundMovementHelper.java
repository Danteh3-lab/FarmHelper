package com.jelly.farmhelperv2.pathfinder.ground;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

public class GroundMovementHelper {
    
    public static boolean canWalkThrough(BlockStateAccessor bsa, int x, int y, int z) {
        IBlockState state = bsa.get(x, y, z);
        return canWalkThrough(bsa, x, y, z, state);
    }
    
    public static boolean canWalkThrough(BlockStateAccessor bsa, int x, int y, int z, IBlockState state) {
        Boolean canWalk = canWalkThroughBlockState(state);
        if (canWalk != null) {
            return canWalk;
        }
        return canWalkThroughPosition(bsa, x, y, z, state);
    }
    
    public static Boolean canWalkThroughBlockState(IBlockState state) {
        Block block = state.getBlock();
        
        if (block == Blocks.air) return true;
        if (block == Blocks.fire || block == Blocks.tripwire || block == Blocks.web || 
            block == Blocks.end_portal || block == Blocks.cocoa || block instanceof BlockSkull || 
            block instanceof BlockTrapDoor) {
            return false;
        }
        
        if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
            if (block == Blocks.iron_door) {
                return false;
            } else {
                return true;
            }
        }
        
        if (block == Blocks.carpet) return null;
        if (block instanceof BlockSnow) return null;
        
        if (block instanceof BlockLiquid) {
            if (state.getValue(BlockLiquid.LEVEL) != 0) {
                return false;
            } else {
                return null;
            }
        }
        
        if (block instanceof BlockCauldron) return false;
        if (block == Blocks.ladder) return false;
        
        try {
            return block.isPassable(null, null);
        } catch (Throwable exception) {
            return null;
        }
    }
    
    public static boolean canWalkThroughPosition(BlockStateAccessor bsa, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        
        if (block == Blocks.carpet) {
            return canStandOn(bsa, x, y - 1, z);
        }
        
        if (block instanceof BlockSnow) {
            if (!bsa.isBlockInLoadedChunks(x, z)) {
                return true;
            }
            if (state.getValue(BlockSnow.LAYERS) >= 1) {
                return false;
            }
            return canStandOn(bsa, x, y - 1, z);
        }
        
        if (block instanceof BlockLiquid) {
            if (isFlowing(x, y, z, state, bsa)) {
                return false;
            }
            
            IBlockState up = bsa.get(x, y + 1, z);
            if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                return false;
            }
            return block == Blocks.water || block == Blocks.flowing_water;
        }
        
        return block.isPassable(bsa.getWorld(), bsa.getMutablePos().set(x, y, z));
    }
    
    public static boolean canStandOn(BlockStateAccessor bsa, int x, int y, int z) {
        return canStandOn(bsa, x, y, z, bsa.get(x, y, z));
    }
    
    public static boolean canStandOn(BlockStateAccessor bsa, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        
        if (block.isNormalCube()) return true;
        if (block == Blocks.redstone_block) return true;
        if (block == Blocks.ladder) return true;
        if (block == Blocks.farmland || block == Blocks.grass) return true;
        if (block == Blocks.ender_chest || block == Blocks.chest || block == Blocks.trapped_chest) return true;
        if (block == Blocks.glass || block == Blocks.stained_glass) return true;
        if (block instanceof BlockStairs) return true;
        if (block == Blocks.sea_lantern) return true;
        
        if (isWater(state)) {
            Block up = bsa.get(x, y + 1, z).getBlock();
            return up == Blocks.waterlily || up == Blocks.carpet;
        }
        
        if (isLava(state)) return false;
        if (block instanceof BlockSlab) return true;
        if (block instanceof BlockSnow) return true;
        
        return false;
    }
    
    public static boolean possiblyFlowing(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) != 0;
    }
    
    public static boolean isFlowing(int x, int y, int z, IBlockState state, BlockStateAccessor bsa) {
        if (!(state.getBlock() instanceof BlockLiquid)) {
            return false;
        }
        if (state.getValue(BlockLiquid.LEVEL) != 0) {
            return true;
        }
        return possiblyFlowing(bsa.get(x + 1, y, z)) ||
               possiblyFlowing(bsa.get(x - 1, y, z)) ||
               possiblyFlowing(bsa.get(x, y, z + 1)) ||
               possiblyFlowing(bsa.get(x, y, z - 1));
    }
    
    public static boolean isWater(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.water || block == Blocks.flowing_water;
    }
    
    public static boolean isLava(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.lava || block == Blocks.flowing_lava;
    }
    
    public static boolean isBottomSlab(IBlockState state) {
        return state.getBlock() instanceof BlockSlab && 
               !((BlockSlab) state.getBlock()).isDouble() && 
               state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
    }
    
    public static boolean isValidStair(IBlockState state, int dx, int dz) {
        if (dx == dz) return false;
        if (!(state.getBlock() instanceof BlockStairs)) return false;
        if (state.getValue(BlockStairs.HALF) != BlockStairs.EnumHalf.BOTTOM) return false;
        
        EnumFacing stairFacing = state.getValue(BlockStairs.FACING);
        
        if (dz == -1) return stairFacing == EnumFacing.NORTH;
        if (dz == 1) return stairFacing == EnumFacing.SOUTH;
        if (dx == -1) return stairFacing == EnumFacing.WEST;
        if (dx == 1) return stairFacing == EnumFacing.EAST;
        
        return false;
    }
    
    public static boolean isValidReversedStair(IBlockState state, int dx, int dz) {
        if (dx == dz) return false;
        if (!(state.getBlock() instanceof BlockStairs)) return false;
        if (state.getValue(BlockStairs.HALF) != BlockStairs.EnumHalf.BOTTOM) return false;
        
        EnumFacing stairFacing = state.getValue(BlockStairs.FACING);
        
        if (dz == 1) return stairFacing == EnumFacing.NORTH;
        if (dz == -1) return stairFacing == EnumFacing.SOUTH;
        if (dx == 1) return stairFacing == EnumFacing.WEST;
        if (dx == -1) return stairFacing == EnumFacing.EAST;
        
        return false;
    }
    
    public static boolean hasTop(IBlockState state, int dX, int dZ) {
        return !(isBottomSlab(state) || isValidStair(state, dX, dZ));
    }
    
    public static boolean avoidWalkingInto(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockLiquid || block instanceof BlockFire || 
               block == Blocks.cactus || block == Blocks.end_portal || block == Blocks.web;
    }
    
    public static EnumFacing getFacing(int dx, int dz) {
        if (dx == 0 && dz == 0) return EnumFacing.UP;
        return EnumFacing.HORIZONTALS[Math.abs(dx) * (2 + dx) + Math.abs(dz) * (1 - dz)];
    }
    
    public static boolean isLadder(IBlockState state) {
        return state.getBlock() == Blocks.ladder;
    }
    
    public static boolean canWalkIntoLadder(IBlockState ladderState, int dx, int dz) {
        return isLadder(ladderState) && ladderState.getValue(BlockLadder.FACING) != getFacing(dx, dz);
    }
}
