# Ground Pathfinding Implementation Guide

## Overview
This document explains the ground-based pathfinding system ported from MightyMiner to FarmHelper. The implementation allows macros to navigate without requiring flight capability.

## Architecture

### Core Components

#### 1. **ActionCosts** (`com.jelly.farmhelperv2.pathfinder.ground.ActionCosts`)
- Calculates movement costs for different actions
- Factors: sprint, walk, sneak speeds
- Jump mechanics with boost support
- Fall distance calculations
- Water movement penalties

#### 2. **BlockStateAccessor** (`com.jelly.farmhelperv2.pathfinder.ground.BlockStateAccessor`)
- Efficient block state queries
- Chunk-aware lookups
- Mutable BlockPos for performance

#### 3. **GroundMovementHelper** (`com.jelly.farmhelperv2.pathfinder.ground.GroundMovementHelper`)
- Block passability checks
- Standing surface validation
- Ladder, stair, slab handling
- Water/lava detection
- Diagonal movement validation

#### 4. **GroundCalculationContext** (`com.jelly.farmhelperv2.pathfinder.ground.GroundCalculationContext`)
- Wraps world, player, and cost data
- Jump boost detection
- Configurable movement factors

#### 5. **MovementResult** (`com.jelly.farmhelperv2.pathfinder.ground.MovementResult`)
- Stores destination coordinates
- Movement cost
- Reusable for performance

### Movement Types

#### Movement Classes (under `com.jelly.farmhelperv2.pathfinder.ground.movements`)

1. **MovementTraverse** - Horizontal movement on same Y level
   - Checks collision heights
   - Handles small height differences (≤0.5 blocks)
   - Jump cost for medium differences (≤1 block)

2. **MovementAscend** - Moving up one block
   - Y+1 destination
   - Validates headroom (3 blocks)
   - Ladder compatibility
   - Height difference calculations

3. **MovementDescend** - Moving down one block
   - Y-1 destination
   - Free fall handling for larger drops
   - Water landing detection
   - Max fall height: 20 blocks

4. **MovementDiagonal** - 45° movement
   - Combined X and Z offset
   - Can ascend or descend diagonally
   - Validates both cardinal paths are clear
   - √2 cost multiplier

### GroundMove Enum

16 movement directions:
- 4 Traverse (N, S, E, W)
- 4 Ascend (N, S, E, W)
- 4 Descend (N, S, E, W)
- 4 Diagonal (NE, NW, SE, SW)

Each enum entry delegates to the appropriate movement calculator.

### GroundPathExecutor

**Pathfinding Algorithm**: A* (A-star)
- Priority queue ordered by f-score (g + h)
- g-score: actual cost from start
- h-score: heuristic (Euclidean distance to goal)
- Max iterations: 5000

**Path Execution**:
- Tick-based waypoint following
- Rotation towards next waypoint
- Keybind-based movement (forward, strafe, jump)
- Automatic path recalculation for moving targets (every 20 ticks)

**Movement Control**:
- Forward movement when facing waypoint (±45°)
- Strafing for larger angles
- Jump when ascending
- Smooth transitions between waypoints

## Integration with Glacite Walker Macro

### Configuration
```java
FarmHelperConfig.glaciteWalkerUseGroundPath = true; // Default
```

### Usage in Macro
```java
if (FarmHelperConfig.glaciteWalkerUseGroundPath) {
    GroundPathExecutor.getInstance().findPath(target, true);
    GroundPathExecutor.getInstance().tick(); // Call every game tick
} else {
    FlyPathFinderExecutor.getInstance().findPath(target, true, true, 0.5f, false);
}
```

### State Management
- **APPROACHING**: Initiates pathfinding
- **ATTACKING**: Stops pathfinding when in range
- **onDisable**: Cleans up executor state

## Porting from Kotlin to Java

### Key Translations

| Kotlin | Java |
|--------|------|
| `object MovementHelper` | `public class GroundMovementHelper` with static methods |
| `fun calculateCost(...)` | `public static void calculateCost(...)` |
| `when { ... }` | `if/else if` chains |
| `?.maxY ?: return` | `if (bb == null) return; double maxY = bb.maxY;` |
| `companion object` | Static methods in class |
| `val` / `var` | `final` / mutable variables |

### Challenges Addressed

1. **Nullable types**: Kotlin's `?` operator → Java null checks
2. **Default parameters**: Overloaded methods in Java
3. **Extension functions**: Static utility methods
4. **Data classes**: POJOs with getters/setters
5. **Ranges**: Explicit for loops

## Performance Considerations

### Optimizations
- Reusable `MovementResult` objects
- `BlockPos.MutableBlockPos` for queries
- Early returns in movement calculators
- Chunk-aware block access
- Priority queue for A* efficiency

### Limitations
- Max pathfinding iterations: 5000
- Max fall height: 20 blocks
- Path recalculation interval: 20 ticks (1 second)
- No multi-threading (runs on main thread during tick)

## Testing

### Unit Test Scenarios
1. **Flat terrain**: Verify traverse movements
2. **Stairs/slabs**: Validate ascend/descend
3. **Obstacles**: Ensure pathfinding routes around
4. **Water**: Check swimming penalties
5. **Cliffs**: Test fall calculations
6. **Diagonal**: Confirm corner cutting works

### Integration Testing
1. Enable ground pathfinding in config
2. Start Glacite Walker macro
3. Verify smooth movement to targets
4. Check no falling through world
5. Monitor CPU usage (should be reasonable)

## Comparison: Ground vs Fly Pathfinding

| Feature | Ground | Fly |
|---------|--------|-----|
| **Flight Required** | No | Yes (Booster Cookie + God Pot) |
| **Algorithm** | A* with movement costs | Flying node processor |
| **Vertical Movement** | Walk/jump/fall | Free 3D movement |
| **AOTV Support** | No | Yes |
| **Terrain Handling** | Walks around obstacles | Flies over obstacles |
| **Performance** | Moderate (A* search) | Fast (direct paths) |
| **Use Case** | No flight access | Garden farming, fast travel |

## Future Enhancements

Potential improvements:
- [ ] Path caching for repeated routes
- [ ] Jump optimization (sprint-jump timing)
- [ ] Parkour movements (4-block jumps)
- [ ] Ladder climbing
- [ ] Boat/minecart support
- [ ] Multi-threaded pathfinding
- [ ] Path smoothing (reduce waypoints)
- [ ] Dynamic obstacle avoidance

## Troubleshooting

### Path Not Found
- **Cause**: Destination unreachable or too far
- **Solution**: Increase max iterations or check terrain

### Jittery Movement
- **Cause**: Waypoints too close together
- **Solution**: Implement path smoothing

### Falls Through World
- **Cause**: Movement validation failed
- **Solution**: Check `canStandOn` logic for block type

### High CPU Usage
- **Cause**: Pathfinding every tick
- **Solution**: Reduce recalculation frequency

### Gets Stuck
- **Cause**: No valid moves from current position
- **Solution**: Add stuck detection and escape logic

## Credits

**Original Implementation**: MightyMiner V2 by Tama, Osama, Nima0908, Mr. Shadow, Nathan, JellyLab
**Ported to Java**: FarmHelper ground pathfinding system
**Algorithm**: A* pathfinding with Minecraft-specific movement costs

---

**Note**: This pathfinding system is designed for ground-based navigation in Minecraft 1.8.9. It respects block collision, terrain height, and player movement mechanics to provide realistic pathing without flight.
