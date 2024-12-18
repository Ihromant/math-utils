package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;
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

public class BibdFinderCyclicTest {
    private static void calcCycles(Group group, int needed, FixBS filter, FixBS whiteList, int[] tuple, Consumer<int[]> sink) {
        int tl = tuple.length;
        int lastVal = tuple[tl - 1];
        if (whiteList.cardinality() < needed) {
            return;
        }
        for (int idx = whiteList.nextSetBit(lastVal + 1); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
            nextTuple[tl] = idx;
            if (needed == 1) {
                sink.accept(nextTuple);
                continue;
            }
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            for (int val : tuple) {
                int diff = group.op(idx, group.inv(val));
                int outDiff = group.op(val, group.inv(idx));
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(idx, rt));
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int nv : nextTuple) {
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear(group.op(idx, diff));
                newWhiteList.clear(group.op(idx, group.inv(diff)));
            }
            newWhiteList.clear(0, idx + 1);
            calcCycles(group, needed - 1, newFilter, newWhiteList, nextTuple, sink);
        }
    }

    private static void calcCycles(Group group, int v, int k, int prev, FixBS filter, Consumer<int[]> sink) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        int idx = whiteList.nextSetBit(prev);
        int[] arr = new int[]{0, idx};
        FixBS newWhiteList = whiteList.copy();
        FixBS newFilter = filter.copy();
        int inv = group.inv(idx);
        newWhiteList.clear(inv);
        for (int rt : group.squareRoots(idx)) {
            newWhiteList.clear(rt);
        }
        for (int rt : group.squareRoots(inv)) {
            newWhiteList.clear(rt);
        }
        newFilter.set(idx);
        newFilter.set(inv);
        for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
            newWhiteList.clear(group.op(idx, diff));
            newWhiteList.clear(group.op(idx, group.inv(diff)));
        }
        newWhiteList.clear(0, idx + 1);
        calcCycles(group, k - 2, newFilter, newWhiteList, arr, sink);
    }

    private static void allDifferenceSets(Group group, int[][] auths, int v, int k, int[][] curr, int needed, FixBS filter, Consumer<int[][]> designSink) {
        int cl = curr.length;
        int prev = cl == 0 ? 0 : filter.nextClearBit(curr[cl - 1][1] + 1);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = block;
            for (int[] auth : auths) {
                int[][] multiplied = new int[cl + 1][block.length];
                for (int i = 0; i <= cl; i++) {
                    for (int j = 0; j < block.length; j++) {
                        multiplied[i][j] = auth[nextCurr[i][j]];
                    }
                    Arrays.sort(multiplied[i]);
                }
                Arrays.sort(multiplied, Comparator.comparingInt(arr -> arr[1]));
                if (less(multiplied, nextCurr)) {
                    return;
                }
            }
            if (needed == 1) {
                designSink.accept(nextCurr);
                return;
            }
            FixBS nextFilter = filter.copy();
            for (int i = 0; i < k; i++) {
                for (int j = i + 1; j < k; j++) {
                    int l = block[j];
                    int s = block[i];
                    nextFilter.set(group.op(l, group.inv(s)));
                    nextFilter.set(group.op(s, group.inv(l)));
                }
            }
            allDifferenceSets(group, auths, v, k, nextCurr, needed - 1, nextFilter, designSink);
        };
        calcCycles(group, v, k, prev, filter, blockSink);
    }

    @Test
    // [[0, 68, 69, 105, 135, 156, 160], [0, 75, 86, 113, 159, 183, 203], [0, 80, 95, 98, 145, 158, 201], [0, 101, 134, 141, 143, 153, 182], [0, 110, 115, 132, 138, 164, 209]]
    public void byHint() {
        CyclicGroup gr = new CyclicGroup(217);
        findByHint(new int[][]{{0, 68, 69, 105, 135, 156, 160}, {0, 75, 86, 113, 159, 183, 203}}, gr, 7);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[][] hints, Group group, int k) {
        int v = group.order();
        System.out.println(v + " " + k + " " + Arrays.deepToString(hints));
        FixBS filter = baseFilter(group, k);
        int[][] auths = group.auth();
        for (int[] hint : hints) {
            for (int i = 0; i < hint.length; i++) {
                int fst = hint[i];
                for (int j = i + 1; j < hint.length; j++) {
                    int snd = hint[j];
                    filter.set(group.op(fst, group.inv(snd)));
                    filter.set(group.op(snd, group.inv(fst)));
                }
            }
        }
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        allDifferenceSets(group.asTable(), auths, v, k, hints, v / k / (k - 1) - hints.length, filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    @Test
    public void logNotEqCycles() throws IOException {
        Group group = new GroupProduct(5, 5, 7);
        int k = 7;
        File f = new File("/home/ihromant/maths/diffSets/beg", k + "-" + group.name() + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logFirstCycles(ps, group, k);
        }
    }

    @Test
    public void logConsoleCycles() {
        Group group = new GroupProduct(5, 5);
        int k = 4;
        logFirstCycles(System.out, group, k);
    }

    private static void logFirstCycles(PrintStream destination, Group group, int k) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(group, k);
        int[][] auths = group.auth();
        calcCycles(group, v, k, 0, filter, arr -> {
            for (int[] auth : auths) {
                int[] multiplied = new int[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    multiplied[i] = auth[arr[i]];
                }
                Arrays.sort(multiplied);
                if (less(multiplied, arr)) {
                    return;
                }
            }
            destination.println(Arrays.toString(arr));
            destination.flush();
        });
    }

    private static boolean less(int[] cand, int[] arr) {
        for (int i = 0; i < cand.length; i++) {
            if (arr[i] < cand[i]) {
                return false;
            }
            if (cand[i] < arr[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean less(int[][] cand, int[][] arr) {
        for (int i = 0; i < cand.length; i++) {
            int[] ca = cand[i];
            int[] aa = arr[i];
            if (less(aa, ca)) {
                return false;
            }
            if (less(ca, aa)) {
                return true;
            }
        }
        return false;
    }

    private static FixBS baseFilter(Group gr, int k) {
        int v = gr.order();
        int sqrt = (int) Math.round(Math.sqrt(k));
        FixBS filter = new FixBS(v);
        for (int i = 0; i < v; i++) {
            int ord = gr.order(i);
            if (ord == k || (sqrt * sqrt == k && ord == sqrt)) {
                filter.set(i);
            }
        }
        return filter;
    }

    @Test
    public void toConsole() throws IOException {
        Group gr = new GroupProduct(2, 2, 7);
        int v = gr.order();
        int k = 4;
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
        File f = new File("/home/ihromant/maths/diffSets/beg", k + "-" + gr.name() + ".txt");
        File beg = new File("/home/ihromant/maths/diffSets/beg", k + "-" + gr.name() + "beg.txt");
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
                if (l.length() > 40) {
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
        FixBS filter = baseFilter(group, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design));
            }
        };
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            FixBS newFilter = filter.copy();
            for (int i = 0; i < init.length; i++) {
                int fst = init[i];
                for (int j = i + 1; j < init.length; j++) {
                    int snd = init[j];
                    int diff = table.op(snd, table.inv(fst));
                    int outDiff = table.op(fst, table.inv(snd));
                    newFilter.set(diff);
                    newFilter.set(outDiff);
                }
            }
            allDifferenceSets(table, auths, v, k, new int[][]{init}, blocksNeeded - 1, newFilter, designConsumer);
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
