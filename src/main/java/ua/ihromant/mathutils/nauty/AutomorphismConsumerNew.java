package ua.ihromant.mathutils.nauty;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AutomorphismConsumerNew implements NodeChecker {
    private final Consumer<int[]> autConsumer;
    private List<FixBS> certs = List.of();
    private int[] permutation;

    public AutomorphismConsumerNew(NautyGraph graph, Consumer<int[]> autConsumer) {
        this.autConsumer = autConsumer;
    }

    @Override
    public boolean check(Partition partition, List<FixBS> path) {
        boolean discrete = partition.isDiscrete();
        if (certs.size() < path.size()) {
            certs = path;
        } else {
            int idx = path.size() - 1;
            FixBS curr = certs.get(idx);
            FixBS cand = path.get(idx);
            if (!Arrays.equals(curr.words(), cand.words())) {
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
}
