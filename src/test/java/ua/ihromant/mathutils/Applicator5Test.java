package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class Applicator5Test {
    @Test
    public void testEven() throws IOException {
        int k = 6;
        boolean infinity = true;
        Group group = new CyclicGroup(55);
        int[] comps = infinity ? new int[]{1, 1, group.order()} : new int[]{1, 1};
        GSpace sp = new GSpace(k, group, false, comps);
        int v = sp.v();
        int sqr = v * v;
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
        System.out.println(group.name() + " " + sp.v() + " " + k + " auths: " + sp.authLength() + " diffs: " + removableDiffs.cardinality());
        State[] base = getRemovable(removableDiffs, sp, v, group);
        System.out.println("Base blocks " + base.length);
        List<State[]> begins = Collections.synchronizedList(new ArrayList<>());
        FixBS[] intersecting = intersecting(base);
        IntStream.range(0, base.length).parallel().forEach(idx -> {
            State st = base[idx];
            find(base, intersecting, Des.of(sp.diffLength(), base.length, st, intersecting[idx], idx), des -> {
                if (!removableDiffs.diff(des.diffSet).isEmpty()) {
                    return false;
                }
                Arrays.sort(des.curr(), Comparator.comparing(State::block));
                begins.add(des.curr());
                return false;
            });
        });
        System.out.println("Initial configs " + begins.size());
        AtomicInteger cnt = new AtomicInteger();
        BiPredicate<State[], FixBS> tCons = (arr, ftr) -> {
            if (!ftr.isFull(sqr)) {
                return false;
            }
            Liner l = new Liner(v, Arrays.stream(arr).flatMap(st -> sp.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        begins.stream().parallel().forEach(tuple -> {
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = sp.emptyFilter().copy();
            for (State st : pr) {
                st.updateFilter(newFilter, sp);
            }
            //searchDesigns(sp, newFilter, pr, tuple[tuple.length - 1], 0, tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
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
            State st = states[i];
            find(states, intersecting, des.accept(st, intersecting[i], i), pr);
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

    private List<List<int[]>> find(State[] basicConf, GSpace sp) {
        int gOrd = sp.gOrd();
        int v = sp.v();
        int orbitCount = v / gOrd;
        List<List<int[]>> res = new ArrayList<>();
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
        int[][] splits = generateSplits(orbitCount, sp.k());
        find(gOrd, orbitCount, used, new ArrayList<>(), splits, 0, res::add);
        return res;
    }

    private int[][] generateSplits(int orbitCount, int k) {
        List<int[]> res = new ArrayList<>();
        generateSplits(k, new int[orbitCount], 0, 0, res::add);
        return res.toArray(int[][]::new);
    }

    private void generateSplits(int k, int[] curr, int idx, int sum, Consumer<int[]> cons) {
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

    private void find(int orbitSize, int orbitCount, int[][] used, List<int[]> lst, int[][] splits, int idx, Consumer<List<int[]>> cons) {
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
}
