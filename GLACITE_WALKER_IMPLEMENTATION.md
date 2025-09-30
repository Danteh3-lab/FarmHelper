# Glacite Walker Combat Macro Implementation Guide

## Overview
This guide explains the Glacite Walker combat macro implementation for FarmHelper. The macro automates combat against Glacite Walkers in the Dwarven Mines while maintaining human-like behavior to avoid detection.

## Files Created/Modified

### New Files:
1. **`src/main/java/com/jelly/farmhelperv2/macro/impl/GlaciteWalkerMacro.java`**
   - Main combat macro implementation
   - Handles target acquisition, pathfinding, rotation, and attacking
   - Implements randomized delays and movement patterns

2. **`src/main/java/com/jelly/farmhelperv2/command/GlaciteWalkerCommand.java`**
   - Command interface for toggling the combat macro
   - Usage: `/glacite`, `/gw`, or `/glacitewalker`

### Modified Files:
1. **`src/main/java/com/jelly/farmhelperv2/config/FarmHelperConfig.java`**
   - Added configuration section for Glacite Walker combat
   - New settings: scan radius, attack range, rotation time, AOTV usage, health management

2. **`src/main/java/com/jelly/farmhelperv2/FarmHelper.java`**
   - Registered the new GlaciteWalkerCommand

## Configuration Options

Access via OneConfig GUI under "Glacite Walker Combat" category:

| Setting | Default | Description |
|---------|---------|-------------|
| Enable Glacite Walker Combat | false | Master toggle for the combat macro |
| Scan Radius | 30 blocks | Maximum distance to scan for targets |
| Attack Range | 4 blocks | Maximum distance to attack from |
| Rotation Time | 250ms | Base time for rotation smoothing |
| Auto Health Management | true | Pause combat when health is low |
| Min Health Threshold | 50% | Health percentage to trigger pause |

## How It Works

### State Machine
The macro operates through 5 states:

1. **IDLE** - Waiting between scans
2. **SEARCHING** - Scanning for Glacite Walker entities
3. **APPROACHING** - Pathfinding to target
4. **ATTACKING** - Engaging in combat
5. **REPOSITIONING** - Brief movement adjustments

### Anti-Detection Features

#### Randomized Timing
- Attack delays: 125-200ms (5-8 CPS)
- Burst attacks: 8-13 hits before pause
- Pause duration: 1-1.8 seconds
- Rotation time: base + 0-100ms jitter

#### Human-Like Movement
- Micro-strafing during combat (5% chance per tick)
- Smooth pathfinding via `FlyPathFinderExecutor`
- Random repositioning after attack bursts (30% chance)
- Eased rotations via `RotationHandler`

#### Safety Mechanisms
- Stuck detection: stops if no kills in 60 seconds
- Failsafe integration: pauses on staff checks
- Health monitoring: auto-pause at low health
- Random pauses after kills: 800-1400ms

## Usage Instructions

### Setup
1. Open FarmHelper config: `/fh`
2. Go to "General" category
3. Select "Combat - Glacite Walker (Dwarven Mines)" from the "Macro Type" dropdown
4. (Optional) Configure settings in "Glacite Walker Combat" category
5. Go to Dwarven Mines where Glacite Walkers spawn

### Starting the Macro

**Method 1: Using the Macro Toggle (Recommended)**
- Press your macro toggle keybind (default: check `/fh` settings)
- The combat macro will start automatically

**Method 2: Using Commands**
```
/glacite
```
or
```
/gw
```
or
```
/glacitewalker
```

### Stopping the Macro
- Press your macro toggle keybind again
- Or run the command again: `/glacite`

## Integration with Existing Systems

### Failsafes
The macro integrates with all existing failsafes:
- `RotationFailsafe` - Detects suspicious rotation packets
- `TeleportFailsafe` - Handles unexpected teleports
- `KnockbackFailsafe` - Manages knockback events
- All other failsafes pause combat automatically

### Rotation Handler
All aiming uses `RotationHandler.easeTo()` with:
- Eased interpolation (not instant snaps)
- Randomness enabled
- Configurable timing
- Server/client sync

### Pathfinding
Uses ground-based pathfinding only:

- Uses `GroundPathExecutor` with A* algorithm
- Ported from MightyMiner's Kotlin pathfinder
- Supports walking, jumping, ascending, descending, diagonal movement
- Calculates movement costs based on terrain
- Works without Booster Cookie or God Pot
- No flight required

## Testing Checklist

Before using in production:

- [ ] Test in a private Dwarven Mines lobby
- [ ] Verify target acquisition works
- [ ] Confirm smooth rotation and movement
- [ ] Check attack timing looks natural
- [ ] Test failsafe triggers (use `/fh` test failsafe)
- [ ] Monitor for stuck detection
- [ ] Adjust config values if needed

## Tuning Recommendations

### If Detection Risk Seems High:
- Increase `glaciteWalkerRotationTime` to 300-400ms
- Reduce attack range to 3 blocks
- Disable AOTV usage
- Add manual pauses between sessions

### If Performance is Poor:
- Increase scan radius to 40-50 blocks
- Increase attack range to 5-6 blocks
- Reduce rotation time to 150-200ms for faster targeting

### If Pathfinding Issues:
- **No path found**: Reduce scan radius or check terrain obstacles
- **Jittery movement**: Increase rotation time to 300-400ms
- **Falls through world**: Report bug with location details
- **Gets stuck on terrain**: Try repositioning manually or restart macro

## Known Limitations

1. **Entity Detection**: Relies on EntityArmorStand with "Glacite Walker" in name
   - May need adjustment if Hypixel changes entity structure
   
2. **Combat Only**: Does not handle:
   - Loot collection
   - Inventory management
   - Potion usage (except health check)
   
3. **Location Specific**: Designed for Dwarven Mines
   - May need modifications for other areas

## Future Enhancements

Potential improvements:
- [ ] Auto-loot collection
- [ ] Potion/ability usage
- [ ] Multi-target prioritization
- [ ] Profit tracking integration
- [ ] Custom entity filters
- [ ] Retreat patterns when overwhelmed

## Troubleshooting

### Macro doesn't start
- Check config is enabled
- Verify you're in Dwarven Mines
- Ensure no farming macro is running

### No targets found
- Increase scan radius
- Check entity name matches "Glacite Walker"
- Verify mobs are spawning

### Stuck detection triggers
- Adjust attack range
- Check pathfinding isn't blocked
- Verify AOTV is equipped if enabled

### Rotation looks robotic
- Increase rotation time
- Check randomness is enabled
- Add more micro-movement

## Safety Reminders

⚠️ **Important**:
- Always monitor the first few runs
- Use in private lobbies when testing
- Don't AFK immediately - watch for issues
- Respect Hypixel's rules and ToS
- This is for educational purposes

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review config settings
3. Test with default values first
4. Check console for debug messages (enable debug mode)

---

**Note**: This implementation follows FarmHelper's existing patterns for randomization, failsafes, and anti-detection. Always use responsibly and at your own risk.
