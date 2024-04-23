package ua.ihromant.mathutils;

public record Pair(int a, int b) {
    public Pair(int a, int b) {
        this.a = Math.min(a, b);
        this.b = Math.max(a, b);
    }
}