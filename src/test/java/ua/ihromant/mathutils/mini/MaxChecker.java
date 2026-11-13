package ua.ihromant.mathutils.mini;

import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxChecker {
    private final int m, n;
    private final Inc a;

    // best leaf so far
    private int[] bestRows;
    private int[][] bestMatrixRows;   // best value, row by row
    private final List<int[]> auts = new ArrayList<>();  // row parts of found automorphisms
    private static final int MAX_AUTS = 5000;

    public MaxChecker(Inc a) {
        this.a = a; this.m = a.b(); this.n = a.v();
    }

    public boolean isMaxLex() {
        auts.clear();
        bestRows = identityPerm(m);
        bestMatrixRows = new int[m][n];

        int[] order = identityPerm(n);
        int[] cellEnd = new int[n];
        Arrays.fill(cellEnd, n);

        // 1. Fast O(m * n) check & initialization:
        // Verify that under identity row/column order, columns are already maximally refined
        // (1s precede 0s in every equivalence cell) and rows are non-increasing.
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                bestMatrixRows[r][c] = a.inc(r, c) ? 1 : 0;
            }
            // If sorting columns within current cells produces a larger row, a column swap alone beats 'a'
            int[] maxForRow = rowString(r, order, cellEnd);
            if (compare(maxForRow, bestMatrixRows[r]) > 0) {
                return false;
            }
            // Quick adjacent row check: in a lex-max matrix, row r cannot be strictly smaller than row r+1
            if (r > 0 && compare(bestMatrixRows[r - 1], bestMatrixRows[r]) < 0) {
                return false;
            }
            int[] o2 = new int[n];
            int[] ce2 = new int[n];
            refine(r, order, cellEnd, o2, ce2);
            order = o2;
            cellEnd = ce2;
        }

        // 2. Seed identical row transpositions as automorphisms
        Map<String, Integer> firstCopy = new HashMap<>();
        for (int r = 0; r < m; r++) {
            StringBuilder sb = new StringBuilder(n);
            for (int c = 0; c < n; c++) sb.append(a.inc(r, c) ? '1' : '0');
            Integer f = firstCopy.putIfAbsent(sb.toString(), r);
            if (f != null) {
                int[] g = identityPerm(m);
                g[f] = r;
                g[r] = f;
                auts.add(g);
            }
        }

        // 3. Run bounded DFS (returns -1 immediately if a strictly larger row prefix is found)
        order = identityPerm(n);
        Arrays.fill(cellEnd, n);
        return dfsCheck(0, new int[m], new boolean[m], order, cellEnd) != -1;
    }

    /**
     * @return -1 if a strictly larger matrix exists, depth to backjump to, or Integer.MAX_VALUE
     */
    private int dfsCheck(int depth, int[] rows, boolean[] used, int[] order, int[] cellEnd) {
        if (depth == m) {
            // Reached a leaf equal to bestMatrixRows -> record automorphism bestRows[i] (which is i) -> rows[i]
            int split = 0;
            while (split < m && rows[split] == bestRows[split]) split++;
            if (split == m) {
                return Integer.MAX_VALUE; // The identity path itself
            }
            if (auts.size() < MAX_AUTS) {
                auts.add(rows.clone()); // Since bestRows[i] == i, g[bestRows[i]] = rows[i] is just rows.clone()
            }
            return split; // Backjump to the split point
        }

        // Find candidate rows and the maximal next row under current column partition
        int[] best = null;
        List<Integer> cands = new ArrayList<>();
        for (int r = 0; r < m; r++) {
            if (used[r]) continue;
            int[] row = rowString(r, order, cellEnd);
            int c = best == null ? 1 : compare(row, best);
            if (c > 0) {
                best = row;
                cands.clear();
            }
            if (c >= 0) cands.add(r);
        }

        // Compare against the original matrix's row at this depth
        int c = compare(best, bestMatrixRows[depth]);
        if (c > 0) return -1;                // Strictly larger row found! Exit immediately
        if (c < 0) return Integer.MAX_VALUE; // This branch is strictly smaller; prune

        List<Integer> done = new ArrayList<>();
        for (int r : cands) {
            if (!done.isEmpty() && sameOrbitAsDone(r, done, rows, depth)) continue;
            done.add(r);

            int[] o2 = new int[n];
            int[] ce2 = new int[n];
            refine(r, order, cellEnd, o2, ce2);
            rows[depth] = r;
            used[r] = true;
            int jump = dfsCheck(depth + 1, rows, used, o2, ce2);
            used[r] = false;
            if (jump < depth) return jump; // Propagates -1 (early exit) or split backjump
        }
        return Integer.MAX_VALUE;
    }

    /** Is r in the orbit (under found automorphisms that fix rows[0..depth-1]) of an already explored child? */
    private boolean sameOrbitAsDone(int r, List<Integer> done, int[] rows, int depth) {
        int[] parent = identityPerm(m);
        boolean any = false;
        ex: for (int[] g : auts) {
            for (int i = 0; i < depth; i++) {
                if (g[rows[i]] != rows[i]) {
                    continue ex;
                }
            }
            any = true;
            for (int x = 0; x < m; x++) {
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

    private int[] rowString(int r, int[] order, int[] cellEnd) {
        int[] out = new int[n];
        int p = 0;
        while (p < n) {
            int e = cellEnd[p];
            int k = 0;
            for (int q = p; q < e; q++) {
                if (a.inc(r, order[q])) {
                    k++;
                }
            }
            for (int q = p; q < p + k; q++) {
                out[q] = 1;
            }
            p = e;
        }
        return out;
    }

    private void refine(int r, int[] order, int[] cellEnd, int[] o2, int[] ce2) {
        int p = 0;
        while (p < n) {
            int e = cellEnd[p], w = p;
            for (int q = p; q < e; q++) if (a.inc(r, order[q])) o2[w++] = order[q];
            int split = w;
            for (int q = p; q < e; q++) if (!a.inc(r, order[q])) o2[w++] = order[q];
            for (int q = p; q < split; q++) ce2[q] = split;
            for (int q = split; q < e; q++) ce2[q] = e;
            p = e;
        }
    }

    // ---------- small utilities ----------
    private static int compare(int[] x, int[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return x[i] - y[i];
        return 0;
    }
    private static int[] identityPerm(int k) { int[] p = new int[k]; for (int i = 0; i < k; i++) p[i] = i; return p; }
    private static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
    private static void union(int[] p, int x, int y) { x = find(p, x); y = find(p, y); if (x != y) p[x] = y; }

    public static String apply(Inc a, int[] rp, int[] cp) {
        StringBuilder sb = new StringBuilder();
        for (int r : rp) for (int c : cp) sb.append(a.inc(r, c) ? '1' : '0');
        return sb.toString();
    }

    public static Inc parse(String... rows) {
        FixBS[] a = new FixBS[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String s = rows[i].replaceAll("\\s", "");
            a[i] = new FixBS(s.length());
            for (int j = 0; j < s.length(); j++) {
                a[i].set(j, s.charAt(j) == '1');
            }
        }
        return new Inc(a, rows[0].length(), 0);
    }
}
