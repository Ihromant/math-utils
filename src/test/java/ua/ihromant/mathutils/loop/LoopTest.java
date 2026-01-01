package ua.ihromant.mathutils.loop;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.util.FixBS;

import java.util.List;

public class LoopTest {
    @Test
    public void test() {
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
}
