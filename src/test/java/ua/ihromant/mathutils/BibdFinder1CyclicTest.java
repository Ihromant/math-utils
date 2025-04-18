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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BibdFinder1CyclicTest {
    private record Design(int[][] design, int idx, int blockIdx) {
        private boolean bigger(Design candidate) {
            int unf = 0;
            int cmp;
            do {
                int[] cnd = candidate.design[unf];
                int[] block = design[unf];
                boolean notFull = unf == blockIdx || unf == candidate.blockIdx;
                cmp = compare(cnd, block, notFull ? idx : block.length);
                if (cmp == 0 && unf == candidate.blockIdx && unf != blockIdx && idx != block.length) {
                    cmp = 1;
                }
            } while (cmp == 0 && unf++ < blockIdx);
            return cmp < 0;
        }

        private Design simpleAdd(int el) {
            int[][] cloned = design.clone();
            int[] last = cloned[blockIdx].clone();
            last[idx] = el;
            cloned[blockIdx] = last;
            return new Design(cloned, idx + 1, blockIdx);
        }

        private Design add(int el, Group group) {
            int[][] cloned = design.clone();
            int newIdx = blockIdx;
            int[] unf = cloned[newIdx].clone();
            int pos = idx;
            while (pos > 0 && unf[pos - 1] > el) {
                pos--;
            }
            System.arraycopy(unf, pos, unf, pos + 1, idx - pos);
            unf[pos] = el;
            if (idx + 1 == unf.length) {
                unf = minimalTuple(unf, group);
            }
            while (newIdx > 0 && unf[1] < cloned[newIdx - 1][1]) {
                newIdx--;
            }
            if (newIdx != blockIdx) {
                System.arraycopy(cloned, newIdx, cloned, newIdx + 1, blockIdx - newIdx);
            }
            cloned[newIdx] = unf;
            return new Design(cloned, idx + 1, newIdx);
        }

        private Design initiateNew(int k, int blockIdx) {
            int[][] newDesign = design.clone();
            newDesign[blockIdx] = new int[k];
            return new Design(newDesign, 1, blockIdx);
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

    private record State(Design curr, FixBS filter, FixBS whiteList, Design[] transformations) {
        private static State forDesign(Group group, int[][] auths, FixBS baseFilter, int[][] baseDesign, int k, int blockIdx) {
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
            Design curr = new Design(nextDesign, k, blockIdx).initiateNew(k, blockIdx);
            Design[] transformations = Arrays.stream(auths).map(aut -> {
                int[][] transformed = IntStream.range(0, baseDesign.length).mapToObj(idx -> {
                    int[] res = new int[k];
                    if (idx > blockIdx) {
                        return res;
                    }
                    int[] arr = baseDesign[idx];
                    for (int i = 0; i < arr.length; i++) {
                        res[i] = aut[arr[i]];
                    }
                    Arrays.sort(res);
                    return minimalTuple(res, group);
                }).toArray(int[][]::new);
                return new Design(transformed, k, blockIdx).initiateNew(k, blockIdx);
            }).toArray(Design[]::new);
            State state = new State(curr, filter, whiteList, transformations);
            return state.acceptElem(group, auths, whiteList.nextSetBit(0), v, k, st -> {});
        }

        private State acceptElem(Group group, int[][] auth, int el, int v, int k, Consumer<State> cons) {
            Design nextCurr = curr.simpleAdd(el);
            Design[] nextTransformations = new Design[transformations.length];
            boolean tupleFinished = nextCurr.tupleFinished();
            for (int i = 0; i < transformations.length; i++) {
                Design nextTransformation = transformations[i].add(auth[i][el], group);
                if (nextCurr.bigger(nextTransformation)) {
                    return null;
                }
                nextTransformations[i] = nextTransformation;
            }
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int[] nextTuple = nextCurr.curr();
            int idx = curr.idx;
            int invEl = group.inv(el);
            for (int i = 0; i < idx; i++) {
                int val = nextTuple[i];
                int diff = group.op(el, group.inv(val));
                int outDiff = group.op(val, invEl);
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(el, rt));
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int j = 0; j <= idx; j++) {
                    int nv = nextTuple[j];
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear(group.op(el, diff));
            }
            State result = new State(nextCurr, newFilter, newWhiteList, nextTransformations);
            if (tupleFinished) {
                if (nextCurr.lastBlock()) {
                    cons.accept(result);
                    return null;
                }
                result = result.initiateNextTuple(newFilter, v, k)
                        .acceptElem(group, auth, newFilter.nextClearBit(1), v, k, st -> {});
            }
            return result;
        }

        private State initiateNextTuple(FixBS filter, int v, int k) {
            int nextBlockIdx = curr.blockIdx + 1;
            Design nextSet = curr.initiateNew(k, nextBlockIdx);
            FixBS nextWhiteList = filter.copy();
            nextWhiteList.flip(1, v);
            Design[] nextTransformations = Arrays.stream(transformations).map(tr -> tr.initiateNew(k, nextBlockIdx)).toArray(Design[]::new);
            return new State(nextSet, filter, nextWhiteList, nextTransformations);
        }
    }

    private static int compare(int[] fst, int[] snd, int cap) {
        for (int i = 1; i < cap; i++) {
            int dff = fst[i] - snd[i];
            if (dff != 0) {
                return dff;
            }
        }
        return 0;
    }

    private static int[] minimalTuple(int[] arr, Group gr) {
        int v = gr.order() + 1;
        FixBS base = FixBS.of(v, arr);
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

    private static void calcCycles(Group group, int[][] auth, int v, int k, State state, Consumer<State> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal()); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElem(group, auth, idx, v, k, sink);
            if (next != null) {
                calcCycles(group, auth, v, k, next, sink);
            }
        }
    }

    @Test
    public void logNotEqCycles() throws IOException {
        Group group = new CyclicProduct(11, 11);
        int k = 5;
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + group.name() + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logFirstCycles(ps, group, k, 2);
        }
    }

    @Test
    public void logConsoleCycles() {
        Group group = new CyclicProduct(5, 5);
        int k = 4;
        logFirstCycles(System.out, group, k, 1);
    }

    @Test
    public void logAllCycles() {
        Group group = new CyclicProduct(7, 7);
        int k = 4;
        logAllCycles(System.out, group, k);
    }

    private static void logFirstCycles(PrintStream destination, Group group, int k, int blockCount) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(group, k);
        int[][] auths = group.auth();
        Group table = group.asTable();
        int[][] design = new int[blockCount][k];
        State initial = State.forDesign(table, auths, filter, design, k, 0);
        calcCycles(table, auths, v, k, initial, cycle -> {
            destination.println(Arrays.stream(cycle.curr.design).map(Arrays::toString).collect(Collectors.joining(" ")));
            destination.flush();
        });
    }

    private static void logAllCycles(PrintStream destination, Group group, int k) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(group, k);
        int blocksNeeded = v / k / (k - 1);
        int[][] auths = group.auth();
        Group table = group.asTable();
        int[][] design = new int[blocksNeeded][k];
        State initial = State.forDesign(table, auths, filter, design, k, 0);
        calcCycles(table, auths, v, k, initial, cycle -> {
            destination.println(Arrays.deepToString(cycle.curr.design));
            destination.flush();
        });
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
        File beg = new File("/home/ihromant/maths/diffSets/beg", k + "-" + gr.name() + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            Set<FixBS> set = allBr.lines().map(l -> FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", "))
                    .mapToInt(Integer::parseInt).toArray())).collect(Collectors.toSet());
            logResultsDepth(System.out, gr, k, set.stream().map(bs -> bs.stream().toArray()).toList());
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
            Set<FixBS> set = allBr.lines().map(l -> FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", "))
                    .mapToInt(Integer::parseInt).toArray())).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[")) {
                    System.out.println(l);
                } else {
                    set.remove(FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", "))
                            .mapToInt(Integer::parseInt).toArray()));
                }
            });
            logResultsDepth(ps, gr, k, set.stream().map(bs -> bs.stream().toArray()).toList());
        }
    }

    private static void logResultsDepth(PrintStream destination, Group group, int k, List<int[]> unProcessed) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        Group table = group.asTable();
        int[][] auths = group.auth();
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
            System.arraycopy(init, 0, design[0], 0, k);
            State initial = State.forDesign(table, auths, baseFilter, design, k, 1);
            calcCycles(table, auths, v, k, initial, designConsumer);
            if (destination != System.out) {
                destination.println(Arrays.toString(init));
                destination.flush();
            }
            int val = cnt.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val);
            }
        });
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }
}
