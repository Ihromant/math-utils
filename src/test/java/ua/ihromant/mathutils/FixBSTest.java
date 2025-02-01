package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class FixBSTest {
    private final FixBS eightBs;

    public FixBSTest() {
        eightBs = new FixBS(8);
        for (int i = 0; i < 8; i++) {
            eightBs.set(i);
        }
    }

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
        assertTrue(fst.compareTo(snd) < 0);
        snd.set(0);
        assertTrue(fst.compareTo(snd) > 0);
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

    @Test
    public void test_ConstructorI() {
        FixBS bs = new FixBS(128);
        // Default size for a FixBS should be 64 elements;
        assertEquals(128, bs.size());
        assertEquals("{}", bs.toString());
        // All FixBSs are created with elements of multiples of 64
        bs = new FixBS(89);
        assertEquals(128, bs.size());
    }

    @Test
    public void test_clone() {
        FixBS bs = eightBs.copy();
        assertEquals(eightBs, bs);
    }

    @Test
    public void test_clear() {
        eightBs.clear();
        for (int i = 0; i < 8; i++) {
            assertFalse(eightBs.get(i));
        }
        assertEquals(0, eightBs.length());
        FixBS bs = new FixBS(3400);
        bs.set(0, bs.size() - 1); // ensure all bits are 1's
        bs.set(bs.size() - 1);
        bs.clear();
        assertEquals(0, bs.length());
        assertTrue(bs.isEmpty());
        assertEquals(0, bs.cardinality());
    }

    @Test
    public void test_clearI() {
        eightBs.clear(7);
        assertFalse(eightBs.get(7));
        // Check to see all other bits are still set
        for (int i = 0; i < 7; i++) {
            assertTrue(eightBs.get(i));
        }
        // Try out of range
        try {
            eightBs.clear(-1);
            fail("Failed to throw expected out of bounds exception");
        } catch (IndexOutOfBoundsException expected) {
        }
        FixBS bs = new FixBS(1);
        assertEquals(0, bs.length());
        assertEquals(64, bs.size());
        bs.clear(0);
        assertEquals(0, bs.length());
        assertEquals(64, bs.size());
        bs.clear(60);
        assertEquals(0, bs.length());
        assertEquals(64, bs.size());
        bs.set(25);
        assertEquals(64, bs.size());
        assertEquals(26, bs.length());
        bs.clear(25);
        assertEquals(64, bs.size());
        assertEquals(0, bs.length());
        bs = new FixBS(1);
        try {
            bs.clear(-1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void test_clearII() throws IndexOutOfBoundsException {
        // Regression for HARMONY-98
        FixBS bitset = new FixBS(1);
        for (int i = 0; i < 20; i++) {
            bitset.set(i);
        }
        bitset.clear(10, 10);
        // pos1 and pos2 are in the same bitset element
        FixBS bs = new FixBS(16);
        int initialSize = bs.size();
        assertEquals(64, initialSize);
        bs.set(0, initialSize);
        bs.clear(5);
        bs.clear(15);
        bs.clear(7, 11);
        for (int i = 0; i < 7; i++) {
            if (i == 5) {
                assertFalse(bs.get(i));
            } else {
                assertTrue(bs.get(i));
            }
        }
        for (int i = 7; i < 11; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 11; i < initialSize; i++) {
            if (i == 15) {
                assertFalse(bs.get(i));
            } else {
                assertTrue(bs.get(i));
            }
        }
        for (int i = initialSize; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 is in the same bitset element, boundary testing
        bs = new FixBS(16);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(7, 64);
        assertEquals(64, bs.size());
        for (int i = 0; i < 7; i++) {
            assertTrue(bs.get(i));
        }
        for (int i = 7; i < 64; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 64; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // more boundary testing
        bs = new FixBS(32);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(0, 64);
        for (int i = 0; i < 64; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 64; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        bs = new FixBS(65);
        initialSize = 64;
        bs.set(0, initialSize);
        bs.clear(0, 65);
        for (int i = 0; i < 65; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 65; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 are in two sequential bitset elements
        bs = new FixBS(128);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(7);
        bs.clear(110);
        bs.clear(9, 74);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertFalse(bs.get(i));
            } else {
                assertTrue(bs.get(i));
            }
        }
        for (int i = 9; i < 74; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 74; i < initialSize; i++) {
            if (i == 110) {
                assertFalse(bs.get(i));
            } else {
                assertTrue(bs.get(i));
            }
        }
        for (int i = initialSize; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new FixBS(256);
        bs.set(0, 256);
        bs.clear(7);
        bs.clear(255);
        bs.clear(9, 219);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertFalse(bs.get(i));
            } else {
                assertTrue(bs.get(i));
            }
        }
        for (int i = 9; i < 219; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 219; i < 255; i++) {
            assertTrue(bs.get(i));
        }
        for (int i = 255; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // test illegal args
        bs = new FixBS(10);
        try {
            bs.clear(-1, 3);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            bs.clear(2, -1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        bs.set(2, 4);
        bs.clear(2, 2);
        assertTrue(bs.get(2));
        bs = new FixBS(105);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.clear(0, 2);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.clear(60, 64);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.clear(64, 120);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.set(25);
        assertEquals(26, bs.length());
        assertEquals(128, bs.size());
        bs.clear(60, 64);
        assertEquals(26, bs.length());
        assertEquals(128, bs.size());
        bs.clear(64, 120);
        assertEquals(128, bs.size());
        assertEquals(26, bs.length());
        bs.clear(80);
        assertEquals(128, bs.size());
        assertEquals(26, bs.length());
        bs.clear(25);
        assertEquals(128, bs.size());
        assertEquals(0, bs.length());
    }

    @Test
    public void test_getI() {
        FixBS bs = new FixBS(100);
        bs.set(8);
        assertFalse(eightBs.get(39));
        assertTrue(eightBs.get(3));
        assertFalse(bs.get(0));
        try {
            bs.get(-1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        bs = new FixBS(100);
        bs.set(63);
        assertTrue(bs.get(63));
        bs = new FixBS(65);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.get(2);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs.get(70);
        assertEquals(0, bs.length());
        assertEquals(128, bs.size());
        bs = new FixBS(3);
        try {
            bs.get(Integer.MIN_VALUE);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

//    public void test_getII() {
//        FixBS bitset = new FixBS(30);
//        bitset.get(3, 3);
//        FixBS bs, resultbs, correctbs;
//        bs = new FixBS(512);
//        bs.set(3, 9);
//        bs.set(10, 20);
//        bs.set(60, 75);
//        bs.set(121);
//        bs.set(130, 140);
//        // pos1 and pos2 are in the same bitset element, at index0
//        resultbs = bs.get(3, 6);
//        correctbs = new FixBS(3);
//        correctbs.set(0, 3);
//        assertEquals("Test1: Returned incorrect FixBS", correctbs, resultbs);
//        // pos1 and pos2 are in the same bitset element, at index 1
//        resultbs = bs.get(100, 125);
//        correctbs = new FixBS(25);
//        correctbs.set(21);
//        assertEquals("Test2: Returned incorrect FixBS", correctbs, resultbs);
//        // pos1 in bitset element at index 0, and pos2 in bitset element at
//        // index 1
//        resultbs = bs.get(15, 125);
//        correctbs = new FixBS(25);
//        correctbs.set(0, 5);
//        correctbs.set(45, 60);
//        correctbs.set(121 - 15);
//        assertEquals("Test3: Returned incorrect FixBS", correctbs, resultbs);
//        // pos1 in bitset element at index 1, and pos2 in bitset element at
//        // index 2
//        resultbs = bs.get(70, 145);
//        correctbs = new FixBS(75);
//        correctbs.set(0, 5);
//        correctbs.set(51);
//        correctbs.set(60, 70);
//        assertEquals("Test4: Returned incorrect FixBS", correctbs, resultbs);
//        // pos1 in bitset element at index 0, and pos2 in bitset element at
//        // index 2
//        resultbs = bs.get(5, 145);
//        correctbs = new FixBS(140);
//        correctbs.set(0, 4);
//        correctbs.set(5, 15);
//        correctbs.set(55, 70);
//        correctbs.set(116);
//        correctbs.set(125, 135);
//        assertEquals("Test5: Returned incorrect FixBS", correctbs, resultbs);
//        // pos1 in bitset element at index 0, and pos2 in bitset element at
//        // index 3
//        resultbs = bs.get(5, 250);
//        correctbs = new FixBS(200);
//        correctbs.set(0, 4);
//        correctbs.set(5, 15);
//        correctbs.set(55, 70);
//        correctbs.set(116);
//        correctbs.set(125, 135);
//        assertEquals("Test6: Returned incorrect FixBS", correctbs, resultbs);
//        assertEquals("equality principle 1 ", bs.get(0, bs.size()), bs);
//        // more tests
//        FixBS bs2 = new FixBS(129);
//        bs2.set(0, 20);
//        bs2.set(62, 65);
//        bs2.set(121, 123);
//        resultbs = bs2.get(1, 124);
//        correctbs = new FixBS(129);
//        correctbs.set(0, 19);
//        correctbs.set(61, 64);
//        correctbs.set(120, 122);
//        assertEquals("Test7: Returned incorrect FixBS", correctbs, resultbs);
//        // equality principle with some boundary conditions
//        bs2 = new FixBS(128);
//        bs2.set(2, 20);
//        bs2.set(62);
//        bs2.set(121, 123);
//        bs2.set(127);
//        resultbs = bs2.get(0, bs2.size());
//        assertEquals("equality principle 2 ", resultbs, bs2);
//        bs2 = new FixBS(128);
//        bs2.set(2, 20);
//        bs2.set(62);
//        bs2.set(121, 123);
//        bs2.set(127);
//        bs2.flip(0, 128);
//        resultbs = bs2.get(0, bs.size());
//        assertEquals("equality principle 3 ", resultbs, bs2);
//        bs = new FixBS(0);
//        assertEquals("Test1: Wrong length,", 0, bs.length());
//        assertEquals("Test1: Wrong size,", 0, bs.size());
//        bs.get(0, 2);
//        assertEquals("Test2: Wrong length,", 0, bs.length());
//        assertEquals("Test2: Wrong size,", 0, bs.size());
//        bs.get(60, 64);
//        assertEquals("Test3: Wrong length,", 0, bs.length());
//        assertEquals("Test3: Wrong size,", 0, bs.size());
//        bs.get(64, 120);
//        assertEquals("Test4: Wrong length,", 0, bs.length());
//        assertEquals("Test4: Wrong size,", 0, bs.size());
//        bs.set(25);
//        assertEquals("Test5: Wrong length,", 26, bs.length());
//        assertEquals("Test5: Wrong size,", 64, bs.size());
//        bs.get(60, 64);
//        assertEquals("Test6: Wrong length,", 26, bs.length());
//        assertEquals("Test6: Wrong size,", 64, bs.size());
//        bs.get(64, 120);
//        assertEquals("Test7: Wrong size,", 64, bs.size());
//        assertEquals("Test7: Wrong length,", 26, bs.length());
//        bs.get(80);
//        assertEquals("Test8: Wrong size,", 64, bs.size());
//        assertEquals("Test8: Wrong length,", 26, bs.length());
//        bs.get(25);
//        assertEquals("Test9: Wrong size,", 64, bs.size());
//        assertEquals("Test9: Wrong length,", 26, bs.length());
//        try {
//            bs2.get(-1, 0);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//        try {
//            bs2.get(bs2.size()/2, 0);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//        try {
//            bs2.get(bs2.size()/2, -1);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//    }

    @Test
    public void test_setI() {
        FixBS bs = new FixBS(130);
        bs.set(8);
        assertTrue(bs.get(8));
        try {
            bs.set(-1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        // Try setting a bit on a 64 boundary
        bs.set(128);
        assertEquals(192, bs.size());
        assertTrue(bs.get(128));
        bs = new FixBS(64);
        for (int i = bs.size(); --i >= 0;) {
            bs.set(i);
            assertTrue(bs.get(i));
            assertEquals(i + 1, bs.length());
            for (int j = bs.size(); --j > i; )
                assertFalse(bs.get(j));
            for (int j = i; --j >= 0; )
                assertFalse(bs.get(j));
            bs.clear(i);
        }
        bs = new FixBS(1);
        assertEquals(0, bs.length());
        bs.set(0);
        assertEquals(1, bs.length());
    }

    @Test
    public void test_setIZ() {
        // Test for method void java.util.FixBS.set(int, boolean)
        eightBs.set(5, false);
        assertFalse(eightBs.get(5));
        eightBs.set(5, true);
        assertTrue(eightBs.get(5));
        try {
            eightBs.set(-5, false);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
    }

    @Test
    public void test_setII() throws IndexOutOfBoundsException {
        FixBS bitset = new FixBS(30);
        bitset.set(29, 29);
        // Test for method void java.util.FixBS.set(int, int)
        // pos1 and pos2 are in the same bitset element
        FixBS bs = new FixBS(16);
        bs.set(5);
        bs.set(15);
        bs.set(7, 11);
        assertEquals("{5, 7, 8, 9, 10, 15}", bs.toString());
        for (int i = 16; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 is in the same bitset element, boundary testing
        bs = new FixBS(65);
        bs.set(7, 64);
        assertEquals(128, bs.size());
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i));
        }
        for (int i = 7; i < 64; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(64));
        // more boundary testing
        bs = new FixBS(65);
        bs.set(0, 64);
        for (int i = 0; i < 64; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(64));
        bs = new FixBS(65);
        bs.set(0, 65);
        for (int i = 0; i < 65; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(65));
        // pos1 and pos2 are in two sequential bitset elements
        bs = new FixBS(128);
        bs.set(7);
        bs.set(110);
        bs.set(9, 74);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertTrue(bs.get(i));
            } else {
                assertFalse(bs.get(i));
            }
        }
        for (int i = 9; i < 74; i++) {
            assertTrue(bs.get(i));
        }
        for (int i = 74; i < bs.size(); i++) {
            if (i == 110) {
                assertTrue(bs.get(i));
            } else {
                assertFalse(bs.get(i));
            }
        }
        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new FixBS(256);
        bs.set(7);
        bs.set(255);
        bs.set(9, 219);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertTrue(bs.get(i));
            } else {
                assertFalse(bs.get(i));
            }
        }
        for (int i = 9; i < 219; i++) {
            assertTrue(bs.get(i));
        }
        for (int i = 219; i < 255; i++) {
            assertFalse(bs.get(i));
        }
        assertTrue(bs.get(255));
        // test illegal args
        bs = new FixBS(10);
        bs.set(2, 2);
        assertFalse(bs.get(2));
        try {
            bs.set(-1, 3);
            fail("Test1: Attempt to flip with  negative index failed to generate exception");
        } catch (IndexOutOfBoundsException e) {
            // Correct behavior
        }
        try {
            bs.set(2, -1);
            fail("Test2: Attempt to flip with negative index failed to generate exception");
        } catch (IndexOutOfBoundsException e) {
            // Correct behavior
        }
    }

//    public void test_setIIZ() {
//        // Test for method void java.util.FixBS.set(int, int, boolean)
//        eightBs.set(3, 6, false);
//        assertTrue("Should have set bits 3, 4, and 5 to false", !eightBs.get(3)
//                && !eightBs.get(4) && !eightBs.get(5));
//        eightBs.set(3, 6, true);
//        assertTrue("Should have set bits 3, 4, and 5 to true", eightBs.get(3)
//                && eightBs.get(4) && eightBs.get(5));
//        try {
//            eightBs.set(-3, 6, false);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//        try {
//            eightBs.set(3, -6, false);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//        try {
//            eightBs.set(6, 3, false);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
//    }

    @Test
    public void test_flipI() {
        FixBS bs = new FixBS(130);
        bs.clear(8);
        bs.clear(9);
        bs.set(10);
        bs.flip(9);
        assertFalse(bs.get(8));
        assertTrue(bs.get(9));
        assertTrue(bs.get(10));
        bs.set(8);
        bs.set(9);
        bs.clear(10);
        bs.flip(9);
        assertTrue(bs.get(8));
        assertFalse(bs.get(9));
        assertFalse(bs.get(10));
        try {
            bs.flip(-1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        // Try setting a bit on a 64 boundary
        bs.flip(128);
        assertEquals(192, bs.size());
        assertTrue(bs.get(128));
        bs = new FixBS(64);
        for (int i = bs.size(); --i >= 0;) {
            bs.flip(i);
            assertTrue(bs.get(i));
            assertEquals(i + 1, bs.length());
            for (int j = bs.size(); --j > i; ) {
                assertFalse(bs.get(j));
            }
            for (int j = i; --j >= 0; ) {
                assertFalse(bs.get(j));
            }
            bs.flip(i);
        }
        FixBS bs0 = new FixBS(65);
        assertEquals(128, bs0.size());
        assertEquals(0, bs0.length());
        bs0.flip(0);
        assertEquals(128, bs0.size());
        assertEquals(1, bs0.length());
        bs0.flip(63);
        assertEquals(128, bs0.size());
        assertEquals(64, bs0.length());
        eightBs.flip(7);
        assertFalse(eightBs.get(7));
        // Check to see all other bits are still set
        for (int i = 0; i < 7; i++) {
            assertTrue(eightBs.get(i));
        }
        eightBs.flip(17);
        assertTrue(eightBs.get(17));
        eightBs.flip(17);
        assertFalse(eightBs.get(17));
    }

    @Test
    public void test_flipII() {
        FixBS bitset = new FixBS(1);
        for (int i = 0; i < 20; i++) {
            bitset.set(i);
        }
        bitset.flip(10, 10);
        // pos1 and pos2 are in the same bitset element
        FixBS bs = new FixBS(16);
        bs.set(7);
        bs.set(10);
        bs.flip(7, 11);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i));
        }
        assertFalse(bs.get(7));
        assertTrue(bs.get(8));
        assertTrue(bs.get(9));
        assertFalse(bs.get(10));
        for (int i = 11; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 is in the same bitset element, boundry testing
        bs = new FixBS(65);
        bs.set(7);
        bs.set(10);
        bs.flip(7, 64);
        assertEquals(128, bs.size());
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i));
        }
        assertFalse(bs.get(7));
        assertTrue(bs.get(8));
        assertTrue(bs.get(9));
        assertFalse(bs.get(10));
        for (int i = 11; i < 64; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(64));
        // more boundary testing
        bs = new FixBS(65);
        bs.flip(0, 64);
        for (int i = 0; i < 64; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(64));
        bs = new FixBS(65);
        bs.flip(0, 65);
        for (int i = 0; i < 65; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(65));
        // pos1 and pos2 are in two sequential bitset elements
        bs = new FixBS(128);
        bs.set(7);
        bs.set(10);
        bs.set(72);
        bs.set(110);
        bs.flip(9, 74);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i));
        }
        assertTrue(bs.get(7));
        assertFalse(bs.get(8));
        assertTrue(bs.get(9));
        assertFalse(bs.get(10));
        for (int i = 11; i < 72; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(72));
        assertTrue(bs.get(73));
        for (int i = 74; i < 110; i++) {
            assertFalse(bs.get(i));
        }
        assertTrue(bs.get(110));
        for (int i = 111; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new FixBS(256);
        bs.set(7);
        bs.set(10);
        bs.set(72);
        bs.set(110);
        bs.set(181);
        bs.set(220);
        bs.flip(9, 219);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i));
        }
        assertTrue(bs.get(7));
        assertFalse(bs.get(8));
        assertTrue(bs.get(9));
        assertFalse(bs.get(10));
        for (int i = 11; i < 72; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(72));
        for (int i = 73; i < 110; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(110));
        for (int i = 111; i < 181; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(181));
        for (int i = 182; i < 219; i++) {
            assertTrue(bs.get(i));
        }
        assertFalse(bs.get(219));
        assertTrue(bs.get(220));
        for (int i = 221; i < bs.size(); i++) {
            assertFalse(bs.get(i));
        }
        // test illegal args
        bs = new FixBS(10);
        try {
            bs.flip(-1, 3);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            bs.flip(2, -1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
    }

    @Test
    public void test_111478() {
        // FixBS shouldn't be modified by any of the operations below,
        // since the affected bits for these methods are defined as inclusive of
        // pos1, exclusive of pos2.
        eightBs.flip(0, 0);
        assertTrue(eightBs.get(0));
        eightBs.set(10, 10);
        assertFalse(eightBs.get(10));
        eightBs.clear(3, 3);
        assertTrue(eightBs.get(3));
    }

    @Test
    public void test_intersectsLjava_util_FixBS() {
        FixBS bs = new FixBS(500);
        bs.set(5);
        bs.set(63);
        bs.set(64);
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(192);
        bs.set(450);
        FixBS bs2 = new FixBS(500);
        assertFalse(bs.intersects(bs2));
        assertFalse(bs2.intersects(bs));
        bs2.set(4);
        assertFalse(bs.intersects(bs2));
        assertFalse(bs2.intersects(bs));
        bs2.clear();
        bs2.set(5);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(63);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(80);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(127);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(192);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(450);
        assertTrue(bs.intersects(bs2));
        assertTrue(bs2.intersects(bs));
        bs2.clear();
        bs2.set(500);
        assertFalse(bs.intersects(bs2));
        assertFalse(bs2.intersects(bs));
    }

    @Test
    public void test_andLjava_util_FixBS() {
        FixBS bs = new FixBS(128);
        // Initialize the bottom half of the FixBS
        for (int i = 64; i < 128; i++) {
            bs.set(i);
        }
        eightBs.and(bs);
        assertNotEquals(eightBs, bs);
        eightBs.set(3);
        bs.set(3);
        eightBs.and(bs);
        assertTrue(bs.get(3));
        bs = new FixBS(64);
        try {
            bs.and(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void test_andNotLjava_util_FixBS() {
        FixBS bs = eightBs.copy();
        bs.clear(5);
        FixBS bs2 = new FixBS(8);
        bs2.set(2);
        bs2.set(3);
        bs.andNot(bs2);
        assertEquals("{0, 1, 4, 6, 7}", bs.toString());
        bs = new FixBS(8);
        bs.andNot(bs2);
        assertEquals(64, bs.size());
        bs = new FixBS(64);
        try {
            bs.andNot(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
        // Regression test for HARMONY-4213
        bs = new FixBS(256);
        bs2 = new FixBS(256);
        bs.set(97);
        bs2.set(37);
        bs.andNot(bs2);
        assertTrue(bs.get(97));
    }

    @Test
    public void test_orLjava_util_FixBS() {
        FixBS bs = new FixBS(3);
        bs.or(eightBs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i));
        }
        bs = new FixBS(1);
        bs.or(eightBs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i));
        }
        eightBs.clear(5);
        bs = new FixBS(3);
        bs.or(eightBs);
        assertFalse(bs.get(5));
    }

    @Test
    public void test_xorLjava_util_FixBS() {
        FixBS bs = eightBs.copy();
        bs.xor(eightBs);
        for (int i = 0; i < 8; i++) {
            assertFalse(bs.get(i));
        }
        bs.xor(eightBs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i));
        }
        bs = new FixBS(1);
        bs.xor(eightBs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i));
        }
        bs = new FixBS(3);
        bs.set(63);
        assertEquals("{63}", bs.toString());
    }

    @Test
    public void test_size() {
        assertEquals(64, eightBs.size());
    }

    @Test
    public void test_toString() {
        assertEquals("{0, 1, 2, 3, 4, 5, 6, 7}", eightBs.toString());
        eightBs.clear(2);
        assertEquals("{0, 1, 3, 4, 5, 6, 7}", eightBs.toString());
    }

    @Test
    public void test_length() {
        FixBS bs = new FixBS(400);
        assertEquals(0, bs.length());
        bs.set(5);
        assertEquals(6, bs.length());
        bs.set(10);
        assertEquals(11, bs.length());
        bs.set(432);
        assertEquals(433, bs.length());
        bs.set(300);
        assertEquals(433, bs.length());
    }

    @Test
    public void test_nextSetBitI() {
        FixBS bs = new FixBS(500);
        bs.set(5);
        bs.set(32);
        bs.set(63);
        bs.set(64);
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(193);
        bs.set(450);
//        try {
//            bs.nextSetBit(-1);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
        assertEquals(5, bs.nextSetBit(0));
        assertEquals(5, bs.nextSetBit(5));
        assertEquals(32, bs.nextSetBit(6));
        assertEquals(32, bs.nextSetBit(32));
        assertEquals(63, bs.nextSetBit(33));
        // boundary tests
        assertEquals(63, bs.nextSetBit(63));
        assertEquals(64, bs.nextSetBit(64));
        // at bitset element 1
        assertEquals(71, bs.nextSetBit(65));
        assertEquals(71, bs.nextSetBit(71));
        assertEquals(72, bs.nextSetBit(72));
        assertEquals(127, bs.nextSetBit(110));
        // boundary tests
        assertEquals(127, bs.nextSetBit(127));
        assertEquals(128, bs.nextSetBit(128));
        // at bitset element 2
        assertEquals(193, bs.nextSetBit(130));
        assertEquals(193, bs.nextSetBit(191));
        assertEquals(193, bs.nextSetBit(192));
        assertEquals(193, bs.nextSetBit(193));
        assertEquals(450, bs.nextSetBit(194));
        assertEquals(450, bs.nextSetBit(255));
        assertEquals(450, bs.nextSetBit(256));
        assertEquals(450, bs.nextSetBit(450));
        assertEquals(-1, bs.nextSetBit(451));
        assertEquals(-1, bs.nextSetBit(511));
        assertEquals(-1, bs.nextSetBit(512));
        assertEquals(-1, bs.nextSetBit(800));
    }

    @Test
    public void test_nextClearBitI() {
        FixBS bs = new FixBS(500);
        // ensure all the bits from 0 to bs.size() - 1 are set to true
        bs.set(0, bs.size() - 1);
        bs.set(bs.size() - 1);
        bs.clear(5);
        bs.clear(32);
        bs.clear(63);
        bs.clear(64);
        bs.clear(71, 110);
        bs.clear(127, 130);
        bs.clear(193);
        bs.clear(450);
//        try {
//            bs.nextClearBit(-1);
//            fail();
//        } catch (IndexOutOfBoundsException expected) {
//        }
        assertEquals(5, bs.nextClearBit(0));
        assertEquals(5, bs.nextClearBit(5));
        assertEquals(32, bs.nextClearBit(6));
        assertEquals(32, bs.nextClearBit(32));
        assertEquals(63, bs.nextClearBit(33));
        // boundary tests
        assertEquals(63, bs.nextClearBit(63));
        assertEquals(64, bs.nextClearBit(64));
        // at bitset element 1
        assertEquals(71, bs.nextClearBit(65));
        assertEquals(71, bs.nextClearBit(71));
        assertEquals(72, bs.nextClearBit(72));
        assertEquals(127, bs.nextClearBit(110));
        // boundary tests
        assertEquals(127, bs.nextClearBit(127));
        assertEquals(128, bs.nextClearBit(128));
        // at bitset element 2
        assertEquals(193, bs.nextClearBit(130));
        assertEquals(193, bs.nextClearBit(191));
        assertEquals(193, bs.nextClearBit(192));
        assertEquals(193, bs.nextClearBit(193));
        assertEquals(450, bs.nextClearBit(194));
        assertEquals(450, bs.nextClearBit(255));
        assertEquals(450, bs.nextClearBit(256));
        assertEquals(450, bs.nextClearBit(450));
        // bitset has 1 still the end of bs.size() -1, but calling nextClearBit
        // with any index value after the last true bit should return bs.size()
        assertEquals(512, bs.nextClearBit(451));
        assertEquals(512, bs.nextClearBit(511));
        assertEquals(512, bs.nextClearBit(512));
        // if the index is larger than bs.size(), nextClearBit should return index
        assertEquals(513, bs.nextClearBit(513));
        assertEquals(800, bs.nextClearBit(800));
        bs.clear();
        assertEquals(0, bs.nextClearBit(0));
        assertEquals(3, bs.nextClearBit(3));
        assertEquals(64, bs.nextClearBit(64));
        assertEquals(128, bs.nextClearBit(128));
    }

    @Test
    public void test_31036_clear() {
        FixBS bs = new FixBS(500);
        for (int i = 0; i < 500; ++i) {
            int nextClear = bs.nextClearBit(0);
            assertEquals(i, nextClear);
            bs.set(i);
        }
    }

    @Test
    public void test_31036_set() {
        FixBS bs = new FixBS(500);
        bs.set(0, 511);
        for (int i = 0; i < 500; ++i) {
            int nextSet = bs.nextSetBit(0);
            assertEquals(i, nextSet);
            bs.clear(i);
        }
    }

    @Test
    public void test_isEmpty() {
        FixBS bs = new FixBS(500);
        assertTrue(bs.isEmpty());
        // at bitset element 0
        bs.set(3);
        assertFalse(bs.isEmpty());
        // at bitset element 1
        bs.clear();
        bs.set(12);
        assertFalse(bs.isEmpty());
        // at bitset element 2
        bs.clear();
        bs.set(128);
        assertFalse(bs.isEmpty());
        // boundary testing
        bs.clear();
        bs.set(459);
        assertFalse(bs.isEmpty());
        bs.clear();
        bs.set(511);
        assertFalse(bs.isEmpty());
    }

    @Test
    public void test_cardinality() {
        FixBS bs = new FixBS(500);
        bs.set(5);
        bs.set(32);
        bs.set(63);
        bs.set(64);
        assertEquals(4, bs.cardinality());
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(193);
        bs.set(450);
        assertEquals(48, bs.cardinality());
        bs.flip(0, 500);
        assertEquals(452, bs.cardinality());
        bs.clear();
        assertEquals(0, bs.cardinality());
        bs.set(0, 500);
        assertEquals(500, bs.cardinality());
        bs.clear();
        bs.set(0, 64);
        assertEquals(64, bs.cardinality());
    }

    @Test
    public void testModularDiff() {
        int v = 275;
        int cut = 125;
        FixBS a = new FixBS(v);
        a.set(0, v);
        FixBS b = FixBS.of(v, 1, 10, 17, 63, 65, 124, 127, 150, 200, 212, 257);
        int[] expected = b.stream().toArray();
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (expected[i] + cut) % v;
        }
        Arrays.sort(expected);
        a.diffModuleShifted(b, v, v - cut);
        a.flip(0, v);
        assertArrayEquals(expected, a.stream().toArray());
    }

    @Test
    public void testModularDiff1() {
        int v = 19;
        int cut = 9;
        FixBS a = new FixBS(v);
        a.set(0, v);
        FixBS b = FixBS.of(v, 1, 3, 15, 18);
        int[] expected = b.stream().toArray();
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (expected[i] + cut) % v;
        }
        Arrays.sort(expected);
        a.diffModuleShifted(b, v, v - cut);
        a.flip(0, v);
        assertArrayEquals(expected, a.stream().toArray());
    }

    @Test
    public void randomizeModularDiff() {
        for (int j = 0; j < 1000000; j++) {
            int v = ThreadLocalRandom.current().nextInt(10, 300);
            int cut = ThreadLocalRandom.current().nextInt(v);
            FixBS a = new FixBS(v);
            a.set(0, v);
            FixBS b = FixBS.of(v);
            for (int i = 0; i < 5; i++) {
                b.set(ThreadLocalRandom.current().nextInt(v));
            }
            int[] expected = b.stream().toArray();
            for (int i = 0; i < expected.length; i++) {
                expected[i] = (expected[i] + cut) % v;
            }
            Arrays.sort(expected);
            a.diffModuleShifted(b, v, v - cut);
            a.flip(0, v);
            assertArrayEquals(expected, a.stream().toArray());
        }
    }
}