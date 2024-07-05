package ua.ihromant.mathutils.nauty;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CanonicalConsumerNew implements NodeChecker {
    private final GraphWrapper graph;
    private List<long[]> certs = new ArrayList<>();

    public CanonicalConsumerNew(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public boolean check(Partition partition, List<long[]> path) {
        boolean discrete = partition.isDiscrete();
        if (certs.size() < path.size()) {
            certs = path;
            return !discrete;
        }
        int idx = path.size() - 1;
        long[] curr = certs.get(idx);
        long[] cand = path.get(idx);
        int cmp = compare(curr, cand);
        if (cmp == 1) {
            certs = path;
            return !discrete;
        }
        return cmp == 0 && !discrete;
    }

    @Override
    public BitSet filter(int lvl, PartitionFragment[] fragments) {
        BitSet result = new BitSet(fragments.length);
        long[] minimal = certs.size() == lvl ? null : certs.get(lvl);
        for (int i = 0; i < fragments.length; i++) {
            long[] fragment = fragments[i].fragment();
            int cmp = compare(minimal, fragment);
            if (cmp == 1) {
                result.clear();
                minimal = fragment;
                result.set(i, !fragments[i].partition().isDiscrete());
            }
            if (cmp == 0) {
                result.set(i, !fragments[i].partition().isDiscrete());
            }
        }
        if (certs.size() == lvl) {
            certs.add(minimal);
        } else {
            certs.set(lvl, minimal);
        }
        return result;
    }

    private int compare(long[] curr, long[] candidate) {
        if (curr == null) {
            return 1;
        }
        for (int i = 0; i < curr.length; i++) {
            int cmp = Long.compareUnsigned(candidate[i], curr[i]);
            if (cmp > 0) {
                return 1;
            }
            if (cmp < 0) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        long[] cert = certs.getLast();
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
        return BitSet.valueOf(certs.getLast());
    }
}
