package com.jelly.farmhelperv2.pathfinder.ground;

import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroundPathExecutor {
    private static GroundPathExecutor instance;
    
    public static GroundPathExecutor getInstance() {
        if (instance == null) {
            instance = new GroundPathExecutor();
        }
        return instance;
    }
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final CopyOnWriteArrayList<Vec3> path = new CopyOnWriteArrayList<>();
    private Vec3 target;
    private Entity targetEntity;
    private boolean running = false;
    private boolean follow = false;
    private Thread pathfinderThread;
    
    public void findPath(Entity target, boolean follow) {
        this.targetEntity = target;
        this.follow = follow;
        findPath(new Vec3(target.posX, target.posY, target.posZ), follow);
    }
    
    public void findPath(Vec3 pos, boolean follow) {
        if (mc.thePlayer.getDistance(pos.xCoord, pos.yCoord, pos.zCoord) < 1.5) {
            stop();
            return;
        }
        
        this.target = pos;
        this.follow = follow;
        this.running = true;
        
        pathfinderThread = new Thread(() -> {
            try {
                List<Vec3> route = calculatePath(mc.thePlayer.getPosition(), new BlockPos(pos));
                if (route != null && !route.isEmpty()) {
                    path.clear();
                    path.addAll(route);
                    LogUtils.sendDebug("[Ground Path] Found path with " + route.size() + " waypoints");
                } else {
                    LogUtils.sendDebug("[Ground Path] No path found");
                    running = false;
                }
            } catch (Exception e) {
                LogUtils.sendError("[Ground Path] Pathfinding error: " + e.getMessage());
                running = false;
            }
        });
        pathfinderThread.start();
    }
    
    private List<Vec3> calculatePath(BlockPos start, BlockPos goal) {
        GroundCalculationContext ctx = new GroundCalculationContext();
        
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, PathNode> allNodes = new HashMap<>();
        
        PathNode startNode = new PathNode(start);
        startNode.gScore = 0;
        startNode.fScore = heuristic(start, goal);
        openSet.add(startNode);
        allNodes.put(start, startNode);
        
        int maxIterations = 5000;
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            PathNode current = openSet.poll();
            
            if (current.pos.distanceSq(goal) < 4) {
                return reconstructPath(current);
            }
            
            current.closed = true;
            
            // Try all possible moves
            MovementResult res = new MovementResult();
            for (GroundMove move : GroundMove.values()) {
                res.reset();
                move.calculate(ctx, current.pos.getX(), current.pos.getY(), current.pos.getZ(), res);
                
                if (res.cost >= ctx.cost.INF_COST) continue;
                
                BlockPos neighborPos = new BlockPos(res.x, res.y, res.z);
                PathNode neighbor = allNodes.computeIfAbsent(neighborPos, PathNode::new);
                
                if (neighbor.closed) continue;
                
                double tentativeGScore = current.gScore + res.cost;
                
                if (tentativeGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = tentativeGScore + heuristic(neighborPos, goal);
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        return null; // No path found
    }
    
    private double heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    private List<Vec3> reconstructPath(PathNode node) {
        List<Vec3> path = new ArrayList<>();
        PathNode current = node;
        while (current != null) {
            path.add(0, new Vec3(current.pos.getX() + 0.5, current.pos.getY(), current.pos.getZ() + 0.5));
            current = current.parent;
        }
        return path;
    }
    
    public void tick() {
        if (!running || path.isEmpty()) return;
        
        if (mc.thePlayer == null || mc.theWorld == null) {
            stop();
            return;
        }
        
        // Update path if following entity
        if (follow && targetEntity != null && mc.thePlayer.ticksExisted % 20 == 0) {
            findPath(targetEntity, true);
        }
        
        Vec3 currentWaypoint = path.get(0);
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        double distance = playerPos.distanceTo(currentWaypoint);
        
        if (distance < 0.5) {
            path.remove(0);
            if (path.isEmpty()) {
                stop();
                return;
            }
            currentWaypoint = path.get(0);
        }
        
        // Rotate towards waypoint
        Rotation targetRotation = RotationHandler.getInstance().getRotation(currentWaypoint, playerPos);
        float yaw = targetRotation.getYaw();
        
        // Move towards waypoint
        float yawDiff = yaw - mc.thePlayer.rotationYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        KeyBindUtils.stopMovement();
        
        if (Math.abs(yawDiff) < 45) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
        } else if (yawDiff > 0) {
            if (yawDiff > 135) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindLeft);
            }
        } else {
            if (yawDiff < -135) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight);
            }
        }
        
        // Jump if needed
        if (currentWaypoint.yCoord > playerPos.yCoord + 0.5) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
        }
    }
    
    public void stop() {
        running = false;
        path.clear();
        target = null;
        targetEntity = null;
        KeyBindUtils.stopMovement();
        if (pathfinderThread != null && pathfinderThread.isAlive()) {
            pathfinderThread.interrupt();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private static class PathNode {
        BlockPos pos;
        PathNode parent;
        double gScore = Double.MAX_VALUE;
        double fScore = Double.MAX_VALUE;
        boolean closed = false;
        
        PathNode(BlockPos pos) {
            this.pos = pos;
        }
    }
}
