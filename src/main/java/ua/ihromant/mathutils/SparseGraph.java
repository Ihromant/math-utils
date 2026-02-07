package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SparseGraph {
    private final List<Set<Integer>> neighbors = new ArrayList<>();

    public void connect(int a, int b) {
        while (neighbors.size() <= Math.max(a, b)) {
            neighbors.add(new HashSet<>());
        }
        neighbors.get(a).add(b);
        neighbors.get(b).add(a);
    }

    public void disconnect(int a, int b) {
        neighbors.get(a).remove(b);
        neighbors.get(b).remove(a);
    }

    public boolean connected(int a, int b) {
        return neighbors.get(a).contains(b);
    }

    public int size() {
        return neighbors.size();
    }

    public List<FixBS> components() {
        QuickFind qf = new QuickFind(size());
        for (int i = 0; i < size(); i++) {
            for (int j : neighbors.get(i)) {
                qf.union(i, j);
            }
        }
        return qf.components();
    }
}
