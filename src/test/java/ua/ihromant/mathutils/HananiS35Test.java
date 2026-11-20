package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class HananiS35Test {

    // --------------------------------------------------
    // GF(27) = GF(3)[t] / (t^3 - t - 2)
    //
    // Element a + b*t + c*t^2 is encoded as:
    // a + 3*b + 9*c
    // --------------------------------------------------

    static int mod3(int x) {
        return ((x % 3) + 3) % 3;
    }

    static int add(int x, int y) {
        int a = mod3(x % 3 + y % 3);
        int b = mod3(x / 3 % 3 + y / 3 % 3);
        int c = mod3(x / 9 + y / 9);

        return a + 3 * b + 9 * c;
    }

    static int neg(int x) {
        return mod3(-x % 3)
                + 3 * mod3(-(x / 3) % 3)
                + 9 * mod3(-(x / 9));
    }

    static int sub(int x, int y) {
        return add(x, neg(y));
    }

    static int mul(int x, int y) {

        int a = x % 3;
        int b = x / 3 % 3;
        int c = x / 9;

        int d = y % 3;
        int e = y / 3 % 3;
        int f = y / 9;

        // Polynomial multiplication.
        int r0 = a * d;
        int r1 = a * e + b * d;
        int r2 = a * f + b * e + c * d;
        int r3 = b * f + c * e;
        int r4 = c * f;

        // t^3 = t + 2
        // t^4 = t^2 + 2t

        int s0 = mod3(r0 + 2 * r3);
        int s1 = mod3(r1 + r3 + 2 * r4);
        int s2 = mod3(r2 + r4);

        return s0 + 3 * s1 + 9 * s2;
    }

    static int pow(int x, int n) {
        int result = 1;

        while (n > 0) {
            if ((n & 1) != 0)
                result = mul(result, x);

            x = mul(x, x);
            n >>= 1;
        }

        return result;
    }

    static int inv(int x) {
        if (x == 0)
            throw new ArithmeticException("Division by zero");

        return pow(x, 25);
    }

    // --------------------------------------------------
    // Projective line:
    //
    // 0,...,26 = elements of GF(27)
    // 27      = infinity
    // --------------------------------------------------

    static final int INF = 27;

    // Mobius transformation:
    //
    // x -> (a*x+b)/(c*x+d)
    //
    // Matrix determinant must be 1.
    // --------------------------------------------------

    static int transform(
            int a, int b, int c, int d,
            int x
    ) {

        if (x == INF) {

            if (c == 0)
                return INF;

            return mul(a, inv(c));
        }

        int numerator = add(mul(a, x), b);
        int denominator = add(mul(c, x), d);

        if (denominator == 0)
            return INF;

        return mul(numerator, inv(denominator));
    }

    // --------------------------------------------------
    // Encode a sorted 7-subset as a long.
    // --------------------------------------------------

    static long blockKey(int[] block) {

        int[] b = block.clone();
        Arrays.sort(b);

        long key = 0;

        for (int x : b)
            key = key * 28 + x;

        return key;
    }

    // --------------------------------------------------
    // Generate the PSL(2,27) orbit of a 7-subset.
    //
    // PSL(2,27) is represented by matrices:
    //
    // [ a b ]
    // [ c d ]
    //
    // satisfying ad-bc = 1.
    //
    // Matrices M and -M induce the same
    // projective transformation, so the Set
    // automatically removes duplicate blocks.
    // --------------------------------------------------

    public static int[][] orbit(int[] baseBlock) {

        Map<Long, int[]> orbit = new HashMap<>();

        for (int a = 0; a < 27; a++) {
            for (int b = 0; b < 27; b++) {
                for (int c = 0; c < 27; c++) {
                    for (int d = 0; d < 27; d++) {

                        if (sub(mul(a, d), mul(b, c)) != 1)
                            continue;

                        int[] block = new int[7];

                        for (int i = 0; i < 7; i++) {

                            block[i] = transform(
                                    a, b, c, d,
                                    baseBlock[i]
                            );
                        }

                        Arrays.sort(block);

                        long key = blockKey(block);

                        orbit.putIfAbsent(key, block);
                    }
                }
            }
        }

        return orbit.values().toArray(new int[0][]);
    }

    // --------------------------------------------------
    // Construct the two base blocks.
    // --------------------------------------------------

    static int[] baseBlock1() {

        int t = 3;

        return new int[] {
                INF,
                0,
                1,
                2,
                t,
                add(t, 1),
                add(t, 2)
        };
    }

    static int[] baseBlock2() {

        int t = 3;
        int t2 = mul(t, t);

        int two = 2;

        return new int[] {
                INF,
                0,
                1,
                add(mul(two, t2), mul(two, t)),
                mul(two, t2),
                add(add(t2, mul(two, t)), two),
                add(t2, mul(two, t))
        };
    }

    // --------------------------------------------------
    // Generate S(5,7,28).
    // --------------------------------------------------

    public static int[][] generateS5728() {

        int[][] orbit1 = orbit(baseBlock1());
        int[][] orbit2 = orbit(baseBlock2());

        System.out.println("Orbit 1: " + orbit1.length);
        System.out.println("Orbit 2: " + orbit2.length);

        if (orbit1.length != 3276)
            throw new IllegalStateException(
                    "Incorrect first orbit."
            );

        if (orbit2.length != 1404)
            throw new IllegalStateException(
                    "Incorrect second orbit."
            );

        List<int[]> result = new ArrayList<>();

        Set<Long> seen = new HashSet<>();

        for (int[] block : orbit1) {
            if (!seen.add(blockKey(block)))
                throw new IllegalStateException("Duplicate block.");

            result.add(block);
        }

        for (int[] block : orbit2) {
            if (!seen.add(blockKey(block)))
                throw new IllegalStateException("Orbits intersect.");

            result.add(block);
        }

        int[][] design = result.toArray(new int[0][]);

        if (!verify(28, 7, 5, design))
            throw new IllegalStateException(
                    "Generated design is not S(5,7,28)."
            );

        return design;
    }

    // --------------------------------------------------
    // Generic Steiner system verifier.
    //
    // Checks that every t-subset occurs exactly once.
    // --------------------------------------------------

    static long binomial(int n, int k) {

        long result = 1;

        for (int i = 1; i <= k; i++)
            result = result * (n - i + 1) / i;

        return result;
    }

    static void enumerate(
            int[] block,
            int t,
            int start,
            int depth,
            long key,
            int v,
            Set<Long> triples
    ) {

        if (depth == t) {

            if (!triples.add(key))
                throw new IllegalStateException(
                        "Repeated t-subset."
                );

            return;
        }

        for (int i = start;
             i <= block.length - (t - depth);
             i++) {

            enumerate(
                    block,
                    t,
                    i + 1,
                    depth + 1,
                    key * v + block[i],
                    v,
                    triples
            );
        }
    }

    public static boolean verify(
            int v,
            int k,
            int t,
            int[][] blocks
    ) {

        try {

            Set<Long> subsets = new HashSet<>();

            for (int[] block : blocks) {

                if (block.length != k)
                    return false;

                int[] b = block.clone();
                Arrays.sort(b);

                for (int i = 0; i < k; i++) {

                    if (b[i] < 0 || b[i] >= v)
                        return false;

                    if (i > 0 && b[i] == b[i - 1])
                        return false;
                }

                enumerate(
                        b, t, 0, 0, 0,
                        v, subsets
                );
            }

            long expected = binomial(v, t);

            long expectedBlocks =
                    expected / binomial(k, t);

            return subsets.size() == expected
                    && blocks.length == expectedBlocks;

        } catch (IllegalStateException e) {
            return false;
        }
    }

    // --------------------------------------------------
    // Derivation:
    //
    // S(t,k,v) -> S(t-1,k-1,v-1)
    // --------------------------------------------------

    public static int[][] derive(
            int[][] blocks,
            int p
    ) {

        List<int[]> result = new ArrayList<>();

        for (int[] block : blocks) {

            boolean contains = false;

            for (int x : block) {
                if (x == p) {
                    contains = true;
                    break;
                }
            }

            if (!contains)
                continue;

            int[] reduced = Arrays.stream(block)
                    .filter(x -> x != p)
                    .map(x -> x > p ? x - 1 : x)
                    .toArray();

            result.add(reduced);
        }

        return result.toArray(new int[0][]);
    }

    // --------------------------------------------------
    // Generate S(3,5,26).
    // --------------------------------------------------

    public static int[][] generateS3526() {

        int[][] s5728 = generateS5728();

        System.out.println("S(5,7,28): " + s5728.length);

        int[][] s4627 = derive(s5728, 27);

        System.out.println("S(4,6,27): " + s4627.length);

        if (!verify(27, 6, 4, s4627))
            throw new IllegalStateException(
                    "S(4,6,27) verification failed."
            );

        int[][] s3526 = derive(s4627, 26);

        System.out.println("S(3,5,26): " + s3526.length);

        if (!verify(26, 5, 3, s3526))
            throw new IllegalStateException(
                    "S(3,5,26) verification failed."
            );

        return s3526;
    }

    @Test
    public void testS3526() {

        int[][] design = generateS3526();

        System.out.println(
                "Final verification: " +
                        verify(26, 5, 3, design)
        );
    }


    // =====================================================
    // GENERAL UTILITIES
    // =====================================================

    static long binom(int n, int k) {
        long r = 1;
        for (int i = 1; i <= k; i++)
            r = r * (n - i + 1) / i;
        return r;
    }

    static int[] sorted(int[] b) {
        int[] r = b.clone();
        Arrays.sort(r);
        return r;
    }

    static long key(int[] b, int v) {
        int[] s = sorted(b);
        long r = 0;
        for (int x : s)
            r = r * v + x;
        return r;
    }

    static boolean contains(int[] b, int x) {
        for (int y : b)
            if (y == x) return true;
        return false;
    }

    static int[] removePoint(int[] b, int p) {
        return Arrays.stream(b)
                .filter(x -> x != p)
                .map(x -> x > p ? x - 1 : x)
                .toArray();
    }

    // =====================================================
    // GENERIC S(3,5,v) VERIFIER
    // =====================================================

    public static boolean verify(
            int v,
            int[][] blocks) {

        if (blocks.length != binom(v, 3) / 10)
            return false;

        Set<Long> triples = new HashSet<>();

        for (int[] block : blocks) {

            if (block.length != 5)
                return false;

            int[] b = sorted(block);

            for (int i = 0; i < 5; i++) {
                if (b[i] < 0 || b[i] >= v)
                    return false;

                if (i > 0 && b[i] == b[i - 1])
                    return false;
            }

            for (int i = 0; i < 5; i++)
                for (int j = i + 1; j < 5; j++)
                    for (int k = j + 1; k < 5; k++) {

                        long h =
                                ((long) b[i] * v + b[j])
                                        * v + b[k];

                        if (!triples.add(h))
                            return false;
                    }
        }

        return triples.size() == binom(v, 3);
    }

    // =====================================================
    // PART A: S(3,5,17)
    // =====================================================

    static final int INF17 = 16;

    static int[] ALPHA = new int[17];
    static int[] BETA = new int[17];

    static {

        for (int i = 0; i < 17; i++) {
            ALPHA[i] = i;
            BETA[i] = i;
        }

        // alpha = (infinity)(0)(1 2 ... 15)

        for (int i = 1; i <= 15; i++)
            ALPHA[i] = i == 15 ? 1 : i + 1;

        // beta = (0 1)(10 3)(2 13)(8 14)
        //        (5 4)(12 15)(6 11)(7 9)

        int[][] transpositions = {
                {0, 1},
                {10, 3},
                {2, 13},
                {8, 14},
                {5, 4},
                {12, 15},
                {6, 11},
                {7, 9}
        };

        for (int[] t : transpositions) {
            BETA[t[0]] = t[1];
            BETA[t[1]] = t[0];
        }
    }

    static int[] apply(int[] permutation, int[] block) {

        int[] result = new int[block.length];

        for (int i = 0; i < block.length; i++)
            result[i] = permutation[block[i]];

        return sorted(result);
    }

    static void addOrbitAlpha(
            int[] base,
            List<int[]> result,
            Set<Long> all) {

        Set<Long> local = new HashSet<>();

        int[] current = sorted(base);

        while (true) {

            long h = key(current, 17);

            if (!local.add(h))
                break;

            if (!all.add(h))
                throw new IllegalStateException(
                        "Overlapping orbits in S(3,5,17).");

            result.add(current);

            current = apply(ALPHA, current);
        }
    }

    static List<int[]> orbitAlphaBeta(int[] base) {

        List<int[]> result = new ArrayList<>();

        Set<Long> seen = new HashSet<>();

        Queue<int[]> queue = new ArrayDeque<>();

        queue.add(sorted(base));

        while (!queue.isEmpty()) {

            int[] block = queue.remove();

            long h = key(block, 17);

            if (!seen.add(h))
                continue;

            result.add(block);

            queue.add(apply(ALPHA, block));
            queue.add(apply(BETA, block));
        }

        return result;
    }

    public static int[][] generateS3517() {

        List<int[]> result = new ArrayList<>();

        Set<Long> all = new HashSet<>();

        int[] first = {
                16, 0, 1, 6, 11
        };

        List<int[]> firstOrbit =
                orbitAlphaBeta(first);

        if (firstOrbit.size() != 20)
            throw new IllegalStateException(
                    "First orbit should have 20 blocks.");

        for (int[] b : firstOrbit) {
            long h = key(b, 17);
            if (!all.add(h))
                throw new IllegalStateException("Duplicate block.");
            result.add(b);
        }

        int[][] remaining = {
                {1, 4, 7, 10, 13},
                {0, 5, 9, 3, 2},
                {13, 4, 14, 6, 8},
                {10, 11, 7, 15, 12}
        };

        int[] expected = {3, 15, 15, 15};

        for (int i = 0; i < remaining.length; i++) {

            int oldSize = result.size();

            addOrbitAlpha(
                    remaining[i], result, all
            );

            if (result.size() - oldSize != expected[i])
                throw new IllegalStateException(
                        "Incorrect alpha orbit.");
        }

        int[][] design =
                result.toArray(new int[0][]);

        if (design.length != 68 || !verify(17, design))
            throw new IllegalStateException(
                    "Invalid S(3,5,17).");

        return design;
    }

    // =====================================================
    // PART B: EXTRACT THE 1-FAN
    // =====================================================

    public static class OneFan {

        int[][] groups;
        int[][] b1;
        int[][] t;

        int[] groupOf = new int[16];
        int[] positionInGroup = new int[16];

        OneFan(
                int[][] groups,
                int[][] b1,
                int[][] t) {

            this.groups = groups;
            this.b1 = b1;
            this.t = t;

            for (int i = 0; i < groups.length; i++) {
                for (int j = 0; j < groups[i].length; j++) {

                    int x = groups[i][j];

                    groupOf[x] = i;
                    positionInGroup[x] = j;
                }
            }
        }
    }

    static boolean disjoint(int[] a, int[] b) {

        for (int x : a)
            for (int y : b)
                if (x == y)
                    return false;

        return true;
    }

    static boolean findParallelClass(
            int[][] lines,
            int start,
            List<int[]> chosen,
            boolean[] used) {

        if (chosen.size() == 4)
            return true;

        for (int i = start; i < lines.length; i++) {

            if (chosen.size() > 0) {

                boolean ok = true;

                for (int[] old : chosen)
                    if (!disjoint(old, lines[i]))
                        ok = false;

                if (!ok)
                    continue;
            }

            chosen.add(lines[i]);

            if (findParallelClass(
                    lines, i + 1, chosen, used))
                return true;

            chosen.remove(chosen.size() - 1);
        }

        return false;
    }

    public static OneFan generateOneFan() {

        int[][] s3517 = generateS3517();

        List<int[]> linesList = new ArrayList<>();
        List<int[]> tList = new ArrayList<>();

        for (int[] block : s3517) {

            if (contains(block, 16)) {
                linesList.add(
                        Arrays.stream(block)
                                .filter(x -> x != 16)
                                .toArray()
                );
            } else {
                tList.add(block);
            }
        }

        int[][] lines =
                linesList.toArray(new int[0][]);

        if (lines.length != 20 || tList.size() != 48)
            throw new IllegalStateException(
                    "Incorrect derived affine plane.");

        List<int[]> chosen = new ArrayList<>();

        if (!findParallelClass(
                lines, 0, chosen, new boolean[16]))
            throw new IllegalStateException(
                    "Parallel class not found.");

        int[][] groups = new int[4][4];

        Set<Long> groupKeys = new HashSet<>();

        for (int i = 0; i < 4; i++) {
            groups[i] = sorted(chosen.get(i));
            groupKeys.add(key(groups[i], 17));
        }

        List<int[]> b1List = new ArrayList<>();

        for (int[] line : lines) {
            if (!groupKeys.contains(key(line, 17)))
                b1List.add(line);
        }

        OneFan fan = new OneFan(
                groups,
                b1List.toArray(new int[0][]),
                tList.toArray(new int[0][])
        );

        if (fan.b1.length != 16 || fan.t.length != 48)
            throw new IllegalStateException(
                    "Incorrect 1-fan.");

        if (!verifyOneFan(fan))
            throw new IllegalStateException(
                    "The ingredient is not a 1-fan.");

        return fan;
    }

    // Checks:
    // (X,G union B1) is a PBD;
    // (X,G union B1 union T) is a 3-BD.

    static boolean verifyOneFan(OneFan fan) {

        Set<Long> pairs = new HashSet<>();
        Set<Long> triples = new HashSet<>();

        List<int[]> all = new ArrayList<>();

        all.addAll(Arrays.asList(fan.groups));
        all.addAll(Arrays.asList(fan.b1));
        all.addAll(Arrays.asList(fan.t));

        for (int[] block : all) {

            int[] b = sorted(block);

            for (int x : b)
                if (x < 0 || x >= 16)
                    return false;

            for (int i = 0; i < b.length; i++) {
                for (int j = i + 1; j < b.length; j++) {

                    long h = (long) b[i] * 16 + b[j];

                    if (fan.t.length > 0 &&
                            !Arrays.asList(fan.groups).contains(block)) {
                        // Pair counting is restricted below.
                    }
                }
            }
        }

        // PBD: groups and B1.

        List<int[]> pbd = new ArrayList<>();
        pbd.addAll(Arrays.asList(fan.groups));
        pbd.addAll(Arrays.asList(fan.b1));

        for (int[] block : pbd) {
            int[] b = sorted(block);

            for (int i = 0; i < b.length; i++)
                for (int j = i + 1; j < b.length; j++)
                    if (!pairs.add((long) b[i] * 16 + b[j]))
                        return false;
        }

        if (pairs.size() != binom(16, 2))
            return false;

        // 3-BD: groups, B1, and T.

        for (int[] block : all) {
            int[] b = sorted(block);

            for (int i = 0; i < b.length; i++)
                for (int j = i + 1; j < b.length; j++)
                    for (int k = j + 1; k < b.length; k++) {

                        long h =
                                ((long) b[i] * 16 + b[j])
                                        * 16 + b[k];

                        if (!triples.add(h))
                            return false;
                    }
        }

        return triples.size() == binom(16, 3);
    }

    // =====================================================
    // PART C: TRIPARTITE DESIGN
    // =====================================================

    // GF(4) = {0,1,alpha,alpha+1}
    // alpha^2 + alpha + 1 = 0.

    static int gfMul(int a, int b) {

        int result = 0;
        int x = a;
        int y = b;

        while (y != 0) {

            if ((y & 1) != 0)
                result ^= x;

            y >>= 1;
            x <<= 1;

            if ((x & 4) != 0)
                x ^= 7;
        }

        return result;
    }

    static final int[][] C = {
            {1, 0, 0},
            {1, 1, 1},
            {1, 2, 3},
            {1, 3, 2},
            {0, 0, 1},
            {0, 1, 0}
    };

    // Delete the sixth group.
    // Result: 5 groups of 4, 64 blocks of size 5.

    public static int[][] tripartiteDesign() {

        List<int[]> result = new ArrayList<>();

        for (int a = 0; a < 4; a++)
            for (int b = 0; b < 4; b++)
                for (int c = 0; c < 4; c++) {

                    int[] block = new int[5];

                    for (int i = 0; i < 5; i++) {

                        int value =
                                gfMul(a, C[i][0])
                                        ^ gfMul(b, C[i][1])
                                        ^ gfMul(c, C[i][2]);

                        block[i] = 4 * i + value;
                    }

                    result.add(block);
                }

        int[][] design =
                result.toArray(new int[0][]);

        if (!verifyTripartite(design))
            throw new IllegalStateException(
                    "Invalid tripartite ingredient.");

        return design;
    }

    static boolean verifyTripartite(int[][] blocks) {

        if (blocks.length != 64)
            return false;

        Set<Long> triples = new HashSet<>();

        for (int[] block : blocks) {

            if (block.length != 5)
                return false;

            boolean[] groups = new boolean[5];

            for (int x : block) {

                int g = x / 4;

                if (x < 0 || x >= 20 || groups[g])
                    return false;

                groups[g] = true;
            }

            int[] b = sorted(block);

            for (int i = 0; i < 5; i++)
                for (int j = i + 1; j < 5; j++)
                    for (int k = j + 1; k < 5; k++) {

                        long h =
                                ((long) b[i] * 20 + b[j])
                                        * 20 + b[k];

                        if (!triples.add(h))
                            return false;
                    }
        }

        return triples.size() == 10 * 64;
    }

    // =====================================================
    // PART D: MAP SLAVE BLOCKS
    // =====================================================

    static int[] mapFanBlock(
            int[] slave,
            OneFan fan,
            int[] master) {

        int[] result = new int[slave.length];

        for (int i = 0; i < slave.length; i++) {

            int x = slave[i];

            int group = fan.groupOf[x];
            int position = fan.positionInGroup[x];

            result[i] =
                    4 * master[group] + position;
        }

        return sorted(result);
    }

    static int[] mapTripartiteBlock(
            int[] slave,
            int[] master) {

        int[] result = new int[slave.length];

        for (int i = 0; i < slave.length; i++) {

            int x = slave[i];

            int group = x / 4;
            int position = x % 4;

            result[i] =
                    4 * master[group] + position;
        }

        return sorted(result);
    }

    static int[] appendPoint(int[] block, int p) {

        int[] result =
                Arrays.copyOf(block, block.length + 1);

        result[block.length] = p;

        return sorted(result);
    }

    // =====================================================
    // PART E: HANANI RECURSION
    //
    // S(3,5,v) -> S(3,5,4v-3)
    // =====================================================

    static void addBlock(
            List<int[]> result,
            Set<Long> seen,
            int[] block,
            int newV) {

        int[] b = sorted(block);

        if (b.length != 5)
            throw new IllegalStateException(
                    "Incorrect output block size.");

        for (int x : b)
            if (x < 0 || x >= newV)
                throw new IllegalStateException(
                        "Point outside output set.");

        long h = key(b, newV);

        if (!seen.add(h))
            throw new IllegalStateException(
                    "Duplicate output block: " +
                            Arrays.toString(b));

        result.add(b);
    }

    public static int[][] hanani(
            int v,
            int[][] inputBlocks) {

        if (!verify(v, inputBlocks))
            throw new IllegalArgumentException(
                    "Input is not S(3,5,v).");

        OneFan fan = generateOneFan();

        int[][] tripartite = tripartiteDesign();

        int deletedPoint = v - 1;

        int infinity = 4 * (v - 1);

        int newV = infinity + 1;

        List<int[]> output = new ArrayList<>();

        Set<Long> seen = new HashSet<>();

        int numberB1 = 0;
        int numberT = 0;

        // ---------------------------------------------
        // Master B1:
        // Blocks containing the deleted point.
        // Remove the deleted point, obtaining 4-sets.
        // ---------------------------------------------

        for (int[] original : inputBlocks) {

            if (!contains(original, deletedPoint))
                continue;

            numberB1++;

            int[] master =
                    removePoint(original, deletedPoint);

            // Slave 1-fan: B1 component.
            //
            // Add infinity to each resulting p-block.

            for (int[] slave : fan.b1) {

                int[] mapped =
                        mapFanBlock(slave, fan, master);

                addBlock(
                        output,
                        seen,
                        appendPoint(mapped, infinity),
                        newV
                );
            }

            // Slave 1-fan: T component.
            //
            // These are already 5-blocks.

            for (int[] slave : fan.t) {

                int[] mapped =
                        mapFanBlock(slave, fan, master);

                addBlock(output, seen, mapped, newV);
            }
        }

        // ---------------------------------------------
        // Master T:
        // Original blocks not containing deleted point.
        // These are 5-sets.
        // ---------------------------------------------

        for (int[] master : inputBlocks) {

            if (contains(master, deletedPoint))
                continue;

            numberT++;

            // Slave tripartite design.

            for (int[] slave : tripartite) {

                int[] mapped =
                        mapTripartiteBlock(slave, master);

                addBlock(output, seen, mapped, newV);
            }
        }

        // ---------------------------------------------
        // Completion:
        //
        // Add infinity to every group S_x.
        // ---------------------------------------------

        for (int x = 0; x < v - 1; x++) {

            int[] group = {
                    4 * x,
                    4 * x + 1,
                    4 * x + 2,
                    4 * x + 3
            };

            addBlock(
                    output,
                    seen,
                    appendPoint(group, infinity),
                    newV
            );
        }

        // ---------------------------------------------
        // Check ingredient counts.
        // ---------------------------------------------

        if (numberB1 != binom(v - 1, 2) / 6)
            throw new IllegalStateException(
                    "Incorrect master B1 size.");

        if (numberB1 + numberT != inputBlocks.length)
            throw new IllegalStateException(
                    "Incorrect master block partition.");

        int[][] result =
                output.toArray(new int[0][]);

        // ---------------------------------------------
        // Independent final verification.
        // ---------------------------------------------

        if (!verify(newV, result))
            throw new IllegalStateException(
                    "Hanani construction failed verification.");

        return result;
    }

    @Test
    public void testHananiRecur() {
        int[][] s3517 = generateS3517();

        System.out.println(
                "S(3,5,17): " + s3517.length);

        OneFan fan = generateOneFan();

        System.out.println(
                "1-fan: groups=" + fan.groups.length
                        + ", B1=" + fan.b1.length
                        + ", T=" + fan.t.length);

        int[][] td = tripartiteDesign();

        System.out.println(
                "Tripartite blocks: " + td.length);

        // Replace this with your existing S(3,5,26).
        int[][] originalBlocks = generateS3526();

        int[][] larger =
                hanani(26, originalBlocks);

        System.out.println(
                "S(3,5,101): " + larger.length);

        System.out.println(
                "Verified: " +
                        verify(101, larger));
    }
}
