/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.minestom;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.tick.AbstractTickHook;
import me.lucko.spark.common.sampler.tick.AbstractTickReporter;
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.sampler.tick.TickReporter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.extensions.Extension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

public class MinestomSparkPlugin extends Extension implements SparkPlugin {

    private SparkPlatform platform;

    @Override
    public void initialize() {
        System.out.println("Spark is loading");
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        MinecraftServer.getCommandManager().register(new SparkCommand());
        System.out.println("Spark is loaded");
    }

    @Override
    public void terminate() {

    }

    @Override
    public String getVersion() {
        return "A performance monitoring plugin";
    }

    @Override
    public Path getPluginDirectory() {
        return Paths.get("plugins-data");
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<MinestomCommandSender> getSendersWithPermission(String permission) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream().map(MinestomCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        MinecraftServer.getSchedulerManager().buildTask(task).schedule();
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new MinestomPlatformInfo();
    }

    public TickHook createTickHook() {
        return new MinestomTickHook();
    }

    public TickReporter createTickReporter() {
        return new MinestomTickReporter();
    }

    private final static class MinestomTickHook extends AbstractTickHook implements TickHook {
        LongConsumer tickHook = (l) -> onTick();

        @Override
        public void start() {
            MinecraftServer.getUpdateManager().addTickStartCallback(tickHook);
        }

        @Override
        public void close() {
            MinecraftServer.getUpdateManager().removeTickStartCallback(tickHook);
        }
    }

    private final static class MinestomTickReporter extends AbstractTickReporter implements TickReporter {
        LongConsumer tickHook = this::onTick;

        @Override
        public void start() {
            MinecraftServer.getUpdateManager().addTickEndCallback(tickHook);
        }

        @Override
        public void close() {
            MinecraftServer.getUpdateManager().removeTickEndCallback(tickHook);
        }
    }

    private final class SparkCommand extends Command {

        SparkCommand() {
            super(getCommandName());
            addSyntax((commandSender, args) -> platform.executeCommand(new MinestomCommandSender(commandSender), args.getStringArray("args")), ArgumentType.StringArray("args"));
            setDefaultExecutor((commandSender, args) -> platform.executeCommand(new MinestomCommandSender(commandSender), new String[0]));
        }
    }
}
