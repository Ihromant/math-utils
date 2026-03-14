package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.NSState;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Applicator7Test {
    @Test
    public void generateAll() throws IOException {
        int gs = 39;
        int k = 6;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            GSpace space;
            try {
                space = new GSpace(k, group, false, 39, 3, 3, 1);
            } catch (IllegalArgumentException e) {
                System.out.println("Not empty");
                continue;
            }
            int v = space.v();
            FixBS evenDiffs = space.evenDiffs();
            State[] stab = Applicator6Test.getStabilizedAlt(space, null);
            System.out.println(stab.length);
            Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
            BiConsumer<State[], NSState[]> fCons = (sts, nst) -> {
                Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                        Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State::block).toList()
                        + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
            };
            if (stab.length == 0) {
                continue;
            }
            List<List<State>> init = new ArrayList<>();
            JNauty.instance().cliques(g, 1, v, a -> {
                FixBS arr = new FixBS(a);
                List<State> states = new ArrayList<>();
                FixBS diffSet = new FixBS(space.diffLength());
                for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                    State st = stab[i];
                    states.add(st);
                    diffSet.or(st.diffSet());
                }
                int card = diffSet.cardinality();
                if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                    return;
                }
                if (card == space.diffLength()) {
                    System.out.println(states.stream().map(State::block).toList());
                    return;
                }
                init.add(states);
            });
            System.out.println("Init " + init.size());
            AtomicInteger ai = new AtomicInteger();
            init.parallelStream().forEach(states -> {
                int dc = space.diffLength();
                FixBS whiteList = space.emptyFilter().copy();
                FixBS diffSet = new FixBS(space.diffLength());
                for (State st : states) {
                    st.updateFilter(whiteList, space);
                    diffSet.or(st.diffSet());
                    dc = dc - st.diffSet().cardinality();
                }
                whiteList.flip(0, v * v);
                int nc = dc / k / (k - 1);
                if (nc == 0) {
                    fCons.accept(states.toArray(State[]::new), new NSState[0]);
                    return;
                }
                int pr = whiteList.nextSetBit(0);
                int fst = pr / v;
                int snd = pr % v;
                NSState in = new NSState(new int[]{fst}, diffSet, whiteList).acceptElem(space, snd);
                searchDesigns(space, new NSState[]{in}, nst -> {
                    if (nst.length < nc) {
                        return false;
                    }
                    fCons.accept(states.toArray(State[]::new), nst);
                    return true;
                });
                int val = ai.incrementAndGet();
                if (val % 1000 == 0) {
                    System.out.println(val);
                }
            });
        }
    }

    @Test
    public void generateByOne() throws IOException {
        int gs = 147;
        int k = 4;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            GSpace space;
            try {
                space = new GSpace(k, group, false, 3);
            } catch (IllegalArgumentException e) {
                System.out.println("Not empty");
                continue;
            }
            int v = space.v();
            FixBS evenDiffs = space.evenDiffs();
            State[] stab = Applicator6Test.getStabilizedAlt(space, null);
            System.out.println(stab.length);
            Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
            BiConsumer<State[], NSState[]> fCons = (sts, nst) -> {
                Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                        Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State::block).toList()
                        + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
            };
            if (stab.length == 0) {
                continue;
            }
            JNauty.instance().cliques(g, 1, space.v(), a -> {
                FixBS arr = new FixBS(a);
                List<State> states = new ArrayList<>();
                FixBS diffSet = new FixBS(space.diffLength());
                FixBS whiteList = space.emptyFilter().copy();
                for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                    State st = stab[i];
                    states.add(st);
                    diffSet.or(st.diffSet());
                    st.updateFilter(whiteList, space);
                }
                int card = diffSet.cardinality();
                if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                    return;
                }
                int nc = (space.diffLength() - card) / k / (k - 1);
                if (nc == 0) {
                    fCons.accept(states.toArray(State[]::new), new NSState[0]);
                    return;
                }
                whiteList.flip(0, v * v);
                int pr = whiteList.nextSetBit(0);
                int fst = pr / v;
                int snd = pr % v;
                NSState in = new NSState(new int[]{fst}, diffSet, whiteList).acceptElem(space, snd);
                searchDesigns(space, new NSState[]{in}, nst -> {
                    if (nst.length < nc) {
                        return false;
                    }
                    fCons.accept(states.toArray(State[]::new), nst);
                    return true;
                });
                System.out.println("Done");
            });
        }
    }

    private static void searchDesigns(GSpace space, NSState[] currDesign, Predicate<NSState[]> cons) {
        int v = space.v();
        int li = currDesign.length - 1;
        NSState last = currDesign[li];
        int bl = last.block().length;
        if (bl == space.k()) {
            if (cons.test(currDesign)) {
                return;
            }
            NSState[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            int pair = last.whiteList().nextSetBit(0);
            int fst = pair / v;
            int snd = pair % v;
            NSState st = new NSState(new int[]{fst}, last.diffSet(), last.whiteList()).acceptElem(space, snd);
            nextDesign[currDesign.length] = st;
            searchDesigns(space, nextDesign, cons);
        } else {
            int prev = last.block()[bl - 1];
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = last.whiteList().nextSetBit(from); pair >= 0 && pair < to; pair = last.whiteList().nextSetBit(pair + 1)) {
                int el = pair % v;
                NSState nextState = last.acceptElem(space, el);
                if (nextState != null) {
                    currDesign[li] = nextState;
                    searchDesigns(space, currDesign, cons);
                }
            }
        }
    }

    @Test
    public void testTrivial() throws IOException {
        Group group = GroupIndex.group(39, 1);
        int k = 7;
        GSpace space = new GSpace(k, group, false, 3, 1, 1);
        int v = space.v();
        FixBS evenDiffs = space.evenDiffs();
        if (!evenDiffs.isEmpty()) {
            throw new IllegalStateException();
        }
        Consumer<NSState[]> fCons = nst -> {
            Liner l = new Liner(space.v(), Arrays.stream(nst).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
        };
        if (space.diffLength() % (k * k - k) != 0) {
            throw new IllegalStateException();
        }
        int nc = space.diffLength() / k / (k - 1);
        FixBS whiteList = space.emptyFilter().copy();
        whiteList.flip(0, v * v);
        int pr = whiteList.nextSetBit(0);
        int fst = pr / v;
        int snd = pr % v;
        NSState in = new NSState(new int[]{fst}, new FixBS(space.diffLength()), whiteList).acceptElem(space, snd);
        searchDesigns(space, new NSState[]{in}, nst -> {
            if (nst.length < nc) {
                return false;
            }
            fCons.accept(nst);
            return true;
        });
    }

    @Test
    public void trivialByGraph() throws IOException {
        Group group = GroupIndex.group(39, 1);
        int k = 6;
        GSpace space = new GSpace(k, group, false, 3, 1, 1);
        int v = space.v();
        FixBS evenDiffs = space.evenDiffs();
        if (!evenDiffs.isEmpty()) {
            throw new IllegalStateException();
        }
        Map<FixBS, NSState> states = new HashMap<>();
        Predicate<NSState[]> cons = (nst) -> {
            states.putIfAbsent(nst[0].diffSet(), nst[0]);
            return true;
        };
        for (int fst : space.oBeg()) {
            FixBS whiteList = space.emptyFilter().copy();
            whiteList.flip(0, v * v);
            NSState nst = new NSState(new int[]{fst}, new FixBS(space.diffLength()), whiteList);
            searchDesigns(space, new NSState[]{nst}, cons);
        }
        System.out.println(states.size());
    }
}
