package ua.ihromant.mathutils;

public record Pair(int f, int s) {
    public Pair {
        if (f > s) {
            int min = s;
            s = f;
            f = min;
        }
    }
}