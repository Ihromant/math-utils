package ua.ihromant.mathutils.fuzzy;

public record Col(int f, int s, int t) implements Rel {
    public static Col ordered(int f, int s, int t) {
        if (f > s) {
            if (s > t) {
                return new Col(t, s, f);
            } else { // t > s
                if (t > f) {
                    return new Col(s, f, t);
                } else { // f > t
                    return new Col(s, t, f);
                }
            }
        } else { // s > f
            if (f > t) {
                return new Col(t, f, s);
            } else { // t > f
                if (s > t) {
                    return new Col(f, t, s);
                } else { // t > s
                    return new Col(f, s, t);
                }
            }
        }
    }

    public Col ordered() {
        return ordered(f, s, t);
    }
}
