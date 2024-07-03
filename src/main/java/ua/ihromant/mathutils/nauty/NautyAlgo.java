package ua.ihromant.mathutils.nauty;

import java.util.function.Consumer;

public class NautyAlgo {
    public static void search(GraphWrapper graph, Consumer<Partition> partitionConsumer) {
        Partition partition = graph.partition();
        partition.refine(graph, partition.subPartition());
        search(graph, partition, partitionConsumer);
    }

    public static void search(GraphWrapper graph, Partition partition, Consumer<Partition> partitionConsumer) {
        if (partition.isDiscrete()) {
            partitionConsumer.accept(partition);
            return;
        }
        int smallestIdx = partition.largestNonTrivial();
        int[] cell = partition.cellByIdx(smallestIdx);
        for (int sh = 0; sh < cell.length; sh++) {
            Partition next = partition.ort(graph, smallestIdx, sh);
            search(graph, next, partitionConsumer);
        }
    }
}
