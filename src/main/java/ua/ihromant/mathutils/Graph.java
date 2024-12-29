package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class Graph {
    private final FixBS[] neighbors;

    public Graph(int size) {
        this.neighbors = IntStream.range(0, size).mapToObj(i -> new FixBS(size)).toArray(FixBS[]::new);
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

    public void bronKerb(IntList r, FixBS p, FixBS x, Consumer<IntList> cons) {
        int fst = p.nextSetBit(0);
        if (fst < 0 && x.isEmpty()) {
            cons.accept(r);
            return;
        }
        FixBS p1 = p.copy();
        for (int v = fst; v >= 0; v = p.nextSetBit(v + 1)) {
            IntList r1 = r.copy();
            r1.add(v);
            FixBS neighbors = neighbors(v);
            bronKerb(r1, p1.intersection(neighbors), x.intersection(neighbors), cons);
            p1.clear(v);
            x.set(v);
        }
    }

    public void bronKerbPivot(IntList r, FixBS p, FixBS x, Consumer<IntList> cons) {
        FixBS un = p.union(x);
        int fst = un.nextSetBit(0);
        if (fst < 0) {
            cons.accept(r);
            return;
        }
        FixBS p1 = p.copy();
        FixBS sub = p.diff(neighbors(fst));
        for (int v = fst; v >= 0; v = sub.nextSetBit(v + 1)) {
            IntList r1 = r.copy();
            r1.add(v);
            FixBS neighbors = neighbors(v);
            bronKerbPivot(r1, p1.intersection(neighbors), x.intersection(neighbors), cons);
            p1.clear(v);
            x.set(v);
        }
    }
}
