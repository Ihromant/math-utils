package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupTest {
    @Test
    public void testGroups() {
        testCorrectness(new CyclicGroup(15), true);
        testCorrectness(new CyclicProduct(3, 3, 5), true);
        testCorrectness(new DihedralGroup(7), false);
    }

    @Test
    public void testEquivalent() {
        CyclicProduct cp = new CyclicProduct(3, 3, 5);
        cp.elements().forEach(i -> assertEquals(i, cp.fromArr(cp.toArr(i))));
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
