package ua.ihromant.mathutils.nauty;

import java.util.function.BiPredicate;

public class NautyAlgo {
    public static void search(GraphWrapper graph, BiPredicate<Partition, int[]> partitionConsumer) {
        Partition partition = graph.partition();
        partition.refine(graph, partition.subPartition());
        search(graph, partition, new int[0], partitionConsumer);
    }

    public static void search(GraphWrapper graph, Partition partition, int[] arr, BiPredicate<Partition, int[]> partitionPruner) {
        int[] smallest = partition.smallestNonTrivial();
        for (int v : smallest) {
            Partition next = partition.ort(graph, v);
            int[] nextArr = addNext(arr, v);
            if (partitionPruner.test(next, nextArr)) {
                search(graph, next, addNext(arr, v), partitionPruner);
            }
        }
    }

    private static int[] addNext(int[] arr, int next) {
        int[] result = new int[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[arr.length] = next;
        return result;
    }
}
