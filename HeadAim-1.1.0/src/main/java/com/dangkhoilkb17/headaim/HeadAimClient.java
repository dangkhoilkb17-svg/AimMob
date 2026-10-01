package com.dangkhoilkb17.headaim;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

/**
 * Smoothly turns the player's camera toward the head/eye point of a nearby living mob.
 *
 * Fabric 1.21.1 / Yarn 1.21.1+build.3.
 */
public final class HeadAimClient implements ClientModInitializer {
    private static final double RANGE = 20.0D;
    private static final double RANGE_SQUARED = RANGE * RANGE;

    // 180 degrees means a 90-degree half-cone in front of the player.
    private static final float FOV_DEGREES = 180.0F;

    // The camera does not jump straight to the target. The step is proportional
    // to the remaining angle and is capped so large turns are still gradual.
    private static final float YAW_RESPONSE = 0.31F;
    private static final float PITCH_RESPONSE = 0.27F;
    private static final float MAX_YAW_STEP = 9.0F;
    private static final float MAX_PITCH_STEP = 7.0F;
    private static final float MIN_YAW_STEP = 0.20F;
    private static final float MIN_PITCH_STEP = 0.16F;

    private static boolean enabled;
    private static LivingEntity lockedTarget;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.headaim.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.headaim"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(HeadAimClient::tick);
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            lockedTarget = null;
        }

        if (!enabled || client.player == null || client.world == null) {
            lockedTarget = null;
            return;
        }

        // Do not rotate the camera while using another screen such as a chest or inventory.
        if (client.currentScreen != null) {
            lockedTarget = null;
            return;
        }

        PlayerEntity player = client.player;

        if (!player.isAlive()) {
            lockedTarget = null;
            return;
        }

        // Keep the current target while it remains valid. This prevents rapid target
        // switching when several mobs are very close to each other.
        if (!isValidTarget(client, lockedTarget)) {
            lockedTarget = findNearestTarget(client);
        }

        if (lockedTarget == null) {
            return;
        }

        rotateTowardHead(player, lockedTarget);
    }

    private static LivingEntity findNearestTarget(MinecraftClient client) {
        PlayerEntity player = client.player;
        Box searchBox = player.getBoundingBox().expand(RANGE);

        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                candidate -> candidate != player
                        && !(candidate instanceof PlayerEntity)
                        && candidate.isAlive()
                        && !candidate.isSpectator()
        )) {
            double distanceSquared = player.squaredDistanceTo(entity);
            if (distanceSquared > RANGE_SQUARED || distanceSquared >= nearestDistanceSquared) {
                continue;
            }

            Vec3d head = entity.getEyePos();
            if (!isInsideFov(player, head)) {
                continue;
            }

            if (!hasLineOfSight(client, player, head)) {
                continue;
            }

            nearest = entity;
            nearestDistanceSquared = distanceSquared;
        }

        return nearest;
    }

    private static boolean isValidTarget(MinecraftClient client, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (target == client.player || target instanceof PlayerEntity || target.isSpectator()) {
            return false;
        }
        if (client.player.squaredDistanceTo(target) > RANGE_SQUARED) {
            return false;
        }

        Vec3d head = target.getEyePos();
        return isInsideFov(client.player, head) && hasLineOfSight(client, client.player, head);
    }

    private static boolean isInsideFov(PlayerEntity player, Vec3d targetPoint) {
        Vec3d from = player.getCameraPosVec(1.0F);
        Vec3d toTarget = targetPoint.subtract(from);
        double lengthSquared = toTarget.lengthSquared();
        if (lengthSquared < 1.0E-8D) {
            return true;
        }

        Vec3d look = player.getRotationVec(1.0F);
        double dot = MathHelper.clamp(
                look.dotProduct(toTarget.normalize()),
                -1.0D,
                1.0D
        );

        double angleDegrees = Math.toDegrees(Math.acos(dot));
        return angleDegrees <= FOV_DEGREES * 0.5D;
    }

    private static boolean hasLineOfSight(MinecraftClient client, PlayerEntity player, Vec3d targetPoint) {
        Vec3d start = player.getCameraPosVec(1.0F);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                start,
                targetPoint,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    private static void rotateTowardHead(PlayerEntity player, LivingEntity target) {
        Vec3d from = player.getCameraPosVec(1.0F);
        Vec3d to = target.getEyePos();
        Vec3d delta = to.subtract(from);

        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yawError = 0.0F;
        if (horizontalDistance >= 1.0E-7D) {
            float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
            yawError = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        }

        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontalDistance));

        float pitchError = targetPitch - player.getPitch();

        float yawStep = smoothStep(yawError, YAW_RESPONSE, MIN_YAW_STEP, MAX_YAW_STEP);
        float pitchStep = smoothStep(pitchError, PITCH_RESPONSE, MIN_PITCH_STEP, MAX_PITCH_STEP);

        float newYaw = player.getYaw() + yawStep;
        float newPitch = MathHelper.clamp(player.getPitch() + pitchStep, -90.0F, 90.0F);

        // Updating yaw/pitch changes the actual camera direction. Keeping head/body yaw
        // synchronized also makes the player's model turn naturally in third person.
        player.setYaw(newYaw);
        player.setPitch(newPitch);
        player.setHeadYaw(newYaw);
        player.setBodyYaw(newYaw);
    }

    private static float smoothStep(float error, float response, float minimum, float maximum) {
        float magnitude = Math.abs(error);
        if (magnitude < 0.001F) {
            return 0.0F;
        }

        float step = magnitude * response;
        step = MathHelper.clamp(step, minimum, maximum);
        step = Math.min(step, magnitude);
        return Math.copySign(step, error);
    }
}
