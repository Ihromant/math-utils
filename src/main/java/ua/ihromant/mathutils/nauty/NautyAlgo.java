package ua.ihromant.mathutils.nauty;

import java.util.function.Consumer;

public class NautyAlgo {
    public static void search(GraphWrapper graph, Consumer<CellStack> partitionConsumer) {
        CellStack partition = graph.partition();
        partition.refine(graph, new CellStack(partition));
        search(graph, partition, partitionConsumer);
    }

    public static void search(GraphWrapper graph, CellStack partition, Consumer<CellStack> partitionConsumer) {
        if (partition.isDiscrete()) {
            partitionConsumer.accept(partition);
            return;
        }
        int[] smallest = partition.smallestNonTrivial();
        for (int v : smallest) {
            CellStack next = partition.ort(graph, v);
            search(graph, next, partitionConsumer);
        }
    }
}
