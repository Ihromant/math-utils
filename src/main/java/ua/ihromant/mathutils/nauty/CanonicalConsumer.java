package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.util.FixBS;

import java.util.function.Consumer;

public class CanonicalConsumer implements Consumer<Partition> {
    private final GraphWrapper graph;
    private FixBS cert;

    public CanonicalConsumer(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public void accept(Partition partition) {
        FixBS permuted = graph.permutedIncidence(partition);
        if (more(permuted)) {
            cert = permuted;
        }
    }

    private boolean more(FixBS candidate) {
        if (cert == null) {
            return true;
        }
        return candidate.compareTo(cert) < 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        for (int i = 0; i < graph.lineCount(); i++) {
            int row = pc * i;
            for (int j = 0; j < pc; j++) {
                int idx = row + j;
                builder.append(cert.get(idx) ? '1' : '0');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public FixBS canonicalForm() {
        return cert;
    }
}

