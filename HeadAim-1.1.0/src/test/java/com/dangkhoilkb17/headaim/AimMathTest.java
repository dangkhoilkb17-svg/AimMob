package com.dangkhoilkb17.headaim;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimMathTest {
    private static final Vec3d FORWARD = new Vec3d(0.0D, 0.0D, 1.0D);

    @Test
    void includesDirectionsInsideTheConfiguredCone() {
        assertTrue(AimMath.isDirectionInsideFov(FORWARD, FORWARD, 0.0D));
        assertTrue(AimMath.isDirectionInsideFov(FORWARD, new Vec3d(1.0D, 0.0D, 0.0D), 0.0D));
    }

    @Test
    void excludesDirectionsOutsideTheConfiguredCone() {
        assertFalse(AimMath.isDirectionInsideFov(FORWARD, new Vec3d(0.0D, 0.0D, -1.0D), 0.0D));
        assertFalse(AimMath.isDirectionInsideFov(FORWARD, new Vec3d(1.0D, 0.0D, 1.732D), Math.cos(Math.toRadians(20.0D))));
    }

    @Test
    void treatsCoincidentPointsAsInsideTheCone() {
        assertTrue(AimMath.isDirectionInsideFov(FORWARD, Vec3d.ZERO, 0.0D));
    }
}
