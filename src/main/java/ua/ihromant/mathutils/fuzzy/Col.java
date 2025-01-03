package ua.ihromant.mathutils.fuzzy;

public record Col(int f, int s, int t) implements Rel {
    public Col {
        int min;
        int av;
        int max;
        if (f > s) {
            if (s > t) {
                min = t;
                av = s;
                max = f;
            } else { // s <= t
                if (f > t) {
                    min = s;
                    av = t;
                    max = f;
                } else {
                    min = s;
                    av = f;
                    max = t;
                }
            }
        } else { // f <= s
            if (s > t) {
                if (f < t) {
                    min = f;
                    av = t;
                    max = s;
                } else {
                    min = t;
                    av = f;
                    max = s;
                }
            } else { // s <= t
                min = f;
                av = s;
                max = t;
            }
        }
        f = min;
        s = av;
        t = max;
    }

    @Override
    public Rel opposite() {
        return new Trg(f, s, t);
    }
}
