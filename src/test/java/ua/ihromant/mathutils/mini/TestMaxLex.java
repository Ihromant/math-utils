package ua.ihromant.mathutils.mini;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.LInc;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMaxLex {
    private static String brute(boolean[][] a) {
        int m = a.length;
        int n = a[0].length;
        List<int[]> rps = Combinatorics.permutations(IntStream.range(0, m).toArray()).toList();
        List<int[]> cps = Combinatorics.permutations(IntStream.range(0, n).toArray()).toList();
        String best = null;
        for (int[] rp : rps) {
            for (int[] cp : cps) {
                String s = MaxLexMatrix.apply(a, rp, cp);
                if (best == null || s.compareTo(best) > 0) {
                    best = s;
                }
            }
        }
        return best;
    }

    private static boolean[][] random(int m, int n, double d) {
        boolean[][] a = new boolean[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = ThreadLocalRandom.current().nextDouble() < d;
            }
        }
        return a;
    }

    private static Inc randomInc(int m, int n, double d) {
        Inc a = Inc.empty(n, 0, m);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (ThreadLocalRandom.current().nextDouble() < d) {
                    a.set(i, j);
                }
            }
        }
        return a;
    }

    private static boolean[][] shuffle(boolean[][] a) {
        int m = a.length;
        int n = a[0].length;
        int[] rp = randPerm(m);
        int[] cp = randPerm(n);
        boolean[][] b = new boolean[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                b[i][j] = a[rp[i]][cp[j]];
            }
        }
        return b;
    }

    private static Inc shuffle(Inc a) {
        int m = a.v();
        int n = a.b();
        int[] rp = randPerm(m);
        int[] cp = randPerm(n);
        Inc b = Inc.empty(a.v(), a.k(), a.b());
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (a.inc(cp[j], rp[i])) {
                    b.set(j, i);
                }
            }
        }
        return b;
    }

    private static int[] randPerm(int k) {
        int[] p = IntStream.range(0, k).toArray();
        for (int i = k - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return p;
    }

    private static boolean[][] projectivePlane(int q) {
        List<int[]> pts = new ArrayList<>();
        for (int x = 0; x < q; x++) {
            for (int y = 0; y < q; y++) {
                pts.add(new int[]{x, y, 1});
            }
        }
        for (int x = 0; x < q; x++) {
            pts.add(new int[]{x, 1, 0});
        }
        pts.add(new int[]{1, 0, 0});
        int N = pts.size();
        boolean[][] a = new boolean[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int[] l = pts.get(i);
                int[] p = pts.get(j);
                a[i][j] = ((l[0] * p[0] + l[1] * p[1] + l[2] * p[2]) % q) == 0;
            }
        }
        return a;
    }

    private static boolean[][] identity(int k) {
        boolean[][] a = new boolean[k][k];
        for (int i = 0; i < k; i++) {
            a[i][i] = true;
        }
        return a;
    }

    @Test
    @Disabled
    public void testFano() {
        boolean[][] fano = MaxLexMatrix.parse("1101000", "0110100", "0011010", "0001101",
                "1000110", "0100011", "1010001");
        SearchResult fr = new MaxLexMatrix(fano).solve();
        assertEquals("1110000100110010000110101010010010100110010010110", fr.value, "Fano value");
        assertEquals(MaxLexMatrix.apply(fano, fr.rowPerm, fr.colPerm), fr.value, "Fano certificate");
        System.out.printf("nodes=%d auts=%d%n", fr.nodes, fr.automorphisms);
        Liner aff = new Liner(new GaloisField(3).generatePlane()).subPlane(IntStream.range(0, 9).toArray());
        boolean[][] a = new boolean[aff.lineCount()][aff.pointCount()];
        for (int i = 0; i < aff.lineCount(); i++) {
            for (int j = 0; j < aff.pointCount(); j++) {
                a[i][j] = aff.flag(i, j);
            }
        }
        SearchResult fr1 = new MaxLexMatrix(a).solve();
        assertEquals("111000000100110000100001100100000011010101000010010010010000101001100001001010100001001010000100110000011001", fr1.value, "Fano value");
        System.out.printf("nodes=%d auts=%d%n", fr1.nodes, fr1.automorphisms);
    }

    @Test
    public void testFanoAlt() {
        Inc fano = MaxLexMatrixAlt.parse("1101000", "0110100", "0011010", "0001101",
                "1000110", "0100011", "1010001");
        SearchResult fr = new MaxLexMatrixAlt(fano).solve();
        assertEquals("1110000100110010000110101010010010100110010010110", fr.value, "Fano value");
        assertEquals(MaxLexMatrixAlt.apply(fano, fr.rowPerm, fr.colPerm), fr.value, "Fano certificate");
        System.out.printf("nodes=%d auts=%d%n", fr.nodes, fr.automorphisms);
        Liner aff = new Liner(new GaloisField(3).generatePlane()).subPlane(IntStream.range(0, 9).toArray());
        Inc a = Inc.empty(aff.pointCount(), 3, aff.lineCount());
        for (int i = 0; i < aff.lineCount(); i++) {
            for (int j = 0; j < aff.pointCount(); j++) {
                if (aff.flag(i, j)) {
                    a.set(i, j);
                }
            }
        }
        SearchResult fr1 = new MaxLexMatrixAlt(a).solve();
        assertEquals("111000000100110000100001100100000011010101000010010010010000101001100001001010100001001010000100110000011001", fr1.value, "Fano value");
        System.out.printf("nodes=%d auts=%d%n", fr1.nodes, fr1.automorphisms);
    }

    private record TestCase(boolean[][] ba, Inc inc, String max) {}

    @Test
    public void testBrute() {
        int testCount = 300_000;
        List<TestCase> cases = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, Runtime.getRuntime().availableProcessors()).parallel().forEach(_ -> {
            while (cases.size() < testCount) {
                int m = 1 + ThreadLocalRandom.current().nextInt(6);
                int n = 1 + ThreadLocalRandom.current().nextInt(6);
                if (m == 6 && n == 6 && ThreadLocalRandom.current().nextInt(10) != 0) {
                    continue;
                }
                double d = new double[]{0.1, 0.3, 0.5, 0.7, 0.9}[ThreadLocalRandom.current().nextInt(5)];
                boolean[][] a = random(m, n, d);
                String b = brute(a);
                cases.add(new TestCase(a, toInc(a), b));
            }
        });
        System.out.println("Running " + cases.size() + " random testcases");
        long time = System.currentTimeMillis();
        for (TestCase c : cases) {
            SearchResult r = new MaxLexMatrix(c.ba).solve();
            assertEquals(r.value, c.max, "brute mismatch " + Arrays.deepToString(c.ba) + "\n got " + r.value + "\n exp " + c.max);
            assertEquals(MaxLexMatrix.apply(c.ba, r.rowPerm, r.colPerm), r.value, "certificate");
        }
        System.out.println("Old took " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        for (TestCase c : cases) {
            SearchResult r = new MaxLexMatrixAlt(c.inc).solve();
            assertEquals(r.value, c.max, "brute mismatch " + Arrays.deepToString(c.ba) + "\n got " + r.value + "\n exp " + c.max);
            assertEquals(MaxLexMatrixAlt.apply(c.inc, r.rowPerm, r.colPerm), r.value, "certificate");
        }
        System.out.println("New took " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        for (TestCase c : cases) {
            String val = MaxLexMatrixAlt.apply(c.inc, IntStream.range(0, c.inc.b()).toArray(), IntStream.range(0, c.inc.v()).toArray());
            boolean max = val.equals(c.max);
            assertEquals(max, new MaxChecker(c.inc).isMaxLex());
            assertEquals(max, c.inc.isMaxLex());
            assertEquals(max, c.inc.toLinc().isMaxLex());
        }
        System.out.println("IsMax took " + (System.currentTimeMillis() - time) + " ms");
    }

    @Test
    @Disabled
    public void testShuffle() {
        int inv = 0;
        for (int it = 0; it < 300; it++) {
            int m = 5 + ThreadLocalRandom.current().nextInt(40);
            int n = 5 + ThreadLocalRandom.current().nextInt(40);
            boolean[][] a = random(m, n, 0.1 + 0.8 * ThreadLocalRandom.current().nextDouble());
            String v = new MaxLexMatrix(a).solve().value;
            for (int k = 0; k < 5; k++) {
                boolean[][] b = shuffle(a);
                SearchResult r = new MaxLexMatrix(b).solve();
                assertEquals(r.value, v, "invariance");
                assertEquals(MaxLexMatrix.apply(b, r.rowPerm, r.colPerm), r.value, "certificate");
                inv++;
            }
        }
        System.out.println("permutation invariance on " + inv + " shuffled matrices (5..44 x 5..44): OK");
    }

    @Test
    public void testShuffleAlt() {
        int inv = 0;
        for (int it = 0; it < 300; it++) {
            int m = 5 + ThreadLocalRandom.current().nextInt(40);
            int n = 5 + ThreadLocalRandom.current().nextInt(40);
            Inc a = randomInc(m, n, 0.1 + 0.8 * ThreadLocalRandom.current().nextDouble());
            String v = new MaxLexMatrixAlt(a).solve().value;
            for (int k = 0; k < 5; k++) {
                Inc b = shuffle(a);
                SearchResult r = new MaxLexMatrixAlt(b).solve();
                assertEquals(r.value, v, "invariance");
                assertEquals(MaxLexMatrixAlt.apply(b, r.rowPerm, r.colPerm), r.value, "certificate");
                inv++;
            }
        }
        System.out.println("permutation invariance on " + inv + " shuffled matrices (5..44 x 5..44): OK");
    }

    @Test
    @Disabled
    public void testCases() {
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
            SearchResult r = new MaxLexMatrix(a).solve();
            double ms = (System.nanoTime() - s) / 1e6;
            assertEquals(MaxLexMatrix.apply(a, r.rowPerm, r.colPerm), r.value, "certificate " + c[0]);
            System.out.printf("    %-22s %10.1f ms   nodes=%d auts=%d%n", c[0], ms, r.nodes, r.automorphisms);
        }
    }

    private static Inc toInc(boolean[][] a) {
        int b = a.length;
        int v = a[0].length;
        Inc res = new Inc(IntStream.range(0, b).mapToObj(_ -> new FixBS(v)).toArray(FixBS[]::new), v, 0);
        for (int i = 0; i < b; i++) {
            for (int j = 0; j < v; j++) {
                if (a[i][j]) {
                    res.set(i, j);
                }
            }
        }
        return res;
    }

    @Test
    public void testCasesAlt() {
        Object[][] cases = {
                {"PG(2,2) Fano 7x7", toInc(projectivePlane(2))},
                {"PG(2,3) 13x13", toInc(projectivePlane(3))},
                {"PG(2,5) 31x31", toInc(projectivePlane(5))},
                {"PG(2,7) 57x57", toInc(projectivePlane(7))},
                {"identity 12x12", toInc(identity(12))},
                {"identity 18x18", toInc(identity(18))},
                {"identity 100x100", toInc(identity(100))},
                {"all-ones 40x40", toInc(random(40, 40, 1.0))},
                {"random 50x50 p=.5", toInc(random(50, 50, 0.5))},
                {"random 200x200 p=.5", toInc(random(200, 200, 0.5))},
                {"random 100x300 p=.2", toInc(random(100, 300, 0.2))},
        };
        for (Object[] c : cases) {
            Inc a = shuffle((Inc) c[1]);
            long s = System.nanoTime();
            SearchResult r = new MaxLexMatrixAlt(a).solve();
            double ms = (System.nanoTime() - s) / 1e6;
            assertEquals(MaxLexMatrixAlt.apply(a, r.rowPerm, r.colPerm), r.value, "certificate " + c[0]);
            System.out.printf("    %-22s %10.1f ms   nodes=%d auts=%d%n", c[0], ms, r.nodes, r.automorphisms);
        }
    }

    @Test
    public void testMaximal() {
        Inc fanoMax = MaxLexMatrixAlt.parse("1110000", "1001100", "1000011", "0101010",
                "0100101", "0011001", "0010110");
        String max = "1110000100110010000110101010010010100110010010110";
        int[][] perms = Combinatorics.permutations(IntStream.range(0, 7).toArray()).toArray(int[][]::new);
        for (int[] rp : perms) {
            for (int[] cp : perms) {
                String permuted = MaxLexMatrixAlt.apply(fanoMax, rp, cp);
                Inc toTest = MaxLexMatrixAlt.parse(splitIntoChunks(permuted, 7));
                assertEquals(permuted.equals(max), new MaxLexMatrixAlt(toTest).isMaxLex());
                assertEquals(permuted.equals(max), new MaxChecker(toTest).isMaxLex());
                assertEquals(permuted.equals(max), toTest.isMaxLex());
                assertEquals(permuted.equals(max), toTest.toLinc().isMaxLex());
            }
        }
    }

    private static String[] splitIntoChunks(String s, int chunkSize) {
        int numOfChunks = (s.length() + chunkSize - 1) / chunkSize;
        String[] chunks = new String[numOfChunks];
        for (int i = 0; i < numOfChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(s.length(), start + chunkSize);
            chunks[i] = s.substring(start, end);
        }
        return chunks;
    }

    @Test
    public void testPerformance() {
        int testCount = 100_000_000;
        List<Inc> cases = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, Runtime.getRuntime().availableProcessors()).parallel().forEach(_ -> {
            while (cases.size() < testCount) {
                int m = 1 + ThreadLocalRandom.current().nextInt(6);
                int n = 1 + ThreadLocalRandom.current().nextInt(6);
                if (m == 6 && n == 6 && ThreadLocalRandom.current().nextInt(10) != 0) {
                    continue;
                }
                double d = new double[]{0.1, 0.3, 0.5, 0.7, 0.9}[ThreadLocalRandom.current().nextInt(5)];
                boolean[][] a = random(m, n, d);
                cases.add(toInc(a));
            }
        });
        System.out.println("Generated");
        int cnt = 0;
        long time = System.currentTimeMillis();
        for (Inc inc : cases) {
            if (inc.isMaxLex()) {
                cnt++;
            }
        }
        System.out.println((System.currentTimeMillis() - time) + " " + cnt);
        List<LInc> lCases = cases.stream().map(Inc::toLinc).toList();
        time = System.currentTimeMillis();
        for (LInc inc : lCases) {
            if (inc.isMaxLex()) {
                cnt++;
            }
        }
        System.out.println((System.currentTimeMillis() - time) + " " + cnt);
    }
}

