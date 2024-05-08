package ua.ihromant.mathutils.vf2;

import ua.ihromant.mathutils.Liner;

public class LinerWrapper implements Graph {
    private final int size;
    private final boolean[][] incidence;
    private final int[][] adjacency;

    public LinerWrapper(Liner liner) {
        this.size = liner.pointCount() + liner.lineCount();
        this.incidence = new boolean[size][size];
        this.adjacency = new int[size][];
        for (int p = 0; p < liner.pointCount(); p++) {
            int[] beam = liner.point(p);
            int[] adj = new int[beam.length];
            for (int i = 0; i < beam.length; i++) {
                int l = beam[i];
                adj[i] = l + liner.pointCount();
            }
            adjacency[p] = adj;
        }
        for (int l = 0; l < liner.lineCount(); l++) {
            int[] line = liner.line(l);
            int[] adj = new int[line.length];
            int lIdx = l + liner.pointCount();
            for (int i = 0; i < line.length; i++) {
                int p = line[i];
                adj[i] = p;
                incidence[p][lIdx] = true;
                incidence[lIdx][p] = true;
            }
            adjacency[lIdx] = adj;
        }
    }

    @Override
    public boolean contains(int from, int to) {
        return incidence[from][to];
    }

    @Override
    public int[] getNeighbors(int vertex) {
        return adjacency[vertex];
    }

    @Override
    public int order() {
        return size;
    }
}
