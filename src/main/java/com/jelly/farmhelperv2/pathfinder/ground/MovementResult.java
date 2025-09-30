package com.jelly.farmhelperv2.pathfinder.ground;

import net.minecraft.util.BlockPos;

public class MovementResult {
    public int x = 0;
    public int y = 0;
    public int z = 0;
    public double cost = 1e6;
    
    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void reset() {
        x = 0;
        y = 0;
        z = 0;
        cost = 1e6;
    }
    
    public BlockPos getDest() {
        return new BlockPos(x, y, z);
    }
}
