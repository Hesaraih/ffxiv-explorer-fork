package com.fragmenterworks.ffxivextract.models;

public class Vector2 {
    private final float x;
    private final float y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("%f, %f", x, y);
    }

}
