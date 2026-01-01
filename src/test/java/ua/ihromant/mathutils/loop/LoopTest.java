package ua.ihromant.mathutils.loop;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoopTest {
    @Test
    public void testCorrectness() {
        testCorrectness(new CheinExtension(new CyclicProduct(2, 2)), true);
        testCorrectness(new CheinExtension(new PermutationGroup(3, false)), false);
        testCorrectness(new SpecialLinearLoop(new GaloisField(2)), false);
    }

    private static void testCorrectness(Loop l, boolean associative) {
        int nonAssoc = l.elements().map(i -> {
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
}
