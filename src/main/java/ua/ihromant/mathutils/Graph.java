package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class Graph {
    private final FixBS[] neighbors;

    public Graph(int size) {
        this.neighbors = IntStream.range(0, size).mapToObj(_ -> new FixBS(size)).toArray(FixBS[]::new);
    }

    public Graph(FixBS[] neighbors) {
        this.neighbors = neighbors;
    }

    public void connect(int a, int b) {
        neighbors[a].set(b);
        neighbors[b].set(a);
    }

    public void disconnect(int a, int b) {
        neighbors[a].clear(b);
        neighbors[b].clear(a);
    }

    public FixBS neighbors(int a) {
        return neighbors[a];
    }

    public boolean connected(int a, int b) {
        return neighbors[a].get(b);
    }

    public void bronKerb(FixBS r, FixBS p, FixBS x, Consumer<FixBS> cons) {
        int fst = p.nextSetBit(0);
        if (fst < 0 && x.isEmpty()) {
            cons.accept(r);
            return;
        }
        FixBS p1 = p.copy();
        for (int v = fst; v >= 0; v = p.nextSetBit(v + 1)) {
            FixBS r1 = r.copy();
            r1.set(v);
            FixBS neighbors = neighbors(v);
            bronKerb(r1, p1.intersection(neighbors), x.intersection(neighbors), cons);
            p1.clear(v);
            x.set(v);
        }
    }

    public void bronKerbPivot(FixBS r, FixBS p, FixBS x, Consumer<FixBS> cons) {
        int pivot = choosePivot(p, x);
        if (pivot < 0) {
            cons.accept(r);
            return;
        }
        FixBS sub = p.diff(neighbors(pivot));
        for (int v = sub.nextSetBit(0); v >= 0; v = sub.nextSetBit(v + 1)) {
            FixBS r1 = r.copy();
            r1.set(v);
            FixBS neighbors = neighbors(v);
            bronKerbPivot(r1, p.intersection(neighbors), x.intersection(neighbors), cons);
            p.clear(v);
            x.set(v);
        }
    }

    private int choosePivot(FixBS p, FixBS x) {
        FixBS union = p.union(x);
        int result = -1;
        int maxCnt = -1;

        for (int u = union.nextSetBit(0); u >= 0; u = union.nextSetBit(u + 1)) {
            int cnt = p.intersection(neighbors[u]).cardinality();
            if (cnt > maxCnt) {
                maxCnt = cnt;
                result = u;
            }
        }
        return result;
    }

    public void bronKerb(Consumer<FixBS> cons) {
        FixBS r = new FixBS(neighbors.length);
        FixBS p = new FixBS(neighbors.length);
        p.set(0, neighbors.length);
        FixBS x = new FixBS(neighbors.length);
        bronKerb(r, p, x, cons);
    }

    public void bronKerbPivot(Consumer<FixBS> cons) {
        FixBS r = new FixBS(neighbors.length);
        FixBS p = new FixBS(neighbors.length);
        p.set(0, neighbors.length);
        FixBS x = new FixBS(neighbors.length);
        bronKerbPivot(r, p, x, cons);
    }
}
