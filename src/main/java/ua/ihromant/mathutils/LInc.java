package ua.ihromant.mathutils;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static int compare(long a, long b) {
        return Long.compareUnsigned(Long.reverse(b), Long.reverse(a));
    }

    public boolean isMaxLex() {
        int b = lines.length;
        if (b == 0) return true;

        // Initial single cell containing all v points: bits 0 .. v-1
        long allPts = v == 64 ? -1L : (1L << v) - 1L;
        long[] cells = new long[]{allPts};

        // 1. Fast check under identity permutation:
        // At each step, cells are contiguous bit ranges. For identity to be optimal so far,
        // line(r) must already have its 1-bits pushed to the low bits of each cell,
        // and rows must be non-increasing.
        for (int r = 0; r < b; r++) {
            long line = lines[r];
            if (r > 0 && compare(lines[r - 1], line) > 0) {
                return false;
            }
            if (maxRow(line, cells) != line) {
                return false;
            }
            cells = refine(line, cells);
        }

        // 2. Seed identical row transpositions as automorphisms
        List<int[]> auts = new ArrayList<>();
        Map<Long, Integer> firstCopy = new HashMap<>();
        for (int r = 0; r < b; r++) {
            Integer f = firstCopy.putIfAbsent(lines[r], r);
            if (f != null) {
                int[] g = identityPerm(b);
                g[f] = r;
                g[r] = f;
                auts.add(g);
            }
        }

        // 3. Bounded DFS using bitmask cells
        return dfsCheck(0, auts, new int[b], new boolean[b], new long[]{allPts}) != -1;
    }

    private static final int MAX_AUTS = 5000;

    private int dfsCheck(int depth, List<int[]> auts, int[] rows, boolean[] used, long[] cells) {
        int b = lines.length;
        if (depth == b) {
            int split = 0;
            while (split < b && rows[split] == split) split++;
            if (split == b) return Integer.MAX_VALUE;
            if (auts.size() < MAX_AUTS) auts.add(rows.clone());
            return split;
        }

        long target = lines[depth];
        long best = 0L;
        int[] cands = new int[b];
        int candCount = 0;

        for (int r = 0; r < b; r++) {
            if (used[r]) continue;
            long row = maxRow(lines[r], cells);
            if (candCount == 0) {
                best = row;
                cands[candCount++] = r;
            } else {
                int c = compare(best, row);
                if (c > 0) {
                    best = row;
                    candCount = 0;
                    cands[candCount++] = r;
                } else if (c == 0) {
                    cands[candCount++] = r;
                }
            }
        }

        int c = compare(target, best);
        if (c > 0) return -1;                // Strictly larger row found
        if (c < 0) return Integer.MAX_VALUE; // Strictly smaller branch, prune

        int[] done = new int[candCount];
        int doneCount = 0;

        for (int i = 0; i < candCount; i++) {
            int r = cands[i];
            if (doneCount > 0 && sameOrbitAsDone(r, auts, done, doneCount, rows, depth)) continue;
            done[doneCount++] = r;

            long[] nextCells = refine(lines[r], cells);
            rows[depth] = r;
            used[r] = true;
            int jump = dfsCheck(depth + 1, auts, rows, used, nextCells);
            used[r] = false;
            if (jump < depth) return jump;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Computes the lexicographically largest bitmask achievable by permuting columns
     * within each cell of the current partition.
     */
    private static long maxRow(long line, long[] cells) {
        long out = 0L;
        int pos = 0;
        for (long cell : cells) {
            int ones = Long.bitCount(cell & line);
            if (ones != 0) {
                // Sets `ones` bits starting at `pos`: ((1L << ones) - 1) << pos
                out |= (-1L >>> (64 - ones)) << pos;
            }
            pos += Long.bitCount(cell);
        }
        return out;
    }

    /**
     * Splits each cell `c` into (c & line) followed by (c & ~line),
     * omitting empty halves.
     */
    private static long[] refine(long line, long[] cells) {
        int newLen = 0;
        for (long cell : cells) {
            long ones = cell & line;
            long zeros = cell & ~line;
            if (ones != 0 && zeros != 0) newLen += 2;
            else newLen += 1;
        }
        if (newLen == cells.length) {
            return cells; // Partition did not split any cell; reuse immutable array!
        }
        long[] next = new long[newLen];
        int idx = 0;
        for (long cell : cells) {
            long ones = cell & line;
            long zeros = cell & ~line;
            if (ones != 0) next[idx++] = ones;
            if (zeros != 0) next[idx++] = zeros;
        }
        return next;
    }

    private boolean sameOrbitAsDone(int r, List<int[]> auts, int[] done, int doneCount, int[] rows, int depth) {
        int b = lines.length;
        int[] parent = identityPerm(b);
        boolean any = false;
        ex: for (int[] g : auts) {
            for (int i = 0; i < depth; i++) {
                if (g[rows[i]] != rows[i]) continue ex;
            }
            any = true;
            for (int x = 0; x < b; x++) {
                union(parent, x, g[x]);
            }
        }
        if (!any) return false;
        int root = find(parent, r);
        for (int i = 0; i < doneCount; i++) {
            if (find(parent, done[i]) == root) return true;
        }
        return false;
    }

    // ---------- small utilities ----------
    private static int[] identityPerm(int k) { int[] p = new int[k]; for (int i = 0; i < k; i++) p[i] = i; return p; }
    private static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
    private static void union(int[] p, int x, int y) { x = find(p, x); y = find(p, y); if (x != y) p[x] = y; }
}
