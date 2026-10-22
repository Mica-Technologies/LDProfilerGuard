# CLAUDE.md

Guidance for Claude Code (claude.ai/code) when working in this repository.

## Purpose

LD Profiler Guard (`ldprofilerguard`): a small Forge 1.12.2 mixin mod that stops the vanilla
`Profiler` crash `ArrayIndexOutOfBoundsException: -1` in `Profiler.endSection`, hit while profiling
is on when a mod calls `endSection` without a matching `startSection`, or when another thread
touches the profiler. Server and client; `acceptableRemoteVersions = "*"`. See README.md.

## Commands

Gradle needs a JDK 17+ (set `JAVA_HOME` if `java` is not on `PATH`).

```sh
./gradlew build                     # compile, unit tests, jar in build/libs
./gradlew test                      # unit tests
./gradlew test --tests '*ProfilerGuardTest.foreignThreadCallsAreIgnored'
./gradlew runClient                 # dev client; log in run/logs/latest.log
./gradlew runServer                 # dev server; log in run/logs/latest.log
```

Do not run `build`/`test` while a `runClient`/`runServer` is live: it swaps the classpath under
the running game.

## Architecture

- `core/ProfilerGuard`, `core/GuardLog`: the decisions and the rate-limited logging. No Minecraft
  imports, so they are unit-tested directly (`src/test`).
- `mixin/MixinProfiler`: HEAD injections on `startSection`, `endSection`, `endStartSection`. Each
  checks `profilingEnabled` first, so nothing runs while profiling is off. The owning thread is
  whichever thread opens the `"root"` section (server thread / client thread).
- `mixin/ProfilerGuardCoreMod`: FML coremod + MixinBooter `IEarlyMixinLoader` that registers
  `mixins.ldprofilerguard.json`. `-Dldprofilerguard.disable=true` skips it (vanilla behaviour).
- `CommandProfilerGuard`: `/ldprofilerguard underflow|foreign` (ops) reproduces the misuse on the
  server profiler after `/debug start`.
- `ClientCommandProfilerGuard`: `/ldprofilerguardclient underflow|foreign`, the same on the
  client profiler (F3 + Shift chart on). Registered only on the client.

## Conventions and gotchas

- GregTechCEu buildscript (RFG). Do not edit `build.gradle` beyond the marked local change;
  settings go in `buildscript.properties`, extras in `addon.gradle`. `modVersion =` stays empty:
  the release workflow pins it from the tag (local builds take it from `git describe`).
- MixinBooter is a runtime dependency, not bundled.
- After a change, check `run/logs/latest.log` for `Adding [mixins.ldprofilerguard.json]` (proves
  MixinBooter loaded the config) and for mixin errors. A wrong injection descriptor disables the
  whole mixin class without failing the build.
- `runObfClient` cannot start this mod: it puts the mod on the classpath as a command-line
  coremod, which loads before MixinBooter from `mods/`. Test packaged jars in a real launcher.
- Workflows in `.github/` are shared across Mica Technologies mods; change only `# ADAPT:` lines.
