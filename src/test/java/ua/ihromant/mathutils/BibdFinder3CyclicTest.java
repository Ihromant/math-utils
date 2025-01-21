package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.SemiDirectProduct;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BibdFinder3CyclicTest {
    private record Design(int[][] design, int idx, int blocksNeeded) {
        private boolean bigger(int[][] candidate) {
            int cmp = 0;
            for (int i = 0; i < design.length; i++) {
                cmp = compare(candidate[i], design[i]);
                if (cmp != 0) {
                    break;
                }
            }
            return cmp < 0;
        }

        private Design simpleAdd(int el) {
            int[][] cloned = design.clone();
            int[] last = cloned[design.length - 1].clone();
            last[idx] = el;
            cloned[design.length - 1] = last;
            int next = idx + 1;
            return new Design(cloned, next, blocksNeeded);
        }

        private int[] curr() {
            return design[design.length - 1];
        }

        private boolean tupleFinished() {
            return idx == design[design.length - 1].length;
        }

        private int lastVal() {
            return design[design.length - 1][idx - 1];
        }

        @Override
        public String toString() {
            return "(" + Arrays.deepToString(design) + ", " + idx + ")";
        }
    }

    private static int blockCount(int[] block, Group gr) {
        int ord = gr.order();
        List<FixBS> set = new ArrayList<>(ord);
        ex: for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(ord + 1);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(i, el));
            }
            for (FixBS os : set) {
                if (os.equals(fbs)) {
                    continue ex;
                }
                if (os.intersection(fbs).cardinality() > 1) {
                    return -1;
                }
            }
            set.add(fbs);
        }
        return set.size();
    }

    private record State(Design curr, FixBS filter, FixBS whiteList, int[][][] transformations) {
        private static State forDesign(Group group, int[][] auths, int[][] baseDesign, int k) {
            int v = group.order() + 1;
            int[][] nextDesign = baseDesign.clone();
            FixBS filter = new FixBS(v);
            int blocksNeeded = v * (v - 1) / k / (k - 1);
            for (int[] block : baseDesign) {
                updateFilter(group, k, block, filter);
                blocksNeeded = blocksNeeded - blockCount(block, group);
            }
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            Design curr = new Design(nextDesign, k, blocksNeeded);
            int[][][] transformations = Arrays.stream(auths).map(aut -> Arrays.stream(baseDesign).map(arr -> minimalTuple(arr, aut, group)).sorted(Comparator.comparingInt(arr -> arr[1] != 0 ? arr[1] : Integer.MAX_VALUE)).toArray(int[][]::new)).toArray(int[][][]::new);
            State state = new State(curr, filter, whiteList, transformations);
            return state.initiateNextTuple(curr, filter, 0, k, group, transformations).acceptElem(group, auths, whiteList.nextSetBit(0), k, st -> {});
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

        private State acceptElem(Group group, int[][] auth, int el, int k, Consumer<Design> cons) {
            Design nextCurr = curr.simpleAdd(el);
            boolean tupleFinished = nextCurr.tupleFinished();
            int[] last = nextCurr.curr();
            State result;
            if (tupleFinished) {
                int blockCount = blockCount(last, group);
                if (blockCount < 0) {
                    return null;
                }
                if (blockCount == nextCurr.blocksNeeded) {
                    cons.accept(nextCurr);
                    return null;
                }
                int[][][] nextTransformations = new int[transformations.length][][];
                for (int i = 0; i < transformations.length; i++) {
                    int[][] nextTransformation = addBlock(transformations[i], minimalTuple(last, auth[i], group));
                    if (nextCurr.bigger(nextTransformation)) {
                        return null;
                    }
                    nextTransformations[i] = nextTransformation;
                }
                FixBS newFilter = filter.copy();
                updateFilter(group, k, last, newFilter);
                result = initiateNextTuple(nextCurr, newFilter, blockCount, k, group, nextTransformations)
                        .acceptElem(group, auth, newFilter.nextClearBit(1), k, st -> {});
            } else {
                FixBS newWhiteList = whiteList.copy();
                if (el != group.order()) {
                    for (int diff = filter.nextSetBit(0); diff >= 0 && diff < group.order(); diff = filter.nextSetBit(diff + 1)) {
                        newWhiteList.clear(group.op(diff, el));
                    }
                }
                result = new State(nextCurr, filter, newWhiteList, transformations);
            }
            return result;
        }

        private State initiateNextTuple(Design design, FixBS newFilter, int blockCount, int k, Group gr, int[][][] nextTransformations) {
            int v = gr.order() + 1;
            FixBS nextWhiteList = newFilter.copy();
            nextWhiteList.flip(1, v);
            int[][] nextDesign = Arrays.copyOf(design.design, curr.design.length + 1);
            nextDesign[curr.design.length] = new int[k];
            return new State(new Design(nextDesign, 1, curr.blocksNeeded - blockCount), newFilter, nextWhiteList, nextTransformations);
        }
    }

    private static int[][] addBlock(int[][] design, int[] block) {
        int[][] cloned = Arrays.copyOf(design, design.length + 1);
        int newIdx = design.length;
        while (newIdx > 0 && block[1] < cloned[newIdx - 1][1]) {
            newIdx--;
        }
        if (newIdx != design.length) {
            System.arraycopy(cloned, newIdx, cloned, newIdx + 1, design.length - newIdx);
        }
        cloned[newIdx] = block;
        return cloned;
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

    private static void calcCycles(Group group, int[][] auth, int k, State state, Consumer<Design> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal() + 1); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElem(group, auth, idx, k, sink);
            if (next != null) {
                calcCycles(group, auth, k, next, sink);
            }
        }
    }

    @Test
    public void toConsole() throws IOException {
        Group gr = new CyclicProduct(11, 11);
        int v = gr.order();
        int k = 5;
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "begrot.txt");
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
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "begrot.txt");
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

    private static int[][] auth(Group group) {
        int ord = group.order();
        if (!group.isCommutative()) {
            return new int[][]{IntStream.range(0, ord + 1).toArray()};
        }
        int[][] auth = group.auth();
        int[][] result = new int[auth.length][ord + 1];
        for (int i = 0; i < auth.length; i++) {
            System.arraycopy(auth[i], 0, result[i], 0, auth[i].length);
            result[i][ord] = ord;
        }
        return result;
    }

    private static void logResultsDepth(PrintStream destination, Group group, int k, List<int[][]> unProcessed) {
        System.out.println(group.name() + " " + k);
        Group table = group.asTable();
        int[][] auths = auth(group);
        System.out.println("Initial size " + unProcessed.size());
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<Design> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design.design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design.design));
            }
        };
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            int[][] design = new int[init.length][k];
            for (int i = 0; i < init.length; i++) {
                System.arraycopy(init[i], 0, design[i], 0, k);
            }
            State initial = State.forDesign(table, auths, design, k);
            calcCycles(table, auths, k, initial, designConsumer);
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
    public void logConsoleCycles() {
        Group group = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        int k = 4;
        logCycles(System.out, group, k);
    }

    @Test
    public void logFileCycles() throws IOException {
        Group group = new CyclicProduct(8, 8);
        int k = 5;
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + group.name() + "begrot.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logCycles(ps, group, k);
        }
    }

    private static void logCycles(PrintStream destination, Group group, int k) {
        System.out.println(group.name() + " " + k);
        int[][] auths = auth(group);
        Group table = group.asTable();
        int[][] design = new int[0][k];
        State initial = State.forDesign(table, auths, design, k);
        calcCycles(table, auths, k, initial, des -> {
            destination.println(Arrays.stream(des.design).map(Arrays::toString).collect(Collectors.joining(" ")));
            destination.flush();
        });
    }

    @Test
    public void filter() throws IOException {
        Group gr = new CyclicProduct(13, 13);
        int k = 7;
        int[][] auths = auth(gr);
        File refined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "ref.txt");
        File unrefined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + ".txt");
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
                System.out.println(cnt + " " + l);
                ps.println(l);
            });
        }
    }
}
