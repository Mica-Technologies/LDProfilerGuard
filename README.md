# LD Profiler Guard

A tiny Forge **1.12.2** mod that stops a vanilla crash in the section profiler. When profiling is
on (`/debug start`, the F3 + Shift profiler chart, or a profiling tool), one misbehaving mod can
take the whole server or client down with this:

```
java.lang.ArrayIndexOutOfBoundsException: -1
	at java.util.ArrayList.elementData(ArrayList.java:424)
	at java.util.ArrayList.remove(ArrayList.java:501)
	at net.minecraft.profiler.Profiler.endSection(Profiler.java:69)
	at net.minecraft.server.MinecraftServer.tick(MinecraftServer.java:785)
	at net.minecraft.server.MinecraftServer.run(MinecraftServer.java:591)
	at java.lang.Thread.run(Thread.java:750)
```

The trace names vanilla code, not the mod that caused it, so the crash is hard to pin on anyone.

## Why it happens

`Profiler` keeps the open section names and start times in two lists, and `endSection()` pops
both with `remove(size() - 1)`. Nothing checks that a section is open. Two kinds of caller leave
the stack unbalanced:

- **Empty-stack pops.** A mod skips its `startSection` (for example it checked
  `profilingEnabled` while profiling was off) but still calls `endSection` after profiling was
  switched on. With nothing open, `size() - 1` is `-1`.
- **Foreign threads.** The profiler is not thread safe. An async worker that opens or closes
  sections on the server (or client) profiler pops sections that belong to the main thread, and
  the main thread later runs dry.

## What it does

A mixin on `net.minecraft.profiler.Profiler` guards `startSection`, `endSection` and
`endStartSection`:

- `endSection` with no open section is **ignored and counted** instead of throwing.
- Calls from a thread other than the profiler's owner are **ignored**. The owner is the thread
  that opens the `"root"` section, which vanilla does at the top of every server tick and every
  client frame. Until that has happened, every thread is allowed, as in vanilla.
- The first event of each kind is logged as a warning **with a stack trace**, so you can see which
  mod is responsible. Later events are summarised at most once a minute.

**Zero cost when profiling is off.** Each injection checks `profilingEnabled` first and returns
at once, as vanilla's own early return does. Normal play never reaches the guard logic.

To turn the guard off and get vanilla behaviour back (for example to reproduce the crash), start
the game with `-Dldprofilerguard.disable=true`.

## Test commands

These reproduce both misuses on purpose, so you can check that the guard works on your install.
They are kept in release builds deliberately.

- `/ldprofilerguard underflow|foreign` runs against the **server** profiler (in singleplayer, the
  integrated server). Ops only (permission level 2). Turn profiling on first with `/debug start`.
  Without the guard, `underflow` crashes the server with the trace above.
- `/ldprofilerguardclient underflow|foreign` runs against the **client's own** profiler. It only
  touches your own game. Open the F3 + Shift profiler chart first so the client profiler is on.

`underflow` calls `endSection` 64 times; `foreign` opens and closes a section from a new thread.
Each prints a confirmation in chat and the guard logs a warning in `logs/latest.log`.

## Install

1. Install [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) (10.x). It is
   required and not bundled. Keep its file name as distributed (`!mixinbooter-<version>.jar`): the
   leading `!` makes Forge load it before the coremods that depend on it.
2. Put `LDProfilerGuard-<version>.jar` in `mods/`.

Works on **dedicated servers, clients and singleplayer**. It is not needed on both sides: a server
with it accepts clients without it, and a client with it can join servers without it
(`acceptableRemoteVersions = "*"`).

## Compatibility

- Minecraft 1.12.2, Forge 14.23.5.x, MixinBooter 10.x.
- Changes only `net.minecraft.profiler.Profiler`, with `HEAD` injections that cancel nothing
  unless a call is unsafe. Other mods that read the profiler see the same results as before.
- Tested in a development client and on a dedicated server. Not yet tested alongside OptiFine;
  reports welcome.

## Building

Uses the GregTechCEu buildscript (RetroFuturaGradle). Gradle needs JDK 17 or newer; the mod
itself targets Java 8.

```sh
./gradlew build   # compile, run unit tests, produce build/libs/LDProfilerGuard-<version>.jar
./gradlew test    # unit tests only
```

Releases are built by GitHub Actions: every push to `main` publishes a dated pre-release, and a
manual run of the release workflow with `release=true` publishes a full release.

## License

LGPL 2.1, the same as Minecraft Forge. See [LICENSE](LICENSE). The build script is from
GregTechCEu Buildscripts under the MIT license; see
[LICENSE-GregTechCEu Buildscripts.txt](LICENSE-GregTechCEu%20Buildscripts.txt).
