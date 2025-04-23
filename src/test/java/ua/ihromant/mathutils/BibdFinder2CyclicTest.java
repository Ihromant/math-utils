package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BibdFinder2CyclicTest {
    private record Design(int[][] design, int idx, int blockIdx) {

        private Design simpleAdd(int el) {
            int[][] cloned = design.clone();
            int[] last = cloned[blockIdx].clone();
            last[idx] = el;
            cloned[blockIdx] = last;
            return new Design(cloned, idx + 1, blockIdx);
        }

        private int[] curr() {
            return design[blockIdx];
        }

        private boolean tupleFinished() {
            return idx == design[blockIdx].length;
        }

        private boolean lastBlock() {
            return blockIdx + 1 == design.length;
        }

        private int lastVal() {
            return design[blockIdx][idx - 1];
        }

        @Override
        public String toString() {
            return "(" + Arrays.deepToString(design) + ", " + idx + ", " + blockIdx + ")";
        }
    }

    private record State(Design curr, FixBS filter, FixBS whiteList) {
        private static State forDesign(Group group, FixBS baseFilter, int[][] baseDesign, int k, int blockIdx) {
            int v = group.order();
            int[][] nextDesign = baseDesign.clone();
            nextDesign[blockIdx] = new int[k];
            FixBS filter = baseFilter.copy();
            for (int bi = 0; bi < blockIdx; bi++) {
                int[] block = baseDesign[bi];
                for (int i = 0; i < k; i++) {
                    int f = block[i];
                    for (int j = i + 1; j < k; j++) {
                        int s = block[j];
                        filter.set(group.op(f, group.inv(s)));
                        filter.set(group.op(s, group.inv(f)));
                    }
                }
            }
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            Design curr = new Design(nextDesign, 1, blockIdx);
            State state = new State(curr, filter, whiteList);
            return state.acceptElem(group, whiteList.nextSetBit(0), v, st -> {});
        }

        private State acceptElem(Group group, int el, int v, Consumer<State> cons) {
            Design nextCurr = curr.simpleAdd(el);
            boolean tupleFinished = nextCurr.tupleFinished();
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int[] nextTuple = nextCurr.curr();
            int idx = curr.idx;
            int invEl = group.inv(el);
            for (int i = 0; i < idx; i++) {
                int val = nextTuple[i];
                int diff = group.op(el, group.inv(val));
                int outDiff = group.op(val, invEl);
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
                    int nv = nextTuple[j];
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            State result = new State(nextCurr, newFilter, newWhiteList);
            if (tupleFinished) {
                if (nextCurr.lastBlock()) {
                    cons.accept(result);
                    return null;
                }
                result = result.initiateNextTuple(newFilter, v)
                        .acceptElem(group, newFilter.nextClearBit(1), v, st -> {});
            } else {
                for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(el, diff));
                }
            }
            return result;
        }

        private State initiateNextTuple(FixBS filter, int v) {
            FixBS nextWhiteList = filter.copy();
            nextWhiteList.flip(1, v);
            return new State(new Design(curr.design, 1, curr.blockIdx + 1), filter, nextWhiteList);
        }
    }

    private static int compare(int[] fst, int[] snd) {
        for (int i = 1; i < fst.length; i++) {
            int dff = fst[i] - snd[i];
            if (dff != 0) {
                return dff;
            }
        }
        return 0;
    }

    private static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
        int v = gr.order() + 1;
        FixBS base = FixBS.of(v);
        for (int val : tuple) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(0); val >= 0; val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(v);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(gr.op(inv, oVal));
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min.toArray();
    }

    private static void calcCycles(Group group, int v, State state, Consumer<State> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal()); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElem(group, idx, v, sink);
            if (next != null) {
                calcCycles(group, v, next, sink);
            }
        }
    }

    private static FixBS baseFilter(Group gr, int k) {
        int v = gr.order();
        FixBS filter = new FixBS(v);
        int rest = v % (k * (k - 1));
        if (rest == k) {
            for (int i = 1; i < v; i++) {
                int ord = gr.order(i);
                if (ord != 1 && k % ord == 0) {
                    filter.set(i);
                }
            }
            if (filter.cardinality() != k - 1) {
                SubGroup sg = gr.subGroups().stream().filter(g -> g.order() == k).findFirst().orElseThrow();
                filter.clear();
                filter.or(sg.elems());
                filter.clear(0);
            }
        }
        if (rest == (k - 1)) {
            for (int i = 1; i < v; i++) {
                int ord = gr.order(i);
                if (ord != 1 && (k - 1) % ord == 0) {
                    filter.set(i);
                }
            }
            if (filter.cardinality() != k - 2) {
                SubGroup sg = gr.subGroups().stream().filter(g -> g.order() == k - 1).findFirst().orElseThrow();
                filter.clear();
                filter.or(sg.elems());
                filter.clear(0);
            }
        }
        return filter;
    }

    @Test
    public void toConsole() throws IOException {
        Group gr = new CyclicProduct(11, 11);
        int v = gr.order();
        int k = 5;
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            Set<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
            logResultsDepth(System.out, gr, k, set.stream().map(st -> st.stream()
                    .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
        }
    }

    @Test
    public void toFile() throws IOException {
        Group gr = new CyclicGroup(13);
        int v = gr.order();
        int k = 3;
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + ".txt");
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
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
            br.lines().forEach(l -> {
                if (l.contains("[[")) {
                    System.out.println(l);
                } else {
                    set.remove(readPartial(l, v));
                }
            });
            logResultsDepth(ps, gr, k, set.stream().map(st -> st.stream()
                    .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
        }
    }

    private static List<FixBS> readPartial(String line, int v) {
        String[] sp = line.substring(1, line.length() - 1).split("] \\[");
        return Arrays.stream(sp).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())).collect(Collectors.toList());
    }

    private static void logResultsDepth(PrintStream destination, Group group, int k, List<int[][]> unProcessed) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        Group table = group.asTable();
        System.out.println("Initial size " + unProcessed.size());
        int blocksNeeded = v / k / (k - 1);
        FixBS baseFilter = baseFilter(group, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<State> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design.curr.design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design.curr.design));
            }
        };
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            int[][] design = new int[blocksNeeded][k];
            for (int i = 0; i < init.length; i++) {
                System.arraycopy(init[i], 0, design[i], 0, k);
            }
            State initial = State.forDesign(table, baseFilter, design, k, init.length);
            calcCycles(table, v, initial, designConsumer);
            if (destination != System.out) {
                destination.println(Arrays.stream(init).map(Arrays::toString).collect(Collectors.joining(" ")));
                destination.flush();
            }
            int val = cnt.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val);
            }
        });
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    @Test
    public void logAllCycles() {
        Group group = new CyclicProduct(5, 5, 13);
        int k = 13;
        logAllCycles(System.out, group, k);
    }

    private static void logAllCycles(PrintStream destination, Group group, int k) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(group, k);
        int blocksNeeded = v / k / (k - 1);
        Group table = group.asTable();
        int[][] design = new int[blocksNeeded][k];
        State initial = State.forDesign(table, filter, design, k, 0);
        calcCycles(table, v, initial, cycle -> {
            destination.println(Arrays.deepToString(cycle.curr.design));
            destination.flush();
        });
    }

    @Test
    public void filter() throws IOException {
        Group gr = new CyclicProduct(13, 13);
        int k = 7;
        int[][] auths = gr.auth();
        FixBS filter = baseFilter(gr, k);
        filter.set(0);
        if (filter.cardinality() == k - 1) {
            filter.set(gr.order());
        }
        File refined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "ref.txt");
        File unrefined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + ".txt");
        AtomicInteger ai = new AtomicInteger();
        try (FileInputStream fis = new FileInputStream(unrefined);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream(refined);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            br.lines().forEach(l -> {
                if (!l.contains("[[")) {
                    return;
                }
                String[] sp = l.substring(2, l.length() - 2).split("], \\[");
                int[][] des = Arrays.stream(sp).map(pt -> Arrays.stream(pt.split(", ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                int cnt = 0;
                for (int[] auth : auths) {
                    int[][] mapped = Arrays.stream(des).map(arr -> minimalTuple(arr, auth, gr)).toArray(int[][]::new);
                    Arrays.sort(mapped, Comparator.comparingInt(arr -> arr[1]));
                    if (bigger(des, mapped, des.length - 1)) {
                        return;
                    }
                    if (Arrays.deepEquals(mapped, des)) {
                        cnt++;
                    }
                }
                int[][] desf = filter.cardinality() == 1 ? des : Stream.concat(Arrays.stream(des), Stream.of(filter.toArray())).toArray(int[][]::new);
                if (cnt > 1) {
                    System.out.println(cnt + " " + Liner.byDiffFamily(gr, desf).hyperbolicFreq() + " " + Arrays.deepToString(desf));
                }
                ps.println(l);
                ai.incrementAndGet();
            });
            System.out.println(ai.get());
        }
    }

    private boolean bigger(int[][] design, int[][] candidate, int blockIdx) {
        int i = 0;
        int cmp;
        do {
            int[] cnd = candidate[i];
            int[] block = design[i];
            cmp = compare(cnd, block);
        } while (cmp == 0 && i++ < blockIdx);
        return cmp < 0;
    }
}
