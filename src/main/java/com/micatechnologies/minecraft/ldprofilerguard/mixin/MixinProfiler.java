package com.micatechnologies.minecraft.ldprofilerguard.mixin;

import com.micatechnologies.minecraft.ldprofilerguard.LDProfilerGuard;
import com.micatechnologies.minecraft.ldprofilerguard.core.ProfilerGuard;
import java.util.List;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards the vanilla profiler's section stack (see {@link ProfilerGuard}). Each injection first
 * reads {@code profilingEnabled} and does nothing if it is off, as vanilla's own early return
 * does, so the guard costs nothing while profiling is off.
 */
@Mixin(Profiler.class)
public abstract class MixinProfiler {

    @Shadow public boolean profilingEnabled;
    @Shadow @Final private List<String> sectionList;
    @Shadow @Final private List<Long> timestampList;

    @Unique
    private final ProfilerGuard ldprofilerguard$guard = new ProfilerGuard(LDProfilerGuard.LOG);

    @Inject(method = "startSection(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void ldprofilerguard$startSection(String name, CallbackInfo ci) {
        if (this.profilingEnabled && ldprofilerguard$guard.skipStart(name, Thread.currentThread())) {
            ci.cancel();
        }
    }

    @Inject(method = "endSection()V", at = @At("HEAD"), cancellable = true)
    private void ldprofilerguard$endSection(CallbackInfo ci) {
        if (this.profilingEnabled && ldprofilerguard$guard.skipEnd(sectionList.size(), timestampList.size(),
                Thread.currentThread())) {
            ci.cancel();
        }
    }

    @Inject(method = "endStartSection(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void ldprofilerguard$endStartSection(String name, CallbackInfo ci) {
        if (this.profilingEnabled && ldprofilerguard$guard.skipEndStart(name, Thread.currentThread())) {
            ci.cancel();
        }
    }
}
