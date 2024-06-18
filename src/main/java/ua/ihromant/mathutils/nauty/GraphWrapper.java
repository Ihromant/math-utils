package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.util.BitSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

public interface GraphWrapper {
    int size();

    int color(int idx);

    int pointCount();

    int lineCount();

    boolean edge(int a, int b);

    static GraphWrapper forFull(Liner liner) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return liner.pointCount() + liner.lineCount();
            }

            @Override
            public int color(int idx) {
                return idx < liner.pointCount() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return liner.pointCount();
            }

            @Override
            public int lineCount() {
                return liner.lineCount();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = liner.pointCount();
                if (a < pc) {
                    return b >= pc && liner.flag(b - pc, a);
                } else {
                    return b < pc && liner.flag(a - pc, b);
                }
            }
        };
    }

    static GraphWrapper forPartial(PartialLiner liner) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return liner.pointCount() + liner.lineCount();
            }

            @Override
            public int color(int idx) {
                return idx < liner.pointCount() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return liner.pointCount();
            }

            @Override
            public int lineCount() {
                return liner.lineCount();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = liner.pointCount();
                if (a < pc) {
                    return b >= pc && liner.flag(b - pc, a);
                } else {
                    return b < pc && liner.flag(a - pc, b);
                }
            }
        };
    }

    default CellStack partition() {
        SortedMap<Integer, BitSet> colorDist = new TreeMap<>();
        IntStream.range(0, size()).forEach(i -> colorDist.computeIfAbsent(color(i), j -> new BitSet(size())).set(i));
        int[][] result = new int[colorDist.lastKey() + 1][0];
        colorDist.forEach((k, v) -> result[k] = v.stream().toArray());
        return new CellStack(size(), result);
    }

    default long[] permutedIncidence(CellStack partition) {
        int pc = pointCount();
        int lc = lineCount();
        int size = size();
        long[] arr = new long[(pc * lc + 63) / 64];
        for (int l = pc; l < size; l++) {
            int pl = partition.permute(l);
            for (int p = 0; p < pc; p++) {
                if (edge(l, p)) {
                    int pp = partition.permute(p);
                    int li = pl - pc;
                    int idx = li * pc + pp;
                    arr[idx >> 6] |= (1L << idx);
                }
            }
        }
        return arr;
    }
}
