package com.dangkhoilkb17.headaim;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

record AimConfig(
        double range,
        double rangeSquared,
        double fovCosine,
        float yawResponse,
        float pitchResponse,
        float maxYawStep,
        float maxPitchStep
) {
    private static final Logger LOGGER = LoggerFactory.getLogger("Head Aim");
    private static final String DEFAULTS = """
            # Maximum distance to target, in blocks (1-64).
            range=20
            # Total aiming cone field of view, in degrees (1-360).
            fov_degrees=180
            # Rotation response per tick (0.01-1).
            yaw_response=0.31
            pitch_response=0.27
            # Maximum rotation step per tick, in degrees (0.1-45).
            max_yaw_step=9
            max_pitch_step=7
            """;

    static AimConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("headaim.properties");
        Properties properties = new Properties();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, DEFAULTS, StandardCharsets.UTF_8);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load Head Aim settings from {}; using defaults.", path, exception);
            properties.clear();
        }

        return fromProperties(properties);
    }

    static AimConfig fromProperties(Properties properties) {
        double range = readDouble(properties, "range", 20.0D, 1.0D, 64.0D);
        float fovDegrees = readFloat(properties, "fov_degrees", 180.0F, 1.0F, 360.0F);
        float yawResponse = readFloat(properties, "yaw_response", 0.31F, 0.01F, 1.0F);
        float pitchResponse = readFloat(properties, "pitch_response", 0.27F, 0.01F, 1.0F);
        float maxYawStep = readFloat(properties, "max_yaw_step", 9.0F, 0.1F, 45.0F);
        float maxPitchStep = readFloat(properties, "max_pitch_step", 7.0F, 0.1F, 45.0F);

        return new AimConfig(
                range,
                range * range,
                Math.cos(Math.toRadians(fovDegrees * 0.5D)),
                yawResponse,
                pitchResponse,
                maxYawStep,
                maxPitchStep
        );
    }

    private static double readDouble(Properties properties, String key, double fallback, double minimum, double maximum) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isFinite(parsed) && parsed >= minimum && parsed <= maximum) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid user settings are reported below and replaced by their defaults.
        }
        LOGGER.warn("Ignoring invalid Head Aim setting '{}={}' (expected {}-{}).", key, value, minimum, maximum);
        return fallback;
    }

    private static float readFloat(Properties properties, String key, float fallback, float minimum, float maximum) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            float parsed = Float.parseFloat(value);
            if (Float.isFinite(parsed) && parsed >= minimum && parsed <= maximum) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid user settings are reported below and replaced by their defaults.
        }
        LOGGER.warn("Ignoring invalid Head Aim setting '{}={}' (expected {}-{}).", key, value, minimum, maximum);
        return fallback;
    }
}
