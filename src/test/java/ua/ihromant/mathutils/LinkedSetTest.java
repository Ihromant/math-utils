package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class LinkedSetTest {
    @Test
    public void test() {
        for (int v = 4; v < 100; v++) {
            FixBS bl = new FixBS(v);
            bl.set(0, v);
            AtomicReference<FixBS> minimal = new AtomicReference<>(bl);
            Consumer<FixBS> cons = block -> minimal.updateAndGet(curr -> {
                if (curr.cardinality() > block.cardinality()) {
                    return block;
                } else {
                    return curr;
                }
            });
            calculate(v, cons);
            System.out.println(v + " " + minimal.get().cardinality() + " " + minimal.get());
        }
    }

    private static void calculate(int v, Consumer<FixBS> blCons) {
        AtomicInteger sz = new AtomicInteger(v);
        FixBS block = FixBS.of(v, 0, 1);
        FixBS reverse = FixBS.of(v, 0, v - 1);
        FixBS diff = new FixBS(v);
        diff.set(2, v - 1);
        int[] multipliers = Combinatorics.multipliers(v);
        Prd cons = (bl, dff, depth) -> {
            if (depth >= sz.get()) {
                return true;
            }
            if (depth < 6) {
                for (int mul : multipliers) {
                    FixBS other = new FixBS(v);
                    for (int i = bl.nextSetBit(0); i >= 0; i = bl.nextSetBit(i + 1)) {
                        other.set(i * mul % v);
                    }
                    if (other.compareTo(bl) < 0) {
                        return true;
                    }
                }
            }
            if (!dff.isEmpty()) {
                return false;
            }
            int cs = sz.updateAndGet(curr -> Math.min(curr, depth));
            if (depth > cs) {
                return true;
            }
            blCons.accept(bl);
            return true;
        };
        recur(v, 2, block, reverse, diff, cons);
    }

    private interface Prd {
        boolean test(FixBS block, FixBS diff, int depth);
    }

    private static void recur(int v, int depth, FixBS block, FixBS reverse, FixBS diff, Prd prd) {
        if (prd.test(block, diff, depth)) {
            return;
        }
        int last = block.previousSetBit(v);
        if (depth < 6) {
            IntStream.range(last + 1, v).parallel().forEach(i -> {
                int inv = v - i;
                FixBS next = block.copy();
                FixBS nextRev = reverse.copy();
                FixBS nextDiff = diff.copy();
                next.set(i);
                nextRev.set(inv);
                nextDiff.diffModuleShifted(next, v, i);
                nextDiff.diffModuleShifted(nextRev, v, inv);
                recur(v, depth + 1, next, nextRev, nextDiff, prd);
            });
        } else {
            for (int i = last + 1; i < v; i++) {
                int inv = v - i;
                FixBS next = block.copy();
                FixBS nextRev = reverse.copy();
                FixBS nextDiff = diff.copy();
                next.set(i);
                nextRev.set(inv);
                nextDiff.diffModuleShifted(next, v, i);
                nextDiff.diffModuleShifted(nextRev, v, inv);
                recur(v, depth + 1, next, nextRev, nextDiff, prd);
            }
        }
    }
}
