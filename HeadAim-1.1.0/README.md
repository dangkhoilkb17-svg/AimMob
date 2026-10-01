# Head Aim 1.1.0 — Minecraft 1.21.1 Fabric

Client-side utility that smoothly turns the player's camera toward the eye/head point of the nearest visible living mob.

## Controls

- **F8** — toggle Head Aim on/off.

## Behavior

- Fabric 1.21.1 / Yarn 1.21.1+build.3.
- Targets living entities except players.
- Range: 20 blocks.
- Target field of view: 180° total (90° each side of the camera direction).
- Picks the nearest valid mob by player-to-entity distance.
- Keeps the current target until it becomes invalid to avoid rapid switching.
- Uses `LivingEntity#getEyePos()` for the head/eye aim point.
- Checks block line-of-sight before locking a target.
- Uses proportional, capped yaw/pitch movement instead of an instant rotation.
- Does not attack, click, send custom packets, or modify server-side combat logic.

## Configuration

On the first launch, Head Aim creates `config/headaim.properties` in the Minecraft game directory. Edit the file and restart the game to change:

- `range`: maximum targeting distance, from 1 to 64 blocks.
- `fov_degrees`: total aiming cone field of view, from 1 to 360 degrees.
- `yaw_response` and `pitch_response`: rotation response per tick, from 0.01 to 1.
- `max_yaw_step` and `max_pitch_step`: maximum rotation step per tick, from 0.1 to 45 degrees.

Invalid settings are logged and replaced with their defaults.

## Build

This project uses Fabric Loom 1.8.13 and Java 21.

Run the build with the Gradle wrapper (use `gradlew.bat build` on Windows):

```text
bash ./gradlew build
```

The remapped mod jar will be placed in `build/libs/`.
