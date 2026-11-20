package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HananiS386Test {

    // -------------------------------------------------------
    // Data structure for the 2-fan ingredient
    // -------------------------------------------------------

    public static class TwoFan {
        public int[][] groups;
        public int[][] b1;
        public int[][] b2;
        public int[][] t;

        public TwoFan(int[][] groups, int[][] b1,
                      int[][] b2, int[][] t) {
            this.groups = groups;
            this.b1 = b1;
            this.b2 = b2;
            this.t = t;
        }
    }

    // -------------------------------------------------------
    // GF(4)
    //
    // Elements:
    // 0 = 0
    // 1 = 1
    // 2 = alpha
    // 3 = alpha + 1
    //
    // alpha^2 + alpha + 1 = 0
    // -------------------------------------------------------

    private static int gfAdd(int a, int b) {
        return a ^ b;
    }

    private static int gfMul(int a, int b) {
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

    // Six columns defining the hyperoval in PG(2,4).
    private static final int[][] C = {
            {1, 0, 0},
            {1, 1, 1},
            {1, 2, 3},
            {1, 3, 2},
            {0, 0, 1},
            {0, 1, 0}
    };

    // -------------------------------------------------------
    // Brouwer's tripartite design:
    // 6 groups of size 4, 64 blocks of size 6.
    // -------------------------------------------------------

    public static int[][] tripartiteDesign4() {

        List<int[]> result = new ArrayList<>();

        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) {
                for (int c = 0; c < 4; c++) {

                    int[] block = new int[6];

                    for (int i = 0; i < 6; i++) {

                        int value = gfAdd(
                                gfAdd(
                                        gfMul(a, C[i][0]),
                                        gfMul(b, C[i][1])
                                ),
                                gfMul(c, C[i][2])
                        );

                        block[i] = 4 * i + value;
                    }

                    result.add(block);
                }
            }
        }

        return result.toArray(new int[0][]);
    }

    // -------------------------------------------------------
    // Extract the 2-fan from S(3,6,22).
    //
    // Distinguished points are 0 and 1.
    //
    // The five groups are the blocks containing both
    // distinguished points, with 0 and 1 removed.
    //
    // All ingredient points are renumbered 0,...,19,
    // with group i occupying [4*i, 4*i+3].
    // -------------------------------------------------------

    public static TwoFan extractTwoFan(int[][] witt) {

        List<int[]> groups = new ArrayList<>();
        List<int[]> rawB1 = new ArrayList<>();
        List<int[]> rawB2 = new ArrayList<>();
        List<int[]> rawT = new ArrayList<>();

        for (int[] block : witt) {

            boolean has0 = false;
            boolean has1 = false;

            for (int x : block) {
                if (x == 0) has0 = true;
                if (x == 1) has1 = true;
            }

            int[] reduced = Arrays.stream(block)
                    .filter(x -> x != 0 && x != 1)
                    .toArray();

            if (has0 && has1) {
                groups.add(reduced);
            } else if (has0) {
                rawB1.add(reduced);
            } else if (has1) {
                rawB2.add(reduced);
            } else {
                rawT.add(reduced);
            }
        }

        if (groups.size() != 5)
            throw new IllegalArgumentException(
                    "Invalid S(3,6,22): expected 5 groups."
            );

        // Map original Witt points to canonical labels.
        int[] map = new int[22];
        Arrays.fill(map, -1);

        for (int i = 0; i < 5; i++) {

            int[] g = groups.get(i);
            Arrays.sort(g);

            if (g.length != 4)
                throw new IllegalArgumentException(
                        "Invalid group size."
                );

            for (int j = 0; j < 4; j++) {
                if (map[g[j]] != -1)
                    throw new IllegalArgumentException(
                            "Groups are not disjoint."
                    );

                map[g[j]] = 4 * i + j;
            }
        }

        // All 20 non-distinguished points must occur.
        for (int x = 2; x < 22; x++) {
            if (map[x] == -1)
                throw new IllegalArgumentException(
                        "Groups do not partition the points."
                );
        }

        int[][] b1 = remap(rawB1, map);
        int[][] b2 = remap(rawB2, map);
        int[][] t = remap(rawT, map);

        return new TwoFan(
                groupsToCanonical(groups),
                b1, b2, t
        );
    }

    private static int[][] groupsToCanonical(List<int[]> groups) {
        int[][] result = new int[5][4];

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 4 * i + j;
            }
        }

        return result;
    }

    private static int[][] remap(List<int[]> blocks, int[] map) {
        int[][] result = new int[blocks.size()][];

        for (int i = 0; i < blocks.size(); i++) {

            int[] oldBlock = blocks.get(i);
            int[] newBlock = new int[oldBlock.length];

            for (int j = 0; j < oldBlock.length; j++) {
                newBlock[j] = map[oldBlock[j]];

                if (newBlock[j] < 0)
                    throw new IllegalArgumentException(
                            "Invalid ingredient point."
                    );
            }

            result[i] = newBlock;
        }

        return result;
    }

    // -------------------------------------------------------
    // Map a slave block to the inflated master points.
    //
    // Local point 4*i + j becomes:
    //
    //     4 * masterBlock[i] + j
    //
    // masterBlock must have length 5 or 6.
    // -------------------------------------------------------

    private static int[] mapBlock(
            int[] slaveBlock,
            int[] masterBlock
    ) {

        int[] result = new int[slaveBlock.length];

        for (int i = 0; i < slaveBlock.length; i++) {

            int x = slaveBlock[i];

            int group = x / 4;
            int value = x % 4;

            if (group >= masterBlock.length)
                throw new IllegalArgumentException(
                        "Slave block references nonexistent group."
                );

            result[i] = 4 * masterBlock[group] + value;
        }

        return result;
    }

    private static int[] append(int[] block, int... points) {

        int[] result = Arrays.copyOf(
                block, block.length + points.length
        );

        for (int i = 0; i < points.length; i++)
            result[block.length + i] = points[i];

        return result;
    }

    // -------------------------------------------------------
    // Main Hanani construction:
    //
    // S(3,6,v) -> S(3,6,4v-2)
    //
    // witt22 must be a verified S(3,6,22).
    // -------------------------------------------------------

    public static int[][] construct(
            int v,
            int[][] inputBlocks,
            int[][] witt22
    ) {

        if (!verify(v, inputBlocks))
            throw new IllegalArgumentException(
                    "Input is not an S(3,6,v)."
            );

        if (!verify(22, witt22))
            throw new IllegalArgumentException(
                    "Ingredient is not an S(3,6,22)."
            );

        TwoFan ingredient = extractTwoFan(witt22);

        int[][] tripartite = tripartiteDesign4();

        int deletedPoint = v - 1;

        // New points:
        //
        // 0,...,4(v-1)-1 : inflated original points
        // 4(v-1)         : first stem point
        // 4(v-1)+1       : second stem point

        int a = 4 * (v - 1);
        int b = a + 1;

        List<int[]> output = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // ---------------------------------------------------
        // Master blocks B1:
        // Original blocks containing deletedPoint.
        // Delete deletedPoint, obtaining 5-point blocks.
        //
        // Use the 2-fan extracted from S(3,6,22).
        // ---------------------------------------------------

        for (int[] original : inputBlocks) {

            boolean containsDeleted = false;

            for (int x : original) {
                if (x == deletedPoint) {
                    containsDeleted = true;
                    break;
                }
            }

            if (!containsDeleted)
                continue;

            int[] master = Arrays.stream(original)
                    .filter(x -> x != deletedPoint)
                    .toArray();

            // Slave component B1:
            // Append first stem point a.
            for (int[] slave : ingredient.b1) {
                int[] mapped = mapBlock(slave, master);
                add(output, seen, append(mapped, a));
            }

            // Slave component B2:
            // Append second stem point b.
            for (int[] slave : ingredient.b2) {
                int[] mapped = mapBlock(slave, master);
                add(output, seen, append(mapped, b));
            }

            // Slave component T:
            // No stem point is added.
            for (int[] slave : ingredient.t) {
                int[] mapped = mapBlock(slave, master);
                add(output, seen, mapped);
            }
        }

        // ---------------------------------------------------
        // Master blocks T:
        // Original blocks not containing deletedPoint.
        //
        // Use the tripartite design with six groups.
        // ---------------------------------------------------

        for (int[] master : inputBlocks) {

            boolean containsDeleted = false;

            for (int x : master) {
                if (x == deletedPoint) {
                    containsDeleted = true;
                    break;
                }
            }

            if (containsDeleted)
                continue;

            for (int[] slave : tripartite) {

                int[] mapped = mapBlock(slave, master);

                add(output, seen, mapped);
            }
        }

        // ---------------------------------------------------
        // Completion:
        //
        // For every inflated group S_x, add:
        //
        //     S_x union {a,b}
        //
        // ---------------------------------------------------

        for (int x = 0; x < v - 1; x++) {

            int[] block = {
                    4 * x,
                    4 * x + 1,
                    4 * x + 2,
                    4 * x + 3,
                    a,
                    b
            };

            add(output, seen, block);
        }

        int[][] result = output.toArray(new int[0][]);

        int newV = 4 * v - 2;

        if (!verify(newV, result))
            throw new IllegalStateException(
                    "Construction failed verification."
            );

        return result;
    }

    private static void add(
            List<int[]> output,
            Set<String> seen,
            int[] block
    ) {

        int[] copy = block.clone();
        Arrays.sort(copy);

        if (copy.length != 6)
            throw new IllegalArgumentException(
                    "Output block does not have size 6."
            );

        String key = Arrays.toString(copy);

        if (!seen.add(key))
            throw new IllegalStateException(
                    "Duplicate block: " + key
            );

        output.add(copy);
    }

    // -------------------------------------------------------
    // Independent verification of S(3,6,v).
    //
    // Every triple must occur exactly once.
    // -------------------------------------------------------

    public static boolean verify(
            int v,
            int[][] blocks
    ) {

        if (v < 6)
            return false;

        Set<Long> triples = new HashSet<>();

        for (int[] block : blocks) {

            if (block.length != 6)
                return false;

            int[] sorted = block.clone();
            Arrays.sort(sorted);

            for (int i = 0; i < 6; i++) {

                if (sorted[i] < 0 || sorted[i] >= v)
                    return false;

                if (i > 0 && sorted[i] == sorted[i - 1])
                    return false;
            }

            for (int i = 0; i < 6; i++) {
                for (int j = i + 1; j < 6; j++) {
                    for (int k = j + 1; k < 6; k++) {

                        long key =
                                ((long) sorted[i] * v + sorted[j])
                                        * v + sorted[k];

                        if (!triples.add(key))
                            return false;
                    }
                }
            }
        }

        long expected =
                ((long) v * (v - 1) * (v - 2)) / 6;

        return triples.size() == expected;
    }

    private static final int[][] WITT_22 = generateWitt22();

    @Test
    public void test86() {
        int v = 22;
        int[][] larger = construct(
                v,
                WITT_22,
                WITT_22
        );
        InversivePlane pl = new InversivePlane(larger);
        System.out.println(JNauty.instance().nauty(pl).autCount());

        System.out.println("Number of blocks: " + larger.length);
        System.out.println("Verified: " +
                verify(4 * v - 2, larger));
    }

    private static final long GENERATOR =
            (1L << 0) |
                    (1L << 2) |
                    (1L << 4) |
                    (1L << 5) |
                    (1L << 6) |
                    (1L << 10) |
                    (1L << 11);

    /**
     * Generates S(3,6,22) from the extended binary Golay code.
     *
     * Points are numbered 0,...,21.
     *
     * Construction:
     * 1. Generate all 4096 Golay codewords.
     * 2. Extend each by an overall parity bit.
     * 3. Select weight-8 codewords containing coordinates 0 and 1.
     * 4. Delete coordinates 0 and 1.
     *
     * Returns exactly 77 blocks.
     */
    public static int[][] generateWitt22() {

        List<int[]> blocks = new ArrayList<>();

        for (int message = 0; message < 4096; message++) {

            long codeword = 0;

            // Multiply message polynomial by generator polynomial.
            for (int i = 0; i < 12; i++) {

                if (((message >>> i) & 1) != 0) {
                    codeword ^= GENERATOR << i;
                }
            }

            // Extend by an overall parity bit at position 23.
            if ((Long.bitCount(codeword) & 1) != 0) {
                codeword |= (1L << 23);
            }

            // Select octads (weight 8).
            if (Long.bitCount(codeword) != 8)
                continue;

            // Fix coordinates 0 and 1.
            if ((codeword & 3L) != 3L)
                continue;

            // Delete coordinates 0 and 1.
            int[] block = new int[6];
            int index = 0;

            for (int i = 2; i < 24; i++) {

                if (((codeword >>> i) & 1L) != 0) {
                    block[index++] = i - 2;
                }
            }

            if (index != 6)
                throw new IllegalStateException(
                        "Incorrect octad."
                );

            blocks.add(block);
        }

        if (blocks.size() != 77)
            throw new IllegalStateException(
                    "Expected 77 blocks, got " + blocks.size()
            );

        int[][] result = blocks.toArray(new int[0][]);

        if (!verify(result))
            throw new IllegalStateException(
                    "Generated design is not S(3,6,22)."
            );

        return result;
    }

    /**
     * Checks whether every 3-subset of {0,...,21}
     * occurs in exactly one block.
     */
    public static boolean verify(int[][] blocks) {

        boolean[][][] unused = null;

        // Number triples using a compact one-dimensional array.
        int[][][] count = new int[22][22][22];

        for (int[] block : blocks) {

            if (block.length != 6)
                return false;

            int[] b = block.clone();
            Arrays.sort(b);

            for (int i = 0; i < 6; i++) {

                if (b[i] < 0 || b[i] >= 22)
                    return false;

                if (i > 0 && b[i] == b[i - 1])
                    return false;
            }

            for (int i = 0; i < 6; i++) {
                for (int j = i + 1; j < 6; j++) {
                    for (int k = j + 1; k < 6; k++) {

                        if (++count[b[i]][b[j]][b[k]] != 1)
                            return false;
                    }
                }
            }
        }

        for (int i = 0; i < 22; i++) {
            for (int j = i + 1; j < 22; j++) {
                for (int k = j + 1; k < 22; k++) {

                    if (count[i][j][k] != 1)
                        return false;
                }
            }
        }

        return blocks.length == 77;
    }

    @Test
    public void witt() {

        int[][] witt = generateWitt22();

        System.out.println("WITT_22 generated.");
        System.out.println("Number of points: 22");
        System.out.println("Number of blocks: " + witt.length);
        System.out.println("Verification: " + verify(witt));

        for (int[] block : witt) {
            System.out.println(Arrays.toString(block));
        }
    }
}
