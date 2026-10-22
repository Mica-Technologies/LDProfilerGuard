# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

LD Profiler Guard (`ldprofilerguard`): a tiny Forge 1.12.2 mixin mod that stops the vanilla
`Profiler` crash `ArrayIndexOutOfBoundsException: -1` in `Profiler.endSection`, hit while profiling
is on when a mod calls `endSection` without a matching `startSection`, or when another thread
touches the profiler. Server and client; `acceptableRemoteVersions = "*"`.

## Commands

```sh
# Build (no java on PATH; set JAVA_HOME explicitly)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew build

# Run all tests
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew test

# Run a single test
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew test --tests '*ProfilerGuardTest.foreignThreadCallsAreIgnored'

# Run / launch locally (log goes to run/logs/latest.log)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew runServer
```

## Architecture

- `core/ProfilerGuard`, `core/GuardLog`: the decisions and the rate-limited logging. No Minecraft
  imports, so they are unit-tested directly (`src/test`).
- `mixin/MixinProfiler`: HEAD injections on `startSection`, `endSection`, `endStartSection`. Each
  checks `profilingEnabled` first, so nothing runs while profiling is off. The owning thread is
  whichever thread opens the `"root"` section (server thread / client thread).
- `mixin/ProfilerGuardCoreMod`: FML coremod + MixinBooter `IEarlyMixinLoader` that registers
  `mixins.ldprofilerguard.json`. `-Dldprofilerguard.disable=true` skips it (vanilla behaviour).
- `CommandProfilerGuard`: `/ldprofilerguard underflow|foreign` reproduces the misuse after
  `/debug start`.

## Conventions & gotchas

- GregTechCEu buildscript (RFG), copied from RCMC. Do not edit `build.gradle`; settings go in
  `buildscript.properties`, extras in `addon.gradle`. `modVersion =` stays empty: the release
  workflow pins it from the tag (local builds take it from `git describe`).
- MixinBooter (10.7 in the pack) is a dependency, not bundled.
- Verify with the `mod-verify` ritual: grep `run/logs/latest.log` for
  `Adding [mixins.ldprofilerguard.json]` and mixin errors.
