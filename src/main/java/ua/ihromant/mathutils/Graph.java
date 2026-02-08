package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
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

    public int[] adjacent(int a) {
        return neighbors[a].toArray();
    }

    public boolean connected(int a, int b) {
        return neighbors[a].get(b);
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
            FixBS r1 = r.copy();
            r1.set(v);
            FixBS adj = neighbors[v];
            bronKerbPivot(r1, p.intersection(adj), x.intersection(adj), sz + 1, cons);
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

    public void altMaxCliques(BiConsumer<FixBS, Integer> cons) {
        SearchContext ctx = new SearchContext(neighbors.length);
        int[] table = new int[neighbors.length];
        for (int i = 0; i < neighbors.length; i++) {
            table[i] = i;
        }
        search(ctx, table, neighbors.length, cons);
    }

    private static class SearchContext {
        private final FixBS currCl;
        private int clSize;
        private final int[][] tmpList;
        private int tmpCount = 0;

        private SearchContext(int sz) {
            this.currCl = new FixBS(sz);
            this.tmpList = new int[sz + 2][];
        }

        private int[] tmpArr(int n) {
            return tmpCount > 0 ? tmpList[--tmpCount] : new int[n];
        }

        private void putArr(int[] arr) {
            tmpList[tmpCount++] = arr;
        }
    }

    private void search(SearchContext ctx, int[] table, int size, BiConsumer<FixBS, Integer> cons) {
        if (size == 0) {
            cons.accept(ctx.currCl, ctx.clSize);
            return;
        }

        int[] newArr = ctx.tmpArr(neighbors.length);

        for (int i = size - 1; i >= 0; i--) {
            int v = table[i];

            int p = 0;
            for (int j = 0; j < i; j++) {
                if (connected(v, table[j])) {
                    newArr[p++] = table[j];
                }
            }

            ctx.currCl.set(v);
            ctx.clSize++;
            search(ctx, newArr, p, cons);
            ctx.currCl.clear(v);
            ctx.clSize--;
        }

        ctx.putArr(newArr);
    }
}
