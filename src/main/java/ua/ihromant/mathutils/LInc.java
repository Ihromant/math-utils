package ua.ihromant.mathutils;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record LInc(long[] lines, int v, int k) implements NautyGraph {
    public static Inc empty(int v, int k, int b) {
        return new Inc(IntStream.range(0, b).mapToObj(_ -> new FixBS(v)).toArray(FixBS[]::new), v, k);
    }

    public int b() {
        return lines.length;
    }

    public boolean inc(int l, int pt) {
        return (lines[l] & (1L << pt)) != 0;
    }

    public void set(int l, int pt) {
        lines[l] |= (1L << pt);
    }

    public long line(int l) {
        return lines[l];
    }

    private static final long WORD_MASK = 0xffffffffffffffffL;

    private static int nextSetBit(long fbs, int fromIndex) {
        long word = fbs & (WORD_MASK << fromIndex);
        return word == 0 ? -1 : Long.numberOfTrailingZeros(word);
    }

    public LInc addLine(int[] line) {
        long[] next = new long[lines.length + 1];
        System.arraycopy(lines, 0, next, 0, lines.length);
        long nl = 0;
        for (int pt : line) {
            nl = nl | (1L << pt);
        }
        next[lines.length] = nl;
        return new LInc(next, v, k);
    }

    @Override
    public int vCount() {
        return v() + b();
    }

    @Override
    public int vColor(int idx) {
        return idx < v() ? 0 : 1;
    }

    @Override
    public boolean edge(int a, int b) {
        int pc = v();
        if (a < pc) {
            return b >= pc && inc(b - pc, a);
        } else {
            return b < pc && inc(a - pc, b);
        }
    }

    @Override
    public int eCount() {
        return lines.length * k * 2;
    }

    public String toLines() {
        return IntStream.range(0, b()).mapToObj(line -> IntStream.range(0, v()).filter(pt -> inc(line, pt)).mapToObj(String::valueOf)
                .collect(Collectors.joining(" "))).collect(Collectors.joining("\n")) + "\n";
    }

    public Iterable<int[]> blocks() {
        return () -> new BlocksIterator(this);
    }

    public FixBS getCanonicalOld() {
        CanonicalConsumer cons = new CanonicalConsumer(this);
        NautyAlgo.search(this, cons);
        return cons.canonicalForm();
    }

    public Matrix sqrInc() {
        int v = v();
        int b = b();
        int[][] res = new int[v][v];
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                int cnt = 0;
                for (int l = 0; l < b; l++) {
                    if (inc(l, i) && inc(l, j)) {
                        cnt++;
                    }
                }
                res[i][j] = res[j][i] = cnt;
            }
        }
        return new Matrix(res);
    }

    public static class BlocksIterator implements Iterator<int[]> {
        private final LInc inc;
        private final int v;
        private final int b;
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator(LInc inc) {
            this.inc = inc;
            this.v = inc.v();
            this.b = inc.b();
            int ll = inc.k();
            this.block = new int[ll];
            int fst = -1;
            if (b == 0) {
                fst = 0;
            } else {
                for (int i = 0; i < v; i++) {
                    if (inc.inc(b - 1, i)) {
                        fst = i;
                        break;
                    }
                }
            }
            int snd;
            do {
                FixBS cand = new FixBS(v);
                cand.set(fst + 1, v);
                for (int l = 0; l < b; l++) {
                    if (!inc.inc(l, fst)) {
                        continue;
                    }
                    for (int pt = fst + 1; pt < v; pt++) {
                        if (inc.inc(l, pt)) {
                            cand.set(pt, false);
                        }
                    }
                }
                snd = cand.nextSetBit(0);
            } while (snd < 0 && ++fst < v);
            block[0] = fst;
            block[1] = snd;
            for (int i = 2; i < ll; i++) {
                block[i] = snd + i - 1;
            }
            this.hasNext = fst < v && findNext(ll - 2);
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(block[len - 1] + 1, block[len]); p < v - moreNeeded + 1; p++) {
                for (int l = 0; l < b; l++) {
                    if (!inc.inc(l, p)) {
                        continue;
                    }
                    for (int i = 0; i < len; i++) {
                        if (inc.inc(l, block[i])) {
                            continue ex;
                        }
                    }
                }
                block[len] = p;
                if (moreNeeded == 1 || findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }

    /** Whole-matrix check: is this incidence matrix lexicographically largest under row and column permutations? */
    public boolean isMaxLex() {
        return MaxLexCheck.isMaxLex(lines, v);
    }

    /**
     * Incremental check: is {@code this + line} lex-max? Assumes {@code this} is already lex-max (as in orderly
     * generation). Does not build a new LInc. Candidates that fail the cheap tests return without any search.
     */
    public boolean isMaxLex(long line) {
        return MaxLexCheck.isMaxLexWith(lines, v, line);
    }

    public boolean isMaxLex(int[] line) {
        return isMaxLex(toMask(line));
    }

    /**
     * For testing many candidate lines against this (lex-max) LInc: the search tree of this LInc is built once,
     * on the first candidate that passes the cheap tests, and reused for every candidate.
     * {@code test(mask)} is equivalent to {@code isMaxLex(mask)}.
     * Not thread-safe: create one per base, per thread.
     */
    public LongPredicate maxLexExtender() {
        return MaxLexCheck.prepare(lines, v)::test;
    }

    public LInc addLine(long line) {
        long[] next = Arrays.copyOf(lines, lines.length + 1);
        next[lines.length] = line;
        return new LInc(next, v, k);
    }

    public static long toMask(int[] line) {
        long nl = 0;
        for (int pt : line) {
            nl |= 1L << pt;
        }
        return nl;
    }
}
