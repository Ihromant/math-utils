package ua.ihromant.mathutils.loop;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.TableGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoopTest {
    @Test
    public void testCorrectness() {
        testCorrectness(new CheinExtension(new CyclicProduct(2, 2)), true, true);
        testCorrectness(new CheinExtension(new PermutationGroup(3, false)), false, true);
        testCorrectness(new SpecialLinearLoop(new GaloisField(2)), false, true);
        testCorrectness(new CheinExtension(new CheinExtension(new CyclicGroup(7))), false, true);
    }

    private static void testCorrectness(Loop l, boolean associative, boolean checkAuth) {
        int nonAssoc = l.elements().parallel().map(i -> {
            assertEquals(i, l.op(i, 0));
            assertEquals(i, l.op(0, i));
            int inv = l.inv(i);
            assertEquals(0, l.op(inv, i));
            assertEquals(0, l.op(i, inv));
            return l.elements().map(j -> {
                return l.elements().map(k -> {
                    assertEquals(l.op(l.op(i, j), l.op(k, i)), l.op(l.op(i, l.op(j, k)), i));
                    return l.op(l.op(i, j), k) != l.op(i, l.op(j, k)) ? 1 : 0;
                }).sum();
            }).sum();
        }).sum();
        assertEquals(associative, nonAssoc == 0);
        if (checkAuth) {
            int[][] auths = l.auth();
            for (int[] auth : auths) {
                for (int i = 0; i < l.order(); i++) {
                    assertEquals(auth[l.inv(i)], l.inv(auth[i]));
                    for (int j = 0; j < l.order(); j++) {
                        assertEquals(l.op(auth[i], auth[j]), auth[l.op(i, j)]);
                    }
                }
            }
        }
    }

    @Test
    public void testPaige() {
        SpecialLinearLoop sll = new SpecialLinearLoop(new GaloisField(2));
        System.out.println(sll.order());
        List<FixBS> sl = sll.subLoops();
        System.out.println(sl.size());
        for (int i = 0; i < sl.size(); i++) {
            FixBS a = sl.get(i);
            int aCard = a.cardinality();
            for (int j = i + 1; j < sl.size(); j++) {
                FixBS b = sl.get(j);
                int bCard = b.cardinality();
                if (aCard * bCard <= sll.order() || a.intersection(b).cardinality() > 1) {
                    continue;
                }
                FixBS prod = new FixBS(sll.order());
                for (int aEl = a.nextSetBit(0); aEl >= 0; aEl = a.nextSetBit(aEl + 1)) {
                    for (int bEl = b.nextSetBit(0); bEl >= 0; bEl = b.nextSetBit(bEl + 1)) {
                        prod.set(sll.op(aEl, bEl));
                    }
                }
                if (prod.cardinality() == sll.order()) {
                    System.out.println(a + " " + b);
                }
            }
        }
    }

    @Test
    public void testChein() throws IOException {
        for (int gs = 1; gs < 31; gs++) {
            int gc = GroupIndex.groupCount(gs);
            for (int k = 1; k <= gc; k++) {
                Group g = GroupIndex.group(gs, k);
                CheinExtension ce = new CheinExtension(g);
                List<FixBS> sl = ce.subLoops();
                System.out.println(sl.size());
                for (int i = 0; i < sl.size(); i++) {
                    FixBS a = sl.get(i);
                    int aCard = a.cardinality();
                    for (int j = i + 1; j < sl.size(); j++) {
                        FixBS b = sl.get(j);
                        int bCard = b.cardinality();
                        if (aCard * bCard <= ce.order() || a.intersection(b).cardinality() > 1) {
                            continue;
                        }
                        FixBS prod = new FixBS(ce.order());
                        for (int aEl = a.nextSetBit(0); aEl >= 0; aEl = a.nextSetBit(aEl + 1)) {
                            for (int bEl = b.nextSetBit(0); bEl >= 0; bEl = b.nextSetBit(bEl + 1)) {
                                prod.set(ce.op(aEl, bEl));
                            }
                        }
                        if (prod.cardinality() == ce.order()) {
                            System.out.println(a + " " + b);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCheinAlt() {
        Loop l = new CheinExtension(new CheinExtension(new CyclicGroup(7)));
        testCorrectness(l, false, true);
        // "(C7 x C7) : (C6 x S3)"
        int[][] auth = l.auth();
        PermutationGroup perm = new PermutationGroup(auth);
        CyclicGroup cg = new CyclicGroup(3);
        int[] els = perm.conjugationClasses().stream().mapToInt(cl -> cl.nextSetBit(0)).filter(el -> perm.order(el) == cg.order()).toArray();
        for (int el : els) {
            int[] psi = new int[cg.order()];
            for (int i = 1; i < cg.order(); i++) {
                psi[i] = perm.mul(el, i);
            }
            int cnt = l.order() * cg.order();
            int[][] table = new int[cnt][cnt];
            for (int a = 0; a < table.length; a++) {
                for (int b = 0; b < table.length; b++) {
                    int h1 = a / cg.order();
                    int k1 = a % cg.order();
                    int h2 = b / cg.order();
                    int k2 = b % cg.order();
                    table[a][b] = l.op(h1, perm.permutation(psi[k1])[h2]) * cg.order() + cg.op(k1, k2);
                }
            }
            TableGroup tg = new TableGroup(table);
            try {
                testCorrectness(tg, false, true);
                System.out.println("Moufang " + el);
            } catch (AssertionFailedError e) {
                System.out.println("Not Moufang " + el);
            }
        }
    }

    @Test
    public void testRajah() {
        int p = 3;
        int q = 7;
        int mu = 2;
        int phi = 0;
        RajahLoop lp = new RajahLoop(p, q, mu, phi);
        testCorrectness(lp, false, false);
    }

    private static int order(RajahLoop lp, int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = lp.opByDef(a, pow);
        } while (pow != 0);
        return counter;
    }

    @Test
    public void testElementaryRajah() {
        for (int q = 7; q < 100; q++) {
            if (!Combinatorics.isPrime(q)) {
                continue;
            }
            int qq = q;
            GaloisField gf = new GaloisField(q);
            int[] factors = Combinatorics.factorize(q - 1);
            for (int p : factors) {
                if (p % 2 == 0) {
                    continue;
                }
                for (int mu = 2; mu < q; mu++) {
                    if (gf.exponent(mu, p) != 1) {
                        continue;
                    }
                    for (int phi = 0; phi < q; phi++) {
                        if (p != 3 && gf.mul(phi, mu - 1) != q - 2) {
                            continue;
                        }
                        System.out.println(p + " " + q + " " + mu + " " + phi);
                        RajahLoop lp = new RajahLoop(p, q, mu, phi);
                        boolean prime = IntStream.range(1, 2 * q * q * q).parallel().allMatch(el -> {
                            int ord = order(lp, el);
                            return ord == p || ord == qq;
                        });
                        System.out.println(prime ? "Prime" : "Not prime");
                    }
                }
            }
        }
    }
}
