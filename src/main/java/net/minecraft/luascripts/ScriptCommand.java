package net.minecraft.luascripts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.luascripts.luaenvironment.LuaEnvironment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.Globals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ScriptCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, Thread> threadsMap = new HashMap<>();

    public ScriptCommand(CommandDispatcher<CommandSource> dispatcher)  {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("script").
                then(Commands.literal("run").
                then(Commands.argument("filepath", StringArgumentType.string()).then(Commands.argument("threadName", StringArgumentType.string()).executes(arguments -> {

                    if (threadsMap.containsKey(arguments.getArgument("threadName", String.class)))
                        throw new CommandException(ITextComponent.getTextComponentOrEmpty("Thread named '"+arguments.getArgument("threadName", String.class)+"' already exists"));

                    MinecraftServer server = arguments.getSource().getServer();

                    File scriptFile = server.getFile("luascripts\\"+arguments.getArgument("filepath", String.class).replace("\"", ""));

                    try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                        StringBuilder scriptBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            scriptBuilder.append(line).append(System.lineSeparator());
                        }
                        String script = scriptBuilder.toString();

                        //System.out.println(script);

                        Runnable executor = () -> {
                            try {
                                Globals globals = LuaEnvironment.setupLuaEnvironment(arguments);
                                globals.load(script).call();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };

                        ManagedThread thread = new ManagedThread(arguments.getArgument("threadName", String.class), executor);

                        threadsMap.put(arguments.getArgument("threadName", String.class), thread);

                        thread.start();

                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new CommandException(ITextComponent.getTextComponentOrEmpty(e.getMessage()));
                    }

                    return 0;
                }))))
                .then(Commands.literal("stop").then(Commands.literal("thread").then(Commands.argument("threadName", StringArgumentType.string()).executes(arguments -> {
                    try {
                        if (!threadsMap.containsKey(arguments.getArgument("threadName", String.class)))
                            throw new CommandException(ITextComponent.getTextComponentOrEmpty("Thread named '"+arguments.getArgument("threadName", String.class)+"' doesn't exist"));
                        threadsMap.get(arguments.getArgument("threadName", String.class)).stop();
                        threadsMap.remove(arguments.getArgument("threadName", String.class));
                        ((PlayerEntity) Objects.requireNonNull(arguments.getSource().getEntity())).sendStatusMessage(new StringTextComponent("Thread '"+arguments.getArgument("threadName", String.class)+"' has been terminated"), (false));
                    } catch (ConcurrentModificationException ignored) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 0;
                }))).then(Commands.literal("all").executes(arguments -> {
                    try {
                        threadsMap.forEach((key, thread) -> {
                            thread.stop();
                            ((PlayerEntity) Objects.requireNonNull(arguments.getSource().getEntity())).sendStatusMessage(new StringTextComponent("Thread '"+key+"' has been terminated"), (false));
                            threadsMap.remove(key);
                        });
                    } catch (ConcurrentModificationException ignored) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 0;
                }))).then(Commands.literal("list").executes(arguments -> {
                    if (threadsMap.size() == 0) {
                        ((PlayerEntity) Objects.requireNonNull(arguments.getSource().getEntity())).sendStatusMessage(new StringTextComponent("No threads are running"), (false));
                    } else {
                        threadsMap.forEach((key, thread) -> {
                            ((PlayerEntity) Objects.requireNonNull(arguments.getSource().getEntity())).sendStatusMessage(new StringTextComponent(key), (false));
                        });
                    }
                    return 0;
                })));
    }
    private class ManagedThread extends Thread {
        private final String key;

        public ManagedThread(String key, Runnable task) {
            super(task);
            this.key = key;
        }

        @Override
        public void run() {
            super.run();
            threadsMap.remove(key);
        }
    }
}
