package ua.ihromant.mathutils.fuzzy;

public record Same(int f, int s) implements Rel {
    public Same {
        boolean b = f > s;
        int min = b ? s : f;
        int max = b ? f : s;
        f = min;
        s = max;
    }

    @Override
    public Rel opposite() {
        return new Dist(f, s);
    }
}
