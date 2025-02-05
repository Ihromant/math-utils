package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
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
        private final List<FixBS> differences;
        private final int[][] diffMap;
        private final FixBS[][] preImages;
        private final State[][] statesCache;

        public GSpace(int k, SubGroup... subs) {
            this.k = k;
            this.group = subs[0].group();
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
            int gOrd = group.order();
            FixBS inter = new FixBS(gOrd);
            inter.set(0, gOrd);
            for (SubGroup sub : subs) {
                for (int x = 0; x < gOrd; x++) {
                    FixBS conj = new FixBS(gOrd);
                    for (int h : sub.arr()) {
                        conj.set(group.op(group.inv(x), group.op(h, x)));
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
            for (int x1 = 0; x1 < v; x1++) {
                for (int x2 = 0; x2 < v; x2++) {
                    int pair = x1 * v + x2;
                    for (int g = 0; g < group.order(); g++) {
                        int gx1 = applyByDef(g, x1);
                        int gx2 = applyByDef(g, x2);
                        int gPair = gx1 * v + gx2;
                        qf.union(pair, gPair);
                    }
                }
            }
            FixBS diagonal = FixBS.of(v * v, IntStream.range(0, v).map(i -> i * v + i).toArray());
            this.differences = qf.components().stream().filter(c -> !c.intersects(diagonal)).toList();
            this.diffMap = new int[v][v];
            for (int i = 0; i < differences.size(); i++) {
                FixBS comp = differences.get(i);
                for (int val = comp.nextSetBit(0); val >= 0; val = comp.nextSetBit(val + 1)) {
                    diffMap[val / v][val % v] = i;
                }
            }
            for (int i = 0; i < v; i++) {
                diffMap[i][i] = -1;
            }
            this.preImages = new FixBS[v][v];
            for (int i = 0; i < v; i++) {
                for (int j = 0; j < v; j++) {
                    preImages[i][j] = new FixBS(gOrd);
                }
            }
            for (int g = 0; g < gOrd; g++) {
                for (int x = 0; x < v; x++) {
                    int gx = apply(g, x);
                    preImages[gx][x].set(g);
                }
            }
            this.statesCache = new State[v][v];
            FixBS emptyFilter = new FixBS(differences.size());
            for (int f = 0; f < v; f++) {
                for (int s = f + 1; s < v; s++) {
                    statesCache[f][s] = new State(FixBS.of(v, f), FixBS.of(group.order(), 0), new IntList[differences.size()], 1)
                            .acceptElem(this, emptyFilter, s);
                }
            }
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
            IntList[] newDiffs = Arrays.stream(diffs).map(il -> il == null ? null : il.copy()).toArray(IntList[]::new);
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
                    int compBx = gSpace.diffMap[b][x];
                    int compXb = gSpace.diffMap[x][b];
                    if (globalFilter.get(compBx) || globalFilter.get(compXb)) {
                        return null;
                    }
                    IntList existingDiffs = newDiffs[compBx];
                    if (existingDiffs == null) {
                        existingDiffs = (newDiffs[compBx] = new IntList(k));
                    } else {
                        for (int i = 0; i < existingDiffs.size(); i++) {
                            int diff = existingDiffs.get(i);
                            int gb = diff / v;
                            int gx = diff % v;
                            FixBS preImg = gSpace.preImages[gb][b].intersection(gSpace.preImages[gx][x]);
                            stabExt.or(preImg);
                        }
                    }
                    existingDiffs.add(b * v + x);

                    existingDiffs = newDiffs[compXb];
                    if (existingDiffs == null) {
                        existingDiffs = (newDiffs[compXb] = new IntList(k));
                    } else {
                        for (int i = 0; i < existingDiffs.size(); i++) {
                            int diff = existingDiffs.get(i);
                            int gb = diff % v;
                            int gx = diff / v;
                            FixBS preImg = gSpace.preImages[gb][b].intersection(gSpace.preImages[gx][x]);
                            stabExt.or(preImg);
                        }
                    }
                    existingDiffs.add(x * v + b);
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

        private FixBS updatedFilter(FixBS oldFilter) {
            FixBS result = oldFilter.copy();
            for (int i = 0; i < diffs.length; i++) {
                if (diffs[i] != null) {
                    result.set(i);
                }
            }
            return result;
        }
    }

    @Test
    public void testApplicator() {
        Group gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        SubGroup tr = new SubGroup(gr, FixBS.of(gr.order(), 0));
        SubGroup small = new SubGroup(gr, FixBS.of(gr.order(), 0, 1, 2));
        FixBS f = new FixBS(gr.order());
        f.set(0, gr.order());
        SubGroup fix = new SubGroup(gr, f);
        GSpace applicator = new GSpace(6, tr, small, small, fix);
        assertEquals(66, applicator.v);
        for (int x = 0; x < applicator.v; x++) {
            for (int j = 0; j < gr.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
        gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        applicator = new GSpace(6, new SubGroup(gr, FixBS.of(gr.order(), 0)), new SubGroup(gr, FixBS.of(gr.order(), 0)));
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
            new GSpace(6, new SubGroup(new CyclicProduct(2, 2), FixBS.of(4, 0, 1)));
            fail();
        } catch (Exception e) {
            // ok
        }
        Group g = new CyclicGroup(21);
        GSpace space = new GSpace(7, new SubGroup(g, FixBS.of(g.order(), 0)));
        FixBS emptyFilter = new FixBS(space.differences.size());
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
        space = new GSpace(6, new SubGroup(g, FixBS.of(g.order(), 0)));
        state = space.statesCache[0][1];
        emptyFilter = new FixBS(space.differences.size());
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 2));
        assertEquals(FixBS.of(space.v, 0, 1, 2), state.block);
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer);
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 3));
        assertEquals(FixBS.of(space.v, 0, 1, 2, 3, 31, 80), state.block);
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer);
    }

    @Test
    public void logDesigns() {
        int k = 3;
        Group group = new CyclicProduct(3, 3);
        GSpace space = new GSpace(k, new SubGroup(group.asTable(), FixBS.of(group.order(), 0)));
        int[][] auths = group.auth();
        System.out.println(group.name() + " " + space.v + " " + k + " auths: " + auths.length);
        int diffs = space.differences.size();
        FixBS filter = new FixBS(diffs);
        State[] design = new State[0];
        BiPredicate<State[], FixBS> cons = (arr, ftr) -> {
            if (ftr.cardinality() < diffs) {
                return false;
            }
            Liner l = new Liner(space.v, Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        int val = 1;
        State state = space.statesCache[0][val];
        searchDesigns(space, filter, design, state, val, cons);
    }

    private static void searchDesigns(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        if (state.size() == space.k) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = state.updatedFilter(filter);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int pair = space.differences.get(nextFilter.nextClearBit(0)).nextSetBit(0);
            int snd = pair % space.v;
            State nextState = space.statesCache[pair / space.v][snd];
            searchDesigns(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            for (int el = prev + 1; el < space.v; el++) {
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }
}
