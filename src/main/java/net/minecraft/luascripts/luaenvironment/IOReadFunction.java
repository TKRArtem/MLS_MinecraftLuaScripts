package net.minecraft.luascripts.luaenvironment;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IOReadFunction extends LuaFunction {
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public Varargs invoke(Varargs args) {
        try {
            LuaValue arg = args.arg1();
            if (arg.isnil()) {
                // Default behavior: Read a single line without the newline character
                return LuaValue.valueOf(reader.readLine());
            } else if (arg.isstring()) {
                String format = arg.tojstring();
                switch (format) {
                    case "*l": // Read a line, excluding the newline character
                        return LuaValue.valueOf(reader.readLine());
                    case "*L": // Read a line, including the newline character
                        return LuaValue.valueOf(reader.readLine() + "\n");
                    case "*a": // Read all input until EOF
                        StringBuilder allData = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            allData.append(line).append("\n");
                        }
                        return LuaValue.valueOf(allData.toString());
                    case "*n": // Read a number
                        String numberString = reader.readLine();
                        try {
                            return LuaValue.valueOf(Double.parseDouble(numberString.trim()));
                        } catch (NumberFormatException e) {
                            return LuaValue.NIL; // Return nil if not a valid number
                        }
                    default:
                        throw new IllegalArgumentException("Unsupported format: " + format);
                }
            } else {
                throw new IllegalArgumentException("Expected string or nil argument.");
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error during read: " + e.getMessage(), e);
        }
    }
}

