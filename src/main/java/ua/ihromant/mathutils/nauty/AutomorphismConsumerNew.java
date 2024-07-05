package ua.ihromant.mathutils.nauty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

public class AutomorphismConsumerNew implements NodeChecker {
    private final Consumer<int[]> autConsumer;
    private List<long[]> certs = new ArrayList<>();
    private int[] permutation;

    public AutomorphismConsumerNew(GraphWrapper graph, Consumer<int[]> autConsumer) {
        this.autConsumer = autConsumer;
    }

    @Override
    public boolean check(Partition partition, List<long[]> path) {
        boolean discrete = partition.isDiscrete();
        if (certs.size() < path.size()) {
            certs = path;
        } else {
            int idx = path.size() - 1;
            long[] curr = certs.get(idx);
            long[] cand = path.get(idx);
            if (!Arrays.equals(curr, cand)) {
                return false;
            }
        }
        if (discrete) {
            if (permutation == null) {
                permutation = partition.permutation();
            }
            int[] reverse = partition.reverse();
            int[] res = new int[reverse.length];
            for (int i = 0; i < reverse.length; i++) {
                res[i] = reverse[permutation[i]];
            }
            autConsumer.accept(res);
        }
        return !discrete;
    }

    @Override
    public BitSet filter(int lvl, PartitionFragment[] arr) {
        BitSet result = new BitSet(arr.length);
        if (certs.size() == lvl) {
            certs.add(arr[0].fragment());
        }
        long[] last = certs.get(lvl);
        for (int i = 0; i < arr.length; i++) {
            long[] fragment = arr[i].fragment();
            if (!Arrays.equals(last, fragment)) {
                continue;
            }
            Partition partition = arr[i].partition();
            if (partition.isDiscrete()) {
                if (permutation == null) {
                    permutation = partition.permutation();
                }
                int[] reverse = partition.reverse();
                int[] res = new int[reverse.length];
                for (int j = 0; j < reverse.length; j++) {
                    res[j] = reverse[permutation[j]];
                }
                autConsumer.accept(res);
            } else {
                result.set(i);
            }
        }
        return result;
    }
}
