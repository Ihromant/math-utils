package ua.ihromant.mathutils.nauty;

import java.util.function.Consumer;

public class NautyAlgo {
    public static void search(GraphWrapper graph, Consumer<Partition> partitionConsumer) {
        Partition partition = graph.partition();
        partition.refine(graph, partition.subPartition());
        search(graph, partition, new int[0], partitionConsumer);
    }

    public static void search(GraphWrapper graph, Partition partition, int[] arr, Consumer<Partition> partitionConsumer) {
        if (partition.isDiscrete()) {
            partitionConsumer.accept(partition);
            return;
        }
        int[] smallest = partition.smallestNonTrivial();
        for (int v : smallest) {
            Partition next = partition.ort(graph, v);
            search(graph, next, addNext(arr, v), partitionConsumer);
        }
    }

    private static int[] addNext(int[] arr, int next) {
        int[] result = new int[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[arr.length] = next;
        return result;
    }
}
