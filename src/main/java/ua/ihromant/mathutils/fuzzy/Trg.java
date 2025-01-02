package ua.ihromant.mathutils.fuzzy;

public record Trg(int f, int s, int t) implements Rel {
    public static Trg ordered(int f, int s, int t) {
        if (f > s) {
            if (s > t) {
                return new Trg(t, s, f);
            } else { // t > s
                if (t > f) {
                    return new Trg(s, f, t);
                } else { // f > t
                    return new Trg(s, t, f);
                }
            }
        } else { // s > f
            if (f > t) {
                return new Trg(t, f, s);
            } else { // t > f
                if (s > t) {
                    return new Trg(f, t, s);
                } else { // t > s
                    return new Trg(f, s, t);
                }
            }
        }
    }

    public Trg ordered() {
        return ordered(f, s, t);
    }
}
