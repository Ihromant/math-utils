package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupTest {
    @Test
    public void testGroups() {
        testCorrectness(new CyclicGroup(15), true);
        testCorrectness(new GroupProduct(3, 3, 5), true);
        testCorrectness(new DihedralGroup(7), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(2)), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3)), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(11), new CyclicGroup(5)), false);
        testCorrectness(new SemiDirectProduct(new CyclicGroup(12), new CyclicGroup(2)), false);
        testCorrectness(new BurnsideGroup(), false);
        testCorrectness(new Liner(new GaloisField(2).generatePlane()).automorphisms(), false);
    }

    @Test
    public void testEquivalent() {
        GroupProduct cp = new GroupProduct(3, 3, 5);
        cp.elements().forEach(i -> assertEquals(i, cp.fromArr(cp.toArr(i))));
    }

    @Test
    public void testSpecific() {
        GroupProduct cp = new GroupProduct(7, 5, 5);
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
        GroupProduct gp = new GroupProduct(2, 2, 2, 2, 2, 2);
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
        Group simple = new GroupProduct(7);
        int[][] auths = simple.auth();
        assertEquals(6, auths.length);
        checkAuth(auths, simple);
        Group product = new GroupProduct(2, 2, 3, 3);
        auths = product.auth();
        assertEquals(288, auths.length);
        checkAuth(auths, product);
        product = new GroupProduct(4, 4);
        auths = product.auth();
        assertEquals(96, auths.length);
        checkAuth(auths, product);
        product = new GroupProduct(2, 4);
        auths = product.auth();
        assertEquals(24, auths.length);
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
    public void findHints() {
        IntStream.range(6, 11).forEach(k -> {
            int mul = k * (k + 1);
            for (int t : (Iterable<Integer>) () -> IntStream.range(1, 4).iterator()) {
                for (int dff : List.of(1, k + 1)) {
                    int v = t * mul + dff;
                    CyclicGroup cg = new CyclicGroup(v);
                    Set<BitSet> dedup = new HashSet<>();
                    BitSet filter = dff == 1 ? new BitSet() : IntStream.range(0, k + 1).map(i -> i * v / (k + 1)).collect(BitSet::new, BitSet::set, BitSet::or);
                    BitSet[] sets = cg.elements().filter(el -> Group.gcd(el, cg.order()) == 1)
                            .boxed().flatMap(el -> Stream.concat(IntStream.range(0, cg.expOrder(el))
                            .mapToObj(st -> {
                                BitSet result = new BitSet();
                                result.set(0);
                                IntStream.range(st, st + k).map(pow -> cg.exponent(el, pow)).forEach(result::set);
                                return result;
                            }), IntStream.range(0, cg.expOrder(el))
                                    .mapToObj(st -> {
                                        BitSet result = new BitSet();
                                        IntStream.range(st, st + k + 1).map(pow -> cg.exponent(el, pow)).forEach(result::set);
                                        return result;
                                    })).filter(bs -> {
                                BitSet res = new BitSet();
                                bs.stream().forEach(a -> bs.stream().filter(b -> a < b)
                                        .map(b -> diff(a, b, cg.order())).filter(d -> !filter.get(d)).forEach(res::set));
                                return res.cardinality() == mul / 2 && dedup.add(res);
                            })
                            )
                            .toArray(BitSet[]::new);
                    if (sets.length > 0) {
                        System.out.println((k + 1) + " " + t + " " + dff + " " + cg.order());
                        Arrays.stream(sets).forEach(System.out::println);
                        System.out.println();
                    }
                }
            }
        });
    }

    private static int diff(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
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
