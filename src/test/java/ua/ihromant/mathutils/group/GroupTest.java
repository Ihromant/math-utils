package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.List;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class GroupTest {
    @Test
    public void testGroups() {
        testCorrectness(new CyclicGroup(15), true);
        testCorrectness(new CyclicProduct(3, 3, 5), true);
        testCorrectness(new QuaternionGroup(), false);
        testCorrectness(new DihedralGroup(7), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(2)), false);
        SemiDirectProduct prod = new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3));
        testCorrectness(prod, false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(4)), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(8), 2), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(8), 4), false);
        testCorrectness(new SemiDirectProduct(new CyclicProduct(2, 2, 3), new CyclicGroup(2)), false);
        testCorrectness(new BurnsideGroup(), false);
        testCorrectness(new Liner(new GaloisField(2).generatePlane()).automorphisms(), false);
        SubGroup left = new SubGroup(prod, FixBS.of(21, 0, 3, 6, 9, 12, 15, 18));
        testCorrectness(left, true);
        assertTrue(left.isNormal());
        SubGroup right = new SubGroup(prod, FixBS.of(3, 0, 1, 2));
        testCorrectness(right, true);
        assertFalse(right.isNormal());
    }

    @Test
    public void testSubGroups() {
        Group gr = new CyclicGroup(6);
        List<SubGroup> subGroups = gr.subGroups();
        assertEquals(4, subGroups.size());
        gr = new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(2));
        subGroups = gr.subGroups();
        assertEquals(6, subGroups.size());
        gr = new QuaternionGroup();
        subGroups = gr.subGroups();
        assertEquals(6, subGroups.size());
        gr = new PermutationGroup(5, true);
        subGroups = gr.subGroups();
        assertEquals(59, subGroups.size());
        assertTrue(subGroups.subList(1, subGroups.size() - 1).stream().noneMatch(SubGroup::isNormal));
        gr = new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3));
        subGroups = gr.subGroups();
        assertEquals(10, subGroups.size());
    }

    @Test
    public void testEquivalent() {
        CyclicProduct cp = new CyclicProduct(3, 3, 5);
        cp.elements().forEach(i -> assertEquals(i, cp.fromArr(cp.toArr(i))));
    }

    @Test
    public void testSpecific() {
        CyclicProduct cp = new CyclicProduct(7, 5, 5);
        assertArrayEquals(new int[]{0, 25, 50, 75, 100, 125, 150}, IntStream.range(0, cp.base()[0]).map(i -> cp.fromArr(i, 0, 0)).toArray());
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
        CyclicProduct gp = new CyclicProduct(2, 2, 2, 2, 2, 2);
        gp.elements().forEach(i -> assertEquals(i == 0 ? 1 : 2, gp.order(i)));
    }

    @Test
    public void testMul() {
        CyclicGroup cg = new CyclicGroup(20);
        assertEquals(7, cg.mul(3, 9));
        assertEquals(0, cg.mul(3, 20));
    }

    @Test
    public void testExp() {
        CyclicGroup cg = new CyclicGroup(125);
        assertEquals(3, cg.exponent(2, 7));
        CyclicGroup cg1 = new CyclicGroup(7);
        assertEquals(3, cg1.expOrder(2));
        assertEquals(2, cg1.expOrder(6));
        assertEquals(6, cg1.expOrder(3));
        CyclicGroup cg2 = new CyclicGroup(8);
        IntStream.range(2, cg2.order()).forEach(i -> assertEquals(i % 2 == 0 ? -1 : 2, cg2.expOrder(i)));
    }

    @Test
    public void testAuth() {
        assertArrayEquals(new int[][]{{0, 1, 2}, {0, 2, 1}}, new CyclicGroup(3).auth());
        assertArrayEquals(new int[][]{{0, 1, 2, 3}, {0, 3, 2, 1}}, new CyclicGroup(4).auth());
        Group product = new CyclicGroup(7);
        int[][] auths = product.auth();
        assertEquals(6, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(2, 2, 3, 3);
        auths = product.auth();
        assertEquals(288, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(4, 4);
        auths = product.auth();
        assertEquals(96, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(2, 4);
        auths = product.auth();
        assertEquals(8, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(3, 9);
        auths = product.auth();
        assertEquals(108, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(2, 4, 3, 9);
        auths = product.auth();
        assertEquals(864, auths.length);
        checkAuth(auths, product);
        product = new CyclicProduct(3, 3, 9);
        auths = product.auth();
        assertEquals(23328, auths.length);
        checkAuth(auths, product);
    }

    @Test
    public void testAuthSemidirect() {
        Group product = new SemiDirectProduct(new CyclicProduct(3), new CyclicGroup(2));
        int[][] auths = product.auth();
        assertEquals(6, auths.length);
        checkAuth(auths, product);
        product = new SemiDirectProduct(new CyclicProduct(7), new CyclicGroup(3));
        auths = product.auth();
        assertEquals(42, auths.length);
        checkAuth(auths, product);
        product = new SemiDirectProduct(new CyclicProduct(2, 2), new CyclicGroup(3));
        auths = product.auth();
        assertEquals(24, auths.length);
        checkAuth(auths, product);
        product = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        auths = product.auth();
        assertEquals(108, auths.length);
        checkAuth(auths, product);
        product = new PermutationGroup(4, false);
        auths = product.auth();
        assertEquals(24, auths.length);
        checkAuth(auths, product);
        product = new PermutationGroup(4, true);
        auths = product.auth();
        assertEquals(12, auths.length);
        checkAuth(auths, product);
    }

    private static void checkAuth(int[][] auths, Group simple) {
        for (int[] aut : auths) {
            for (int a = 0; a < simple.order(); a++) {
                for (int b = 0; b < simple.order(); b++) {
                    assertEquals(simple.op(aut[a], aut[b]), aut[simple.op(a, b)]);
                }
            }
        }
    }

    @Test
    public void testBurnsideAssociativity() {
        BurnsideGroup bg = new BurnsideGroup();
        IntBinaryOperator op = (x, y) -> bg.op(bg.op(bg.inv(x), y), bg.inv(x));
        bg.elements().forEach(i -> {
            assertEquals(i, op.applyAsInt(i, 0));
            assertEquals(i, op.applyAsInt(0, i));
            bg.elements().forEach(j -> {
                assertEquals(op.applyAsInt(i, j), op.applyAsInt(i, j));
                bg.elements().forEach(k -> assertEquals(op.applyAsInt(i, op.applyAsInt(j, k)), op.applyAsInt(op.applyAsInt(i, j), k)));
            });
        });
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
