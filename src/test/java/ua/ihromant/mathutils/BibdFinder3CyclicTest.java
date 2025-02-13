package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static int[] add(int[] block, int idx, int el) {
        int[] unf = block.clone();
        int pos = idx;
        while (pos > 0 && unf[pos - 1] > el) {
            pos--;
        }
        System.arraycopy(unf, pos, unf, pos + 1, idx - pos);
        unf[pos] = el;
        return unf;
    }

    private static int blockCount(int[] block, int v, Group gr) {
        int ord = gr.order();
        List<FixBS> set = new ArrayList<>(ord);
        ex: for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
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

    private static Stream<int[]> blocks(int[] block, int v, Group gr) {
        int ord = gr.order();
        Set<FixBS> set = new HashSet<>(ord);
        for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(i, el));
            }
            set.add(fbs);
        }
        return set.stream().map(FixBS::toArray);
    }

    private record State(Design curr, FixBS filter, FixBS whiteList, int[][][] transformations) {
        private static State forDesign(Group group, int[][] auths, int[][] baseDesign, int v, int k) {
            int[][] nextDesign = baseDesign.clone();
            FixBS filter = new FixBS(v);
            int blocksNeeded = v * (v - 1) / k / (k - 1);
            for (int[] block : baseDesign) {
                updateFilter(group, k, block, filter);
                blocksNeeded = blocksNeeded - blockCount(block, v, group);
            }
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            Design curr = new Design(nextDesign, k, blocksNeeded);
            int[][][] transformations = Arrays.stream(auths).map(aut -> Arrays.stream(baseDesign).map(arr -> minimalTuple(arr, aut, group)).sorted(Comparator.comparingInt(arr -> arr[1] != 0 ? arr[1] : Integer.MAX_VALUE)).toArray(int[][]::new)).toArray(int[][][]::new);
            State state = new State(curr, filter, whiteList, transformations);
            return state.initiateNextTuple(curr, filter, 0, v, k, transformations).acceptElem(group, auths, v, k, whiteList.nextSetBit(0), st -> {});
        }

        private State acceptElemTrans(Group group, int[][] auth, int v, int el, Consumer<Design> cons) {
            Design nextCurr = curr.simpleAdd(el);
            boolean tupleFinished = nextCurr.tupleFinished();
            int[] last = nextCurr.curr();
            int[][][] nextTransformations = new int[transformations.length][][];
            for (int i = 0; i < transformations.length; i++) {
                int[][] nextTransformation = new int[][]{tupleFinished ? minimalTuple(last, auth[i], group) : add(transformations[i][0], curr.idx, auth[i][el])};
                if (nextCurr.bigger(nextTransformation)) {
                    return null;
                }
                nextTransformations[i] = nextTransformation;
            }
            if (tupleFinished) {
                int blockCount = blockCount(last, v, group);
                if (blockCount < 0) {
                    return null;
                }
                cons.accept(nextCurr);
                return null;
            }
            return new State(nextCurr, filter, whiteList, nextTransformations);
        }

        private State acceptElem(Group group, int[][] auth, int v, int k, int el, Consumer<Design> cons) {
            Design nextCurr = curr.simpleAdd(el);
            boolean tupleFinished = nextCurr.tupleFinished();
            int[] last = nextCurr.curr();
            State result;
            if (tupleFinished) {
                int blockCount = blockCount(last, v, group);
                if (blockCount < 0) {
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
                if (blockCount == nextCurr.blocksNeeded) {
                    cons.accept(nextCurr);
                    return null;
                }
                FixBS newFilter = filter.copy();
                updateFilter(group, k, last, newFilter);
                result = initiateNextTuple(nextCurr, newFilter, blockCount, v, k, nextTransformations)
                        .acceptElem(group, auth, v, k, newFilter.nextClearBit(1), st -> {});
            } else {
                FixBS newWhiteList = whiteList.copy();
                if (el != group.order()) {
                    for (int diff = filter.nextSetBit(0); diff >= 0 && diff < group.order(); diff = filter.nextSetBit(diff + 1)) {
                        newWhiteList.clear(group.op(el, diff));
                    }
                }
                result = new State(nextCurr, filter, newWhiteList, transformations);
            }
            return result;
        }

        private State initiateNextTuple(Design design, FixBS newFilter, int blockCount, int v, int k, int[][][] nextTransformations) {
            FixBS nextWhiteList = newFilter.copy();
            nextWhiteList.flip(1, v);
            int[][] nextDesign = Arrays.copyOf(design.design, curr.design.length + 1);
            nextDesign[curr.design.length] = new int[k];
            return new State(new Design(nextDesign, 1, curr.blocksNeeded - blockCount), newFilter, nextWhiteList, nextTransformations);
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
        if (newIdx != design.length) {
            System.arraycopy(cloned, newIdx, cloned, newIdx + 1, design.length - newIdx);
        }
        cloned[newIdx] = block;
        return cloned;
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

    private static void calcCycles(Group group, int[][] auth, int v, int k, State state, Consumer<Design> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal() + 1); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElem(group, auth, v, k, idx, sink);
            if (next != null) {
                calcCycles(group, auth, v, k, next, sink);
            }
        }
    }

    private static void calcCyclesTrans(Group group, int[][] auth, int v, State state, Consumer<Design> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal() + 1); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElemTrans(group, auth, v, idx, sink);
            if (next != null) {
                calcCyclesTrans(group, auth, v, next, sink);
            }
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

    @Test
    public void dumpInitial() throws IOException {
        int fixed = 1;
        Group group = new CyclicProduct(8, 8);
        int v = group.order() + fixed;
        int k = 5;
        int[][] auths = auth(group);
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Consumer<int[][]> cons = arr -> {
                ps.println(Arrays.stream(arr).map(Arrays::toString).collect(Collectors.joining(" ")));
                ps.flush();
            };
            getInitial(table, auths, v, k, cons);
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
            logResultsByInitial(ps, table, auths, v, k, set.stream().map(st -> st.stream()
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
        List<int[][]> base = new ArrayList<>();
        getInitial(table, auths, v, k, base::add);
        logResultsByInitial(System.out, table, auths, v, k, base, new ArrayList<>());
    }

    private static void getInitial(Group group, int[][] auths, int v, int k, Consumer<int[][]> cons) {
        Group table = group.asTable();
        int[][] design = new int[1][k];
        FixBS filter = new FixBS(v);
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        design[0][1] = 1;
        int[][][] transformations = new int[auths.length][1][k];
        for (int i = 0; i < auths.length; i++) {
            transformations[i][0][1] = auths[i][1];
        }
        State initial = new State(new Design(design, 2, Integer.MAX_VALUE), filter, whiteList, transformations);
        calcCyclesTrans(table, auths, v, initial, des -> cons.accept(des.design));
    }

    private static void logResultsByInitial(PrintStream destination, Group group, int[][] auths, int v, int k, List<int[][]> unProcessed, List<Liner> liners) {
        System.out.println("Processing initial of size " + unProcessed.size());
        long time = System.currentTimeMillis();
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            int[][] design = new int[init.length][k];
            for (int i = 0; i < init.length; i++) {
                System.arraycopy(init[i], 0, design[i], 0, k);
            }
            State initial = State.forDesign(group, auths, design, v, k);
            calcCycles(group, auths, v, k, initial, des -> {
                destination.println(Arrays.deepToString(des.design));
                destination.flush();
                if (destination != System.out) {
                    System.out.println(Arrays.deepToString(des.design));
                }
                liners.add(new Liner(v, Arrays.stream(des.design).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new)));
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
