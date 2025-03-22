package net.minecraft.luascripts.luaenvironment;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.io.PrintStream;

public class IOWriteFunction extends LuaFunction {
    private final PrintStream out;

    public IOWriteFunction() {
        this(System.out); // Default to standard output
    }

    public IOWriteFunction(PrintStream out) {
        this.out = out;
    }

    @Override
    public Varargs invoke(Varargs args) {
        int n = args.narg();
        for (int i = 1; i <= n; i++) {
            LuaValue arg = args.arg(i);
            out.print(arg.tojstring()); // Convert LuaValue to string and write to output
        }
        return LuaValue.NONE; // io.write returns no values
    }
}
