package net.minecraft.luascripts.luaenvironment;

import net.minecraft.util.math.BlockPos;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public class ClassParser {

    public static int parseInt(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'number' expected");
            }
            return argument.getValue().toint();
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (int) argument.getDefaultValue();
            else
                throw e;
        }
    }

    public static boolean parseBoolean(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().isboolean()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'boolean' expected");
            }
            return argument.getValue().toboolean();
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (boolean) argument.getDefaultValue();
            else
                throw e;
        }
    }

    public static String parseString(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().isstring()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'string' expected");
            }
            return argument.getValue().tojstring();
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (String) argument.getDefaultValue();
            else
                throw e;
        }
    }

    public static float parseFloat(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'number' expected");
            }
            return argument.getValue().tofloat();
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (float) argument.getDefaultValue();
            else
                throw e;
        }
    }

    public static double parseDouble(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'number' expected");
            }
            return argument.getValue().todouble();
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (double) argument.getDefaultValue();
            else
                throw e;
        }
    }

    public static BlockPos parseBlockPos(Argument argument) throws LuaError {
        try {
            if (!argument.getValue().istable()) {
                throw new LuaError("argument #"+argument.getArgumentId()+" type is '"+argument.getValue().typename()+"', 'table' expected");
            }
            if (!argument.getValue().get("x").isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+".x type is '"+argument.getValue().get("x").typename()+"', 'number' expected");
            }
            if (!argument.getValue().get("y").isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+".y type is '"+argument.getValue().get("y").typename()+"', 'number' expected");
            }
            if (!argument.getValue().get("z").isnumber()) {
                throw new LuaError("argument #"+argument.getArgumentId()+".z type is '"+argument.getValue().get("z").typename()+"', 'number' expected");
            }

            return new BlockPos(argument.getValue().get("x").toint(), argument.getValue().get("y").toint(), argument.getValue().get("z").toint());
        } catch (Exception e) {
            if (argument.hasDefaultValue())
                return (BlockPos) argument.getDefaultValue();
            else
                throw e;
        }
    }

}
