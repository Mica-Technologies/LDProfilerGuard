package com.micatechnologies.minecraft.ldprofilerguard;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * {@code /ldprofilerguard <underflow|foreign>} reproduces the two profiler misuses on the server
 * profiler (switch it on first with {@code /debug start}). Without the guard, {@code underflow}
 * crashes the server with vanilla's ArrayIndexOutOfBoundsException. Ops only.
 */
public class CommandProfilerGuard extends CommandBase {

    @Override
    public String getName() {
        return "ldprofilerguard";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ldprofilerguard <underflow|foreign>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        Profiler profiler = server.profiler;
        if (!profiler.profilingEnabled) {
            sender.sendMessage(new TextComponentString("Profiler is off; run /debug start first."));
            return;
        }
        switch (args[0]) {
            case "underflow":
                // Pops more sections than are open, so the stack runs dry partway through.
                for (int i = 0; i < 64; i++) {
                    profiler.endSection();
                }
                sender.sendMessage(new TextComponentString("Called endSection 64 times; server still up."));
                break;
            case "foreign":
                Thread t = new Thread(() -> {
                    profiler.startSection("ldprofilerguard-foreign");
                    profiler.endSection();
                }, "LDProfilerGuard-test-foreign");
                t.start();
                sender.sendMessage(new TextComponentString("Started a foreign-thread start/endSection."));
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }
}
