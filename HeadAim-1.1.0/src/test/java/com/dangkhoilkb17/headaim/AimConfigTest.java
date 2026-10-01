package com.dangkhoilkb17.headaim;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AimConfigTest {
    @Test
    void usesExistingDefaultsWhenPropertiesAreEmpty() {
        AimConfig config = AimConfig.fromProperties(new Properties());

        assertEquals(20.0D, config.range());
        assertEquals(400.0D, config.rangeSquared());
        assertEquals(0.0D, config.fovCosine(), 1.0E-12D);
        assertEquals(0.31F, config.yawResponse());
        assertEquals(0.27F, config.pitchResponse());
        assertEquals(9.0F, config.maxYawStep());
        assertEquals(7.0F, config.maxPitchStep());
    }

    @Test
    void loadsValidCustomProperties() {
        Properties properties = new Properties();
        properties.setProperty("range", "32.5");
        properties.setProperty("fov_degrees", "120");
        properties.setProperty("yaw_response", "0.5");
        properties.setProperty("pitch_response", "0.4");
        properties.setProperty("max_yaw_step", "12");
        properties.setProperty("max_pitch_step", "10");

        AimConfig config = AimConfig.fromProperties(properties);

        assertEquals(32.5D, config.range());
        assertEquals(1056.25D, config.rangeSquared());
        assertEquals(0.5D, config.fovCosine(), 1.0E-12D);
        assertEquals(0.5F, config.yawResponse());
        assertEquals(0.4F, config.pitchResponse());
        assertEquals(12.0F, config.maxYawStep());
        assertEquals(10.0F, config.maxPitchStep());
    }

    @Test
    void fallsBackForOutOfRangeAndMalformedProperties() {
        Properties properties = new Properties();
        properties.setProperty("range", "NaN");
        properties.setProperty("fov_degrees", "400");
        properties.setProperty("yaw_response", "not-a-number");

        AimConfig config = AimConfig.fromProperties(properties);

        assertEquals(20.0D, config.range());
        assertEquals(0.0D, config.fovCosine(), 1.0E-12D);
        assertEquals(0.31F, config.yawResponse());
    }
}
