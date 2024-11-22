package ua.ihromant.mathutils.plane;

public record Characteristic(int a1, int a2, int a3, int mul1, int mul2) {
    public static Characteristic simpleChr = new Characteristic(2, 2, 2, 1, 1);
}
