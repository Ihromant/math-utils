package ua.ihromant.mathutils.gomoku;

public record Range(int min, int max) {
    public int fit(int value) {
        return Math.max(min, Math.min(value, max));
    }

    public int idx(int value) {
        return value - min;
    }

    public boolean contains(int value) {
        return min <= value && max >= value;
    }
}
