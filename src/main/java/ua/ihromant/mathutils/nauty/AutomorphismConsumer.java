package ua.ihromant.mathutils.nauty;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class AutomorphismConsumer implements BiPredicate<Partition, int[]> {
    private final GraphWrapper graph;
    private final Consumer<int[]> autConsumer;
    private long[][] cert = new long[0][];
    private int[] permutation;

    public AutomorphismConsumer(GraphWrapper graph, Consumer<int[]> autConsumer) {
        this.graph = graph;
        this.autConsumer = autConsumer;
    }

    @Override
    public boolean test(Partition partition, int[] ints) {
        long[] nextCert = graph.permutedIncidence(partition);
        if (ints.length > cert.length) {
            recalculate(ints.length, nextCert);
            if (partition.isDiscrete() && permutation == null) {
                permutation = partition.shiftedPermutation();
            }
        }
        long[] currCert = cert[ints.length - 1];
        int cmp = graph.compareLex(currCert, nextCert);
        if (cmp != 0) {
            return false;
        }
        if (partition.isDiscrete()) {
            int[] reverse = partition.reverse();
            int[] res = new int[reverse.length];
            for (int i = 0; i < reverse.length; i++) {
                res[i] = reverse[permutation[i]];
            }
            autConsumer.accept(res);
            return false;
        } else {
            return true;
        }
    }

    private void recalculate(int newLen, long[] nextCert) {
        long[][] newCert = new long[newLen][];
        System.arraycopy(this.cert, 0, newCert, 0, newLen - 1);
        newCert[newLen - 1] = nextCert;
        this.cert = newCert;
    }
}
