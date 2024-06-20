package ua.ihromant.mathutils.nauty;

import java.util.BitSet;
import java.util.function.Consumer;

public class CanonicalConsumer implements Consumer<CellStack> {
    private final GraphWrapper graph;
    private BitSet cert;

    public CanonicalConsumer(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public void accept(CellStack partition) {
        BitSet permuted = graph.permutedIncidence(partition);
        if (less(permuted)) {
            cert = permuted;
        }
    }

    private boolean less(BitSet candidate) {
        if (cert == null) {
            return true;
        }
        int len = graph.pointCount() * graph.lineCount();
        for (int i = 0; i < len; i++) {
            if (candidate.get(i) && !cert.get(i)) {
                return true;
            }
            if (cert.get(i) && !candidate.get(i)) {
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
            int idx = pc * i;
            for (int j = 0; j < pc; j++) {
                builder.append(cert.get(idx + j) ? '1' : '0');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public BitSet canonicalForm() {
        return cert;
    }
}
