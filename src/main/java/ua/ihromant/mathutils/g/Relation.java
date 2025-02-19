package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

public class Relation {
    private final FixBS dom;
    private final FixBS rng;
    private final FixBS diffs;
    private final FixBS[] domMap;
    private final FixBS[] rngMap;

    public Relation(int v, FixBS comp) {
        this.dom = new FixBS(v);
        this.rng = new FixBS(v);
        this.domMap = new FixBS[v];
        this.rngMap = new FixBS[v];
        this.diffs = comp;
        for (int pair = comp.nextSetBit(0); pair >= 0; pair = comp.nextSetBit(pair + 1)) {
            int f = pair / v;
            int s = pair % v;
            acceptPair(v, f, s);
        }
    }

    public FixBS diffs() {
        return diffs;
    }

    private void acceptPair(int v, int f, int s) {
        if (domMap[f] == null) {
            domMap[f] = new FixBS(v);
        }
        if (rngMap[s] == null) {
            rngMap[s] = new FixBS(v);
        }
        dom.set(f);
        rng.set(s);
        domMap[f].set(s);
        rngMap[s].set(f);
    }

    public FixBS firstFor(int snd) {
        return rngMap[snd];
    }

    public FixBS secondFor(int fst) {
        return domMap[fst];
    }
}
