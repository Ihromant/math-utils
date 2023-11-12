package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
        GroupProduct cp = new GroupProduct(7, 5, 5);
        assertArrayEquals(new int[]{0, 25, 50, 75, 100, 125, 150}, IntStream.range(0, cp.base().get(0).order()).map(i -> cp.fromArr(i, 0, 0)).toArray());
        GroupProduct cp1 = new GroupProduct(new GroupProduct(2, 2), new CyclicGroup(7));
        assertArrayEquals(new int[]{0, 7, 14, 21}, IntStream.range(0, cp1.base().get(0).order()).map(i -> cp1.fromArr(i, 0)).toArray());
    }

    @Test
    public void testEuler() {
        assertEquals(576, Group.euler(2520));
    }

    @Test
    public void testFactorize() {
        assertArrayEquals(new int[]{2, 2, 2, 3, 3, 5, 7}, Group.factorize(2520));
        assertArrayEquals(new int[]{17, 19}, Group.factorize(17 * 19));
        assertArrayEquals(new int[]{333667}, Group.factorize(333667));
        assertArrayEquals(new int[]{3, 7, 11, 13, 37}, Group.factorize(111111));
        assertArrayEquals(new int[]{23, 23}, Group.factorize(529));
        assertArrayEquals(IntStream.range(0, 25).map(i -> 2).toArray(), Group.factorize(1 << 25));
    }

    @Test
    public void testOrder() {
        CyclicGroup cg = new CyclicGroup(113);
        cg.elements().forEach(i -> assertEquals(i == 0 ? 1 : 113, cg.order(i)));
        GroupProduct gp = new GroupProduct(2, 2, 2, 2, 2, 2);
        gp.elements().forEach(i -> assertEquals(i == 0 ? 1 : 2, gp.order(i)));
    }

    @Test
    public void testPower() {
        CyclicGroup cg = new CyclicGroup(20);
        assertEquals(7, cg.power(3, 9));
        assertEquals(0, cg.power(3, 20));
    }

    private void testCorrectness(Group g, boolean commutative) {
        Group tg = g.asTable();
        int nonComm = g.elements().flatMap(i -> {
            assertEquals(i, g.op(i, 0));
            assertEquals(i, g.op(0, i));
            int inv = g.inv(i);
            assertEquals(0, g.op(inv, i));
            assertEquals(0, g.op(i, inv));
            return g.elements().map(j -> {
                assertEquals(g.op(i, j), tg.op(i, j));
                g.elements().forEach(k -> assertEquals(g.op(i, g.op(j, k)), g.op(g.op(i, j), k)));
                return g.op(i, j) != g.op(j, i) ? 1 : 0;
            });
        }).sum();
        assertEquals(commutative, nonComm == 0);
    }
}
