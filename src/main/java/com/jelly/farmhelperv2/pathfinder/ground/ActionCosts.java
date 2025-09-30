package com.jelly.farmhelperv2.pathfinder.ground;

public class ActionCosts {
    public final double INF_COST = 1e6;
    public final double[] N_BLOCK_FALL_COST;
    public final double ONE_UP_LADDER_COST = 1.0 / (0.12 * 9.8);
    public final double ONE_DOWN_LADDER_COST = 1.0 / 0.15;
    
    public final double JUMP_ONE_BLOCK_COST;
    public final double ONE_BLOCK_WALK_COST;
    public final double ONE_BLOCK_SPRINT_COST;
    public final double ONE_BLOCK_SNEAK_COST;
    public final double ONE_BLOCK_WALK_IN_WATER_COST;
    public final double ONE_BLOCK_WALK_OVER_SOUL_SAND_COST;
    public final double WALK_OFF_ONE_BLOCK_COST;
    public final double CENTER_AFTER_FALL_COST;
    public final double SPRINT_MULTIPLIER;
    
    private final double SPRINT_MOVEMENT_FACTOR;
    private final double WALKING_MOVEMENT_FACTOR;
    private final double SNEAKING_MOVEMENT_FACTOR;
    private final int JUMP_BOOST_LEVEL;
    
    public ActionCosts(double sprintFactor, double walkFactor, double sneakFactor, int jumpBoostLevel) {
        this.SPRINT_MOVEMENT_FACTOR = sprintFactor;
        this.WALKING_MOVEMENT_FACTOR = walkFactor;
        this.SNEAKING_MOVEMENT_FACTOR = sneakFactor;
        this.JUMP_BOOST_LEVEL = jumpBoostLevel;
        
        this.ONE_BLOCK_WALK_COST = 1.0 / actionTime(getWalkingFriction(WALKING_MOVEMENT_FACTOR));
        this.ONE_BLOCK_SPRINT_COST = 1.0 / actionTime(getWalkingFriction(SPRINT_MOVEMENT_FACTOR));
        this.ONE_BLOCK_SNEAK_COST = 1.0 / actionTime(getWalkingFriction(SNEAKING_MOVEMENT_FACTOR));
        this.ONE_BLOCK_WALK_IN_WATER_COST = 20 * actionTime(getWalkingInWaterFriction(WALKING_MOVEMENT_FACTOR));
        this.ONE_BLOCK_WALK_OVER_SOUL_SAND_COST = ONE_BLOCK_WALK_COST * 2;
        this.WALK_OFF_ONE_BLOCK_COST = ONE_BLOCK_WALK_COST * 0.8;
        this.CENTER_AFTER_FALL_COST = ONE_BLOCK_WALK_COST * 0.2;
        this.SPRINT_MULTIPLIER = WALKING_MOVEMENT_FACTOR / SPRINT_MOVEMENT_FACTOR;
        
        // Calculate jump cost
        double vel = 0.42 + (JUMP_BOOST_LEVEL + 1) * 0.1;
        double height = 0.0;
        double time = 1.0;
        for (int i = 1; i <= 20; i++) {
            height += vel;
            vel = (vel - 0.08) * 0.98;
            if (vel < 0) break;
            time++;
        }
        this.JUMP_ONE_BLOCK_COST = time + fallDistanceToTicks(height - 1);
        
        this.N_BLOCK_FALL_COST = generateNBlocksFallCost();
    }
    
    public ActionCosts() {
        this(0.13, 0.1, 0.03, -1);
    }
    
    private double getWalkingFriction(double landMovementFactor) {
        return landMovementFactor * ((0.16277136) / (0.91 * 0.91 * 0.91));
    }
    
    private double getWalkingInWaterFriction(double landMovementFactor) {
        return 0.02 + (landMovementFactor - 0.02) * (1.0 / 3.0);
    }
    
    private double actionTime(double friction) {
        return friction * 10;
    }
    
    public double motionYAtTick(int tick) {
        double velocity = -0.0784000015258789;
        for (int i = 1; i <= tick; i++) {
            velocity = (velocity - 0.08) * 0.9800000190734863;
        }
        return velocity;
    }
    
    public double fallDistanceToTicks(double distance) {
        if (distance == 0.0) return 0.0;
        double tmpDistance = distance;
        int tickCount = 0;
        while (true) {
            double fallDistance = downwardMotionAtTick(tickCount);
            if (tmpDistance <= fallDistance) {
                return tickCount + tmpDistance / fallDistance;
            }
            tmpDistance -= fallDistance;
            tickCount++;
        }
    }
    
    private double downwardMotionAtTick(int tick) {
        return (Math.pow(0.98, tick) - 1) * -3.92;
    }
    
    private double[] generateNBlocksFallCost() {
        double[] timeCost = new double[257];
        double currentDistance = 0.0;
        int targetDistance = 1;
        int tickCount = 0;
        
        while (true) {
            double velocityAtTick = downwardMotionAtTick(tickCount);
            
            if (currentDistance + velocityAtTick >= targetDistance) {
                timeCost[targetDistance] = tickCount + (targetDistance - currentDistance) / velocityAtTick;
                targetDistance++;
                if (targetDistance > 256) break;
                continue;
            }
            
            currentDistance += velocityAtTick;
            tickCount++;
        }
        return timeCost;
    }
}
