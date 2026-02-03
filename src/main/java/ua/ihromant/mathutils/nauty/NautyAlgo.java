package ua.ihromant.mathutils.nauty;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.function.Consumer;

public class NautyAlgo {
    public static void search(NautyGraph graph, Consumer<Partition> partitionConsumer) {
        Partition partition = Partition.partition(graph);
        partition.refine(graph, partition.subPartition(), new FixBS(graph.vCount()));
        search(graph, partition, partitionConsumer);
    }

    public static void search(NautyGraph graph, Partition partition, Consumer<Partition> partitionConsumer) {
        if (partition.isDiscrete()) {
            partitionConsumer.accept(partition);
            return;
        }
        int smallestIdx = partition.largestNonTrivial();
        int[] cell = partition.cellByIdx(smallestIdx);
        for (int sh = 0; sh < cell.length; sh++) {
            Partition next = new Partition(partition);
            next.ort(graph, smallestIdx, sh);
            search(graph, next, partitionConsumer);
        }
    }
}

