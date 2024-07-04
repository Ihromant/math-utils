package ua.ihromant.mathutils.nauty;

import java.util.BitSet;
import java.util.function.Consumer;

public class CanonicalConsumer implements Consumer<Partition> {
    private final GraphWrapper graph;
    private long[] cert;

    public CanonicalConsumer(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public void accept(Partition partition) {
        long[] permuted = graph.permutedIncidence(partition);
        if (more(permuted)) {
            cert = permuted;
        }
    }

    private boolean more(long[] candidate) {
        if (cert == null) {
            return true;
        }
        for (int i = 0; i < cert.length; i++) {
            int cmp = Long.compareUnsigned(candidate[i], cert[i]);
            if (cmp > 0) {
                return true;
            }
            if (cmp < 0) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        for (int i = 0; i < graph.lineCount(); i++) {
            int row = pc * i;
            for (int j = 0; j < pc; j++) {
                int idx = row + j;
                builder.append((cert[idx >> 6] & (1L << idx)) != 0 ? '1' : '0');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public BitSet canonicalForm() {
        return BitSet.valueOf(cert);
    }
}

