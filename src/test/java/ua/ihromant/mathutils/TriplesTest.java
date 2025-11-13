package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpaceTr;
import ua.ihromant.mathutils.g.StateTr;
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

public class TriplesTest {
    @Test
    public void logDesigns() throws IOException {
        int k = 5;
        Group group = GroupIndex.group(26, 1);
        GSpaceTr space = new GSpaceTr(k, group, true, 1);
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        int cube = v * v * v;
        List<StateTr> singles = Collections.synchronizedList(new ArrayList<>());
        StateTr state = space.forInitial(0, 1, 2);
        searchDesignsFirst(space, state, 2, singles::add);
        System.out.println("Singles size: " + singles.size());
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<StateTr[], FixBS> fCons = (arr, ftr) -> {
            if (!ftr.isFull(cube)) {
                return false;
            }
            ai.incrementAndGet();
            InversivePlane pl = new InversivePlane(Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(pl.fingerprint() + " " + Arrays.stream(arr).map(StateTr::block).toList());
            return true;
        };
        singles.stream().parallel().forEach(st -> {
            searchDesigns(space, space.emptyFilter(), new StateTr[0], st, 0, 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    @Test
    public void twoStageMul() throws IOException {
        int k = 5;
        int gs = 26;
        int mt = 1;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            Map<Integer, List<SubGroup>> subs = group.groupedSubGroups();
            for (int t = 0; t < subs.getOrDefault(mt, List.of()).size(); t++) {
                //for (int u = 0; u < subs.getOrDefault(48, List.of()).size(); u++) {
                GSpaceTr space;
                try {
                    space = new GSpaceTr(k, group, true, new int[][]{{mt, t}});
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
                int cube = v * v * v;
                List<StateTr> singles = Collections.synchronizedList(new ArrayList<>());
                StateTr state = space.forInitial(0, 1, 2);
                if (state == null) {
                    continue;
                }
                searchDesignsFirst(space, state, 2, singles::add);
                System.out.println("Singles size: " + singles.size());
                List<StateTr[]> tuples = new ArrayList<>();
                List<StateTr[]> sync = Collections.synchronizedList(tuples);
                BiPredicate<StateTr[], FixBS> sCons = (arr, _) -> {
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
                    searchDesigns(space, space.emptyFilter(), new StateTr[0], st, 0, 0, sCons);
                    int vl = cnt.incrementAndGet();
                    if (vl % 10 == 0) {
                        System.out.println(vl);
                    }
                });
                System.out.println("Tuples " + tuples.size());
                AtomicInteger ai = new AtomicInteger();
                cnt.set(0);
                BiPredicate<StateTr[], FixBS> tCons = (arr, ftr) -> {
                    if (!ftr.isFull(cube)) {
                        return false;
                    }
                    if (!space.parMinimal(arr)) {
                        return true;
                    }
                    ai.incrementAndGet();
                    InversivePlane pl = new InversivePlane(Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                    System.out.println(pl.fingerprint() + " " + Arrays.stream(arr).map(StateTr::block).toList());
                    return true;
                };
                tuples.stream().parallel().forEach(tuple -> {
                    StateTr[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                    FixBS newFilter = space.emptyFilter().copy();
                    for (StateTr st : pr) {
                        st.updateFilter(newFilter, space);
                    }
                    searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, 0, tCons);
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
        int k = 4;
        int v = 16;
        int orbits = 2;
        int[][] splits = splits(v, orbits);
        generateConfigs(splits, (group, conf) -> {
            GSpaceTr space;
            try {
                space = new GSpaceTr(k, group, true, conf);
            } catch (IllegalArgumentException e) {
                System.out.println("Not empty");
                return;
            }
            System.out.println(group.name() + " " + space.v() + " " + k + " conf: " + Arrays.deepToString(conf) + " auths: " + space.authLength());
            int cube = v * v * v;
            List<StateTr> singles = Collections.synchronizedList(new ArrayList<>());
            StateTr state = space.forInitial(0, 1, 2);
            if (state == null) {
                return;
            }
            searchDesignsFirst(space, state, 2, singles::add);
            System.out.println("Singles size: " + singles.size());
            List<StateTr[]> tuples = new ArrayList<>();
            List<StateTr[]> sync = Collections.synchronizedList(tuples);
            BiPredicate<StateTr[], FixBS> sCons = (arr, _) -> {
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
                searchDesigns(space, space.emptyFilter(), new StateTr[0], st, 0, 0, sCons);
                int vl = cnt.incrementAndGet();
                if (vl % 10 == 0) {
                    System.out.println(vl);
                }
            });
            System.out.println("Tuples " + tuples.size());
            AtomicInteger ai = new AtomicInteger();
            cnt.set(0);
            BiPredicate<StateTr[], FixBS> tCons = (arr, ftr) -> {
                if (!ftr.isFull(cube)) {
                    return false;
                }
                if (!space.parMinimal(arr)) {
                    return true;
                }
                ai.incrementAndGet();
                InversivePlane pl = new InversivePlane(Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                System.out.println(pl.fingerprint() + " " + Arrays.stream(arr).map(StateTr::block).toList());
                return true;
            };
            tuples.stream().parallel().forEach(tuple -> {
                StateTr[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                FixBS newFilter = space.emptyFilter().copy();
                for (StateTr st : pr) {
                    st.updateFilter(newFilter, space);
                }
                searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, 0, tCons);
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

    private static void searchDesigns(GSpaceTr space, FixBS filter, StateTr[] currDesign, StateTr state, int fst, int prev, BiPredicate<StateTr[], FixBS> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            StateTr[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int triple = nextFilter.nextClearBit(0);
            int nextFst = triple / v / v;
            int trd = triple % v;
            StateTr nextState = space.forInitial(nextFst, triple / v % v, trd);
            searchDesigns(space, nextFilter, nextDesign, nextState, nextFst, trd, cons);
        } else {
            int bs = fst * v * v + prev * v;
            int from = bs + prev + 1;
            int to = bs + v;
            for (int tr = filter.nextClearBit(from); tr >= 0 && tr < to; tr = filter.nextClearBit(tr + 1)) {
                int el = tr % v;
                StateTr nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, fst, el, cons);
                }
            }
        }
    }

    private static void searchDesignsFirst(GSpaceTr space, StateTr state, int prev, Consumer<StateTr> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                StateTr nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, nextState, el, cons);
                }
            }
        }
    }
}
