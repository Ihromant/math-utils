package ua.ihromant.mathutils.fuzzy;

public record Same(int f, int s) implements Rel {
    public static Same ordered(int f, int s) {
        if (f > s) {
            return new Same(s, f);
        } else {
            return new Same(f, s);
        }
    }

    public Same ordered() {
        return ordered(f, s);
    }
}
