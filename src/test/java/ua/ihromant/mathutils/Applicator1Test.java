package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.plane.AffinePlane;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Applicator1Test {
    @Test
    public void findPossible() {
        int v = 53;
        int k = 6;
        List<IntList> res = getSuitable(v, k);
        res.forEach(System.out::println);
    }

    private static List<IntList> getSuitable(int v, int k) {
        IntList il = new IntList(v);
        List<IntList> res = new ArrayList<>();
        find(v, k, v % k == 0 ? k - 1 : 0, v % k == 0 ? k - 1 : 0, 0, il, res::add);
        return res;
    }

    private static void find(int v, int k, int left, int right, int inter, IntList lst, Consumer<IntList> cons) {
        if (left == v - 1 && right == v - 1 && inter == v) {
            cons.accept(lst);
            return;
        }
        if (left >= v || right >= v || inter > v) {
            return;
        }
        int prev = lst.isEmpty() ? 0 : lst.get(lst.size() - 1);
        for (int i = prev; i <= k; i++) {
            IntList nextLst = lst.copy();
            nextLst.add(i);
            find(v, k, left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst, cons);
        }
    }

    private record State(IntList block, FixBS filter, FixBS whiteList) {
        public static State fromBlock(int[] block, int v, int k) {
            FixBS whiteList = new FixBS(v);
            whiteList.set(1, v);
            State result = new State(new IntList(k), new FixBS(v), whiteList);
            for (int el : block) {
                result = result.acceptElem(el, v);
            }
            return result;
        }

        private State acceptElem(int el, int v) {
            int sz = block.size();
            block.add(el);
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < sz; i++) {
                int val = block.get(i);
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = block.get(j);
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(block, newFilter, newWhiteList);
        }

        private int size() {
            return block.size();
        }
    }

    @Test
    public void generate() {
        int v = 48;
        int k = 6;
        int[][] suitable = getSuitable(v, k).stream().map(IntList::toArray).toArray(int[][]::new);
        int idx = 0;
        int[] freq = new int[k + 1];
        for (int val : suitable[idx]) {
            if (val > 1) {
                freq[val]++;
            }
        }
        int[] multipliers = Combinatorics.multipliers(v);
        FixBS[] currDes = new FixBS[0];
    }

    private static FixBS minimalTuple(FixBS tuple, int multiplier, int v) {
        FixBS mapped = new FixBS(v);
        for (int i = tuple.nextSetBit(0); i >= 0; i = tuple.nextSetBit(i + 1)) {
            mapped.set((i * multiplier) % v);
        }
        FixBS result = mapped;
        for (int i = mapped.nextSetBit(1); i >= 0; i = mapped.nextSetBit(i + 1)) {
            FixBS cand = new FixBS(v);
            for (int j = mapped.nextSetBit(0); j >= 0; j = mapped.nextSetBit(j + 1)) {
                int dff = i - j;
                cand.set(dff < 0 ? v + dff : dff);
            }
            if (cand.compareTo(result) < 0) {
                result = cand;
            }
        }
        return result;
    }

    @Test
    public void test() throws IOException {
        Liner pr = new Liner(new GaloisField(5).generatePlane());
        Liner pl = new AffinePlane(pr, 0).toLiner();
        Group g = pl.automorphisms();
        System.out.println(GroupIndex.identify(g));
    }
}
