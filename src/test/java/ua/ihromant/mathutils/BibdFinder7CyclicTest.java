package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class BibdFinder7CyclicTest {
    @Test
    public void testShifts() {
        Group g = new SemiDirectProduct(new CyclicProduct(11), new CyclicGroup(5));
        Map<Integer, List<SubGroup>> sgs = g.groupedSubGroups();
        SubGroup sg = sgs.get(5).getFirst();
        System.out.println(sg.elems());
        int gOrd = g.order();
        for (int b = 0; b < gOrd; b++) {
            FixBS left = new FixBS(gOrd);
            FixBS right = new FixBS(gOrd);
            for (int h = sg.elems().nextSetBit(0); h >= 0; h = sg.elems().nextSetBit(h + 1)) {
                left.set(g.op(b, h));
                right.set(g.op(h, b));
            }
            System.out.println(b + " " + left + " " + right);
        }
    }

    @Test
    public void tst() throws IOException {
        int v = 13;
        int k = 3;
        int mul = 3;
        CyclicGroup h = new CyclicGroup(mul);
        Group x = new CyclicGroup(v);
        SemiDirectProduct g = new SemiDirectProduct(x, h);
        generate(h, g, v, k);
        //generate(new CyclicGroup(1), new CyclicGroup(19), v, k);
    }

    private static int projection(Group h, Group g, int el) {
        return el / h.order();
    }

    private static void generate(CyclicGroup h, Group g, int v, int k) throws IOException {
        FixBS hInG = new FixBS(g.order());
        hInG.set(0, h.order());
        System.out.println(g.name() + " " + v + " " + k);
        FixBS filterX = new FixBS(projection(h, g, g.order()));
        State[] design = new State[0];
        List<State> states = new ArrayList<>();
        BiPredicate<State[], Integer> cons = (arr, from) -> {
            State st = arr[0];
            st.filterInG.clear(0);
            states.add(st);
            return true;
        };
        FixBS zeroX = FixBS.of(v, 0);
        FixBS zeroG = FixBS.of(g.order(), 0);
        int val = 1;
        State state = Objects.requireNonNull(new State(zeroX, hInG, zeroG, new FixBS(v), zeroG, 1).acceptElem(h, g, filterX, val, v, k));
        searchDesigns(h, g, filterX, design, state, v, k, val, cons);
        System.out.println("Initial size " + states.size());
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State[], Integer> fCons = (arr, from) -> {
            if (from < v) {
                return false;
            }
            int[][] base = Arrays.stream(arr).map(st -> st.blockInX.toArray()).toArray(int[][]::new);
            //int[][] ars = Arrays.stream(base).flatMap(bl -> blocks(bl, v, x)).toArray(int[][]::new);
            //Liner l = new Liner(v, ars);
            //liners.add(l);
            System.out.println(/*l.autCountOld() + " " + l.hyperbolicFreq() + " " + */Arrays.deepToString(base));
            return true;
        };
        states.stream()/*.parallel()*/.forEach(st -> {
            State[] des = new State[]{st};
            int from = st.filterInG.nextClearBit(1);
            State init = new State(zeroX, hInG, zeroG, new FixBS(v), zeroG, 1).acceptElem(h, g, filterX, from, v, k);
            if (init == null) {
                return;
            }
            searchDesigns(h, g, st.filterInG, des, init, v, k, from, fCons);
            int cnt = ai.incrementAndGet();
            if (cnt % 100 == 0) {
                System.out.println(cnt);
            }
        });
        System.out.println("Results: " + liners.size());
    }

    private static void searchDesigns(CyclicGroup h, Group g, FixBS filter, State[] currDesign, State state, int v, int k, int prev, BiPredicate<State[], Integer> cons) {
        if (state.size() == k) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = filter.copy();
            for (int bDiff = state.filterInG.nextSetBit(h.order()); bDiff >= 0; bDiff = state.filterInG.nextSetBit(bDiff + 1)) {
                nextFilter.set(projection(h, g, bDiff));
            }
            int val = nextFilter.nextClearBit(1);
            if (cons.test(nextDesign, val)) {
                return;
            }
            FixBS zeroX = FixBS.of(v, 0);
            FixBS zeroG = FixBS.of(g.order(), 0);
            FixBS hInG = new FixBS(g.order());
            hInG.set(0, h.order());
            State nextState = Objects.requireNonNull(new State(zeroX, hInG, zeroG, zeroX, zeroG, 1).acceptElem(h, g, filter, val, v, k));
            searchDesigns(h, g, nextFilter, nextDesign, nextState, v, k, val, cons);
        } else {
            for (int el = filter.nextClearBit(prev + 1); el >= 0 && el < v; el = filter.nextClearBit(el + 1)) {
                if (state.blockInX.get(el)) {
                    continue;
                }
                State nextState = state.acceptElem(h, g, filter, el, v, k);
                if (nextState != null) {
                    searchDesigns(h, g, filter, currDesign, nextState, v, k, el, cons);
                }
            }
        }
    }

    public static Stream<int[]> blocks(int[] block, int v, Group gr) {
        int ord = gr.order();
        Set<FixBS> set = new HashSet<>(ord);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(i, el));
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    private static boolean bigger(int[][] fst, int[][] snd) {
        int cmp = 0;
        for (int i = 0; i < fst.length; i++) {
            cmp = Combinatorics.compareArr(snd[i], fst[i]);
            if (cmp != 0) {
                break;
            }
        }
        return cmp < 0;
    }

    private static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
        int v = gr.order() + 1;
        FixBS base = new FixBS(v);
        for (int val : tuple) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(0); val >= 0 && val < gr.order(); val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(v);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(oVal == gr.order() ? oVal : gr.op(inv, oVal));
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min.toArray();
    }

    private record State(FixBS blockInX, FixBS blockInG, FixBS stabilizer, FixBS filterInG, FixBS selfDiff, int size) {
        private State acceptElem(CyclicGroup h, Group g, FixBS filterX, int val, int v, int k) {
            FixBS newBlock = blockInX.copy();
            FixBS newBlockInG = blockInG.copy();
            FixBS queue = new FixBS(v);
            int hOrd = h.order();
            queue.set(val);
            int sz = size;
            FixBS newSelfDiff = selfDiff.copy();
            FixBS newStabilizer = stabilizer.copy();
            FixBS newFilter = filterInG.copy();
//            if (val == group.order()) {
//                newFilter.set(val);
//                newBlock.set(val);
//                return new State(newBlock, newStabilizer, newFilter, newSelfDiff, sz + 1);
//            }
            while (!queue.isEmpty()) {
                if (++sz > k) {
                    return null;
                }
                int x = queue.nextSetBit(0);
                int xTimesE = x * h.order();
                FixBS xInG = new FixBS(g.order());
                int xInGInv = g.inv(xTimesE);
                xInG.set(xTimesE, xTimesE + h.order());
                if (x < val) {
                    return null;
                }
                FixBS stabExt = new FixBS(g.order());
                FixBS selfDiffExt = new FixBS(g.order());

                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    int bTimesE = b * h.order();
                    int bInGInv = g.inv(bTimesE);
                    FixBS xBInvInG = new FixBS(g.order()); // xHb^-1
                    for (int hEl = 0; hEl < hOrd; hEl++) {
                        xBInvInG.set(g.op(g.op(xTimesE, hEl), bInGInv));
                    }
                    selfDiffExt.or(xBInvInG);
                    if (newSelfDiff.intersects(xBInvInG)) {
                        stabExt.or(xBInvInG);
                    }
                    for (int xHBInv = xBInvInG.nextSetBit(0); xHBInv >= 0; xHBInv = xBInvInG.nextSetBit(xHBInv + 1)) {
                        if (blockInG.get(g.op(xHBInv, xTimesE))) {
                            for (int b1 = newBlock.nextSetBit(0); b1 >= 0; b1 = newBlock.nextSetBit(b1 + 1)) {
                                for (int h1 = 0; h1 < hOrd; h1++) {
                                    stabExt.set(g.op(xTimesE + h1, g.inv(b1 * h.order())));
                                }
                            }
                        }
                    }
                    FixBS bxInvInG = new FixBS(g.order()); // bHx^-1
                    for (int hEl = 0; hEl < hOrd; hEl++) {
                        bxInvInG.set(g.op(g.op(bTimesE, hEl), xInGInv));
                    }
                    selfDiffExt.or(bxInvInG);
                    if (newSelfDiff.intersects(bxInvInG)) {
                        stabExt.or(bxInvInG);
                    }
                    for (int hEl = 0; hEl < hOrd; hEl++) {
                        int nEl = g.op(g.op(hEl, g.inv(bTimesE)), xTimesE);
                        if (filterX.get(projection(h, g, nEl))) {
                            return null;
                        }
                        newFilter.set(nEl);
                    }
                    for (int hEl = 0; hEl < hOrd; hEl++) {
                        int nEl = g.op(g.op(hEl, g.inv(xTimesE)), bTimesE);
                        if (filterX.get(projection(h, g, nEl))) {
                            return null;
                        }
                        newFilter.set(nEl);
                    }
                }
                newBlock.set(x);
                newBlockInG.or(xInG);
                stabExt.andNot(newStabilizer);
                for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                    queue.set(projection(h, g, g.op(st, xTimesE)));
                }
                for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                    for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                        queue.set(projection(h, g, g.op(st, b * h.order())));
                    }
                }
                newStabilizer.or(stabExt);
                newSelfDiff.or(selfDiffExt);
                queue.andNot(newBlock);
            }
            return new State(newBlock, newBlockInG, newStabilizer, newFilter, newSelfDiff, sz);
        }
    }
}
