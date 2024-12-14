package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class QuickFind {
    private final int[] nodes;
    private final int[] sz;

    public QuickFind(int size) {
        this.nodes = IntStream.range(0, size).toArray();
        this.sz = IntStream.range(0, size).map(i -> 1).toArray();
    }

    public int size() {
        return nodes.length;
    }

    public void union(int p, int q) {
        int pRoot = root(p);
        int qRoot = root(q);

        if (pRoot == qRoot) {
            return;
        }

        if (sz[pRoot] < sz[qRoot]) {
            nodes[pRoot] = qRoot;
            sz[qRoot] += sz[pRoot];
        } else {
            nodes[qRoot] = pRoot;
            sz[pRoot] += sz[qRoot];
        }
    }

    public boolean connected(int p, int q) {
        return root(p) == root(q);
    }

    public int root(int p) {
        while (p != nodes[p]) {
            nodes[p] = nodes[nodes[p]];
            p = nodes[p];
        }
        return p;
    }

    public List<FixBS> components() {
        Map<Integer, FixBS> map = new HashMap<>();
        for (int i = 0; i < size(); i++) {
            map.computeIfAbsent(root(i), k -> new FixBS(size())).set(i);
        }
        return map.values().stream().sorted(Comparator.comparingInt(bs -> bs.nextSetBit(0))).toList();
    }
}
