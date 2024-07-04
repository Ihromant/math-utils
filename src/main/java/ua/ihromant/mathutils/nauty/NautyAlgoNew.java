package ua.ihromant.mathutils.nauty;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class NautyAlgoNew {
    public static void search(GraphWrapper graph, NodeChecker checker) {
        Partition partition = graph.partition();
        BitSet singulars = new BitSet(graph.size());
        partition.refine(graph, partition.subPartition(), singulars);
        long[] fragment = graph.fragment(singulars, partition.permutation());
        List<long[]> path = new ArrayList<>();
        path.add(fragment);
        if (checker.check(partition, path)) {
            search(graph, partition, path, checker);
        }
    }

    public static void search(GraphWrapper graph, Partition partition, NodeChecker checker) {
        BitSet singulars = partition.singulars();
        partition.refine(graph, partition.subPartition(), singulars);
        long[] fragment = graph.fragment(singulars, partition.permutation());
        List<long[]> path = new ArrayList<>();
        path.add(fragment);
        if (checker.check(partition, path)) {
            search(graph, partition, path, checker);
        }
    }

    public static void search(GraphWrapper graph, Partition partition, List<long[]> path, NodeChecker checker) {
        int smallestIdx = partition.firstNonTrivial();
        int[] cell = partition.cellByIdx(smallestIdx);
        for (int sh = 0; sh < cell.length; sh++) {
            Partition next = new Partition(partition);
            BitSet singulars = next.ort(graph, smallestIdx, sh);
            long[] fragment = or(graph.fragment(singulars, next.permutation()), path.getLast());
            List<long[]> newPath = new ArrayList<>(path);
            newPath.add(fragment);
            if (checker.check(next, newPath)) {
                search(graph, next, newPath, checker);
            }
        }
    }

    private static long[] or(long[] to, long[] from) {
        for (int i = 0; i < to.length; i++) {
            to[i] |= from[i];
        }
        return to;
    }
}
