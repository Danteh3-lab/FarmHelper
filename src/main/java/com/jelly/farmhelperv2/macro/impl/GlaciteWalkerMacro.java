package com.jelly.farmhelperv2.macro.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GlaciteWalkerMacro extends AbstractMacro {
    private static GlaciteWalkerMacro instance;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private CombatState combatState = CombatState.IDLE;
    private Optional<Entity> currentTarget = Optional.empty();
    private final Clock attackDelay = new Clock();
    private final Clock targetScanDelay = new Clock();
    private final Clock stuckCheckClock = new Clock();
    private final Clock repositionDelay = new Clock();
    private long lastKillTime = 0;
    private int attackCount = 0;
    private boolean isRepositioning = false;

    public static GlaciteWalkerMacro getInstance() {
        if (instance == null) {
            instance = new GlaciteWalkerMacro();
        }
        return instance;
    }

    @Override
    public void onEnable() {
        LogUtils.sendSuccess("[Glacite Walker] Combat macro enabled!");
        combatState = CombatState.IDLE;
        currentTarget = Optional.empty();
        attackDelay.reset();
        targetScanDelay.reset();
        stuckCheckClock.schedule(60_000); // Check if stuck after 60 seconds
        lastKillTime = System.currentTimeMillis();
        attackCount = 0;
        isRepositioning = false;
        setEnabled(true);
    }

    @Override
    public void onDisable() {
        LogUtils.sendSuccess("[Glacite Walker] Combat macro disabled!");
        combatState = CombatState.IDLE;
        currentTarget = Optional.empty();
        attackDelay.reset();
        targetScanDelay.reset();
        stuckCheckClock.reset();
        repositionDelay.reset();
        FlyPathFinderExecutor.getInstance().stop();
        KeyBindUtils.stopMovement();
        RotationHandler.getInstance().reset();
        setEnabled(false);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            KeyBindUtils.stopMovement();
            return;
        }

        // Check if stuck (no kills in 60 seconds)
        if (stuckCheckClock.isScheduled() && stuckCheckClock.passed()) {
            LogUtils.sendWarning("[Glacite Walker] No kills detected for 60 seconds. Stopping macro.");
            MacroHandler.getInstance().disableMacro();
            return;
        }

        LogUtils.sendDebug("[Glacite Walker] State: " + combatState);

        switch (combatState) {
            case IDLE:
                handleIdle();
                break;
            case SEARCHING:
                handleSearching();
                break;
            case APPROACHING:
                handleApproaching();
                break;
            case ATTACKING:
                handleAttacking();
                break;
            case REPOSITIONING:
                handleRepositioning();
                break;
        }
    }

    private void handleIdle() {
        if (targetScanDelay.passed()) {
            combatState = CombatState.SEARCHING;
            targetScanDelay.schedule(500 + (long) (Math.random() * 300));
        }
    }

    private void handleSearching() {
        currentTarget = findGlaciteWalker();

        if (currentTarget.isPresent()) {
            LogUtils.sendDebug("[Glacite Walker] Target found: " + currentTarget.get().getName());
            combatState = CombatState.APPROACHING;
            attackCount = 0;
        } else {
            combatState = CombatState.IDLE;
        }
    }

    private void handleApproaching() {
        if (!currentTarget.isPresent() || !currentTarget.get().isEntityAlive()) {
            LogUtils.sendDebug("[Glacite Walker] Target lost during approach.");
            currentTarget = Optional.empty();
            combatState = CombatState.IDLE;
            FlyPathFinderExecutor.getInstance().stop();
            return;
        }

        Entity target = currentTarget.get();
        double distance = mc.thePlayer.getDistanceToEntity(target);

        // If within attack range, start attacking
        if (distance <= FarmHelperConfig.glaciteWalkerAttackRange) {
            LogUtils.sendDebug("[Glacite Walker] In attack range. Starting combat.");
            combatState = CombatState.ATTACKING;
            FlyPathFinderExecutor.getInstance().stop();
            return;
        }

        // Path to target if not already pathing
        if (!FlyPathFinderExecutor.getInstance().isRunning()) {
            LogUtils.sendDebug("[Glacite Walker] Pathing to target.");
            FlyPathFinderExecutor.getInstance().setUseAOTV(FarmHelperConfig.glaciteWalkerUseAOTV);
            FlyPathFinderExecutor.getInstance().findPath(target, true, true, 0.5f, false);
        }
    }

    private void handleAttacking() {
        if (!currentTarget.isPresent() || !currentTarget.get().isEntityAlive()) {
            LogUtils.sendDebug("[Glacite Walker] Target died or lost.");
            currentTarget = Optional.empty();
            combatState = CombatState.IDLE;
            lastKillTime = System.currentTimeMillis();
            stuckCheckClock.schedule(60_000); // Reset stuck timer
            
            // Random pause after kill
            Multithreading.schedule(() -> {
                if (combatState == CombatState.IDLE) {
                    combatState = CombatState.SEARCHING;
                }
            }, (long) (800 + Math.random() * 600), TimeUnit.MILLISECONDS);
            return;
        }

        Entity target = currentTarget.get();
        double distance = mc.thePlayer.getDistanceToEntity(target);

        // If too far, go back to approaching
        if (distance > FarmHelperConfig.glaciteWalkerAttackRange + 2) {
            LogUtils.sendDebug("[Glacite Walker] Target moved out of range.");
            combatState = CombatState.APPROACHING;
            return;
        }

        // Rotate to target
        if (!RotationHandler.getInstance().isRotating()) {
            Rotation targetRotation = RotationHandler.getInstance().getRotation(target);
            if (RotationHandler.getInstance().shouldRotate(targetRotation, 5f)) {
                float rotationTime = FarmHelperConfig.glaciteWalkerRotationTime + (float) (Math.random() * 100);
                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(target),
                        (long) rotationTime,
                        null
                ).randomness(true));
            }
        }

        // Attack with randomized delay
        if (attackDelay.passed() && !RotationHandler.getInstance().isRotating()) {
            performAttack();
            
            // Randomized attack delay (CPS between 5-8)
            long nextDelay = (long) (125 + Math.random() * 75); // ~5-8 CPS
            attackDelay.schedule(nextDelay);
            
            attackCount++;
            
            // Occasional pause after burst
            if (attackCount >= (8 + (int) (Math.random() * 5))) {
                LogUtils.sendDebug("[Glacite Walker] Taking attack pause.");
                attackDelay.schedule((long) (1000 + Math.random() * 800));
                attackCount = 0;
                
                // Occasionally reposition
                if (Math.random() < 0.3) {
                    combatState = CombatState.REPOSITIONING;
                    repositionDelay.schedule((long) (400 + Math.random() * 300));
                }
            }
        }

        // Strafe slightly during combat for more human-like movement
        if (Math.random() < 0.05 && !FlyPathFinderExecutor.getInstance().isRunning()) {
            performMicroStrafe();
        }
    }

    private void handleRepositioning() {
        if (!currentTarget.isPresent() || !currentTarget.get().isEntityAlive()) {
            currentTarget = Optional.empty();
            combatState = CombatState.IDLE;
            return;
        }

        if (repositionDelay.passed()) {
            combatState = CombatState.APPROACHING;
        }
    }

    private Optional<Entity> findGlaciteWalker() {
        List<Entity> entities = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityArmorStand)
                .filter(entity -> entity.getName().toLowerCase().contains("glacite walker"))
                .filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= FarmHelperConfig.glaciteWalkerScanRadius)
                .collect(Collectors.toList());

        if (entities.isEmpty()) {
            return Optional.empty();
        }

        // Find closest entity
        Entity closest = entities.stream()
                .min((e1, e2) -> Double.compare(
                        mc.thePlayer.getDistanceToEntity(e1),
                        mc.thePlayer.getDistanceToEntity(e2)
                ))
                .orElse(null);

        return Optional.ofNullable(closest);
    }

    private void performAttack() {
        KeyBindUtils.leftClick();
        LogUtils.sendDebug("[Glacite Walker] Attacking target.");
    }

    private void performMicroStrafe() {
        // Small random strafe movement
        int direction = (int) (Math.random() * 4);
        switch (direction) {
            case 0:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
                break;
            case 1:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                break;
            case 2:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
                break;
            case 3:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                break;
        }
        
        Multithreading.schedule(() -> KeyBindUtils.stopMovement(), (long) (100 + Math.random() * 150), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onChatMessageReceived(String message) {
        // Handle death or other chat events if needed
    }

    @Override
    public void onLastRender() {
        // Render debug info if needed
    }

    @Override
    public void onOverlayRender(RenderGameOverlayEvent.Post event) {
        // Render overlay info if needed
    }

    @Override
    public void onPacketReceived(ReceivePacketEvent event) {
        // Handle packets if needed
    }

    @Override
    public void saveState() {
        // Save state for failsafe recovery
    }

    @Override
    public void actionAfterTeleport() {
        // Handle post-teleport actions
    }

    public enum CombatState {
        IDLE,
        SEARCHING,
        APPROACHING,
        ATTACKING,
        REPOSITIONING
    }
}
