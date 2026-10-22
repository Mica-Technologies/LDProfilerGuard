package com.micatechnologies.minecraft.ldprofilerguard.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

/**
 * Registers {@code mixins.ldprofilerguard.json} with MixinBooter as an early (vanilla-target)
 * config. MixinBooter does not act on a jar's {@code MixinConfigs} manifest attribute; this loader
 * is what makes the mixin apply, in dev and in a packaged jar alike.
 *
 * <p>{@code -Dldprofilerguard.disable=true} skips the config and leaves vanilla behaviour, which
 * is how the crash is reproduced for comparison.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("LD Profiler Guard")
@IFMLLoadingPlugin.SortingIndex(1001)
public class ProfilerGuardCoreMod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        if (Boolean.getBoolean("ldprofilerguard.disable")) {
            return Collections.emptyList();
        }
        return Collections.singletonList("mixins.ldprofilerguard.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    @Nullable
    public String getModContainerClass() {
        return null;
    }

    @Override
    @Nullable
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    @Nullable
    public String getAccessTransformerClass() {
        return null;
    }
}
