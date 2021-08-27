package com.afterfinal.android.permissions;

public enum TipMode {
    None(0),
    Toast(1),
    Dialog(2);

    private final int value;

    TipMode(int value) {
        this.value = value;
    }

    public static TipMode parse(int v) {
        for (TipMode value : values()) {
            if(value.value == v){
                return value;
            }
        }
        return None;
    }

    public int getValue() {
        return value;
    }
}
