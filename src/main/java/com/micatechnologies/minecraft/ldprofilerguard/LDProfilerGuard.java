package com.micatechnologies.minecraft.ldprofilerguard;

import com.micatechnologies.minecraft.ldprofilerguard.core.GuardLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, acceptableRemoteVersions = "*",
        dependencies = "required-after:mixinbooter")
public class LDProfilerGuard {

    public static final Logger LOGGER = LogManager.getLogger("LDProfilerGuard");

    /** Shared by every guarded profiler (client and server). */
    public static final GuardLog LOG = new GuardLog((kind, message, trace) -> {
        if (trace != null) {
            LOGGER.warn(message, trace);
        } else {
            LOGGER.warn(message);
        }
    }, System::nanoTime, GuardLog.DEFAULT_INTERVAL_NANOS);

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandProfilerGuard());
    }
}
