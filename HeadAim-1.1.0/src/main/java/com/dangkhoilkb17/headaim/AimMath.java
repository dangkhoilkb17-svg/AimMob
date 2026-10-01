package com.dangkhoilkb17.headaim;

import net.minecraft.util.math.Vec3d;

final class AimMath {
    private AimMath() {
    }

    static boolean isDirectionInsideFov(Vec3d look, Vec3d toTarget, double fovCosine) {
        double lengthSquared = toTarget.lengthSquared();
        if (lengthSquared < 1.0E-8D) {
            return true;
        }

        double targetLength = Math.sqrt(lengthSquared * look.lengthSquared());
        double minimumDot = fovCosine * targetLength;
        return look.dotProduct(toTarget) + 1.0E-12D * targetLength >= minimumDot;
    }
}
