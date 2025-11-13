package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpaceQuad;
import ua.ihromant.mathutils.g.StateQuad;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class QuadTest {
    @Test
    public void logDesigns() throws IOException {
        int k = 7;
        Group group = new CyclicGroup(23);
        GSpaceQuad space = new GSpaceQuad(k, group, true, 1);
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        int biQuad = v * v * v * v;
        List<StateQuad> singles = Collections.synchronizedList(new ArrayList<>());
        StateQuad state = space.forInitial(0, 1, 2, 3);
        searchDesignsFirst(space, state, 3, singles::add);
        System.out.println("Singles size: " + singles.size());
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<StateQuad[], FixBS> fCons = (arr, ftr) -> {
            if (!ftr.isFull(biQuad)) {
                return false;
            }
            ai.incrementAndGet();
            System.out.println(Arrays.stream(arr).map(StateQuad::block).toList());
            return true;
        };
        singles.stream().parallel().forEach(st -> {
            searchDesigns(space, space.emptyFilter(), new StateQuad[0], st, 0, 0, 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    @Test
    public void twoStageMul1() throws IOException {
        int k = 7;
        int gs = 23;
        int mt = 1;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            Map<Integer, List<SubGroup>> subs = group.groupedSubGroups();
            for (int t = 0; t < subs.getOrDefault(mt, List.of()).size(); t++) {
                //for (int u = 0; u < subs.getOrDefault(48, List.of()).size(); u++) {
                GSpaceQuad space;
                try {
                    space = new GSpaceQuad(k, group, true, new int[][]{{mt, t}});
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
                int biQuad = v * v * v * v;
                List<StateQuad> singles = Collections.synchronizedList(new ArrayList<>());
                StateQuad state = space.forInitial(0, 1, 2, 3);
                if (state == null) {
                    continue;
                }
                searchDesignsFirst(space, state, 3, singles::add);
                System.out.println("Singles size: " + singles.size());
                List<StateQuad[]> tuples = new ArrayList<>();
                List<StateQuad[]> sync = Collections.synchronizedList(tuples);
                BiPredicate<StateQuad[], FixBS> sCons = (arr, _) -> {
                    if (!space.parMinimal(arr)) {
                        return true;
                    }
                    if (arr.length < 8) {
                        return false;
                    }
                    sync.add(arr);
                    return true;
                };
                AtomicInteger cnt = new AtomicInteger();
                singles.stream().parallel().forEach(st -> {
                    searchDesigns(space, space.emptyFilter(), new StateQuad[0], st, 0, 0, 0, sCons);
                    int vl = cnt.incrementAndGet();
                    if (vl % 10 == 0) {
                        System.out.println(vl);
                    }
                });
                System.out.println("Tuples " + tuples.size());
                AtomicInteger ai = new AtomicInteger();
                cnt.set(0);
                BiPredicate<StateQuad[], FixBS> tCons = (arr, ftr) -> {
                    if (!ftr.isFull(biQuad)) {
                        return false;
                    }
                    if (!space.parMinimal(arr)) {
                        return true;
                    }
                    ai.incrementAndGet();
                    System.out.println(Arrays.stream(arr).map(StateQuad::block).toList());
                    return true;
                };
                tuples.stream().parallel().forEach(tuple -> {
                    StateQuad[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                    FixBS newFilter = space.emptyFilter().copy();
                    for (StateQuad st : pr) {
                        st.updateFilter(newFilter, space);
                    }
                    searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, 0, 0, tCons);
                    int vl = cnt.incrementAndGet();
                    if (vl % 100 == 0) {
                        System.out.println(vl);
                    }
                });
                System.out.println("Results " + ai);
                //}
            }
        }
    }

    @Test
    public void tractor() throws IOException {
        int k = 7;
        int v = 23;
        int orbits = 1;
        int[][] splits = splits(v, orbits);
        generateConfigs(splits, (group, conf) -> {
            GSpaceQuad space;
            try {
                space = new GSpaceQuad(k, group, true, conf);
            } catch (IllegalArgumentException e) {
                System.out.println("Not empty");
                return;
            }
            System.out.println(group.name() + " " + space.v() + " " + k + " conf: " + Arrays.deepToString(conf) + " auths: " + space.authLength());
            int biQuad = v * v * v * v;
            List<StateQuad> singles = Collections.synchronizedList(new ArrayList<>());
            StateQuad state = space.forInitial(0, 1, 2, 3);
            if (state == null) {
                return;
            }
            searchDesignsFirst(space, state, 3, singles::add);
            System.out.println("Singles size: " + singles.size());
            List<StateQuad[]> tuples = new ArrayList<>();
            List<StateQuad[]> sync = Collections.synchronizedList(tuples);
            BiPredicate<StateQuad[], FixBS> sCons = (arr, _) -> {
                if (!space.parMinimal(arr)) {
                    return true;
                }
                if (arr.length < 5) {
                    return false;
                }
                sync.add(arr);
                return true;
            };
            AtomicInteger cnt = new AtomicInteger();
            singles.stream().parallel().forEach(st -> {
                searchDesigns(space, space.emptyFilter(), new StateQuad[0], st, 0, 0, 0, sCons);
                int vl = cnt.incrementAndGet();
                if (vl % 10 == 0) {
                    System.out.println(vl);
                }
            });
            System.out.println("Tuples " + tuples.size());
            AtomicInteger ai = new AtomicInteger();
            cnt.set(0);
            BiPredicate<StateQuad[], FixBS> tCons = (arr, ftr) -> {
                if (!ftr.isFull(biQuad)) {
                    return false;
                }
                if (!space.parMinimal(arr)) {
                    return true;
                }
                ai.incrementAndGet();
                System.out.println(Arrays.stream(arr).map(StateQuad::block).toList());
                return true;
            };
            tuples.stream().parallel().forEach(tuple -> {
                StateQuad[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                FixBS newFilter = space.emptyFilter().copy();
                for (StateQuad st : pr) {
                    st.updateFilter(newFilter, space);
                }
                searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, 0, 0, tCons);
                int vl = cnt.incrementAndGet();
                if (vl % 100 == 0) {
                    System.out.println(vl);
                }
            });
            System.out.println("Results " + ai);
        });
    }

    private static void generateConfigs(int[][] splits, BiConsumer<Group, int[][]> cons) throws IOException {
        for (int[] split : splits) {
            int lcm = lcm(split);
            if (lcm % 16 == 0) {
                System.out.println("Skipping " + Arrays.toString(split) + " as it will have too big group");
                continue;
            }
            int gc = GroupIndex.groupCount(lcm);
            for (int i = 1; i <= gc; i++) {
                Group gr = GroupIndex.group(lcm, i);
                recur(gr, split, new int[split.length][], 0, cons);
            }
        }
    }

    private static void recur(Group gr, int[] split, int[][] curr, int idx, BiConsumer<Group, int[][]> cons) {
        if (idx == split.length) {
            cons.accept(gr, curr);
            return;
        }
        int ol = split[idx];
        int gs = gr.order() / ol;
        List<SubGroup> sgs = gr.groupedSubGroups().getOrDefault(gs, List.of());
        for (int i = 0; i < sgs.size(); i++) {
            int[][] nextCurr = curr.clone();
            nextCurr[idx] = new int[]{gs, i};
            recur(gr, split, nextCurr, idx + 1, cons);
        }
    }

    private static int lcm(int[] from) {
        return Arrays.stream(from).reduce(1, (a, b) -> a * b / Combinatorics.gcd(a, b));
    }

    private static int[][] splits(int from, int parts) {
        List<int[]> result = new ArrayList<>();
        int[] base = new int[parts];
        recur(base, from, 0, result::add);
        return result.toArray(int[][]::new);
    }

    private static void recur(int[] base, int left, int idx, Consumer<int[]> cons) {
        int lstIdx = idx - 1;
        int from = lstIdx < 0 ? 1 : base[lstIdx];
        if (left < from) {
            return;
        }
        if (idx == base.length - 1) {
            int[] res = base.clone();
            res[idx] = left;
            cons.accept(res);
            return;
        }
        for (int i = from; i < left; i++) {
            int[] next = base.clone();
            next[idx] = i;
            recur(next, left - i, idx + 1, cons);
        }
    }

    private static void searchDesigns(GSpaceQuad space, FixBS filter, StateQuad[] currDesign, StateQuad state, int fst, int snd, int last, BiPredicate<StateQuad[], FixBS> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            StateQuad[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int triple = nextFilter.nextClearBit(0);
            int nextFst = triple / v / v / v;
            int nextSnd = triple / v / v % v;
            int nextLast = triple % v;
            StateQuad nextState = space.forInitial(nextFst, nextSnd, triple / v % v, nextLast);
            searchDesigns(space, nextFilter, nextDesign, nextState, nextFst, nextSnd, nextLast, cons);
        } else {
            int bs = fst * v * v * v + snd * v * v + last * v;
            int from = bs + last + 1;
            int to = bs + v;
            for (int tr = filter.nextClearBit(from); tr >= 0 && tr < to; tr = filter.nextClearBit(tr + 1)) {
                int el = tr % v;
                StateQuad nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, fst, snd, el, cons);
                }
            }
        }
    }

    private static void searchDesignsFirst(GSpaceQuad space, StateQuad state, int prev, Consumer<StateQuad> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                StateQuad nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, nextState, el, cons);
                }
            }
        }
    }
}
