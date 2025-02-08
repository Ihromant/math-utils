package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicatorTest {
    private static class GCosets {
        private final Group gr;
        private final int[][] cosets;
        private final int[] idx;

        public GCosets(SubGroup sg) {
            this.gr = sg.group();
            Set<FixBS> set = new HashSet<>();
            int order = gr.order();
            for (int i = 0; i < order; i++) {
                FixBS coset = new FixBS(order);
                for (int el : sg.arr()) {
                    coset.set(gr.op(i, el));
                }
                set.add(coset);
            }
            this.cosets = set.stream().map(FixBS::toArray).toArray(int[][]::new);
            Arrays.sort(cosets, Group::compareArr);
            this.idx = new int[order];
            for (int i = 0; i < cosets.length; i++) {
                for (int el : cosets[i]) {
                    idx[el] = i;
                }
            }
        }

        private int apply(int g, int x) {
            int min = cosets[x][0];
            return idx[gr.op(g, min)];
        }
    }

    private static class GSpace {
        private final Group group;
        private final GCosets[] cosets;
        private final int[] oBeg;
        private final int v;
        private final int k;
        private final int[][] cayley;
        private final int[][] auths;
        private final List<FixBS> differences;
        private final int[] diffMap;
        private final List<Map<Integer, FixBS>> preImages;
        private final State[][] statesCache;

        public GSpace(int k, Group group, int... comps) {
            this.k = k;
            Group table = group.asTable();
            this.group = table;
            List<SubGroup> subGroups = table.subGroups();
            SubGroup[] subs = Arrays.stream(comps).mapToObj(sz -> subGroups.stream().filter(sg -> sg.order() == sz).findAny().orElseThrow()).toArray(SubGroup[]::new);
            this.cosets = new GCosets[subs.length];
            this.oBeg = new int[subs.length];
            int min = 0;
            for (int i = 0; i < subs.length; i++) {
                oBeg[i] = min;
                GCosets conf = new GCosets(subs[i]);
                this.cosets[i] = conf;
                min = min + conf.cosets.length;
            }
            this.v = min;
            int gOrd = table.order();
            PermutationGroup auths = new PermutationGroup(group.auth());
            FixBS suitableAuths = new FixBS(auths.order());
            ex: for (int el = 0; el < auths.order(); el++) {
                for (GCosets coset : cosets) {
                    int[][] csts = coset.cosets;
                    Set<FixBS> set = Arrays.stream(csts).map(arr -> FixBS.of(gOrd, arr)).collect(Collectors.toSet());
                    FixBS fbs = set.iterator().next();
                    if (!set.contains(auths.apply(el, fbs))) {
                        continue ex;
                    }
                }
                suitableAuths.set(el);
            }
            PermutationGroup suitable = auths.subset(suitableAuths);
            List<List<Integer>> grouped = IntStream.range(0, comps.length).boxed().collect(Collectors.groupingBy(i -> comps[i]))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
            int[] base = IntStream.range(0, comps.length).toArray();
            List<PermutationGroup> permutations = grouped.stream().map(lst -> {
                int[] arr = lst.subList(0, Math.min(lst.size(), 7)).stream().mapToInt(Integer::intValue).toArray();
                int[][] perms;
                if (comps[arr[0]] == gOrd) {
                    perms = IntStream.range(0, arr.length).mapToObj(sh -> {
                        int[] copy = base.clone();
                        for (int i = 0; i < arr.length; i++) {
                            copy[arr[i]] = arr[(i + sh) % arr.length];
                        }
                        return copy;
                    }).toArray(int[][]::new);
                } else {
                    perms = GaloisField.permutations(arr).map(perm -> {
                        int[] copy = base.clone();
                        for (int i = 0; i < arr.length; i++) {
                            copy[arr[i]] = perm[i];
                        }
                        return copy;
                    }).toArray(int[][]::new);
                }
                return new PermutationGroup(perms);
            }).toList();
            int prod = permutations.stream().mapToInt(Group::order).reduce(1, (a, b) -> a * b);
            this.auths = new int[suitable.order() * prod][v];
            int cnt = 0;
            for (int el = 0; el < suitable.order(); el++) {
                int[] perm = suitable.permutation(el);
                int[] permuted = new int[v];
                for (int x = 0; x < v; x++) {
                    int idx = Arrays.binarySearch(oBeg, x);
                    if (idx < 0) {
                        idx = -idx - 2;
                    }
                    int cosMin = oBeg[idx];
                    GCosets conf = cosets[idx];
                    int gBase = conf.cosets[x - cosMin][0];
                    int gMapped = perm[gBase];
                    permuted[x] = conf.idx[gMapped] + cosMin;
                }
                cnt = generateAuth(permutations, permuted, cnt, 0);
            }
            FixBS inter = new FixBS(gOrd);
            inter.set(0, gOrd);
            for (SubGroup sub : subs) {
                for (int x = 0; x < gOrd; x++) {
                    FixBS conj = new FixBS(gOrd);
                    for (int h : sub.arr()) {
                        conj.set(table.op(table.inv(x), table.op(h, x)));
                    }
                    inter.and(conj);
                }
            }
            if (inter.cardinality() != 1) {
                throw new IllegalArgumentException("Non empty intersection " + inter);
            }
            this.cayley = new int[gOrd][v];
            for (int g = 0; g < gOrd; g++) {
                for (int x = 0; x < v; x++) {
                    cayley[g][x] = applyByDef(g, x);
                }
            }
            QuickFind qf = new QuickFind(v * v);
            this.preImages = IntStream.range(0, v * v).<Map<Integer, FixBS>>mapToObj(i -> new HashMap<>()).toList();
            for (int x1 = 0; x1 < v; x1++) {
                for (int x2 = 0; x2 < v; x2++) {
                    int pair = x1 * v + x2;
                    for (int g = 0; g < gOrd; g++) {
                        int gx1 = applyByDef(g, x1);
                        int gx2 = applyByDef(g, x2);
                        int gPair = gx1 * v + gx2;
                        qf.union(pair, gPair);
                        preImages.get(gPair).computeIfAbsent(pair, key -> new FixBS(gOrd)).set(g);
                    }
                }
            }
            FixBS diagonal = FixBS.of(v * v, IntStream.range(0, v).map(i -> i * v + i).toArray());
            this.differences = qf.components().stream().filter(c -> !c.intersects(diagonal)).toList();
            this.diffMap = new int[v * v];
            for (int i = 0; i < differences.size(); i++) {
                FixBS comp = differences.get(i);
                for (int val = comp.nextSetBit(0); val >= 0; val = comp.nextSetBit(val + 1)) {
                    diffMap[val] = i;
                }
            }
            for (int i = 0; i < v; i++) {
                diffMap[i * v + i] = -1;
            }
            this.statesCache = new State[v][v];
            FixBS emptyFilter = new FixBS(v * v);
            for (int f = 0; f < v; f++) {
                for (int s = f + 1; s < v; s++) {
                    statesCache[f][s] = new State(FixBS.of(v, f), FixBS.of(gOrd, 0), new IntList[differences.size()], 1)
                            .acceptElem(this, emptyFilter, s);
                }
            }
        }

        private int generateAuth(List<PermutationGroup> permutations, int[] base, int counter, int idx) {
            if (idx == permutations.size()) {
                System.arraycopy(base, 0, auths[counter], 0, base.length);
                return ++counter;
            }
            PermutationGroup gr = permutations.get(idx);
            for (int i = 0; i < gr.order(); i++) {
                int[] newBase = base.clone();
                int[] perm = gr.permutation(i);
                for (int j = 0; j < perm.length; j++) {
                    int map = perm[j];
                    if (map == j) {
                        continue;
                    }
                    int from = oBeg[j];
                    int sz = (j == perm.length - 1 ? v : oBeg[j + 1]) - from;
                    System.arraycopy(base, from, newBase, oBeg[map], sz);
                }
                counter = generateAuth(permutations, newBase, counter, idx + 1);
            }
            return counter;
        }

        private int applyByDef(int g, int x) {
            int idx = Arrays.binarySearch(oBeg, x);
            if (idx < 0) {
                idx = -idx - 2;
            }
            int min = oBeg[idx];
            GCosets conf = cosets[idx];
            return conf.apply(g, x - min) + min;
        }

        public int apply(int g, int x) {
            return cayley[g][x];
        }

        private Stream<int[]> blocks(FixBS block) {
            Set<FixBS> set = new HashSet<>(2 * group.order());
            List<int[]> res = new ArrayList<>();
            for (int g = 0; g < group.order(); g++) {
                FixBS fbs = new FixBS(v);
                for (int x = block.nextSetBit(0); x >= 0; x = block.nextSetBit(x + 1)) {
                    fbs.set(apply(g, x));
                }
                if (set.add(fbs)) {
                    res.add(fbs.toArray());
                }
            }
            return res.stream();
        }

        private boolean minimal(FixBS block) {
            for (int[] auth : auths) {
                FixBS altBlock = new FixBS(v);
                for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                    altBlock.set(auth[el]);
                }
                for (int sh = altBlock.nextSetBit(0); sh >= 0 && sh < group.order(); sh = altBlock.nextSetBit(sh + 1)) {
                    FixBS shifted = new FixBS(v);
                    int inv = group.inv(sh);
                    for (int el = altBlock.nextSetBit(0); el >= 0; el = altBlock.nextSetBit(el + 1)) {
                        shifted.set(apply(inv, el));
                    }
                    if (shifted.compareTo(block) < 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean minimal(State[] states) {
            FixBS[] blocks = new FixBS[states.length];
            for (int i = 0; i < states.length; i++) {
                blocks[i] = states[i].block;
            }
            ex: for (int[] auth : auths) {
                FixBS[] altBlocks = new FixBS[blocks.length];
                for (int i = 0; i < blocks.length; i++) {
                    FixBS block = blocks[i];
                    FixBS altBlock = new FixBS(v);
                    for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                        altBlock.set(auth[el]);
                    }
                    FixBS min = altBlock;
                    for (int sh = altBlock.nextSetBit(0); sh >= 0 && sh < group.order(); sh = altBlock.nextSetBit(sh + 1)) {
                        FixBS shifted = new FixBS(v);
                        int inv = group.inv(sh);
                        for (int el = altBlock.nextSetBit(0); el >= 0; el = altBlock.nextSetBit(el + 1)) {
                            shifted.set(apply(inv, el));
                        }
                        if (shifted.compareTo(min) < 0) {
                            min = shifted;
                        }
                    }
                    altBlocks[i] = min;
                }
                Arrays.sort(altBlocks);
                for (int i = 0; i < blocks.length; i++) {
                    int cmp = altBlocks[i].compareTo(blocks[i]);
                    if (cmp < 0) {
                        return false;
                    }
                    if (cmp > 0) {
                        continue ex;
                    }
                }
            }
            return true;
        }
    }

    private record State(FixBS block, FixBS stabilizer, IntList[] diffs, int size) {
        private State acceptElem(GSpace gSpace, FixBS globalFilter, int val) {
            int v = gSpace.v;
            int k = gSpace.k;
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(v);
            queue.set(val);
            int sz = size;
            FixBS newStabilizer = stabilizer.copy();
            IntList[] newDiffs = new IntList[diffs.length];
            for (int i = 0; i < diffs.length; i++) {
                IntList lst = diffs[i];
                if (lst != null) {
                    newDiffs[i] = lst.copy();
                }
            }
            while (!queue.isEmpty()) {
                if (++sz > k) {
                    return null;
                }
                int x = queue.nextSetBit(0);
                if (x < val) {
                    return null;
                }
                FixBS stabExt = new FixBS(gSpace.group.order());
                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    int bx = b * v + x;
                    int xb = x * v + b;
                    if (globalFilter.get(bx) || globalFilter.get(xb)) {
                        return null;
                    }
                    int compBx = gSpace.diffMap[bx];
                    int compXb = gSpace.diffMap[xb];
                    IntList existingDiffs = newDiffs[compBx];
                    if (existingDiffs == null) {
                        existingDiffs = (newDiffs[compBx] = new IntList(3 * k));
                    } else {
                        for (int i = 0; i < existingDiffs.size(); i++) {
                            int diff = existingDiffs.get(i);
                            stabExt.or(gSpace.preImages.get(diff).get(bx));
                        }
                    }
                    existingDiffs.add(bx);

                    existingDiffs = newDiffs[compXb];
                    if (existingDiffs == null) {
                        existingDiffs = (newDiffs[compXb] = new IntList(3 * k));
                    } else {
                        for (int i = 0; i < existingDiffs.size(); i++) {
                            int diff = existingDiffs.get(i);
                            stabExt.or(gSpace.preImages.get(diff).get(xb));
                        }
                    }
                    existingDiffs.add(xb);
                }
                newBlock.set(x);
                stabExt.andNot(newStabilizer);
                for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                    queue.set(gSpace.apply(st, x));
                }
                for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                    for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                        queue.set(gSpace.apply(st, b));
                    }
                }
                newStabilizer.or(stabExt);
                queue.andNot(newBlock);
            }
            return new State(newBlock, newStabilizer, newDiffs, sz);
        }

        private FixBS updatedFilter(FixBS oldFilter, GSpace space) {
            FixBS result = oldFilter.copy();
            for (int i = 0; i < diffs.length; i++) {
                if (diffs[i] != null) {
                    result.or(space.differences.get(i));
                }
            }
            return result;
        }
    }

    @Test
    public void testApplicator() {
        Group gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace applicator = new GSpace(6, gr, 1, 3, 3, 39);
        assertEquals(66, applicator.v);
        for (int x = 0; x < applicator.v; x++) {
            for (int j = 0; j < gr.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
        gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        applicator = new GSpace(6, gr, 1, 1);
        for (int g = 0; g < gr.order(); g++) {
            for (int x = 0; x < 2 * gr.order(); x++) {
                int app = applicator.apply(g, x);
                int expected = x < gr.order() ? gr.op(g, x) : gr.op(g, x - gr.order()) + gr.order();
                assertEquals(expected, app);
            }
        }
    }

    @Test
    public void testState() {
        try {
            new GSpace(6, new CyclicProduct(2, 2), 2);
            fail();
        } catch (Exception e) {
            // ok
        }
        Group g = new CyclicGroup(21);
        GSpace space = new GSpace(7, g, 1);
        FixBS emptyFilter = new FixBS(space.v * space.v);
        State state = space.statesCache[0][3];
        assertEquals(FixBS.of(space.v, 0, 3), state.block);
        assertEquals(FixBS.of(g.order(), 0), state.stabilizer);
        assertNull(state.acceptElem(space, emptyFilter, 12));
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 6));
        FixBS bs = FixBS.of(g.order(), IntStream.range(0, 7).map(i -> i * 3).toArray());
        assertEquals(bs, state.stabilizer);
        assertEquals(bs, state.block);
        state = space.statesCache[0][7];
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 14));
        assertNull(state.acceptElem(space, emptyFilter, 1));
        g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        space = new GSpace(6, g, 1);
        state = space.statesCache[0][1];
        emptyFilter = new FixBS(space.v * space.v);
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 2));
        assertEquals(FixBS.of(space.v, 0, 1, 2), state.block);
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer);
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 3));
        assertEquals(FixBS.of(space.v, 0, 1, 2, 3, 31, 80), state.block);
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer);
    }

    @Test
    public void testAutomorphisms() {
        Group g = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(100, g, 3);
        PermutationGroup gr = new PermutationGroup(g.auth());
        int sz = gr.order();
        System.out.println(sz);
        int cnt = 0;
        ex: for (int el = 0; el < sz; el++) {
            System.out.println(el + " ************************************");
            int[][] cosets = space.cosets[0].cosets;
            Set<FixBS> set = Arrays.stream(cosets).map(arr -> FixBS.of(g.order(), arr)).collect(Collectors.toSet());
            for (FixBS fbs : set) {
                if (!set.contains(gr.apply(el, fbs))) {
                    System.out.println("Not contains " + fbs + " applied " + gr.apply(el, fbs));
                    continue ex;
                }
            }
            cnt++;
        }
        System.out.println(cnt);
    }

    @Test
    public void logDesigns() {
        int k = 6;
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(k, group, 1, 3, 3, 39);
        System.out.println(group.name() + " " + space.v + " " + k + " auths: " + space.auths.length);
        int sqr = space.v * space.v;
        FixBS filter = new FixBS(sqr);
        for (int i = 0; i < space.v; i++) {
            filter.set(i * space.v + i);
        }
        State[] design = new State[0];
        List<State> initial = new ArrayList<>();
        BiPredicate<State[], FixBS> cons = (arr, ftr) -> {
            initial.add(arr[0]);
            return true;
        };
        int val = 1;
        State state = space.statesCache[0][val];
        searchDesignsMinimal(space, filter, design, state, val, cons);
        BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
            if (arr.length > 1 && arr.length < 5 && !space.minimal(arr)) {
                return true;
            }
            if (ftr.cardinality() < sqr) {
                return false;
            }
            Liner l = new Liner(space.v, Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        System.out.println("Initial length: " + initial.size());
        AtomicInteger cnt = new AtomicInteger();
        initial.stream().parallel().forEach(st -> {
            searchDesigns(space, filter, design, st, 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
    }

    private static void searchDesigns(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        if (state.size() == space.k) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = state.updatedFilter(filter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int pair = nextFilter.nextClearBit(0);
            int snd = pair % space.v;
            State nextState = space.statesCache[pair / space.v][snd];
            searchDesigns(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            int v = space.v;
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = filter.nextClearBit(from); pair >= 0 && pair < to; pair = filter.nextClearBit(pair + 1)) {
                int el = pair % v;
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }

    private static void searchDesignsMinimal(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        if (state.size() == space.k) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = state.updatedFilter(filter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int pair = nextFilter.nextClearBit(0);
            int snd = pair % space.v;
            State nextState = space.statesCache[pair / space.v][snd];
            searchDesignsMinimal(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            int v = space.v;
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = filter.nextClearBit(from); pair >= 0 && pair < to; pair = filter.nextClearBit(pair + 1)) {
                int el = pair % v;
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null && space.minimal(nextState.block)) {
                    searchDesignsMinimal(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }

    private record BlockDiff(FixBS block, FixBS diff) {}

    @Test
    public void logByDiff() {
        int k = 4;
        Group group = new CyclicGroup(3);
        GSpace space = new GSpace(k, group, 1, 1, 1, 1, 1, 1, 1, 1, 3);
        System.out.println(group.name() + " " + space.v + " " + k + " auths: " + space.auths.length);
        int sqr = space.v * space.v;
        FixBS filter = new FixBS(sqr);
        for (int i = 0; i < space.v; i++) {
            filter.set(i * space.v + i);
        }
        State[] design = new State[0];
        List<BlockDiff> initial = new ArrayList<>();
        int sz = space.differences.size();
        BiPredicate<State[], FixBS> cons = (arr, ftr) -> {
            State st = arr[0];
            FixBS block = st.block;
            for (int g = 1; g < space.group.order(); g++) {
                FixBS altBlock = new FixBS(space.v);
                for (int x = block.nextSetBit(0); x >= 0; x = block.nextSetBit(x + 1)) {
                    altBlock.set(space.apply(g, x));
                }
                if (altBlock.compareTo(block) < 0) {
                    return true;
                }
            }
            FixBS diff = new FixBS(sz);
            for (int i = 0; i < sz; i++) {
                if (st.diffs[i] != null) {
                    diff.set(i);
                }
            }
            initial.add(new BlockDiff(block, diff));
            return true;
        };
        for (int fst = 0; fst < space.v; fst++) {
            State state = new State(FixBS.of(space.v, fst), FixBS.of(space.group.order(), 0), new IntList[sz], 1);
            searchDesigns(space, filter, design, state, fst, cons);
        }
        BlockDiff[] blocks = initial.toArray(BlockDiff[]::new);
        Arrays.parallelSort(blocks, Comparator.comparing(BlockDiff::diff));
        int[] order = calcOrder(sz, blocks);
        System.out.println("Initial length: " + initial.size());
        AtomicInteger ai = new AtomicInteger();
        Map<FixBS, Liner> liners = new ConcurrentHashMap<>();
        IntStream.range(order[0], order[1]).parallel().forEach(i -> {
            BlockDiff bd = blocks[i];
            IntList base = new IntList(sz / k);
            base.add(i);
            calculate(blocks, order, bd.diff().cardinality(), bd.diff(), base, (idx, card) -> {
                int size = idx.size();
                if (size < 7) {
                    FixBS[] init = new FixBS[size];
                    for (int j = 0; j < size; j++) {
                        init[j] = blocks[idx.get(j)].block;
                    }
                    ex: for (int[] auth : space.auths) {
                        FixBS[] altBlocks = new FixBS[size];
                        for (int j = 0; j < size; j++) {
                            FixBS altBlock = new FixBS(space.v);
                            FixBS bb = init[j];
                            for (int el = bb.nextSetBit(0); el >= 0; el = bb.nextSetBit(el + 1)) {
                                altBlock.set(auth[el]);
                            }
                            altBlocks[j] = altBlock;
                        }
                        Arrays.sort(altBlocks);
                        for (int j = 0; j < size; j++) {
                            int cmp = altBlocks[j].compareTo(init[j]);
                            if (cmp < 0) {
                                return true;
                            }
                            if (cmp > 0) {
                                continue ex;
                            }
                        }
                    }
                }
                if (card < sz) {
                    return false;
                }
                int[][] ars = Arrays.stream(idx.toArray()).boxed().flatMap(j -> space.blocks(blocks[j].block)).toArray(int[][]::new);
                Liner l = new Liner(space.v, ars);
                if (liners.putIfAbsent(l.getCanonicalOld(), l) == null) {
                    System.out.println(l.hyperbolicFreq() + " " + GroupIndex.identify(l.automorphisms()) + " " + Arrays.deepToString(l.lines()));
                }
                return true;
            });
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println(ai + " " + liners.size());
    }

    private static int[] calcOrder(int sz, BlockDiff[] comps) {
        int[] res = new int[sz];
        for (int i = 1; i < res.length; i++) {
            int prev = res[i - 1];
            FixBS top = FixBS.of(sz, i - 1, sz - 1);
            res[i] = -Arrays.binarySearch(comps, prev, comps.length, new BlockDiff(null, top), Comparator.comparing(BlockDiff::diff)) - 1;
        }
        return res;
    }

    private static void calculate(BlockDiff[] blockDiffs, int[] order, int currCard, FixBS union, IntList curr, BiPredicate<IntList, Integer> cons) {
        if (cons.test(curr, currCard)) {
            return;
        }
        int hole = union.nextClearBit(0);
        for (int i = order[hole]; i < order[hole + 1]; i++) {
            BlockDiff c = blockDiffs[i];
            if (union.intersects(c.diff())) {
                continue;
            }
            IntList newCurr = curr.copy();
            newCurr.add(i);
            calculate(blockDiffs, order, currCard + c.diff().cardinality(), union.union(c.diff()), newCurr, cons);
        }
    }
}
