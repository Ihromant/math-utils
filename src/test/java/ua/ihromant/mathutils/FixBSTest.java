package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixBSTest {
    @Test
    public void testChoices() {
        assertEquals(35, FixBS.choices(7, 4).count());
        assertEquals(4, FixBS.fixedFirst(7, 4, 2).count());
    }

    @Test
    public void testCompare() {
        FixBS fst = new FixBS(5);
        FixBS snd = new FixBS(5);
        fst.set(0);
        fst.set(2);
        snd.set(1);
        assertTrue(fst.compareTo(snd) > 0);
        snd.set(0);
        assertTrue(fst.compareTo(snd) < 0);
        fst.clear(2);
        snd.clear(1);
        assertEquals(0, fst.compareTo(snd));
    }

    @Test
    public void testLength() {
        FixBS fbs = new FixBS(193);
        assertEquals(0, fbs.length());
        fbs.set(192);
        assertEquals(193, fbs.length());
        fbs.clear(192);
        fbs.set(65);
        assertEquals(66, fbs.length());
    }
}
