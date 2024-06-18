package ua.ihromant.mathutils.nauty;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CanonicalConsumer implements Consumer<CellStack> {
    private final GraphWrapper graph;
    private long[] arr;
    private final AtomicLong counter = new AtomicLong();

    public CanonicalConsumer(GraphWrapper graph) {
        this.graph = graph;
    }

    @Override
    public void accept(CellStack partition) {
        counter.incrementAndGet();
        long[] permuted = graph.permutedIncidence(partition);
        if (less(permuted)) {
            arr = permuted;
        }
    }

    private boolean less(long[] candidate) {
        if (arr == null) {
            return true;
        }
        for (int i = 0; i < arr.length; i++) {
            if (Long.compareUnsigned(arr[i], candidate[i]) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String val = Arrays.stream(arr).mapToObj(v -> {
            char[] chs = new char[64];
            Arrays.fill(chs, '0');
            char[] chars = Long.toBinaryString(v).toCharArray();
            System.arraycopy(chars, 0, chs, 0, chars.length);
            return new String(chs);
        }).collect(Collectors.joining());
        StringBuilder builder = new StringBuilder();
        int pc = graph.pointCount();
        for (int i = 0; i < graph.lineCount(); i++) {
            int idx = pc * i;
            builder.append(val, idx, idx + pc);
            builder.append('\n');
        }
        return builder.toString();
    }

    public long[] canonicalForm() {
        return arr;
    }

    public long count() {
        return counter.longValue();
    }
}
