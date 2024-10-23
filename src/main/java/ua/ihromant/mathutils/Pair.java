package ua.ihromant.mathutils;

public record Pair(int a, int b) {
    public static Pair of(int a, int b) {
        return new Pair(Math.min(a, b), Math.max(a, b));
    }
}