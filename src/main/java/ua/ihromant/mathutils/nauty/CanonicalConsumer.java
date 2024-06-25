package ua.ihromant.mathutils.nauty;

import java.util.BitSet;
import java.util.function.BiPredicate;

public class CanonicalConsumer implements BiPredicate<Partition, int[]> {
    private final GraphWrapper graph;
    private long[][] cert = new long[0][];

    public CanonicalConsumer(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public boolean test(Partition partition, int[] ints) {
        long[] nextCert = graph.permutedIncidence(partition);
        if (ints.length > cert.length) {
            recalculate(ints.length, nextCert);
            return !partition.isDiscrete();
        }
        long[] currCert = cert[ints.length - 1];
        int cmp = graph.compareLex(currCert, nextCert);
        if (cmp > 0) {
            return false;
        }
        if (cmp < 0) {
            recalculate(ints.length, nextCert);
        }
        return !partition.isDiscrete();
    }

    private void recalculate(int newLen, long[] nextCert) {
        long[][] newCert = new long[newLen][];
        System.arraycopy(this.cert, 0, newCert, 0, newLen - 1);
        newCert[newLen - 1] = nextCert;
        this.cert = newCert;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        long[] curr = cert[cert.length - 1];
        for (int i = 0; i < graph.lineCount(); i++) {
            int row = pc * i;
            for (int j = 0; j < pc; j++) {
                int idx = row + j;
                builder.append((curr[idx >> 6] & (1L << idx)) != 0 ? '1' : '0');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public BitSet canonicalForm() {
        return BitSet.valueOf(cert[cert.length - 1]);
    }
}
