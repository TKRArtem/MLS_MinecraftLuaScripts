package net.minecraft.luascripts.luaenvironment;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaEnvironment {

    public static Globals setupLuaEnvironment(MinecraftServer server) {
        Globals globals = JsePlatform.standardGlobals();

        globals.set("server", MinecraftLuaParser.parseServer(server));

        return globals;
    }

    public static Globals setupLuaEnvironment(CommandContext<CommandSource> arguments) {
        Globals globals = JsePlatform.standardGlobals();

        globals.set("server", MinecraftLuaParser.parseServer(arguments.getSource().getServer()));
        globals.set("position", MinecraftLuaParser.parseBlockPos(new BlockPos(
                (int) arguments.getSource().getPos().getX(),
                (int) arguments.getSource().getPos().getY(),
                (int) arguments.getSource().getPos().getZ()
        )));
        globals.set("world", MinecraftLuaParser.parseWorld(arguments.getSource().getWorld()));
        globals.set("entity", MinecraftLuaParser.parseEntity(arguments.getSource().getEntity()));

        globals.set("BlockPos", getBlockPosConstructor());

        return globals;
    }

    private static LuaValue getBlockPosConstructor() {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int x = ClassParser.parseInt(new Argument(args.arg(1), 1));
                int y = ClassParser.parseInt(new Argument(args.arg(2), 2));
                int z = ClassParser.parseInt(new Argument(args.arg(3), 3));
                LuaTable blockPos = new LuaTable();
                blockPos.set("x", x);
                blockPos.set("y", y);
                blockPos.set("z", z);
                return blockPos;
            }
        };
    }
}