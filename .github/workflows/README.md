# BaseProtect — Minecraft 26.2 Fabric

A server-side Fabric mod for protecting a base with four custom corners. Players who are not on the safe list are struck by lightning when they enter the protected polygon.

## Requirements
- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer compatible 26.2 loader
- Fabric API 0.152.0+26.2 or newer compatible 26.2 API
- Java 25

Fabric's 26.2 documentation specifies Java 25, and the 26.2 release guidance specifies Loom 1.17 and Gradle 9.5.1-era tooling. See Fabric's official 26.2 documentation before building.

## Build
On Windows PowerShell:

    .\\gradlew.bat build

On macOS/Linux:

    ./gradlew build

The finished mod JAR will be in `build/libs/`.

## Commands
- `/cor1`
- `/cor2`
- `/cor3`
- `/cor4`
- `/saveperson <player>`
- `/unsaveperson <player>`
- `/baseprotect on`
- `/baseprotect off`
- `/baseprotect status`
- `/baseprotect list`
- `/baseprotect clear`

All management commands require permission level 2.
