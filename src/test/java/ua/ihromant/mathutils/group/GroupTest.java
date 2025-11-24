package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.ApplicatorTest;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class GroupTest {
    @Test
    public void testGroups() {
        testCorrectness(new CyclicGroup(15), true);
        testCorrectness(new CyclicProduct(3, 3, 5), true);
        testCorrectness(new QuaternionGroup(), false);
        testCorrectness(new DihedralGroup(7), false);
        testCorrectness(new SimpleLinearCyclic(2, 4), false);
        GeneralLinear gl = new GeneralLinear(2, new GaloisField(4));
        assertEquals(180, gl.order());
        testCorrectness(gl, false);
        SpecialLinear sl = new SpecialLinear(2, new GaloisField(4));
        assertEquals(60, sl.order());
        assertTrue(sl.isSimple());
        testCorrectness(sl, false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(2)), false);
        SemiDirectProduct prod = new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3));
        testCorrectness(prod, false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(4)), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(8), 2), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(8), 4), false);
        testCorrectness(new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(2), 1, true), false);
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
        subGroups.sort(Comparator.comparingInt(sg -> sg.arr().length));
        assertTrue(subGroups.subList(1, subGroups.size() - 1).stream().noneMatch(SubGroup::isNormal));
        gr = new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3));
        subGroups = gr.subGroups();
        assertEquals(10, subGroups.size());
    }

    @Test
    public void testGroupedSubgroups() {
        Group gr = new CyclicProduct(17, 17).asTable();
        System.out.println(gr.groupedSubGroups().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
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
        assertEquals(576, Combinatorics.euler(2520));
    }

    @Test
    public void testFactorize() {
        assertArrayEquals(new int[]{2, 2, 2, 3, 3, 5, 7}, Combinatorics.factorize(2520));
        assertArrayEquals(new int[]{17, 19}, Combinatorics.factorize(17 * 19));
        assertArrayEquals(new int[]{333667}, Combinatorics.factorize(333667));
        assertArrayEquals(new int[]{3, 7, 11, 13, 37}, Combinatorics.factorize(111111));
        assertArrayEquals(new int[]{23, 23}, Combinatorics.factorize(529));
        assertArrayEquals(IntStream.range(0, 25).map(_ -> 2).toArray(), Combinatorics.factorize(1 << 25));
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
    public void testCyclicAuth() {
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
    public void testDefaultAuth() {
        Group group = new QuaternionGroup();
        int[][] auths = group.auth();
        assertEquals(24, auths.length);
        checkAuth(auths, group);
        group = new SemiDirectProduct(new CyclicProduct(7), new CyclicGroup(3)).asTable();
        auths = group.auth();
        assertEquals(42, auths.length);
        checkAuth(auths, group);
        assertArrayEquals(new SemiDirectProduct(new CyclicProduct(7), new CyclicGroup(3)).auth(), auths);
        group = new SemiDirectProduct(new CyclicProduct(2, 2), new CyclicGroup(3));
        auths = group.auth();
        assertEquals(24, auths.length);
        checkAuth(auths, group);
        group = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        auths = group.auth();
        assertEquals(432, auths.length);
        checkAuth(auths, group);
        group = new PermutationGroup(4, true);
        auths = group.auth();
        assertEquals(24, auths.length);
        checkAuth(auths, group);
        group = new PermutationGroup(6, false).asTable();
        auths = group.auth();
        assertEquals(1440, auths.length);
        checkAuth(auths, group);
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

    public static void testCorrectness(Group g, boolean commutative) {
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

    @Test
    public void test56() {
        Group g = new SemiDirectProduct(new CyclicProduct(2, 2, 2), new CyclicGroup(7));
        System.out.println(Arrays.toString(IntStream.range(0, g.order()).map(g::order).toArray()));
        List<SubGroup> sgs = g.subGroups();
        for (SubGroup sg : sgs) {
            System.out.println(sg.order() + " " + sg.isCommutative() + " " + sg.isNormal());
        }
        Group simple = new PermutationGroup(new CyclicProduct(2, 2, 2).auth()).asTable();
        System.out.println(Arrays.toString(IntStream.range(0, simple.order()).map(simple::order).toArray()));
        sgs = simple.subGroups();
        for (SubGroup sg : sgs) {
            System.out.println(sg.order() + " " + sg.isCommutative() + " " + sg.isNormal());
        }
    }

    @Test
    public void testGap() throws IOException {
        Group g = new GapInteractor().smallGroup(40, 7);
        testCorrectness(g, false);
    }

    private static Group readGroup(String name) throws IOException {
        try (InputStream is = ApplicatorTest.class.getResourceAsStream("/group/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return new TableGroup(new ObjectMapper().readValue(br.readLine(), int[][].class));
        }
    }

    @Test
    public void identify() throws IOException {
        GapInteractor inter = new GapInteractor();
        System.out.println(inter.identifyGroup(readGroup("hall9")));
        inter = new GapInteractor();
        System.out.println(inter.groupCount(64));
    }

    @Test
    public void testFactor() {
        SpecialLinear sl = new SpecialLinear(2, new GaloisField(5));
        FactorGroup psl = sl.psl();
        testCorrectness(psl, false);
        assertEquals(60, psl.order());
        assertTrue(psl.isSimple());
    }

    @Test
    public void testMathieu() {
        Group mathieu = PermutationGroup.mathieu11().asTable();
        assertEquals(7920, mathieu.order());
        assertEquals(7920, mathieu.innerAuth().length);
        assertEquals(7920, mathieu.auth().length);
    }
}
