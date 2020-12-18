package com.afterfinal.android.permissions;

public enum TipMode {
    None(0),
    Toast(1),
    Dialog(2);

    private final int value;

    TipMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
