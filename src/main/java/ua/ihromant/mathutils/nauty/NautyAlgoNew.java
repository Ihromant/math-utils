package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;

public class NautyAlgoNew {
    public static void search(GraphWrapper graph, NodeChecker checker) {
        Partition partition = Partition.partition(graph);
        FixBS singulars = new FixBS(graph.size());
        partition.refine(graph, partition.subPartition(), singulars);
        FixBS fragment = fragment(graph, singulars, partition.permutation());
        List<FixBS> path = new ArrayList<>();
        path.add(fragment);
        if (checker.check(partition, path)) {
            search(graph, partition, path, checker);
        }
    }

    public static void search(GraphWrapper graph, Partition partition, NodeChecker checker) {
        FixBS singulars = partition.singulars();
        partition.refine(graph, partition.subPartition(), singulars);
        FixBS fragment = fragment(graph, singulars, partition.permutation());
        List<FixBS> path = new ArrayList<>();
        path.add(fragment);
        if (checker.check(partition, path)) {
            search(graph, partition, path, checker);
        }
    }

    public static void search(GraphWrapper graph, Partition partition, List<FixBS> path, NodeChecker checker) {
        int smallestIdx = partition.firstNonTrivial();
        int[] cell = partition.cellByIdx(smallestIdx);
        for (int sh = 0; sh < cell.length; sh++) {
            Partition next = new Partition(partition);
            FixBS singulars = next.ort(graph, smallestIdx, sh);
            FixBS fragment = or(fragment(graph, singulars, next.permutation()), path.getLast());
            List<FixBS> newPath = new ArrayList<>(path);
            newPath.add(fragment);
            if (checker.check(next, newPath)) {
                search(graph, next, newPath, checker);
            }
        }
    }

    private static FixBS or(FixBS to, FixBS from) {
        to.or(from);
        return to;
    }

    private static FixBS fragment(GraphWrapper graph, FixBS singulars, int[] permutation) {
        int vc = graph.size();
        FixBS res = new FixBS(vc * vc);
        for (int u = singulars.nextSetBit(0); u >= 0; u = singulars.nextSetBit(u + 1)) {
            int ut = permutation[u];
            for (int v = 0; v < vc; v++) {
                if (graph.edge(u, v)) {
                    int vt = permutation[v];
                    res.set(ut * vc + vt);
                }
            }
        }
        return res;
    }
}
