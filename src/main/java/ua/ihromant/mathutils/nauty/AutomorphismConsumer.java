package ua.ihromant.mathutils.nauty;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.function.Consumer;

public class AutomorphismConsumer implements Consumer<Partition> {
    private final NautyGraph graph;
    private final Consumer<int[]> autConsumer;
    private FixBS cert;
    private int[] permutation;

    public AutomorphismConsumer(NautyGraph graph, Consumer<int[]> autConsumer) {
        this.graph = graph;
        this.autConsumer = autConsumer;
    }

    @Override
    public void accept(Partition partition) {
        FixBS currCert = partition.permutedIncidence(graph);
        if (cert == null) {
            cert = currCert;
            permutation = partition.permutation();
        }
        if (Arrays.equals(cert.words(), currCert.words())) {
            int[] reverse = partition.reverse();
            int[] res = new int[reverse.length];
            for (int i = 0; i < reverse.length; i++) {
                res[i] = reverse[permutation[i]];
            }
            autConsumer.accept(res);
        }
    }
}
