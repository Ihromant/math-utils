package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.util.FixBS;

import java.util.List;

public class CanonicalConsumerNew implements NodeChecker {
    private final GraphWrapper graph;
    private List<FixBS> certs = List.of();

    public CanonicalConsumerNew(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public boolean check(Partition partition, List<FixBS> path) {
        boolean discrete = partition.isDiscrete();
        if (certs.size() < path.size()) {
            certs = path;
            return !discrete;
        }
        int idx = path.size() - 1;
        FixBS curr = certs.get(idx);
        FixBS cand = path.get(idx);
        int cmp = curr.compareTo(cand);
        if (cmp > 0) {
            certs = path;
            return !discrete;
        }
        return cmp == 0 && !discrete;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        FixBS cert = certs.getLast();
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
        return certs.getLast();
    }
}
