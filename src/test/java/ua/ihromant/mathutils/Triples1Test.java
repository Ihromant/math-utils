package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.g.GSpaceTr;
import ua.ihromant.mathutils.g.StateTr;
import ua.ihromant.mathutils.group.GapGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Triples1Test {
    @Test
    public void tst() throws IOException {
        int gs = 72;
        int k = 6;
        int[] orbits = new int[]{18, 24};
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            System.out.println("Reading SmallGroup(" + gs + "," + j + ")");
            GapGroup gg = GroupIndex.gapGroup(gs, j);
            Group group = gg.group();
            List<int[][]> configs = ApplicatorTest.configs(group, orbits);
            for (int[][] config : configs) {
                GSpaceTr space;
                try {
                    space = new GSpaceTr(k, group, false, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                StateTr[] stab = space.getStabilized();
                FixBS notExcluded = evenDiffs.copy();
                int evenCard = evenDiffs.cardinality();
                int sum = 0;
                PriorityQueue<Integer> queue = new PriorityQueue<>();
                for (StateTr st : stab) {
                    int intCard = evenDiffs.intersection(st.diffSet()).cardinality();
                    sum = sum + intCard;
                    queue.offer(intCard);
                    notExcluded.andNot(st.diffSet());
                    while (!queue.isEmpty() && sum - queue.peek() >= evenCard) {
                        sum = sum - queue.poll();
                    }
                }
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenCard + " min size: " + queue.size());
                if (stab.length == 0 || !notExcluded.isEmpty()) {
                    System.out.println("Excluded due to " + ((stab.length == 0) ? "stab empty" : "not covering"));
                    continue;
                }
                Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                BiConsumer<StateTr[], NSState[]> fCons = (sts, nst) -> {
                    InversivePlane pl = new InversivePlane(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(pl.fingerprint() + " " + Arrays.stream(sts).map(StateTr::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                List<List<StateTr>> init = new ArrayList<>();
                JNauty.instance().cliques(g, queue.size(), v, a -> {
                    FixBS arr = new FixBS(a);
                    List<StateTr> states = new ArrayList<>();
                    FixBS diffSet = new FixBS(space.diffLength());
                    for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                        StateTr st = stab[i];
                        states.add(st);
                        diffSet.or(st.diffSet());
                    }
                    int card = diffSet.cardinality();
                    if ((space.diffLength() - card) % (k * (k - 1) * (k - 2)) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                        return;
                    }
                    if (card == space.diffLength()) {
                        fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                        return;
                    }
                    init.add(states);
                });
                System.out.println("Init " + init.size());
                AtomicInteger ai = new AtomicInteger();
                init.parallelStream().forEach(states -> {
                    int dc = space.diffLength();
                    FixBS diffSet = new FixBS(space.diffLength());
                    FixBS filter = space.emptyFilter();
                    for (StateTr st : states) {
                        diffSet.or(st.diffSet());
                        for (int i = st.diffSet().nextSetBit(0); i >= 0; i = st.diffSet().nextSetBit(i + 1)) {
                            filter.or(space.difference(i));
                        }
                    }
                    int nc = (dc - diffSet.cardinality()) / k / (k - 1) / (k - 2);
                    if (nc == 0) {
                        fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                        return;
                    }
                    int triple = filter.nextClearBit(0);
                    int x = triple / v / v;
                    int y = triple / v % v;
                    int z = triple % v;
                    NSState in = new NSState(new int[]{x, y}, diffSet).acceptElem(space, z);
                    searchDesigns(space, new NSState[]{in}, filter, nst -> {
                        if (nst.length < nc) {
                            return false;
                        }
                        fCons.accept(states.toArray(StateTr[]::new), nst);
                        return true;
                    });
                    int val = ai.incrementAndGet();
                    if (val % 1000 == 0) {
                        System.out.println(val);
                    }
                });
            }
        }
    }

    @Test
    public void tractor() throws IOException {
        int k = 4;
        int v = 16;
        int orbits = 2;
        int[][] splits = TriplesTest.splits(v, orbits);
        TriplesTest.generateConfigs(splits, (group, config) -> {
            GSpaceTr space;
            try {
                space = new GSpaceTr(k, group, false, config);
            } catch (IllegalArgumentException e) {
                System.out.println("Not empty");
                return;
            }
            FixBS evenDiffs = space.evenDiffs();
            StateTr[] stab = space.getStabilized();
            FixBS notExcluded = evenDiffs.copy();
            int evenCard = evenDiffs.cardinality();
            int sum = 0;
            PriorityQueue<Integer> queue = new PriorityQueue<>();
            for (StateTr st : stab) {
                int intCard = evenDiffs.intersection(st.diffSet()).cardinality();
                sum = sum + intCard;
                queue.offer(intCard);
                notExcluded.andNot(st.diffSet());
                while (!queue.isEmpty() && sum - queue.peek() >= evenCard) {
                    sum = sum - queue.poll();
                }
            }
            try {
                System.out.println(GroupIndex.identify(group) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenCard + " min size: " + queue.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (stab.length == 0 || !notExcluded.isEmpty()) {
                System.out.println("Excluded due to " + ((stab.length == 0) ? "stab empty" : "not covering"));
                return;
            }
            Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
            BiConsumer<StateTr[], NSState[]> fCons = (sts, nst) -> {
                InversivePlane pl = new InversivePlane(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                        Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                System.out.println(pl.fingerprint() + " " + Arrays.stream(sts).map(StateTr::block).toList()
                        + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
            };
            List<List<StateTr>> init = new ArrayList<>();
            JNauty.instance().cliques(g, Math.max(1, queue.size()), v, a -> {
                FixBS arr = new FixBS(a);
                List<StateTr> states = new ArrayList<>();
                FixBS diffSet = new FixBS(space.diffLength());
                for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                    StateTr st = stab[i];
                    states.add(st);
                    diffSet.or(st.diffSet());
                }
                int card = diffSet.cardinality();
                if ((space.diffLength() - card) % (k * (k - 1) * (k - 2)) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                    return;
                }
                if (card == space.diffLength()) {
                    fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                    return;
                }
                init.add(states);
            });
            System.out.println("Init " + init.size());
            AtomicInteger ai = new AtomicInteger();
            init.parallelStream().forEach(states -> {
                int dc = space.diffLength();
                FixBS diffSet = new FixBS(space.diffLength());
                FixBS filter = space.emptyFilter();
                for (StateTr st : states) {
                    diffSet.or(st.diffSet());
                    for (int i = st.diffSet().nextSetBit(0); i >= 0; i = st.diffSet().nextSetBit(i + 1)) {
                        filter.or(space.difference(i));
                    }
                }
                int nc = (dc - diffSet.cardinality()) / k / (k - 1) / (k - 2);
                if (nc == 0) {
                    fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                    return;
                }
                int triple = filter.nextClearBit(0);
                int x = triple / v / v;
                int y = triple / v % v;
                int z = triple % v;
                NSState in = new NSState(new int[]{x, y}, diffSet).acceptElem(space, z);
                searchDesigns(space, new NSState[]{in}, filter, nst -> {
                    if (nst.length < nc) {
                        return false;
                    }
                    fCons.accept(states.toArray(StateTr[]::new), nst);
                    return true;
                });
                int val = ai.incrementAndGet();
                if (val % 1000 == 0) {
                    System.out.println(val);
                }
            });
        });
    }

    private static void searchDesigns(GSpaceTr space, NSState[] currDesign, FixBS globalFilter, Predicate<NSState[]> cons) {
        int v = space.v();
        int li = currDesign.length - 1;
        NSState last = currDesign[li];
        int[] block = last.block();
        int bl = block.length;
        if (bl == space.k()) {
            if (cons.test(currDesign)) {
                return;
            }
            FixBS newFilter = globalFilter.copy();
            for (int i = last.diffSet().nextSetBit(0); i >= 0; i = last.diffSet().nextSetBit(i + 1)) {
                newFilter.or(space.difference(i));
            }
            int triple = newFilter.nextClearBit(0);
            int x = triple / v / v;
            int y = triple / v % v;
            int z = triple % v;
            NSState st = new NSState(new int[]{x, y}, last.diffSet()).acceptElem(space, z);
            NSState[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = st;
            searchDesigns(space, nextDesign, newFilter, cons);
        } else {
            int fst = last.block[0];
            int snd = last.block[last.block.length - 1];
            int from = space.to(fst, snd, snd + 1);
            int to = space.to(fst, snd, v);
            for (int el = globalFilter.nextClearBit(from); el >= 0 && el < to; el = globalFilter.nextClearBit(el + 1)) {
                NSState nextState = last.acceptElem(space, el % v);
                if (nextState != null) {
                    currDesign[li] = nextState;
                    searchDesigns(space, currDesign, globalFilter, cons);
                }
            }
        }
    }

    @Test
    public void testWithAuth() throws IOException {
        int gs = 72;
        int k = 6;
        int[] orbits = new int[]{18, 24};
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            System.out.println("Reading SmallGroup(" + gs + "," + j + ")");
            GapGroup gg = GroupIndex.gapGroup(gs, j);
            Group group = gg.group();
            List<int[][]> configs = ApplicatorTest.configs(group, orbits);
            for (int[][] config : configs) {
                GSpaceTr space;
                try {
                    space = new GSpaceTr(k, group, true, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                StateTr[] stab = space.getStabilized();
                FixBS notExcluded = evenDiffs.copy();
                int evenCard = evenDiffs.cardinality();
                int sum = 0;
                PriorityQueue<Integer> queue = new PriorityQueue<>();
                for (StateTr st : stab) {
                    int intCard = evenDiffs.intersection(st.diffSet()).cardinality();
                    sum = sum + intCard;
                    queue.offer(intCard);
                    notExcluded.andNot(st.diffSet());
                    while (!queue.isEmpty() && sum - queue.peek() >= evenCard) {
                        sum = sum - queue.poll();
                    }
                }
                if (stab.length == 0 || !notExcluded.isEmpty()) {
                    System.out.println("Excluded due to " + ((stab.length == 0) ? "stab empty" : "not covering"));
                    continue;
                }
                Arrays.parallelSort(stab, Comparator.comparing(StateTr::block));
                int[] idxes = IntStream.range(0, stab.length).filter(i -> space.minimal(stab[i].block())).toArray();
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenCard + " min size: " + queue.size()
                        + " auths: " + space.authLength() + " minimals: " + idxes.length);
                BiConsumer<StateTr[], NSState[]> fCons = (sts, nst) -> {
                    InversivePlane pl = new InversivePlane(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(pl.fingerprint() + " " + Arrays.stream(sts).map(StateTr::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                List<List<StateTr>> init = Collections.synchronizedList(new ArrayList<>());
                Arrays.stream(idxes).parallel().forEach(idx -> {
                    StateTr fstBlock = stab[idx];
                    List<StateTr> initList = List.of(fstBlock);
                    if ((space.diffLength() - fstBlock.diffSet().cardinality()) % (k * (k - 1) * (k - 2)) == 0 && evenDiffs.diff(fstBlock.diffSet()).isEmpty()) {
                        init.add(initList);
                    }
                    StateTr[] filteredStab = Arrays.stream(stab, idx, stab.length).filter(st -> !st.diffSet().intersects(fstBlock.diffSet())).toArray(StateTr[]::new);
                    System.out.println("For block with idx " + idx + " filtered size " + filteredStab.length);
                    Graph g = Graph.by(filteredStab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                    JNauty.instance().cliques(g, Math.max(1, queue.size() - 1), v, a -> {
                        FixBS arr = new FixBS(a);
                        List<StateTr> states = new ArrayList<>(initList);
                        FixBS diffSet = fstBlock.diffSet().copy();
                        for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                            StateTr st = filteredStab[i];
                            states.add(st);
                            diffSet.or(st.diffSet());
                        }
                        int card = diffSet.cardinality();
                        if ((space.diffLength() - card) % (k * (k - 1) * (k - 2)) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                            return;
                        }
                        if (card == space.diffLength()) {
                            fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                            return;
                        }
                        if (space.minimal(states.toArray(StateTr[]::new))) {
                            init.add(states);
                        }
                    });
                    System.out.println("Block with idx " + idx + " done");
                });
                System.out.println("Init " + init.size());
                AtomicInteger ai = new AtomicInteger();
                init.parallelStream().forEach(states -> {
                    int dc = space.diffLength();
                    FixBS diffSet = new FixBS(space.diffLength());
                    FixBS filter = space.emptyFilter();
                    for (StateTr st : states) {
                        diffSet.or(st.diffSet());
                        for (int i = st.diffSet().nextSetBit(0); i >= 0; i = st.diffSet().nextSetBit(i + 1)) {
                            filter.or(space.difference(i));
                        }
                    }
                    int nc = (dc - diffSet.cardinality()) / k / (k - 1) / (k - 2);
                    if (nc == 0) {
                        fCons.accept(states.toArray(StateTr[]::new), new NSState[0]);
                        return;
                    }
                    int triple = filter.nextClearBit(0);
                    int x = triple / v / v;
                    int y = triple / v % v;
                    int z = triple % v;
                    NSState in = new NSState(new int[]{x, y}, diffSet).acceptElem(space, z);
                    searchDesigns(space, new NSState[]{in}, filter, nst -> {
                        if (nst.length < nc) {
                            return false;
                        }
                        fCons.accept(states.toArray(StateTr[]::new), nst);
                        return true;
                    });
                    int val = ai.incrementAndGet();
                    if (val % 1000 == 0) {
                        System.out.println(val);
                    }
                });
            }
        }
    }

    private static FixBS diffs(GSpaceTr sp, int a, int b, int p) {
        FixBS diffs = new FixBS(sp.diffLength());
        diffs.set(sp.diffIdx(a, b, p));
        diffs.set(sp.diffIdx(b, a, p));
        diffs.set(sp.diffIdx(a, p, b));
        diffs.set(sp.diffIdx(b, p, a));
        diffs.set(sp.diffIdx(p, a, b));
        diffs.set(sp.diffIdx(p, b, a));
        return diffs;
    }

    public record NSState(int[] block, FixBS diffSet) {
        public NSState acceptElem(GSpaceTr sp, int p) {
            int[] newBlock = Arrays.copyOf(block, block.length + 1);
            newBlock[block.length] = p;
            FixBS newDiffSet = diffSet.copy();
            for (int a : block) {
                for (int b : block) {
                    if (a >= b) {
                        continue;
                    }
                    FixBS diffs = diffs(sp, a, b, p);
                    if (diffs.cardinality() < 6 || newDiffSet.intersects(diffs)) {
                        return null;
                    }
                    newDiffSet.or(diffs);
                }
            }
            return new NSState(newBlock, newDiffSet);
        }
    }
}
