package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

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
    }

    @Test
    public void testIsomorphic() {
        PartialLiner first7 = new PartialLiner(Liner.byStrings(new String[]{
                "0001123",
                "1242534",
                "3654656"
        }).lines());
        PartialLiner second7 = new PartialLiner(new Liner(new GaloisField(2).generatePlane()).lines());
        assertTrue(first7.isomorphic(second7));
        assertTrue(second7.isomorphic(first7));
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
        assertTrue(first13.isomorphic(second13));
        assertTrue(second13.isomorphic(first13));
        assertFalse(alt13.isomorphic(first13));
        PartialLiner first9 = new PartialLiner(Liner.byStrings(new String[]{
                "000011122236",
                "134534534547",
                "268787676858"
        }).lines());
        PartialLiner second9 = new PartialLiner(new AffinePlane(new Liner(new GaloisField(3).generatePlane()), 0).toLiner().lines());
        assertTrue(first9.isomorphic(second9));
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
        assertTrue(firstFlat15.isomorphic(secondFlat15));
        assertTrue(firstSpace15.isomorphic(secondSpace15));
        assertTrue(firstSpace15.isomorphic(thirdSpace15));
        assertTrue(firstSpace15.isomorphic(thirdSpace15));
        assertFalse(firstFlat15.isomorphic(firstSpace15));
        PartialLiner firstPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}});
        PartialLiner secondPartial = new PartialLiner(9, new int[][]{{6, 7, 8}, {4, 5, 8}});
        PartialLiner thirdPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {3, 4, 5}});
        PartialLiner fourthPartial = new PartialLiner(9, new int[][]{{0, 3, 6}, {1, 4, 7}});
        PartialLiner fifthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}});
        PartialLiner sixthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}});
        assertTrue(firstPartial.isomorphic(secondPartial));
        assertFalse(firstPartial.isomorphic(thirdPartial));
        assertTrue(thirdPartial.isomorphic(fourthPartial));
        assertTrue(fifthPartial.isomorphic(sixthPartial));
    }
}