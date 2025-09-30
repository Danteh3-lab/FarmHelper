package com.jelly.farmhelperv2.pathfinder.ground;

import com.jelly.farmhelperv2.pathfinder.ground.movements.MovementAscend;
import com.jelly.farmhelperv2.pathfinder.ground.movements.MovementDescend;
import com.jelly.farmhelperv2.pathfinder.ground.movements.MovementDiagonal;
import com.jelly.farmhelperv2.pathfinder.ground.movements.MovementTraverse;

public enum GroundMove {
    TRAVERSE_NORTH(0, -1),
    TRAVERSE_SOUTH(0, 1),
    TRAVERSE_EAST(1, 0),
    TRAVERSE_WEST(-1, 0),
    
    ASCEND_NORTH(0, -1),
    ASCEND_SOUTH(0, 1),
    ASCEND_EAST(1, 0),
    ASCEND_WEST(-1, 0),
    
    DESCEND_NORTH(0, -1),
    DESCEND_SOUTH(0, 1),
    DESCEND_EAST(1, 0),
    DESCEND_WEST(-1, 0),
    
    DIAGONAL_NORTHEAST(1, -1),
    DIAGONAL_NORTHWEST(-1, -1),
    DIAGONAL_SOUTHEAST(1, 1),
    DIAGONAL_SOUTHWEST(-1, 1);
    
    public final int offsetX;
    public final int offsetZ;
    
    GroundMove(int offsetX, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }
    
    public void calculate(GroundCalculationContext ctx, int parentX, int parentY, int parentZ, MovementResult res) {
        switch (this) {
            case TRAVERSE_NORTH:
            case TRAVERSE_SOUTH:
            case TRAVERSE_EAST:
            case TRAVERSE_WEST:
                MovementTraverse.calculateCost(ctx, parentX, parentY, parentZ, parentX + offsetX, parentZ + offsetZ, res);
                break;
                
            case ASCEND_NORTH:
            case ASCEND_SOUTH:
            case ASCEND_EAST:
            case ASCEND_WEST:
                MovementAscend.calculateCost(ctx, parentX, parentY, parentZ, parentX + offsetX, parentZ + offsetZ, res);
                break;
                
            case DESCEND_NORTH:
            case DESCEND_SOUTH:
            case DESCEND_EAST:
            case DESCEND_WEST:
                MovementDescend.calculateCost(ctx, parentX, parentY, parentZ, parentX + offsetX, parentZ + offsetZ, res);
                break;
                
            case DIAGONAL_NORTHEAST:
            case DIAGONAL_NORTHWEST:
            case DIAGONAL_SOUTHEAST:
            case DIAGONAL_SOUTHWEST:
                MovementDiagonal.calculateCost(ctx, parentX, parentY, parentZ, parentX + offsetX, parentZ + offsetZ, res);
                break;
        }
    }
}
