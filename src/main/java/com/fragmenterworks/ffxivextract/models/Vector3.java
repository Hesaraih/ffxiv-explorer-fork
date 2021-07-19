package com.fragmenterworks.ffxivextract.models;

public class Vector3 {
    public static Vector3 Zero = new Vector3(0,0,0);
    public static Vector3 One = new Vector3(1,1,1);
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
