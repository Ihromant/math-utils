package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

public record FixInc(FixBS[] lines, int v) implements Inc {
    @Override
    public int b() {
        return lines.length;
    }

    @Override
    public boolean inc(int l, int pt) {
        return lines[l].get(pt);
    }

    @Override
    public void set(int l, int pt) {
        lines[l].set(pt);
    }

    @Override
    public Inc removeTwins() {
        return this; // TODO
    }

    @Override
    public Inc addLine(int[] line) {
        FixBS[] next = new FixBS[lines.length + 1];
        System.arraycopy(lines, 0, next, 0, lines.length);
        FixBS ln = new FixBS(v);
        for (int pt : line) {
            ln.set(pt);
        }
        next[lines.length] = ln;
        return new FixInc(next, v);
    }
}
