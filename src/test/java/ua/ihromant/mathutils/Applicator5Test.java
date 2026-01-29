package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Applicator5Test {
    @Test
    public void testEven() throws IOException {
        int k = 4;
        int v = 37;
        int gs = 18;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            boolean infinity = v % gs == 1;
            int[] comps = infinity ? new int[]{1, 1, group.order()} : new int[]{1, 1};
            GSpace sp = new GSpace(k, group, false, comps);
            int orbitCount = v / gs;
            FixBS removableDiffs = new FixBS(sp.diffLength());
            for (int fst : sp.oBeg()) {
                for (int snd = fst + 1; snd < v; snd++) {
                    int lft = sp.diffIdx(fst * v + snd);
                    if (lft == sp.diffIdx(snd * v + fst)) {
                        removableDiffs.set(lft);
                    }
                }
            }
            if (infinity) {
                int inf = v - 1;
                for (int pt = 0; pt < v - 1; pt++) {
                    removableDiffs.set(sp.diffIdx(pt * v + inf));
                    removableDiffs.set(sp.diffIdx(inf * v + pt));
                }
            }
            System.out.println(GroupIndex.identify(group) + " " + sp.v() + " " + k + " auths: " + sp.authLength() + " diffs: " + removableDiffs.cardinality());
            State[] base = getRemovable(removableDiffs, sp, v, group);
            System.out.println("Base blocks " + base.length);
            List<State[]> begins = generateBegins(base, sp, removableDiffs);
            System.out.println("Initial configs " + begins.size());
            for (State[] states : begins) {
                FixBS[][] filters = filters(states, sp, group);
                List<LeftState[]> snc = Collections.synchronizedList(new ArrayList<>());
                int[][] suitable = firstSuitable(states, sp);
                for (int[] sizes : suitable) {
                    int[] rev = new int[sizes.length];
                    for (int i = 0; i < rev.length; i++) {
                        rev[i] = k - sizes[rev.length - i - 1];
                    }
                    if (orbitCount == 2 && Combinatorics.compareArr(rev, sizes) < 0) {
                        continue;
                    }
                    generateChunks(filters, sizes, sp, group, snc::add);
                }
                System.out.println("Lefts size: " + snc.size());
                snc.stream().parallel().forEach(left -> {
                    int ll = left.length;
                    Predicate<RightState[]> cons = arr -> {
                        if (arr[ll - 1] == null) {
                            return false;
                        }
                        Liner lnr = new Liner(v, Stream.concat(Arrays.stream(states).flatMap(st -> sp.blocks(st.block())),
                                IntStream.range(0, left.length).mapToObj(i -> {
                                    FixBS result = new FixBS(v);
                                    IntList l = left[i].block;
                                    IntList r = arr[i].block;
                                    for (int ii = 0; ii < l.size(); ii++) {
                                        result.set(l.get(ii));
                                    }
                                    for (int ii = 0; ii < r.size(); ii++) {
                                        result.set(r.get(ii) + gs);
                                    }
                                    return result;
                                }).flatMap(sp::blocks)).toArray(int[][]::new));
                        int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left[i].block.toArray(), arr[i].block.toArray()}).toArray(int[][][]::new);
                        System.out.println(lnr.hyperbolicFreq() + " " + Arrays.deepToString(res));
                        return true;
                    };
                    LeftState fstLeft = left[0];
                    RightState[] rights = new RightState[ll];
                    FixBS whiteList = new FixBS(gs);
                    whiteList.set(0, gs);
                    FixBS outerFilter = filters[0][1];
                    for (int i = 0; i < fstLeft.block().size(); i++) {
                        int el = fstLeft.block().get(i);
                        for (int diff = outerFilter.nextSetBit(0); diff >= 0; diff = outerFilter.nextSetBit(diff + 1)) {
                            whiteList.clear(group.op(el, diff));
                        }
                    }
                    RightState state = new RightState(new IntList(k), filters[1][1], outerFilter, whiteList, 0);
                    if (outerFilter.isEmpty()) {
                        state = state.acceptElem(0, fstLeft, group);
                    }
                    find(left, rights, state, k, group, cons);
                });
                for (LeftState[] beg : snc) {
                    System.out.println(Arrays.deepToString(Arrays.stream(states).map(State::block).toArray()) + " " + Arrays.deepToString(beg));
                }
            }
        }
    }

    private record Wrap(int[][] used) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Wrap(int[][] used1))) return false;
            return Arrays.deepEquals(used, used1);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(used);
        }
    }

    private static List<State[]> generateBegins(State[] base, GSpace sp, FixBS removableDiffs) {
        List<State[]> begins = Collections.synchronizedList(new ArrayList<>());
        if (removableDiffs.isEmpty()) {
            begins.add(new State[0]);
        }
        FixBS[] intersecting = intersecting(base);
        Map<Wrap, List<List<int[]>>> confs = new ConcurrentHashMap<>();
        IntStream.range(0, base.length).parallel().forEach(idx -> {
            State st = base[idx];
            find(base, intersecting, Des.of(sp.diffLength(), base.length, st, intersecting[idx], idx), des -> {
                if (!removableDiffs.diff(des.diffSet).isEmpty()) {
                    return false;
                }
                int[][] used = usedDiffs(des.curr, sp, sp.v() / sp.gOrd(), sp.v());
                if (confs.computeIfAbsent(new Wrap(used), w -> find(w.used, sp.gOrd(), sp.k())).isEmpty()) {
                    return false;
                }
                Arrays.sort(des.curr(), Comparator.comparing(State::block));
                begins.add(des.curr());
                return false;
            });
        });
        begins.sort((a, b) -> Integer.compare(b.length, a.length));
        return begins;
    }

    private static void find(LeftState[] lefts, RightState[] rights, RightState currState, int k, Group group, Predicate<RightState[]> cons) {
        int idx = currState.idx;
        LeftState left = lefts[idx];
        if (currState.block().size() == k - left.size()) {
            RightState[] nextDesign = rights.clone();
            nextDesign[idx] = currState;
            if (cons.test(nextDesign)) {
                return;
            }
            int nextIdx = idx + 1;
            IntList nextLeft = lefts[nextIdx].block();
            FixBS nextWhitelist = new FixBS(group.order());
            nextWhitelist.flip(0, group.order());
            FixBS outerFilter = currState.outerFilter;
            for (int i = 0; i < nextLeft.size(); i++) {
                int el = nextLeft.get(i);
                for (int diff = outerFilter.nextSetBit(0); diff >= 0; diff = outerFilter.nextSetBit(diff + 1)) {
                    nextWhitelist.clear(group.op(el, diff));
                }
            }
            RightState nextState = new RightState(new IntList(k), currState.filter, currState.outerFilter, nextWhitelist, nextIdx);
            find(lefts, nextDesign, nextState, k, group, cons);
        } else {
            FixBS whiteList = currState.whiteList;
            for (int el = whiteList.nextSetBit(currState.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                RightState nextState = currState.acceptElem(el, left, group);
                find(lefts, rights, nextState, k, group, cons);
            }
        }
    }

    private void generateChunks(FixBS[][] filters, int[] sizes, GSpace sp, Group group, Consumer<LeftState[]> cons) {
        int[] freq = new int[sp.k() + 1];
        for (int val : sizes) {
            freq[val]++;
        }
        int total = Arrays.stream(freq).sum();
        int totalNonTrivial = Arrays.stream(freq, 2, freq.length).sum();
        System.out.println("Generate for " + sp.v() + " " + sp.k() + " " + Arrays.toString(sizes) + " " + totalNonTrivial);
        int[][] auths = group.auth();
        IntList newBlock = new IntList(sp.k());
        newBlock.add(0);
        FixBS filter = filters[0][0];
        FixBS whiteList = filter.copy();
        whiteList.flip(1, group.order());
        List<LeftState[]> triples = new ArrayList<>();
        searchDesigns(new LeftState[0], freq, new LeftState(newBlock, filter, new FixBS(group.order()), whiteList).acceptElem(whiteList.nextSetBit(1), group), group, sp.k(), des -> {
            FixBS[] base = Arrays.stream(des).map(st -> FixBS.of(group.order(), st.block.toArray())).toArray(FixBS[]::new);
            for (int[] auth : auths) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, group)).sorted().toArray(FixBS[]::new))) {
                    return true;
                }
            }
            if (des.length < Math.min(totalNonTrivial / 2, 2)) {
                return false;
            }
            triples.add(des);
            return true;
        });
        System.out.println("Triples size: " + triples.size());
        triples.stream().parallel().forEach(des -> {
            int[] rem = freq.clone();
            for (LeftState st : des) {
                rem[st.block.size()]--;
            }
            FixBS ftr = des[des.length - 1].filter;
            FixBS whL = ftr.copy();
            whL.flip(1, group.order());
            IntList nwb = new IntList(sp.k());
            nwb.add(0);
            searchDesigns(des, rem, new LeftState(nwb, ftr, new FixBS(group.order()), whL).acceptElem(whL.nextSetBit(0), group), group, sp.k(), finDes -> {
                if (finDes.length < totalNonTrivial) {
                    return false;
                }
                FixBS[] base = Arrays.stream(finDes).map(st -> FixBS.of(group.order(), st.block.toArray())).toArray(FixBS[]::new);
                for (int[] auth : auths) {
                    if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, group)).sorted().toArray(FixBS[]::new))) {
                        return true;
                    }
                }
                LeftState[] res = Arrays.copyOf(finDes, total);
                for (int i = totalNonTrivial; i < totalNonTrivial + freq[1]; i++) {
                    IntList block = new IntList(sp.k());
                    block.add(0);
                    res[i] = new LeftState(block, finDes[finDes.length - 1].filter, new FixBS(group.order()), new FixBS(group.order()));
                }
                for (int i = totalNonTrivial + freq[1]; i < total; i++) {
                    res[i] = new LeftState(new IntList(sp.k()), finDes[finDes.length - 1].filter, new FixBS(group.order()), new FixBS(group.order()));
                }
                cons.accept(res);
                return true;
            });
        });
    }

    private static FixBS minimalTuple(FixBS tuple, int[] auth, Group gr) {
        int ord = gr.order();
        FixBS base = new FixBS(ord);
        for (int val = tuple.nextSetBit(0); val >= 0; val = tuple.nextSetBit(val + 1)) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(1); val >= 0 && val < ord; val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(ord);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(gr.op(inv, oVal));
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min;
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

    private static void searchDesigns(LeftState[] currDesign, int[] freq, LeftState state, Group gr, int k, Predicate<LeftState[]> cons) {
        IntList block = state.block;
        int size = state.size();
        if (hasNext(freq, size + 1)) {
            for (int el = state.whiteList.nextSetBit(block.getLast() + 1); el >= 0; el = state.whiteList.nextSetBit(el + 1)) {
                LeftState nextState = state.acceptElem(el, gr);
                searchDesigns(currDesign, freq, nextState, gr, k, cons);
            }
        }
        if (freq[size] > 0) {
            LeftState[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign)) {
                return;
            }
            IntList newBlock = new IntList(k);
            newBlock.add(0);
            FixBS whiteList = state.filter.copy();
            whiteList.flip(1, gr.order());
            LeftState nextState = new LeftState(newBlock, state.filter, new FixBS(gr.order()), whiteList).acceptElem(whiteList.nextSetBit(0), gr);
            int[] newFreq = freq.clone();
            newFreq[size]--;
            searchDesigns(nextDesign, newFreq, nextState, gr, k, cons);
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

    private static State[] getRemovable(FixBS removableDiffs, GSpace sp, int v, Group group) {
        Map<FixBS, State> singles = new ConcurrentHashMap<>();
        boolean inf = v % group.order() == 1;
        Consumer<State> cons = st -> {
            if (!st.diffSet().intersects(removableDiffs)) {
                return;
            }
            State minimized = st.minimizeBlock(sp);
            singles.putIfAbsent(minimized.block(), minimized);
        };
        IntStream.of(sp.oBeg()).parallel().forEach(fst -> {
            int[] even = IntStream.range(fst + 1, v).filter(snd -> removableDiffs.get(sp.diffIdx(fst * v + snd))).toArray();
            Arrays.stream(even).parallel().forEach(snd -> {
                State state = new State(FixBS.of(v, fst), FixBS.of(group.order(), 0), new FixBS(sp.diffLength()), new int[sp.diffLength()][], 1)
                        .acceptElem(sp, sp.emptyFilter(), snd);
                if (state == null) {
                    return;
                }
                searchDesignsFirstNoMin(sp, state, -1, cons);
            });
        });
        if (inf) {
            IntStream.of(sp.oBeg()).parallel().forEach(fst -> {
                IntStream.range(fst + 1, v - 1).parallel().forEach(snd -> {
                    State state = sp.forInitial(fst, snd, v - 1);
                    if (state == null) {
                        return;
                    }
                    searchDesignsFirstNoMin(sp, state, snd, cons);
                });
            });
        }
        State[] base = singles.values().toArray(State[]::new);
        Arrays.sort(base, Comparator.comparing(st -> removableDiffs.intersection(st.diffSet())));
        return base;
    }

    private static void searchDesignsFirstNoMin(GSpace space, State state, int prev, Consumer<State> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                State nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null) {
                    searchDesignsFirstNoMin(space, nextState, el, cons);
                }
            }
        }
    }

    private static FixBS[] intersecting(State[] states) {
        FixBS[] intersecting = new FixBS[states.length];
        IntStream.range(0, states.length).parallel().forEach(i -> {
            FixBS comp = new FixBS(states.length);
            FixBS ftr = states[i].diffSet();
            for (int j = 0; j < states.length; j++) {
                if (ftr.intersects(states[j].diffSet())) {
                    comp.set(j);
                }
            }
            intersecting[i] = comp;
        });
        return intersecting;
    }

    private static void find(State[] states, FixBS[] intersecting, Des des, Predicate<Des> pr) {
        if (pr.test(des)) {
            return;
        }
        FixBS available = des.available;
        for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
            find(states, intersecting, des.accept(states[i], intersecting[i], i), pr);
        }
    }

    private record Des(State[] curr, FixBS diffSet, FixBS available, int idx) {
        private Des accept(State state, FixBS intersecting, int idx) {
            int cl = curr.length;
            State[] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = state;
            return new Des(nextCurr, diffSet.union(state.diffSet()), available.diff(intersecting), idx);
        }

        private static Des empty(int ord, int statesSize) {
            FixBS available = new FixBS(statesSize);
            available.set(0, statesSize);
            return new Des(new State[0], new FixBS(ord), available, -1);
        }

        private static Des of(int ord, int statesSize, State state, FixBS intersecting, int idx) {
            return empty(ord, statesSize).accept(state, intersecting, idx);
        }
    }

    private static List<List<int[]>> find(int[][] used, int gOrd, int k) {
        int orbitCount = used.length;
        List<List<int[]>> res = new ArrayList<>();
        int[][] splits = generateSplits(orbitCount, k);
        find(gOrd, orbitCount, used, new ArrayList<>(), splits, 0, res::add);
        return res;
    }

    private static List<List<int[]>> find(State[] basicConf, GSpace sp) {
        int gOrd = sp.gOrd();
        int v = sp.v();
        int orbitCount = v / gOrd;
        return find(usedDiffs(basicConf, sp, orbitCount, v), gOrd, sp.k());
    }

    private static int[][] usedDiffs(State[] basicConf, GSpace sp, int orbitCount, int v) {
        int[][] used = new int[orbitCount][orbitCount];
        for (int i = 0; i < orbitCount; i++) {
            used[i][i] = 1;
        }
        for (State st : basicConf) {
            for (int diff = st.diffSet().nextSetBit(0); diff >= 0; diff = st.diffSet().nextSetBit(diff + 1)) {
                int pr = sp.difference(diff).nextSetBit(0);
                int fst = sp.orbIdx(pr / v);
                int snd = sp.orbIdx(pr % v);
                if (fst >= orbitCount || snd >= orbitCount) {
                    continue;
                }
                used[fst][snd]++;
            }
        }
        return used;
    }

    private static int[][] generateSplits(int orbitCount, int k) {
        List<int[]> res = new ArrayList<>();
        generateSplits(k, new int[orbitCount], 0, 0, res::add);
        return res.toArray(int[][]::new);
    }

    private static void generateSplits(int k, int[] curr, int idx, int sum, Consumer<int[]> cons) {
        for (int i = 0; i <= k - sum; i++) {
            int[] nextCurr = curr.clone();
            nextCurr[idx] = i;
            if (idx < curr.length - 2) {
                generateSplits(k, nextCurr, idx + 1, sum + i, cons);
            } else {
                nextCurr[curr.length - 1] = k - sum - i;
                cons.accept(nextCurr);
            }
        }
    }

    private static void find(int orbitSize, int orbitCount, int[][] used, List<int[]> lst, int[][] splits, int idx, Consumer<List<int[]>> cons) {
        ex: for (int i = idx; i < splits.length; i++) {
            int[] split = splits[i];
            int[][] nextUsed = new int[orbitCount][orbitCount];
            boolean allSize = true;
            for (int j = 0; j < orbitCount; j++) {
                for (int k = 0; k < orbitCount; k++) {
                    int addition = j == k ? split[j] * (split[j] - 1) : split[j] * split[k];
                    int nextVal = used[j][k] + addition;
                    if (nextVal > orbitSize) {
                        continue ex;
                    }
                    allSize = allSize && nextVal == orbitSize;
                    nextUsed[j][k] = nextVal;
                }
            }
            List<int[]> nextLst = new ArrayList<>(lst);
            nextLst.add(split);
            if (allSize) {
                cons.accept(nextLst);
            } else {
                find(orbitSize, orbitCount, nextUsed, nextLst, splits, i, cons);
            }
        }
    }

    private FixBS[][] filters(State[] states, GSpace space, Group group) {
        int gOrd = space.gOrd();
        int v = space.v();
        int orbitCount = v / gOrd;
        FixBS[][] result = new FixBS[orbitCount][orbitCount];
        for (int i = 0; i < orbitCount; i++) {
            for (int j = 0; j < orbitCount; j++) {
                result[i][j] = new FixBS(gOrd);
            }
        }
        for (State state : states) {
            for (int diff = state.diffSet().nextSetBit(0); diff >= 0; diff = state.diffSet().nextSetBit(diff + 1)) {
                int pr = space.difference(diff).nextSetBit(0);
                int fst = pr / v;
                int snd = pr % v;
                int fstOrb = fst / gOrd;
                int sndOrb = snd / gOrd;
                if (fstOrb >= orbitCount || sndOrb >= orbitCount) {
                    continue;
                }
                result[fstOrb][sndOrb].set(group.op(group.inv(fst % gOrd), snd % gOrd));
            }
        }
        return result;
    }

    public int[][] firstSuitable(State[] basicConf, GSpace sp) {
        return groupedSuitable(basicConf, sp).keySet().toArray(int[][]::new);
    }

    public Map<int[], List<int[][]>> groupedSuitable(State[] basicConf, GSpace sp) {
        TreeMap<int[], List<int[][]>> result = new TreeMap<>(Combinatorics::compareArr);
        Arrays.stream(suitable(basicConf, sp)).forEach(arr -> {
            int[] fst = Arrays.stream(arr).mapToInt(pr -> pr[0]).toArray();
            result.computeIfAbsent(fst, _ -> new ArrayList<>()).add(arr);
        });
        return result;
    }

    public int[][][] suitable(State[] basicConf, GSpace sp) {
        return find(basicConf, sp).stream().map(l -> l.toArray(int[][]::new)).toArray(int[][][]::new);
    }

    private record LeftState(IntList block, FixBS filter, FixBS revDiff, FixBS whiteList) {
        private LeftState acceptElem(int el, Group group) {
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            boolean tupleFinished = nextBlock.size() == nextBlock.arr().length;
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            FixBS newRevDiff = revDiff.copy();
            int invEl = group.inv(el);
            for (int i = 0; i < block.size(); i++) {
                int val = block.get(i);
                int invVal = group.inv(val);
                int diff = group.op(invVal, el);
                int outDiff = group.op(invEl, val);
                newFilter.set(diff);
                newFilter.set(outDiff);
                newRevDiff.set(group.op(el, invVal));
                newRevDiff.set(group.op(val, invEl));
                if (tupleFinished) {
                    continue;
                }
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(el, rt));
                }
                for (int j = 0; j <= block.size(); j++) {
                    int nv = nextBlock.get(j);
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            if (!tupleFinished) {
                for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(el, diff));
                }
            }
            return new LeftState(nextBlock, newFilter, newRevDiff, newWhiteList);
        }

        private int size() {
            return block.size();
        }
    }

    private record RightState(IntList block, FixBS filter, FixBS outerFilter, FixBS whiteList, int idx) {
        private RightState acceptElem(int el, LeftState left, Group group) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newOuterFilter = outerFilter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = group.inv(el);
            for (int i = 0; i < sz; i++) {
                int val = nextBlock.get(i);
                int diff = group.op(group.inv(val), el);
                int outDiff = group.op(invEl, val);
                newFilter.set(diff);
                newFilter.set(outDiff);
                // if tuple finished probably
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(el, rt));
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = nextBlock.get(j);
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear(group.op(el, diff));
            }
            for (int revDiff = left.revDiff.nextSetBit(0); revDiff >= 0; revDiff = left.revDiff.nextSetBit(revDiff + 1)) {
                newWhiteList.clear(group.op(revDiff, el));
            }
            for (int j = 0; j < left.size(); j++) {
                newOuterFilter.set(group.op(group.inv(left.block().get(j)), el)); // TODO check right to left
            }

            return new RightState(nextBlock, newFilter, newOuterFilter, newWhiteList, idx);
        }

        public int last() {
            return block.isEmpty() ? -1 : block.getLast();
        }
    }
}
