package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TranslationPlaneTest {
    @Test
    public void test() {
        int p = 3;
        int n = 4;
        int h = n / 2;
        int crd = LinearSpace.pow(p, n);
        int halfCrd = LinearSpace.pow(p, h) - 1;
        int cnt = (crd - 1) / halfCrd;
        LinearSpace sp = new LinearSpace(p, n);
        FixBS[] curr = new FixBS[cnt];
        FixBS union = new FixBS(crd);
        Set<Set<FixBS>> unique = new HashSet<>();
        AtomicInteger counter = new AtomicInteger();
        Consumer<FixBS[]> cons = arr -> {
            Set<FixBS> set = Arrays.stream(arr).collect(Collectors.toSet());
            if (!unique.add(set)) {
                return;
            }
            int v = counter.incrementAndGet();
            if (v % 1000 == 0) {
                System.out.println(v);
            }
            //System.out.println(set);
        };
        generate(sp, curr, union, cnt, halfCrd, cons);
        System.out.println(counter);
    }

    private static void generate(LinearSpace space, FixBS[] curr, FixBS union, int needed, int halfCrd, Consumer<FixBS[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        Set<FixBS> unique = new HashSet<>();
        Consumer<int[]> consumer = arr -> {
            FixBS bs = space.hull(arr);
            if (bs.intersects(union) || bs.cardinality() != halfCrd || !unique.add(bs)) {
                return;
            }
            FixBS[] newCurr = curr.clone();
            newCurr[curr.length - needed] = bs;
            FixBS newUnion = union.union(bs);
            generate(space, newCurr, newUnion, needed - 1, halfCrd, cons);
        };
        int half = space.getN() / 2;
        int[] arr = new int[half];
        arr[0] = union.nextClearBit(1);
        generateOne(space, arr, half - 1, consumer);
    }

    private static void generateOne(LinearSpace sp, int[] curr, int needed, Consumer<int[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        int prev = curr[curr.length - needed - 1];
        for (int i = prev + 1; i < sp.cardinality(); i++) {
            int[] newCurr = curr.clone();
            newCurr[curr.length - needed] = i;
            generateOne(sp, newCurr, needed - 1, cons);
        }
    }
}
