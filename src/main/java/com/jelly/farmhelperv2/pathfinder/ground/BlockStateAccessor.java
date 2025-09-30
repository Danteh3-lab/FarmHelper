package com.jelly.farmhelperv2.pathfinder.ground;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class BlockStateAccessor {
    private final World world;
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    
    public BlockStateAccessor(World world) {
        this.world = world;
    }
    
    public IBlockState get(int x, int y, int z) {
        if (!isBlockInLoadedChunks(x, z)) {
            return Blocks.air.getDefaultState();
        }
        return world.getBlockState(mutablePos.set(x, y, z));
    }
    
    public boolean isBlockInLoadedChunks(int x, int z) {
        return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }
    
    public World getWorld() {
        return world;
    }
    
    public BlockPos.MutableBlockPos getMutablePos() {
        return mutablePos;
    }
}
