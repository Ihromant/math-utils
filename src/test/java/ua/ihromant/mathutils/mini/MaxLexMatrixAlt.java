package ua.ihromant.mathutils.mini;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxLexMatrixAlt {
    public static final class Result {
        public final int[] rowPerm;   // rowPerm[i] = original row placed at position i
        public final int[] colPerm;   // colPerm[j] = original column placed at position j
        public final String value;    // the maximal matrix, row-major
        public final long nodes;      // search-tree nodes visited
        public final int automorphisms;
        Result(int[] r, int[] c, String v, long nodes, int auts) {
            rowPerm = r; colPerm = c; value = v; this.nodes = nodes; automorphisms = auts;
        }
    }

    private final int m, n;
    private final boolean[][] a;

    // best leaf so far
    private int[] bestRows, bestCols;
    private int[][] bestMatrixRows;   // best value, row by row
    private final List<int[]> auts = new ArrayList<>();  // row parts of found automorphisms
    private long nodes;
    private static final int MAX_AUTS = 5000;

    public MaxLexMatrixAlt(boolean[][] a) {
        this.a = a; this.m = a.length; this.n = m == 0 ? 0 : a[0].length;
    }

    public MaxLexMatrix.Result solve() {
        bestRows = null; auts.clear(); nodes = 0;
        bestMatrixRows = new int[m][];

        // identical rows: swapping them is an automorphism, seed the generator list with those transpositions
        Map<String, Integer> firstCopy = new HashMap<>();
        for (int r = 0; r < m; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < n; c++) sb.append(a[r][c] ? '1' : '0');
            Integer f = firstCopy.putIfAbsent(sb.toString(), r);
            if (f != null) {
                int[] g = identityPerm(m); g[f] = r; g[r] = f; auts.add(g);
            }
        }

        int[] order = identityPerm(n);
        int[] cellEnd = new int[n];
        Arrays.fill(cellEnd, n);
        dfs(0, new int[m], new boolean[m], order, cellEnd, true);

        StringBuilder sb = new StringBuilder();
        for (int[] row : bestMatrixRows) for (int b : row) sb.append(b);
        return new MaxLexMatrix.Result(bestRows.clone(), bestCols.clone(), sb.toString(), nodes, auts.size());
    }

    /**
     * @param equalSoFar true if the current prefix equals the best leaf's prefix (false = strictly larger)
     * @return depth to backjump to, or Integer.MAX_VALUE for "no jump"
     */
    private int dfs(int depth, int[] rows, boolean[] used, int[] order, int[] cellEnd, boolean equalSoFar) {
        nodes++;
        if (depth == m) {
            if (bestRows == null || !equalSoFar) {          // new best leaf
                bestRows = rows.clone(); bestCols = order.clone();
                return Integer.MAX_VALUE;
            }
            // same matrix as best leaf -> automorphism  bestRows[i] -> rows[i]
            int[] g = new int[m];
            for (int i = 0; i < m; i++) g[bestRows[i]] = rows[i];
            if (auts.size() < MAX_AUTS) auts.add(g);
            int split = 0;
            while (split < m && rows[split] == bestRows[split]) split++;
            return split;   // abandon everything below the node where the two paths split
        }

        // candidate rows and the maximal next row
        int[] best = null;
        List<Integer> cands = new ArrayList<>();
        for (int r = 0; r < m; r++) {
            if (used[r]) continue;
            int[] row = rowString(r, order, cellEnd);
            int c = best == null ? 1 : compare(row, best);
            if (c > 0) { best = row; cands.clear(); }
            if (c >= 0) cands.add(r);
        }

        // bound against best leaf
        boolean eq = equalSoFar;
        if (bestRows != null && equalSoFar) {
            int c = compare(best, bestMatrixRows[depth]);
            if (c < 0) return Integer.MAX_VALUE;   // this branch can't win
            if (c > 0) eq = false;                   // strictly better from here on
        }
        if (!eq || bestRows == null) bestMatrixRows[depth] = best;  // record (overwritten if a better leaf appears)

        List<Integer> done = new ArrayList<>();
        for (int r : cands) {
            if (!done.isEmpty() && sameOrbitAsDone(r, done, rows, depth)) continue;
            done.add(r);

            int[] o2 = new int[n], ce2 = new int[n];
            refine(r, order, cellEnd, o2, ce2);
            rows[depth] = r; used[r] = true;
            int jump = dfs(depth + 1, rows, used, o2, ce2, eq);
            used[r] = false;
            if (jump < depth) return jump;
            // once a better leaf exists below, the rest of this node must be compared against it
            if (!eq) eq = true;
        }
        return Integer.MAX_VALUE;
    }

    /** Is r in the orbit (under found automorphisms that fix rows[0..depth-1]) of an already explored child? */
    private boolean sameOrbitAsDone(int r, List<Integer> done, int[] rows, int depth) {
        int[] parent = identityPerm(m);
        boolean any = false;
        for (int[] g : auts) {
            boolean fixes = true;
            for (int i = 0; i < depth && fixes; i++) if (g[rows[i]] != rows[i]) fixes = false;
            if (!fixes) continue;
            any = true;
            for (int x = 0; x < m; x++) union(parent, x, g[x]);
        }
        if (!any) return false;
        int root = find(parent, r);
        for (int d : done) if (find(parent, d) == root) return true;
        return false;
    }

    private int[] rowString(int r, int[] order, int[] cellEnd) {
        int[] out = new int[n];
        int p = 0;
        while (p < n) {
            int e = cellEnd[p], k = 0;
            for (int q = p; q < e; q++) if (a[r][order[q]]) k++;
            for (int q = p; q < p + k; q++) out[q] = 1;
            p = e;
        }
        return out;
    }

    private void refine(int r, int[] order, int[] cellEnd, int[] o2, int[] ce2) {
        int p = 0;
        while (p < n) {
            int e = cellEnd[p], w = p;
            for (int q = p; q < e; q++) if (a[r][order[q]]) o2[w++] = order[q];
            int split = w;
            for (int q = p; q < e; q++) if (!a[r][order[q]]) o2[w++] = order[q];
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

    public static String apply(boolean[][] a, int[] rp, int[] cp) {
        StringBuilder sb = new StringBuilder();
        for (int r : rp) for (int c : cp) sb.append(a[r][c] ? '1' : '0');
        return sb.toString();
    }

    public static boolean[][] parse(String... rows) {
        boolean[][] a = new boolean[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String s = rows[i].replaceAll("\\s", "");
            a[i] = new boolean[s.length()];
            for (int j = 0; j < s.length(); j++) a[i][j] = s.charAt(j) == '1';
        }
        return a;
    }

    public static void main(String[] args) {
        boolean[][] fano = parse(
                "1101000", "0110100", "0011010", "0001101",
                "1000110", "0100011", "1010001");
        MaxLexMatrix.Result r = new MaxLexMatrix(fano).solve();
        System.out.println(r.value);
        System.out.println("row order " + Arrays.toString(r.rowPerm) + "  column order " + Arrays.toString(r.colPerm));
    }
}
