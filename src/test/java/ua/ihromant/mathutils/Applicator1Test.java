package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.plane.AffinePlane;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < sz; i++) {
                int val = nextBlock.get(i);
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = nextBlock.get(j);
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(nextBlock, newFilter, newWhiteList);
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
        IntList newBlock = new IntList(k);
        newBlock.add(0);
        FixBS filter = baseFilter(v, k);
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        Map<List<FixBS>, Liner> liners = new ConcurrentHashMap<>();
        searchDesigns(new State[0], freq, new State(newBlock, filter, whiteList).acceptElem(1, v), v, k, des -> {
            if (des[des.length - 1].filter.cardinality() != v - 1) {
                return false;
            }
            FixBS[] base = Arrays.stream(des).map(st -> FixBS.of(v, st.block.toArray())).toArray(FixBS[]::new);
            for (int mul : multipliers) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, mul, v)).sorted().toArray(FixBS[]::new))) {
                    return true;
                }
            }
            Liner l = new Liner(v, Arrays.stream(des).flatMap(bl -> blocks(bl.block.toArray(), v)).toArray(int[][]::new));
            if (liners.putIfAbsent(Arrays.stream(base).toList(), l) == null) {
                System.out.println(l.hyperbolicFreq() + " " + Arrays.toString(Arrays.stream(des).map(State::block).toArray()));
            }
            return true;
        });
    }

    private static Stream<int[]> blocks(int[] block, int v) {
        Set<FixBS> set = new HashSet<>(2 * v);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set((el + i) % v);
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    private boolean bigger(FixBS[] base, FixBS[] cand) {
        for (int i = 0; i < base.length; i++) {
            int cmp = base[i].compareTo(cand[i]);
            if (cmp < 0) {
                return false;
            }
            if (cmp > 0) {
                return true;
            }
        }
        return false;
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        int rest = v % (k * (k - 1));
        if (rest == k) {
            for (int i = 1; i < v; i++) {
                if (i * k % v == 0) {
                    filter.set(i);
                }
            }
        }
        if (rest == (k - 1)) {
            for (int i = 1; i < v; i++) {
                if (i * (k - 1) % v == 0) {
                    filter.set(i);
                }
            }
        }
        return filter;
    }

    private static void searchDesigns(State[] currDesign, int[] freq, State state, int v, int k, Predicate<State[]> cons) {
        IntList block = state.block;
        if (freq[state.size()] > 0) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign)) {
                return;
            }
            IntList newBlock = new IntList(k);
            newBlock.add(0);
            FixBS whiteList = state.filter.copy();
            whiteList.flip(1, v);
            State nextState = new State(newBlock, state.filter, whiteList).acceptElem(whiteList.nextSetBit(0), v);
            int[] newFreq = freq.clone();
            newFreq[state.size()]--;
            searchDesigns(nextDesign, newFreq, nextState, v, k, cons);
        }
        if (hasNext(freq, state.size() + 1)) {
            for (int el = state.whiteList.nextSetBit(block.getLast()); el >= 0; el = state.whiteList.nextSetBit(el + 1)) {
                State nextState = state.acceptElem(el, v);
                searchDesigns(currDesign, freq, nextState, v, k, cons);
            }
        }
    }

    private static boolean hasNext(int[] freq, int from) {
        for (int i = from; i < freq.length; i++) {
            if (freq[i] > 0) {
                return true;
            }
        }
        return false;
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
