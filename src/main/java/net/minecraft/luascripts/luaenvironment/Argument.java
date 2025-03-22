package net.minecraft.luascripts.luaenvironment;

import org.luaj.vm2.LuaValue;

public class Argument {
    private final LuaValue value;
    private final int argumentId;
    private final Object _default;
    private final boolean _defaultvalue;

    public Argument(LuaValue value, int argumentId) {
        this.value = value;
        this.argumentId = argumentId;
        _default = null;
        _defaultvalue = false;
    }
    public Argument(LuaValue value, int argumentId, Object _default) {
        this.value = value;
        this.argumentId = argumentId;
        this._default = _default;
        _defaultvalue = true;
    }

    public LuaValue getValue() {
        return value;
    }

    public int getArgumentId() {
        return argumentId;
    }

    public Object getDefaultValue() {
        return _default;
    }

    public boolean hasDefaultValue() {
        return _defaultvalue;
    }
}

