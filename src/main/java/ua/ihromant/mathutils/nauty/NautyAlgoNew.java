package ua.ihromant.mathutils.nauty;

import java.util.BitSet;
import java.util.stream.IntStream;

public class NautyAlgoNew {
    public static void search(GraphWrapper graph, NodeChecker checker) {
        Partition partition = graph.partition();
        BitSet singulars = new BitSet(graph.size());
        partition.refine(graph, partition.subPartition(), singulars);
        long[] fragment = graph.fragment(singulars, partition.permutation());
        PartitionFragment[] arr = new PartitionFragment[]{new PartitionFragment(partition, fragment)};
        BitSet filter = checker.filter(0, arr);
        for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
            search(graph, arr[i], 1, checker);
        }
    }

    public static void search(GraphWrapper graph, Partition partition, NodeChecker checker) {
        BitSet singulars = partition.singulars();
        partition.refine(graph, partition.subPartition(), singulars);
        long[] fragment = graph.fragment(singulars, partition.permutation());
        PartitionFragment[] arr = new PartitionFragment[]{new PartitionFragment(partition, fragment)};
        BitSet filter = checker.filter(0, arr);
        for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
            search(graph, arr[i], 1, checker);
        }
    }

    public static void search(GraphWrapper graph, PartitionFragment pf, int lvl, NodeChecker checker) {
        Partition partition = pf.partition();
        int smallestIdx = partition.largestNonTrivial();
        int[] cell = partition.cellByIdx(smallestIdx);
        PartitionFragment[] arr = IntStream.range(0, cell.length).mapToObj(sh -> {
            Partition nextPart = new Partition(partition);
            BitSet singulars = nextPart.ort(graph, smallestIdx, sh);
            long[] nextFragment = or(graph.fragment(singulars, nextPart.permutation()), pf.fragment());
            return new PartitionFragment(nextPart, nextFragment);
        }).toArray(PartitionFragment[]::new);
        BitSet filter = checker.filter(lvl, arr);
        for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
            search(graph, arr[i], lvl + 1, checker);
        }
    }

    private static long[] or(long[] to, long[] from) {
        for (int i = 0; i < to.length; i++) {
            to[i] |= from[i];
        }
        return to;
    }
}
