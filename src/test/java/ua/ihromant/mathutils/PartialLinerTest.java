package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

public class PartialLinerTest {
    @Test
    public void testPartial() {
        int[][] arr = new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 5}
        };
        PartialLiner base = new PartialLiner(9, arr);
        int[][] arr1 = new int[6][];
        int[][] arr2 = new int[6][];
        System.arraycopy(arr, 0, arr1, 0, arr.length);
        System.arraycopy(arr, 0, arr2, 0, arr.length);
        int[] line1 = new int[]{1, 4, 6};
        int[] line2 = new int[]{1, 4, 7};
        arr1[5] = line1;
        arr2[5] = line2;
        PartialLiner byArr1 = new PartialLiner(9, arr1);
        PartialLiner byArr2 = new PartialLiner(9, arr2);
        PartialLiner byLine1 = new PartialLiner(base, line1);
        PartialLiner byLine2 = new PartialLiner(base, line2);

        assertEquals(byArr1.pointCount(), byLine1.pointCount());
        assertEquals(byArr2.pointCount(), byLine2.pointCount());

        assertArrayEquals(byArr1.lines(), byLine1.lines());
        assertArrayEquals(byArr2.lines(), byLine2.lines());

        assertArrayEquals(byArr1.beamCounts(), byLine1.beamCounts());
        assertArrayEquals(byArr2.beamCounts(), byLine2.beamCounts());

        assertArrayEquals(byArr1.beamLengths(), byLine1.beamLengths());
        assertArrayEquals(byArr2.beamLengths(), byLine2.beamLengths());

        assertArrayEquals(byArr1.lookup(), byLine1.lookup());
        assertArrayEquals(byArr2.lookup(), byLine2.lookup());

        assertArrayEquals(byArr1.beams(), byLine1.beams());
        assertArrayEquals(byArr2.beams(), byLine2.beams());

        assertArrayEquals(byArr1.beamDist(), byLine1.beamDist());
        assertArrayEquals(byArr2.beamDist(), byLine2.beamDist());

        assertArrayEquals(byArr1.intersections(), byLine1.intersections());
        assertArrayEquals(byArr2.intersections(), byLine2.intersections());

        assertArrayEquals(byArr1.lineInter(), byLine1.lineInter());
        assertArrayEquals(byArr2.lineInter(), byLine2.lineInter());

        assertArrayEquals(byArr1.lineFreq(), byLine1.lineFreq());
        assertArrayEquals(byArr2.lineFreq(), byLine2.lineFreq());

        List<int[]> byConsNext = new ArrayList<>();
        List<int[]> byIteratorNext = new ArrayList<>();

        base.blocks(bl -> byConsNext.add(bl.clone()));
        for (int[] bl : base.blocks()) {
            byIteratorNext.add(bl);
        }
        assertArrayEquals(byConsNext.toArray(int[][]::new), byIteratorNext.toArray(int[][]::new));
        assertEquals(3, byIteratorNext.size());
        byConsNext.clear();
        byIteratorNext.clear();

        int[][] spr = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {0, 7, 8, 9},
                {0, 10, 11, 12},
                {1, 4, 7, 10}
        };
        PartialLiner par = new PartialLiner(spr);
        par.blocks(bl -> byConsNext.add(bl.clone()));
        for (int[] bl : par.blocks()) {
            byIteratorNext.add(bl);
        }
        assertArrayEquals(byConsNext.toArray(int[][]::new), byIteratorNext.toArray(int[][]::new));
        assertEquals(4, byIteratorNext.size());
        byIteratorNext.clear();

        int[][] spr1 = new int[][]{
                {0, 1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11},
                {12, 13, 14, 15},
                {0, 4, 8, 12}
        };
        par = new PartialLiner(spr1);
        for (int[] bl : par.blocksResolvable()) {
            byIteratorNext.add(bl);
        }
        assertEquals(27, byIteratorNext.size());
    }

    @Test
    public void testIsomorphic() {
        testSample(PartialLiner::isomorphicSel);
        testSample(PartialLiner::isomorphic);
        testSample(PartialLiner::isomorphicL);
    }

    private void testSample(BiPredicate<PartialLiner, PartialLiner> iso) {
        PartialLiner first7 = new PartialLiner(Liner.byStrings(new String[]{
                "0001123",
                "1242534",
                "3654656"
        }).lines());
        PartialLiner second7 = new PartialLiner(new Liner(new GaloisField(2).generatePlane()).lines());
        assertTrue(iso.test(first7, second7));
        assertTrue(iso.test(second7, first7));
        PartialLiner first13 = new PartialLiner(Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"
        }).lines());
        PartialLiner alt13 = new PartialLiner(Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9"
        }).lines());
        PartialLiner second13 = new PartialLiner(Liner.byDiffFamily(new int[]{0, 6, 8}, new int[]{0, 9, 10}).lines());
        assertTrue(iso.test(first13, second13));
        assertTrue(iso.test(second13, first13));
        assertFalse(iso.test(alt13, first13));
        PartialLiner first9 = new PartialLiner(Liner.byStrings(new String[]{
                "000011122236",
                "134534534547",
                "268787676858"
        }).lines());
        PartialLiner second9 = new PartialLiner(new AffinePlane(new Liner(new GaloisField(3).generatePlane()), 0).toLiner().lines());
        assertTrue(iso.test(first9, second9));
        PartialLiner firstFlat15 = new PartialLiner(Liner.byStrings(new String[] {
                "00000001111112222223333444455566678",
                "13579bd3469ac34578b678a58ab78979c9a",
                "2468ace578bde96aecdbcded9cebecaeddb"
        }).lines());
        PartialLiner firstSpace15 = new PartialLiner(Liner.byStrings(new String[]{
                "00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb"
        }).lines());
        PartialLiner secondFlat15 = new PartialLiner(Liner.byDiffFamily(15, new int[]{0, 6, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10}).lines());
        PartialLiner secondSpace15 = new PartialLiner(Liner.byDiffFamily(15, new int[]{0, 2, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10}).lines());
        PartialLiner thirdSpace15 = new PartialLiner(new Liner(new GaloisField(2).generateSpace()).lines());
        assertTrue(iso.test(firstFlat15, secondFlat15));
        assertTrue(iso.test(firstSpace15, secondSpace15));
        assertTrue(iso.test(firstSpace15, thirdSpace15));
        assertTrue(iso.test(firstSpace15, thirdSpace15));
        assertFalse(iso.test(firstFlat15, firstSpace15));
        PartialLiner firstPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}});
        PartialLiner secondPartial = new PartialLiner(9, new int[][]{{6, 7, 8}, {4, 5, 8}});
        PartialLiner thirdPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {3, 4, 5}});
        PartialLiner fourthPartial = new PartialLiner(9, new int[][]{{0, 3, 6}, {1, 4, 7}});
        PartialLiner fifthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}});
        PartialLiner sixthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}});
        assertTrue(iso.test(firstPartial, secondPartial));
        assertFalse(iso.test(firstPartial, thirdPartial));
        assertTrue(iso.test(thirdPartial, fourthPartial));
        assertTrue(iso.test(fifthPartial, sixthPartial));
        for (int i = 0; i < partials.length; i++) {
            PartialLiner pl1 = new PartialLiner(partials[i]);
            for (int j = 0; j < partials.length; j++) {
                if (i == j) {
                    continue;
                }
                PartialLiner pl2 = new PartialLiner(partials[j]);
                assertFalse(iso.test(pl1, pl2));
                assertFalse(iso.test(pl2, pl1));
            }
        }
    }

    private static final int[][][] partials = new int[][][]{
            {{0, 1, 2, 3, 4, 5, 6},
                    {0, 7, 8, 9, 10, 11, 12},
                    {0, 13, 14, 15, 16, 17, 18},
                    {0, 19, 20, 21, 22, 23, 24},
                    {0, 25, 26, 27, 28, 29, 30},
                    {0, 31, 32, 33, 34, 35, 36},
                    {0, 37, 38, 39, 40, 41, 42},
                    {1, 7, 13, 19, 25, 31, 37},
                    {1, 8, 14, 20, 26, 32, 38},
                    {1, 9, 15, 21, 27, 33, 39},
                    {1, 10, 16, 22, 28, 34, 40},
                    {1, 11, 17, 23, 29, 35, 41},
                    {1, 12, 18, 24, 30, 36, 42},
                    {2, 7, 14, 21, 28, 35, 42},
                    {2, 8, 13, 22, 27, 36, 41}},

            {{0, 1, 2, 3, 4, 5, 6},
                    {0, 7, 8, 9, 10, 11, 12},
                    {0, 13, 14, 15, 16, 17, 18},
                    {0, 19, 20, 21, 22, 23, 24},
                    {0, 25, 26, 27, 28, 29, 30},
                    {0, 31, 32, 33, 34, 35, 36},
                    {0, 37, 38, 39, 40, 41, 42},
                    {1, 7, 13, 19, 25, 31, 37},
                    {1, 8, 14, 20, 26, 32, 38},
                    {1, 9, 15, 21, 27, 33, 39},
                    {1, 10, 16, 22, 28, 34, 40},
                    {1, 11, 17, 23, 29, 35, 41},
                    {1, 12, 18, 24, 30, 36, 42},
                    {2, 7, 14, 21, 28, 35, 42},
                    {2, 8, 13, 22, 29, 36, 39}},

            {{0, 1, 2, 3, 4, 5, 6},
                    {0, 7, 8, 9, 10, 11, 12},
                    {0, 13, 14, 15, 16, 17, 18},
                    {0, 19, 20, 21, 22, 23, 24},
                    {0, 25, 26, 27, 28, 29, 30},
                    {0, 31, 32, 33, 34, 35, 36},
                    {0, 37, 38, 39, 40, 41, 42},
                    {1, 7, 13, 19, 25, 31, 37},
                    {1, 8, 14, 20, 26, 32, 38},
                    {1, 9, 15, 21, 27, 33, 39},
                    {1, 10, 16, 22, 28, 34, 40},
                    {1, 11, 17, 23, 29, 35, 41},
                    {1, 12, 18, 24, 30, 36, 42},
                    {2, 7, 14, 21, 28, 35, 42},
                    {2, 8, 15, 19, 29, 36, 40}},

            {{0, 1, 2, 3, 4, 5, 6},
                    {0, 7, 8, 9, 10, 11, 12},
                    {0, 13, 14, 15, 16, 17, 18},
                    {0, 19, 20, 21, 22, 23, 24},
                    {0, 25, 26, 27, 28, 29, 30},
                    {0, 31, 32, 33, 34, 35, 36},
                    {0, 37, 38, 39, 40, 41, 42},
                    {1, 7, 13, 19, 25, 31, 37},
                    {1, 8, 14, 20, 26, 32, 38},
                    {1, 9, 15, 21, 27, 33, 39},
                    {1, 10, 16, 22, 28, 34, 40},
                    {1, 11, 17, 23, 29, 35, 41},
                    {1, 12, 18, 24, 30, 36, 42},
                    {2, 7, 14, 21, 28, 35, 42},
                    {2, 8, 15, 22, 29, 36, 37}}
    };
}
