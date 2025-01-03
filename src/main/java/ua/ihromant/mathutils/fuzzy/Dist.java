package ua.ihromant.mathutils.fuzzy;

public record Dist(int f, int s) implements Rel {
    public Dist {
        boolean b = f > s;
        int min = b ? s : f;
        int max = b ? f : s;
        f = min;
        s = max;
    }

    @Override
    public Rel opposite() {
        return new Same(f, s);
    }
}
