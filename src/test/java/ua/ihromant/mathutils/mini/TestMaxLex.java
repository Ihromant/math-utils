package ua.ihromant.mathutils.mini;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TestMaxLex {
    static Random rnd = new Random(12345);

    // ---- brute force: all row perms x all col perms ----
    static String brute(boolean[][] a) {
        int m = a.length, n = a[0].length;
        List<int[]> rps = perms(m), cps = perms(n);
        String best = null;
        for (int[] rp : rps) for (int[] cp : cps) {
            String s = MaxLexMatrix.apply(a, rp, cp);
            if (best == null || s.compareTo(best) > 0) best = s;
        }
        return best;
    }
    static List<int[]> perms(int k) {
        List<int[]> out = new ArrayList<>();
        int[] p = new int[k]; for (int i = 0; i < k; i++) p[i] = i;
        permRec(p, 0, out); return out;
    }
    static void permRec(int[] p, int i, List<int[]> out) {
        if (i == p.length) { out.add(p.clone()); return; }
        for (int j = i; j < p.length; j++) {
            int t = p[i]; p[i] = p[j]; p[j] = t;
            permRec(p, i + 1, out);
            t = p[i]; p[i] = p[j]; p[j] = t;
        }
    }

    static boolean[][] random(int m, int n, double d) {
        boolean[][] a = new boolean[m][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) a[i][j] = rnd.nextDouble() < d;
        return a;
    }
    static boolean[][] shuffle(boolean[][] a) {
        int m = a.length, n = a[0].length;
        int[] rp = randPerm(m), cp = randPerm(n);
        boolean[][] b = new boolean[m][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) b[i][j] = a[rp[i]][cp[j]];
        return b;
    }
    static int[] randPerm(int k) {
        int[] p = new int[k]; for (int i = 0; i < k; i++) p[i] = i;
        for (int i = k - 1; i > 0; i--) { int j = rnd.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }
        return p;
    }

    // projective plane PG(2,q), q prime: q^2+q+1 points/lines
    static boolean[][] projectivePlane(int q) {
        List<int[]> pts = new ArrayList<>();
        for (int x = 0; x < q; x++) for (int y = 0; y < q; y++) pts.add(new int[]{x, y, 1});
        for (int x = 0; x < q; x++) pts.add(new int[]{x, 1, 0});
        pts.add(new int[]{1, 0, 0});
        int N = pts.size();
        boolean[][] a = new boolean[N][N];
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
            int[] l = pts.get(i), p = pts.get(j);
            a[i][j] = ((l[0] * p[0] + l[1] * p[1] + l[2] * p[2]) % q) == 0;
        }
        return a;
    }
    static boolean[][] identity(int k) {
        boolean[][] a = new boolean[k][k]; for (int i = 0; i < k; i++) a[i][i] = true; return a;
    }

    static void check(boolean cond, String msg) {
        if (!cond) { System.out.println("FAIL: " + msg); System.exit(1); }
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();

        // 1) Fano example from the question
        boolean[][] fano = MaxLexMatrix.parse("1101000", "0110100", "0011010", "0001101",
                "1000110", "0100011", "1010001");
        MaxLexMatrix.Result fr = new MaxLexMatrix(fano).solve();
        check(fr.value.equals("1110000100110010000110101010010010100110010010110"), "Fano value");
        check(MaxLexMatrix.apply(fano, fr.rowPerm, fr.colPerm).equals(fr.value), "Fano certificate");
        System.out.println("[1] Fano: OK  " + fr.value);

        // 2) brute force cross-check on random small matrices (sizes up to 6x6)
        int bf = 0;
        long bfEnd = System.currentTimeMillis() + 4 * 60_000; // ~4 minutes
        while (System.currentTimeMillis() < bfEnd) {
            int m = 1 + rnd.nextInt(6), n = 1 + rnd.nextInt(6);
            if (m == 6 && n == 6 && rnd.nextInt(10) != 0) continue; // 6x6 brute is slow-ish
            double d = new double[]{0.1, 0.3, 0.5, 0.7, 0.9}[rnd.nextInt(5)];
            boolean[][] a = random(m, n, d);
            MaxLexMatrix.Result r = new MaxLexMatrix(a).solve();
            String b = brute(a);
            check(r.value.equals(b), "brute mismatch " + Arrays.deepToString(a) + "\n got " + r.value + "\n exp " + b);
            check(MaxLexMatrix.apply(a, r.rowPerm, r.colPerm).equals(r.value), "certificate");
            bf++;
        }
        System.out.println("[2] brute-force agreement on " + bf + " random matrices (<=6x6): OK");

        // 3) invariance: shuffled copies of larger matrices give the same canonical value
        int inv = 0;
        for (int it = 0; it < 300; it++) {
            int m = 5 + rnd.nextInt(40), n = 5 + rnd.nextInt(40);
            boolean[][] a = random(m, n, 0.1 + 0.8 * rnd.nextDouble());
            String v = new MaxLexMatrix(a).solve().value;
            for (int k = 0; k < 5; k++) {
                boolean[][] b = shuffle(a);
                MaxLexMatrix.Result r = new MaxLexMatrix(b).solve();
                check(r.value.equals(v), "invariance");
                check(MaxLexMatrix.apply(b, r.rowPerm, r.colPerm).equals(r.value), "certificate");
                inv++;
            }
        }
        System.out.println("[3] permutation invariance on " + inv + " shuffled matrices (5..44 x 5..44): OK");

        // 4) timing on structured / symmetric and large random inputs
        System.out.println("[4] timing:");
        Object[][] cases = {
                {"PG(2,2) Fano 7x7", projectivePlane(2)},
                {"PG(2,3) 13x13", projectivePlane(3)},
                {"PG(2,5) 31x31", projectivePlane(5)},
                {"PG(2,7) 57x57", projectivePlane(7)},
                {"identity 12x12", identity(12)},
                {"identity 18x18", identity(18)},
                {"identity 100x100", identity(100)},
                {"all-ones 40x40", random(40, 40, 1.0)},
                {"random 50x50 p=.5", random(50, 50, 0.5)},
                {"random 200x200 p=.5", random(200, 200, 0.5)},
                {"random 100x300 p=.2", random(100, 300, 0.2)},
        };
        for (Object[] c : cases) {
            boolean[][] a = shuffle((boolean[][]) c[1]);
            long s = System.nanoTime();
            MaxLexMatrix.Result r = new MaxLexMatrix(a).solve();
            double ms = (System.nanoTime() - s) / 1e6;
            check(MaxLexMatrix.apply(a, r.rowPerm, r.colPerm).equals(r.value), "certificate " + c[0]);
            System.out.printf("    %-22s %10.1f ms   nodes=%d auts=%d%n", c[0], ms, r.nodes, r.automorphisms);
        }
        // PG(2,3) value printed for reference
        System.out.println("    PG(2,3) canonical: " + new MaxLexMatrix(projectivePlane(3)).solve().value);

        System.out.printf("ALL TESTS PASSED in %.1f s%n", (System.currentTimeMillis() - t0) / 1000.0);
    }
}

