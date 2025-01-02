package ua.ihromant.mathutils.fuzzy;

public record Dist(int f, int s) implements Rel {
    public static Dist ordered(int f, int s) {
        if (f > s) {
            return new Dist(s, f);
        } else {
            return new Dist(f, s);
        }
    }

    public Dist ordered() {
        return ordered(f, s);
    }
}
