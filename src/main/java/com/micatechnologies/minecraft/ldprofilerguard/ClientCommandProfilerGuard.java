package com.micatechnologies.minecraft.ldprofilerguard;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * {@code /ldprofilerguardclient <underflow|foreign>}: the same two misuses as
 * {@link CommandProfilerGuard}, on the client's own profiler. Switch the client profiler on first
 * with the F3 + Shift profiler chart. Runs on the client thread and only touches this client.
 */
@SideOnly(Side.CLIENT)
public class ClientCommandProfilerGuard extends CommandBase {

    static void register() {
        ClientCommandHandler.instance.registerCommand(new ClientCommandProfilerGuard());
    }

    @Override
    public String getName() {
        return "ldprofilerguardclient";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ldprofilerguardclient <underflow|foreign>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        Profiler profiler = Minecraft.getMinecraft().profiler;
        if (!profiler.profilingEnabled) {
            sender.sendMessage(new TextComponentString("Client profiler is off; open the F3 + Shift chart first."));
            return;
        }
        switch (args[0]) {
            case "underflow":
                for (int i = 0; i < 64; i++) {
                    profiler.endSection();
                }
                sender.sendMessage(new TextComponentString("Called endSection 64 times on the client profiler; client still up."));
                break;
            case "foreign":
                Thread t = new Thread(() -> {
                    profiler.startSection("ldprofilerguard-foreign");
                    profiler.endSection();
                }, "LDProfilerGuard-test-foreign-client");
                t.start();
                sender.sendMessage(new TextComponentString("Started a foreign-thread start/endSection on the client profiler."));
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }
}
