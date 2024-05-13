package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertArrayEquals(byArr1.flags(), byLine1.flags());
        assertArrayEquals(byArr2.flags(), byLine2.flags());

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
}
