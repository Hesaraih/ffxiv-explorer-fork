package com.fragmenterworks.ffxivextract.models;

public class Vector3 {
    private final float x;
    private final float y;
    private final float z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%f, %f, %f", x, y, z);
    }

}
