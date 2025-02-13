package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BibdFinder4CyclicTest {
    private static int[] addEl(int[] block, int idx, int el) {
        int pos = idx;
        while (pos > 0 && block[pos - 1] > el) {
            pos--;
        }
        System.arraycopy(block, pos, block, pos + 1, idx - pos);
        block[pos] = el;
        return block;
    }

    private static int diffCount(int[] block, int v, Group gr) {
        FixBS diffs = new FixBS(v);
        for (int i = 0; i < block.length; i++) {
            int x = block[i];
            for (int j = i + 1; j < block.length; j++) {
                int y = block[j];
                if (y == gr.order()) {
                    continue;
                }
                diffs.set(gr.op(y, gr.inv(x)));
                diffs.set(gr.op(x, gr.inv(y)));
            }
        }
        return diffs.cardinality();
    }

    private static Stream<int[]> blocks(int[] block, int v, Group gr) {
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

    private record State(int[] block, int diffNeeded, int idx, FixBS filter, FixBS whiteList) {
        private static State forInitial(Group group, int[][] initial, int v, int k) {
            FixBS filter = new FixBS(v);
            int diffNeeded = group.order() - 1;
            for (int[] block : initial) {
                updateFilter(group, k, block, filter);
                diffNeeded = diffNeeded - diffCount(block, v, group);
            }
            FixBS newWhiteList = filter.copy();
            newWhiteList.flip(1, v);
            return new State(initial.length == 0 ? new int[k] : initial[initial.length - 1], diffNeeded, k, filter, newWhiteList);
        }

        private State initiateSubGroup(SubGroup sub, int v, int k) {
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = filter.copy();
            newWhiteList.flip(1, v);
            newFilter.or(sub.elems());
            newFilter.clear(0);
            Group group = sub.group();
            int[] arr = sub.arr();
            for (int i = 0; i < arr.length; i++) {
                int x = arr[i];
                for (int diff = newFilter.nextSetBit(0); diff >= 0 && diff < group.order(); diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(x, diff));
                }
                for (int j = i + 1; j < arr.length; j++) {
                    int y = arr[j];
                    int sqr = group.op(y, group.inv(x));
                    for (int root : group.squareRoots(sqr)) {
                        whiteList.clear(group.op(x, root));
                    }
                    int invSqr = group.op(x, group.inv(y));
                    for (int root : group.squareRoots(invSqr)) {
                        whiteList.clear(group.op(y, root));
                    }
                }
            }
            int[] block = new int[k];
            System.arraycopy(arr, 0, block, 0, arr.length);
            return new State(block, diffNeeded - arr.length + 1, arr.length, newFilter, newWhiteList);
        }

        private State acceptElem(Group group, int el) {
            int[] nextBlock = addEl(block.clone(), idx, el);
            int nextIdx = idx + 1;
            boolean tupleFinished = nextIdx == block.length;
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            if (el == group.order()) {
                newFilter.set(el);
                newWhiteList.clear(el);
                return new State(nextBlock, diffNeeded, nextIdx, newFilter, newWhiteList);
            }
            int invEl = group.inv(el);
            for (int i = 0; i < idx; i++) {
                int val = block[i];
                int diff = group.op(group.inv(val), el);
                int outDiff = group.op(invEl, val);
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (tupleFinished) {
                    continue;
                }
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(el, rt));
                }
                for (int j = 0; j <= idx; j++) {
                    int nv = nextBlock[j];
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            if (!tupleFinished) {
                for (int diff = newFilter.nextSetBit(0); diff >= 0 && diff < group.order(); diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(el, diff));
                }
            }
            return new State(nextBlock, diffNeeded - idx * 2, nextIdx, newFilter, newWhiteList);
        }
    }

    private static void updateFilter(Group group, int k, int[] block, FixBS filter) {
        for (int i = 0; i < k; i++) {
            int f = block[i];
            if (f == group.order()) {
                filter.set(f);
                continue;
            }
            for (int j = i + 1; j < k; j++) {
                int s = block[j];
                if (s == group.order()) {
                    continue;
                }
                filter.set(group.op(group.inv(s), f));
                filter.set(group.op(group.inv(f), s));
            }
        }
    }

    private static int[][] addBlock(int[][] design, int[] block) {
        int[][] cloned = Arrays.copyOf(design, design.length + 1);
        int newIdx = design.length;
        while (newIdx > 0 && block[1] < cloned[newIdx - 1][1]) {
            newIdx--;
        }
        System.arraycopy(cloned, newIdx, cloned, newIdx + 1, design.length - newIdx);
        cloned[newIdx] = block;
        return cloned;
    }

    private static int compare(int[] fst, int[] snd, int len) {
        for (int i = 1; i < len; i++) {
            int dff = fst[i] - snd[i];
            if (dff != 0) {
                return dff;
            }
        }
        return 0;
    }

    private static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
        int k = tuple.length;
        int[] arr = new int[k];
        int minDiff = Integer.MAX_VALUE;
        int ord = gr.order();
        boolean infty = tuple[k - 1] == ord;
        int top = infty ? k - 1 : k;
        for (int j = 1; j < top; j++) {
            int el = tuple[j];
            int mapped = auth[el];
            arr[j] = mapped;
            if (mapped < minDiff) {
                minDiff = mapped;
            }
        }
        int[] min = arr;
        if (infty) {
            arr[top] = ord;
        }
        for (int j = 1; j < top; j++) {
            int inv = gr.inv(arr[j]);
            int[] cnd = new int[k];
            if (infty) {
                cnd[top] = ord;
            }
            for (int i = 0; i < top; i++) {
                if (i == j) {
                    continue;
                }
                int diff = gr.op(inv, arr[i]);
                cnd[i] = diff;
                if (diff < minDiff) {
                    minDiff = diff;
                    min = cnd;
                }
            }
        }
        Arrays.sort(min);
        return min;
    }

    private static void calcCycles(List<SubGroup> subGroups, SubGroup currSub, int[][] design, int v, int k, State state, Consumer<int[][]> sink) {
        int idx = state.idx;
        if (idx == k) {
            int[][] nextDesign = Arrays.copyOf(design, design.length + 1);
            nextDesign[design.length] = state.block;
            if (state.diffNeeded == 0) {
                sink.accept(nextDesign);
                return;
            } else {
                FixBS filter = state.filter();
                int next = filter.nextClearBit(1);
                for (SubGroup sg : subGroups) {
                    if (filter.intersects(sg.elems())) {
                        continue;
                    }
                    State nextState = state.initiateSubGroup(sg, v, k);
                    if (sg.elems().get(next)) {
                        calcCycles(subGroups, sg, nextDesign, v, k, nextState, sink);
                        continue;
                    }
                    if (sg.arr().length == k || !nextState.whiteList().get(next)) {
                        continue;
                    }
                    nextState = nextState.acceptElem(sg.group(), next);
                    calcCycles(subGroups, sg, nextDesign, v, k, nextState, sink);
                }
            }
            return;
        }
        int lastVal = 1;
        for (int i = idx - 1; i >= 0; i--) {
            int val = state.block[i];
            if (!currSub.elems().get(val)) {
                lastVal = val;
                break;
            }
        }
        FixBS whiteList = state.whiteList();
        for (int el = whiteList.nextSetBit(lastVal); el >= 0; el = whiteList.nextSetBit(el + 1)) {
            State next = state.acceptElem(currSub.group(), el);
            calcCycles(subGroups, currSub, design, v, k, next, sink);
        }
    }

    @Test
    public void testDifferences() {
        Group g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        int[] block = new int[]{0, 1, 2, 3, 31, 80};
        System.out.println(differences(block, g.order(), g));
        blocks(block, g.order(), g).forEach(arr -> System.out.println(Arrays.toString(arr) + " " + Arrays.stream(arr)
                .mapToObj(g::elementName).collect(Collectors.joining(", ", "[", "]"))));
    }

    private static FixBS differences(int[] block, int v, Group gr) {
        FixBS res = new FixBS(v);
        for (int i = 0; i < block.length; i++) {
            int x = block[i];
            for (int j = i + 1; j < block.length; j++) {
                int y = block[j];
                res.set(gr.op(gr.inv(x), y));
                res.set(gr.op(gr.inv(y), x));
                System.out.println(x + " " + y + " " + gr.op(gr.inv(x), y) + " " + gr.op(gr.inv(y), x));
            }
        }
        return res;
    }

    private static void calcCyclesTrans(SubGroup currGroup, int[][] auths, int k, State state, int[][] transformations, Consumer<int[][]> sink) {
        int idx = state.idx;
        if (idx == k) {
            sink.accept(new int[][]{state.block});
            return;
        }
        FixBS whiteList = state.whiteList();
        int lastVal = 1;
        for (int i = idx - 1; i >= 0; i--) {
            int val = state.block[i];
            if (!currGroup.elems().get(val)) {
                lastVal = val;
                break;
            }
        }
        ex: for (int el = whiteList.nextSetBit(lastVal); el >= 0; el = whiteList.nextSetBit(el + 1)) {
            State next = state.acceptElem(currGroup.group(), el);
            int[][] nextTransformations = new int[auths.length][k];
            for (int i = 0; i < auths.length; i++) {
                int[] bl = nextTransformations[i];
                System.arraycopy(transformations[i], 0, bl, 0, idx);
                addEl(bl, idx, auths[i][el]);
                if (compare(next.block, bl, idx + 1) > 0) {
                    continue ex;
                }
            }
            calcCyclesTrans(currGroup, auths, k, next, nextTransformations, sink);
        }
    }

    private static int[][] auth(Group group) {
        int ord = group.order();
        int[][] auth = group.auth();
        int[][] result = new int[auth.length][ord + 1];
        for (int i = 0; i < auth.length; i++) {
            System.arraycopy(auth[i], 0, result[i], 0, auth[i].length);
            result[i][ord] = ord;
        }
        return result;
    }

    private static List<SubGroup> configs(Group gr, int v, int k) {
        return gr.subGroups().stream().filter(s -> s.order() <= k).toList();
    }

    @Test
    public void dumpInitial() throws IOException {
        int fixed = 1;
        Group group = new CyclicProduct(8, 8);
        int v = group.order() + fixed;
        int k = 5;
        int[][] auths = auth(group);
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        List<SubGroup> subGroups = configs(table, v, k);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Consumer<int[][]> cons = arr -> {
                ps.println(Arrays.stream(arr).map(Arrays::toString).collect(Collectors.joining(" ")));
                ps.flush();
            };
            getInitial(subGroups, auths, v, k, cons);
        }
    }

    private static List<FixBS> readPartial(String line, int v) {
        String[] sp = line.substring(1, line.length() - 1).split("] \\[");
        return Arrays.stream(sp).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())).collect(Collectors.toList());
    }

    @Test
    public void toFile() throws IOException {
        int fixed = 1;
        Group group = new CyclicProduct(8, 8);
        int v = group.order() + fixed;
        int k = 5;
        int[][] auths = auth(group);
        Group table = group.asTable();
        List<SubGroup> subGroups = configs(table, v, k);
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + ".txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
            List<Liner> liners = new ArrayList<>();
            br.lines().forEach(l -> {
                if (l.contains("[[")) {
                    System.out.println(l);
                    String[] split = l.substring(2, l.length() - 2).split("], \\[");
                    int[][] base = Arrays.stream(split).map(bl -> Arrays.stream(bl.split(", "))
                            .mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                    liners.add(new Liner(v, Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new)));
                } else {
                    set.remove(readPartial(l, v));
                }
            });
            logResultsByInitial(ps, table, subGroups, auths, v, k, set.stream().map(st -> st.stream()
                    .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList(), liners);
        }
    }

    @Test
    public void logConsoleCycles() {
        Group group = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        int v = group.order() + 1;
        int k = 4;
        int[][] auths = auth(group);
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        List<SubGroup> subGroups = configs(table, v, k);
        List<int[][]> base = new ArrayList<>();
        getInitial(subGroups, auths, v, k, base::add);
        logResultsByInitial(System.out, table, subGroups, auths, v, k, base, new ArrayList<>());
    }

    private static void getInitial(List<SubGroup> subGroups, int[][] auths, int v, int k, Consumer<int[][]> cons) {
        FixBS filter = new FixBS(v);
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        ex: for (SubGroup sub : subGroups) {
            Group group = sub.group();
            int[] arr = sub.arr();
            State initial = State.forInitial(group, new int[0][k], v, k);
            initial = initial.initiateSubGroup(sub, v, k);
            int[][] transformations = new int[auths.length][k];
            for (int i = 0; i < auths.length; i++) {
                int[] bl = transformations[i];
                for (int j = 0; j < arr.length; j++) {
                    addEl(bl, j, auths[i][arr[j]]);
                }
                if (compare(initial.block, bl, arr.length) > 0) {
                    continue ex;
                }
            }
            if (sub.elems().get(1)) {
                calcCyclesTrans(sub, auths, k, initial, transformations, cons);
                continue;
            }
            if (arr.length == k || !initial.whiteList().get(1)) {
                continue;
            }
            int el = 1;
            initial = initial.acceptElem(group, el);
            for (int i = 0; i < auths.length; i++) {
                int[] bl = transformations[i];
                addEl(bl, arr.length, auths[i][el]);
                if (compare(initial.block, bl, arr.length + 1) > 0) {
                    continue ex;
                }
            }
            calcCyclesTrans(sub, auths, k, initial, transformations, cons);
        }
    }

    private static void logResultsByInitial(PrintStream destination, Group group, List<SubGroup> subGroups,
                                            int[][] auths, int v, int k, List<int[][]> unProcessed, List<Liner> liners) {
        System.out.println("Processing initial of size " + unProcessed.size());
        long time = System.currentTimeMillis();
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            State initial = State.forInitial(group, init, v, k);
            calcCycles(subGroups, null, Arrays.copyOf(init, init.length - 1), v, k, initial, des -> {
                try {
                    liners.add(new Liner(v, Arrays.stream(des).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new)));
                    destination.println(Arrays.deepToString(des));
                    destination.flush();
                    if (destination != System.out) {
                        System.out.println(Arrays.deepToString(des));
                    }
                } catch (IllegalStateException e) {
                    // TODO ok for now, fix later
                }
            });
            if (destination != System.out) {
                destination.println(Arrays.stream(init).map(Arrays::toString).collect(Collectors.joining(" ")));
                destination.flush();
            }
            int val = cnt.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println("Unprocessed " + liners.size());
        Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
        liners.stream().parallel().forEach(l -> {
            FixBS canon = l.getCanonicalOld();
            if (unique.putIfAbsent(canon, l) == null) {
                System.out.println(l.autCountOld() + " " + l.hyperbolicFreq() + " " + Arrays.deepToString(l.lines()));
            }
        });
        System.out.println("Results: " + unique.size() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static final int[][][] sample = {
            {{0, 1, 2, 3, 31, 80}, {0, 4, 7, 12, 32, 71}, {0, 5, 19, 46, 53, 75}, {0, 6, 17, 22, 62, 73}, {0, 9, 48, 58, 69, 93}, {0, 13, 23, 26, 30, 91}, {0, 15, 40, 44, 79, 98}}
    };

    @Test
    public void testSample() {
        Group gr = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        int v = gr.order();
        List<Liner> liners = Arrays.stream(sample).map(des -> new Liner(v, Arrays.stream(des).flatMap(bl -> blocks(bl, v, gr)).toArray(int[][]::new))).toList();
        liners.forEach(l -> System.out.println(l.hyperbolicFreq()));
    }
}
