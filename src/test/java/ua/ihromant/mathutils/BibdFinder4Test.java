package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
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
import java.util.stream.IntStream;

public class BibdFinder4Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int v, int k, int needed, FixBS filter, FixBS whiteList,
                                   int[] tuple, Consumer<int[]> sink) {
        int tl = tuple.length;
        int lastVal = tuple[tl - 1];
        int second = tuple[1];
        int unset = k - tl;
        int smallest = second - unset;
        int spaceMax = smallest * unset + unset * (unset - 1) / 2;
        int sp = v - lastVal;
        int dff = sp - spaceMax;
        int min = lastVal + Math.max(1, dff);
        int max = Math.min(v - bounds[unset], lastVal + second - (tl == 2 ? 1 : 0));
        if (tl == 2) {
            max = Math.min(max, lastVal + (sp - bounds[unset - 1]) / 2);
        } else {
            max = Math.min(max, v - tuple[2] + second - bounds[unset - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
            nextTuple[tl] = idx;
            if (needed == 1) {
                sink.accept(nextTuple);
                continue;
            }
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            for (int val : tuple) {
                int diff = idx - val;
                int outDiff = v - idx + val;
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((idx + outDiff / 2) % v);
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int nv : nextTuple) {
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear((idx + diff) % v);
            }
            calcCycles(v, k, needed - 1, newFilter, newWhiteList, nextTuple, sink);
        }
    }

    private static void calcCycles(int v, int k, int sizeNeeded, int prev, FixBS filter, int blocksNeeded, Consumer<int[]> sink) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        int cap = v - blocksNeeded * bounds[k - 1];
        for (int idx = whiteList.nextSetBit(prev); idx >= 0 && idx < cap; idx = whiteList.nextSetBit(idx + 1)) {
            int[] arr = new int[]{0, idx};
            if (sizeNeeded == 2) {
                sink.accept(arr);
                continue;
            }
            FixBS newWhiteList = whiteList.copy();
            FixBS newFilter = filter.copy();
            int rev = v - idx;
            newWhiteList.clear(rev);
            if (rev % 2 == 0) {
                newWhiteList.clear(idx + rev / 2);
            }
            newFilter.set(idx);
            newFilter.set(rev);
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear((idx + diff) % v);
            }
            calcCycles(v, k, sizeNeeded - 2, newFilter, newWhiteList, arr, sink);
        }
    }

    private static void allDifferenceSets(int v, int k, int[][] curr, int needed, FixBS filter, int[] multipliers, Consumer<int[][]> designSink) {
        int cl = curr.length;
        int prev = cl == 0 ? start(v, k) : filter.nextClearBit(curr[cl - 1][1] + 1);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = block;
            if (needed == 1) {
                designSink.accept(nextCurr);
                return;
            }
            for (int m : multipliers) {
                int[][] multiplied = new int[cl + 1][block.length];
                for (int i = 0; i <= cl; i++) {
                    for (int j = 0; j < block.length; j++) {
                        multiplied[i][j] = nextCurr[i][j] * m % v;
                    }
                    multiplied[i] = minimalTuple(multiplied[i], v);
                }
                Arrays.sort(multiplied, Comparator.comparingInt(arr -> arr[1]));
                if (less(multiplied, nextCurr)) {
                    return;
                }
            }
            FixBS nextFilter = filter.copy();
            for (int i = 0; i < k; i++) {
                for (int j = i + 1; j < k; j++) {
                    int l = block[j];
                    int s = block[i];
                    nextFilter.set(l - s);
                    nextFilter.set(v - l + s);
                }
            }
            allDifferenceSets(v, k, nextCurr, needed - 1, nextFilter, multipliers, designSink);
        };
        calcCycles(v, k, k, prev, filter, needed, blockSink);
    }

    @Test // [[0, 68, 69, 105, 135, 156, 160], [0, 75, 86, 113, 159, 183, 203], [0, 80, 95, 98, 145, 158, 201], [0, 101, 134, 141, 143, 153, 182], [0, 110, 115, 132, 138, 164, 209]]
    public void byHint() {
        findByHint(new int[][]{{0, 68, 69, 105, 135, 156, 160}, {0, 75, 86, 113, 159, 183, 203}}, 217, 7);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[][] hints, int v, int k) {
        System.out.println(v + " " + k + " " + Arrays.deepToString(hints));
        FixBS filter = baseFilter(v, k);
        for (int[] hint : hints) {
            for (int i : hint) {
                for (int j : hint) {
                    if (i >= j) {
                        continue;
                    }
                    filter.set(j - i);
                    filter.set(v - j + i);
                }
            }
        }
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        allDifferenceSets(v, k, hints, v / k / (k - 1) - hints.length, filter, multipliers(v), designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    @Test
    public void logNotEqCycles() throws IOException {
        int v = 91;
        int k = 6;
        File f = new File("/home/ihromant/maths/diffSets/beg", k + "-" + v + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logFirstCycles(ps, v, k);
        }
    }

    @Test
    public void logConsoleCycles() {
        int v = 91;
        int k = 6;
        logFirstCycles(System.out, v, k);
    }

    private static void logFirstCycles(PrintStream destination, int v, int k) {
        System.out.println(v + " " + k);
        int blocksNeeded = v / k / (k - 1);
        FixBS filter = baseFilter(v, k);
        int[] multipliers = multipliers(v);
        calcCycles(v, k, k, start(v, k), filter, blocksNeeded, arr -> {
            for (int m : multipliers) {
                int[] multiplied = new int[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    multiplied[i] = arr[i] * m % v;
                }
                int[] minimal = minimalTuple(multiplied, v);
                if (less(minimal, arr)) {
                    return;
                }
            }
            destination.println(Arrays.toString(arr));
            destination.flush();
        });
    }

    private static int[] multipliers(int v) {
        return IntStream.range(2, v).filter(m -> Combinatorics.gcd(m, v) == 1).toArray();
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

    private static int[] minimalTuple(int[] arr, int v) {
        Arrays.sort(arr);
        int l = arr.length;
        int maxIdx = l - 1;
        int last = arr[l - 1];
        int max = Math.min(last, v - last);
        for (int i = 0; i < l - 1; i++) {
            int d = arr[i + 1] - arr[i];
            int diff = Math.min(d, v - d);
            if (diff > max) {
                maxIdx = i;
                max = diff;
            }
        }
        int val = arr[maxIdx];
        int[] res = new int[l];
        for (int i = maxIdx + 1; i < l; i++) {
            res[i - maxIdx] = arr[i] - val;
        }
        for (int i = 0; i < maxIdx; i++) {
            res[i + l - maxIdx] = v + arr[i] - val;
        }
        return res;
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        if (v % k == 0) {
            IntStream.range(1, k).forEach(i -> filter.set(i * v / k));
        }
        return filter;
    }

    @Test
    public void toConsole() throws IOException {
        int v = 91;
        int k = 6;
        File beg = new File("/home/ihromant/maths/diffSets/beg", k + "-" + v + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            Set<FixBS> set = allBr.lines().map(l -> FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", "))
                    .mapToInt(Integer::parseInt).toArray())).collect(Collectors.toSet());
            logResultsDepth(System.out, v, k, set.stream().map(bs -> bs.stream().toArray()).toList());
        }
    }

    @Test
    public void toFile() throws IOException {
        int v = 91;
        int k = 6;
        File f = new File("/home/ihromant/maths/diffSets/beg", k + "-" + v + ".txt");
        File beg = new File("/home/ihromant/maths/diffSets/beg", k + "-" + v + "beg.txt");
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
            logResultsDepth(ps, v, k, set.stream().map(bs -> bs.stream().toArray()).filter(arr -> arr[1] >= 31).toList());
        }
    }

    private static void logResultsDepth(PrintStream destination, int v, int k, List<int[]> unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.size());
        int blocksNeeded = v / k / (k - 1);
        FixBS filter = baseFilter(v, k);
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
                    int diff = snd - fst;
                    int outDiff = v - snd + fst;
                    newFilter.set(diff);
                    newFilter.set(outDiff);
                }
            }
            allDifferenceSets(v, k, new int[][]{init}, blocksNeeded - 1, newFilter, multipliers(v), designConsumer);
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
