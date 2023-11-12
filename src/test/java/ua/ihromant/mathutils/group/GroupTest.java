package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupTest {
    @Test
    public void testGroups() {
        testCorrectness(new CyclicGroup(15), true);
        testCorrectness(new GroupProduct(3, 3, 5), true);
        testCorrectness(new DihedralGroup(7), false);
    }

    @Test
    public void testEquivalent() {
        GroupProduct cp = new GroupProduct(3, 3, 5);
        cp.elements().forEach(i -> assertEquals(i, cp.fromArr(cp.toArr(i))));
    }

    @Test
    public void testSpecific() {
        int specific = 7;
        GroupProduct cp = new GroupProduct(specific, 5, 5);
        assertArrayEquals(new int[]{0, 25, 50, 75, 100, 125, 150}, IntStream.range(0, specific).map(i -> cp.fromArr(i, 0, 0)).toArray());
        GroupProduct gp2 = new GroupProduct(2, 2);
        GroupProduct cp1 = new GroupProduct(gp2, new CyclicGroup(7));
        assertArrayEquals(new int[]{0, 7, 14, 21}, IntStream.range(0, gp2.order()).map(i -> cp1.fromArr(i, 0)).toArray());
    }

    private void testCorrectness(Group g, boolean commutative) {
        Group tg = g.asTable();
        g.elements().forEach(i -> {
            assertEquals(i, g.op(i, 0));
            assertEquals(i, g.op(0, i));
            int inv = g.inv(i);
            assertEquals(0, g.op(inv, i));
            assertEquals(0, g.op(i, inv));
            g.elements().forEach(j -> {
                if (commutative) {
                    assertEquals(g.op(i, j), g.op(j, i));
                }
                assertEquals(g.op(i, j), tg.op(i, j));
                g.elements().forEach(k -> assertEquals(g.op(i, g.op(j, k)), g.op(g.op(i, j), k)));
            });
        });
    }
}
