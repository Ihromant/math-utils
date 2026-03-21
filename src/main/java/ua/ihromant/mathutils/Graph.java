package ua.ihromant.mathutils;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

public class Graph implements NautyGraph {
    private final FixBS[] neighbors;

    public Graph(int size) {
        this.neighbors = IntStream.range(0, size).mapToObj(_ -> new FixBS(size)).toArray(FixBS[]::new);
    }

    public Graph(FixBS[] neighbors) {
        this.neighbors = neighbors;
    }

    public static <T> Graph by(T[] arr, BiPredicate<T, T> pr) {
        int len = arr.length;
        Graph res = new Graph(len);
        IntStream.range(0, len).parallel().forEach(i -> {
            for (int j = 0; j < len; j++) {
                if (pr.test(arr[i], arr[j])) {
                    res.neighbors[i].set(j);
                }
            }
        });
        return res;
    }

    public static <T> Graph by(List<T> arr, BiPredicate<T, T> pr) {
        int len = arr.size();
        Graph res = new Graph(len);
        IntStream.range(0, len).parallel().forEach(i -> {
            for (int j = 0; j < len; j++) {
                if (pr.test(arr.get(i), arr.get(j))) {
                    res.neighbors[i].set(j);
                }
            }
        });
        return res;
    }

    public static Graph by(int[] arr, IntBiPredicate pr) {
        int len = arr.length;
        Graph res = new Graph(len);
        IntStream.range(0, len).parallel().forEach(i -> {
            for (int j = 0; j < len; j++) {
                if (pr.test(arr[i], arr[j])) {
                    res.neighbors[i].set(j);
                }
            }
        });
        return res;
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

    public int size() {
        return neighbors.length;
    }

    public void bronKerb(FixBS r, FixBS p, FixBS x, int sz, BiConsumer<FixBS, Integer> cons) {
        int fst = p.nextSetBit(0);
        if (fst < 0 && x.isEmpty()) {
            cons.accept(r, sz);
            return;
        }
        FixBS p1 = p.copy();
        for (int v = fst; v >= 0; v = p.nextSetBit(v + 1)) {
            FixBS r1 = r.copy();
            r1.set(v);
            FixBS adj = neighbors[v];
            bronKerb(r1, p1.intersection(adj), x.intersection(adj), sz + 1, cons);
            p1.clear(v);
            x.set(v);
        }
    }

    public void bronKerbPivot(FixBS r, FixBS p, FixBS x, int sz, BiConsumer<FixBS, Integer> cons) {
        int pivot = choosePivot(p, x);
        if (pivot < 0) {
            cons.accept(r, sz);
            return;
        }
        FixBS sub = p.diff(neighbors[pivot]);
        for (int v = sub.nextSetBit(0); v >= 0; v = sub.nextSetBit(v + 1)) {
            FixBS adj = neighbors[v];
            r.set(v);
            bronKerbPivot(r, p.intersection(adj), x.intersection(adj), sz + 1, cons);
            r.clear(v);
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

    public void bronKerb(BiConsumer<FixBS, Integer> cons) {
        FixBS r = new FixBS(neighbors.length);
        FixBS p = new FixBS(neighbors.length);
        p.set(0, neighbors.length);
        FixBS x = new FixBS(neighbors.length);
        bronKerb(r, p, x, 0, cons);
    }

    public void bronKerbPivot(BiConsumer<FixBS, Integer> cons) {
        FixBS r = new FixBS(neighbors.length);
        FixBS p = new FixBS(neighbors.length);
        p.set(0, neighbors.length);
        FixBS x = new FixBS(neighbors.length);
        bronKerbPivot(r, p, x, 0, cons);
    }

    public void bronKerbPivotPar(BiConsumer<FixBS, Integer> cons) {
        int pivot = -1;
        int maxCnt = -1;
        for (int i = 0; i < neighbors.length; i++) {
            int card = neighbors[i].cardinality();
            if (card > maxCnt) {
                maxCnt = card;
                pivot = i;
            }
        }
        FixBS p = new FixBS(neighbors.length);
        p.set(0, neighbors.length);
        FixBS sub = p.diff(neighbors[pivot]);
        FixBS x = new FixBS(neighbors.length);
        List<FixBS[]> triples = new ArrayList<>();
        for (int v = sub.nextSetBit(0); v >= 0; v = sub.nextSetBit(v + 1)) {
            FixBS r = new FixBS(neighbors.length);
            r.set(v);
            FixBS adj = neighbors[v];
            triples.add(new FixBS[]{r, p.intersection(adj), x.intersection(adj)});
            p.clear(v);
            x.set(v);
        }
        triples.parallelStream().forEach(t -> bronKerbPivot(t[0], t[1], t[2], 1, cons));
    }

    @Override
    public int vCount() {
        return neighbors.length;
    }

    @Override
    public boolean edge(int i, int j) {
        return neighbors[i].get(j);
    }

    @Override
    public long[] neighborsArr(int i) {
        return neighbors[i].words();
    }

    @FunctionalInterface
    public interface IntBiPredicate {
        boolean test(int a, int b);
    }
}
