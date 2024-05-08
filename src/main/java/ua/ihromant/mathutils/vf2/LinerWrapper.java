package ua.ihromant.mathutils.vf2;

import ua.ihromant.mathutils.Liner;

public record LinerWrapper(Liner liner) implements Graph {
    @Override
    public boolean contains(int from, int to) {
        if (from < liner.pointCount()) {
            return to >= liner.pointCount() && liner.flag(to - liner.pointCount(), from);
        } else {
            return to < liner.pointCount() && liner.flag(from - liner.pointCount(), to);
        }
    }

    @Override
    public int[] getNeighbors(int vertex) {
        if (vertex < liner.pointCount()) {
            int[] beams = liner.lines(vertex).clone();
            for (int i = 0; i < beams.length; i++) {
                beams[i] = beams[i] + liner.pointCount();
            }
            return beams;
        } else {
            return liner.line(vertex - liner.pointCount());
        }
    }

    @Override
    public int order() {
        return liner.pointCount() + liner.lineCount();
    }
}
