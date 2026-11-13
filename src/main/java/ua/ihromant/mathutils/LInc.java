package ua.ihromant.mathutils;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
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

    public Inc addLine(int[] line) {
        FixBS[] next = new FixBS[lines.length + 1];
        System.arraycopy(lines, 0, next, 0, lines.length);
        next[lines.length] = FixBS.of(v, line);
        return new Inc(next, v, k);
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
            int ll = (int) IntStream.range(0, v).filter(pt -> inc.inc(0, pt)).count();
            this.block = new int[ll];
            int fst = -1;
            for (int i = 0; i < v; i++) {
                if (inc.inc(b - 1, i)) {
                    fst = i;
                    break;
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
        int[] order = identityPerm(v);
        int[] cellEnd = new int[v];
        Arrays.fill(cellEnd, v);

        // 1. Fast O(m * n) check & initialization:
        // Verify that under identity row/column order, columns are already maximally refined
        // (1s precede 0s in every equivalence cell) and rows are non-increasing.
        for (int r = 0; r < lines.length; r++) {
            // If sorting columns within current cells produces a larger row, a column swap alone beats 'a'
            long maxForRow = rowString(r, order, cellEnd);
            if (compare(maxForRow, line(r)) < 0) {
                return false;
            }
            // Quick adjacent row check: in a lex-max matrix, row r cannot be strictly smaller than row r+1
            if (r > 0 && compare(line(r - 1), line(r)) > 0) {
                return false;
            }
            int[] o2 = new int[v];
            int[] ce2 = new int[v];
            refine(r, order, cellEnd, o2, ce2);
            order = o2;
            cellEnd = ce2;
        }

        List<int[]> auts = new ArrayList<>();

        // 2. Seed identical row transpositions as automorphisms
        Map<Long, Integer> firstCopy = new HashMap<>();
        for (int r = 0; r < lines.length; r++) {
            Integer f = firstCopy.putIfAbsent(line(r), r);
            if (f != null) {
                int[] g = identityPerm(lines.length);
                g[f] = r;
                g[r] = f;
                auts.add(g);
            }
        }

        // 3. Run bounded DFS (returns -1 immediately if a strictly larger row prefix is found)
        order = identityPerm(v);
        Arrays.fill(cellEnd, v);
        return dfsCheck(0, auts, new int[lines.length], new boolean[lines.length], order, cellEnd) != -1;
    }

    private static final int MAX_AUTS = 5000;

    /**
     * @return -1 if a strictly larger matrix exists, depth to backjump to, or Integer.MAX_VALUE
     */
    private int dfsCheck(int depth, List<int[]> auts, int[] rows, boolean[] used, int[] order, int[] cellEnd) {
        if (depth == lines.length) {
            // Reached a leaf equal to bestMatrixRows -> record automorphism bestRows[i] (which is i) -> rows[i]
            int split = 0;
            while (split < lines.length && rows[split] == split) split++;
            if (split == lines.length) {
                return Integer.MAX_VALUE; // The identity path itself
            }
            if (auts.size() < MAX_AUTS) {
                auts.add(rows.clone()); // Since bestRows[i] == i, g[bestRows[i]] = rows[i] is just rows.clone()
            }
            return split; // Backjump to the split point
        }

        // Find candidate rows and the maximal next row under current column partition
        long best = 0L;
        List<Integer> cands = new ArrayList<>();
        for (int r = 0; r < lines.length; r++) {
            if (used[r]) continue;
            long row = rowString(r, order, cellEnd);
            int c = best == 0 ? 1 : compare(best, row);
            if (c > 0) {
                best = row;
                cands.clear();
            }
            if (c >= 0) cands.add(r);
        }

        // Compare against the original matrix's row at this depth
        int c = compare(line(depth), best);
        if (c > 0) return -1;                // Strictly larger row found! Exit immediately
        if (c < 0) return Integer.MAX_VALUE; // This branch is strictly smaller; prune

        List<Integer> done = new ArrayList<>();
        for (int r : cands) {
            if (!done.isEmpty() && sameOrbitAsDone(r, auts, done, rows, depth)) continue;
            done.add(r);

            int[] o2 = new int[v];
            int[] ce2 = new int[v];
            refine(r, order, cellEnd, o2, ce2);
            rows[depth] = r;
            used[r] = true;
            int jump = dfsCheck(depth + 1, auts, rows, used, o2, ce2);
            used[r] = false;
            if (jump < depth) return jump; // Propagates -1 (early exit) or split backjump
        }
        return Integer.MAX_VALUE;
    }

    /** Is r in the orbit (under found automorphisms that fix rows[0..depth-1]) of an already explored child? */
    private boolean sameOrbitAsDone(int r, List<int[]> auts, List<Integer> done, int[] rows, int depth) {
        int[] parent = identityPerm(lines.length);
        boolean any = false;
        ex: for (int[] g : auts) {
            for (int i = 0; i < depth; i++) {
                if (g[rows[i]] != rows[i]) {
                    continue ex;
                }
            }
            any = true;
            for (int x = 0; x < lines.length; x++) {
                union(parent, x, g[x]);
            }
        }
        if (!any) {
            return false;
        }
        int root = find(parent, r);
        for (int d : done) {
            if (find(parent, d) == root) {
                return true;
            }
        }
        return false;
    }

    private long rowString(int r, int[] order, int[] cellEnd) {
        long out = 0;
        int p = 0;
        while (p < v) {
            int e = cellEnd[p];
            int k = 0;
            for (int q = p; q < e; q++) {
                if (inc(r, order[q])) {
                    k++;
                }
            }
            for (int q = p; q < p + k; q++) {
                out = out | (1L << q);
            }
            p = e;
        }
        return out;
    }

    private void refine(int r, int[] order, int[] cellEnd, int[] o2, int[] ce2) {
        int p = 0;
        while (p < v) {
            int e = cellEnd[p], w = p;
            for (int q = p; q < e; q++) if (inc(r, order[q])) o2[w++] = order[q];
            int split = w;
            for (int q = p; q < e; q++) if (!inc(r, order[q])) o2[w++] = order[q];
            for (int q = p; q < split; q++) ce2[q] = split;
            for (int q = split; q < e; q++) ce2[q] = e;
            p = e;
        }
    }

    // ---------- small utilities ----------
    private static int[] identityPerm(int k) { int[] p = new int[k]; for (int i = 0; i < k; i++) p[i] = i; return p; }
    private static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
    private static void union(int[] p, int x, int y) { x = find(p, x); y = find(p, y); if (x != y) p[x] = y; }
}
