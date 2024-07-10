package ua.ihromant.mathutils;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record BSInc(BitSet bs, int v, int b) implements Inc {
    private BSInc(BSInc that, int[] row) {
        this(mergeRow(that, row), that.v, that.b + 1);
    }

    private static BitSet mergeRow(BSInc that, int[] row) {
        int all = that.v * that.b;
        BitSet result = new BitSet(all + that.v);
        result.or(that.bs);
        for (int p : row) {
            result.set(all + p);
        }
        return result;
    }

    @Override
    public boolean inc(int l, int pt) {
        return bs.get(idx(l, pt));
    }

    @Override
    public void set(int l, int pt) {
        bs.set(idx(l, pt));
    }

    @Override
    public Inc removeTwins() {
        return this;
    }

    @Override
    public Inc addLine(int[] line) {
        return new BSInc(this, line);
    }

    private int idx(int l, int pt) {
        return l * v + pt;
    }

    @Override
    public String toString() {
        return IntStream.range(0, b).mapToObj(row -> IntStream.range(0, v).mapToObj(col -> inc(row, col) ? "1" : "0")
                .collect(Collectors.joining())).collect(Collectors.joining("\n"));
    }
}
