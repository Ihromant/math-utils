package ua.ihromant.mathutils.nauty;

import java.util.Arrays;
import java.util.function.Consumer;

public class AutomorphismConsumer implements Consumer<Partition> {
    private final GraphWrapper graph;
    private final Consumer<int[]> autConsumer;
    private long[] cert;
    private int[] permutation;

    public AutomorphismConsumer(GraphWrapper graph, Consumer<int[]> autConsumer) {
        this.graph = graph;
        this.autConsumer = autConsumer;
    }

    @Override
    public void accept(Partition partition) {
        long[] currCert = graph.permutedIncidence(partition);
        if (cert == null) {
            cert = currCert;
            permutation = partition.permutation();
        }
        if (Arrays.equals(cert, currCert)) {
            int[] reverse = partition.reverse();
            int[] res = new int[reverse.length];
            for (int i = 0; i < reverse.length; i++) {
                res[i] = reverse[permutation[i]];
            }
            autConsumer.accept(res);
        }
    }
}
