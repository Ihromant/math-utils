package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
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

public class BibdFinder6Test {
    private record State(int[][] design, FixBS filter, FixBS whiteList) {
        private static State forDesign(int v, FixBS baseFilter, int[][] baseDesign, int k, int blockIdx) {
            FixBS filter = baseFilter.copy();
            for (int bi = 0; bi < blockIdx; bi++) {
                int[] block = baseDesign[bi];
                for (int i = 0; i < k; i++) {
                    int f = block[i];
                    for (int j = i + 1; j < k; j++) {
                        int s = block[j];
                        filter.set(v + f - s);
                        filter.set(s - f);
                    }
                }
            }
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            State state = new State(baseDesign, filter, whiteList);
            return state.acceptElem(whiteList.nextSetBit(0), v, false, 1, blockIdx, st -> {});
        }

        private State acceptElem(int el, int v, boolean tupleFinished, int idx, int blockIdx, Consumer<State> cons) {
            int[][] cloned = design.clone();
            int[] nextTuple = cloned[blockIdx].clone();
            nextTuple[idx] = el;
            cloned[blockIdx] = nextTuple;
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < idx; i++) {
                int val = nextTuple[i];
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (tupleFinished) {
                    continue;
                }
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= idx; j++) {
                    int nv = nextTuple[j];
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            State result = new State(cloned, newFilter, newWhiteList);
            if (tupleFinished) {
                int nextBlockIdx = blockIdx + 1;
                if (nextBlockIdx == cloned.length) {
                    cons.accept(result);
                    return null;
                }
                result = result.initiateNextTuple(newFilter, v)
                        .acceptElem(newFilter.nextClearBit(1), v, false, 1, nextBlockIdx, st -> {});
            } else {
                newWhiteList.diffModuleShifted(newFilter, v, invEl);
            }
            return result;
        }

        private State initiateNextTuple(FixBS filter, int v) {
            FixBS nextWhiteList = filter.copy();
            nextWhiteList.flip(1, v);
            return new State(design, filter, nextWhiteList);
        }
    }

    private static void calcCycles(int v, int k, State state, int idx, int blockIdx, Consumer<State> sink) {
        FixBS whiteList = state.whiteList();
        int[] currBlock = state.design[blockIdx];
        int lastVal = currBlock[idx - 1];
        boolean first = idx == 2;
        boolean last = idx + 1 == k;
        int midCnt = k - idx - 1;
        int from = 0;
        int minMidSpace = 0;
        while (--midCnt >= 0) {
            from = state.filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + lastVal - minMidSpace + 1) / 2 : v - currBlock[2] + currBlock[1] - minMidSpace;
        for (int el = whiteList.nextSetBit(lastVal); el >= 0 && el < max; el = whiteList.nextSetBit(el + 1)) {
            State next = state.acceptElem(el, v, last, idx, blockIdx, sink);
            if (next != null) {
                calcCycles(v, k, next, last ? 2 : idx + 1, last ? blockIdx + 1 : blockIdx, sink);
            }
        }
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        for (int i = 1; i < v; i++) {
            if (i * k % v == 0) {
                filter.set(i);
            }
        }
        return filter;
    }

    @Test
    public void toConsole() throws IOException {
        Group gr = new CyclicGroup(91);
        int v = gr.order();
        int k = 6;
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            List<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).toList();
            logResultsDepth(System.out, v, k, set.stream().map(st -> st.stream()
                    .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
        }
    }

    @Test
    public void toFile() throws IOException {
        Group gr = new CyclicGroup(156);
        int v = gr.order();
        int k = 6;
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
            logResultsDepth(ps, v, k, set.stream().map(st -> st.stream()
                    .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
        }
    }

    private static List<FixBS> readPartial(String line, int v) {
        String[] sp = line.substring(1, line.length() - 1).split("] \\[");
        return Arrays.stream(sp).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())).collect(Collectors.toList());
    }

    private static void logResultsDepth(PrintStream destination, int v, int k, List<int[][]> unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.size());
        int blocksNeeded = v / k / (k - 1);
        FixBS baseFilter = baseFilter(v, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<State> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design.design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design.design));
            }
        };
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            int[][] design = new int[blocksNeeded][k];
            for (int i = 0; i < init.length; i++) {
                System.arraycopy(init[i], 0, design[i], 0, k);
            }
            State initial = State.forDesign(v, baseFilter, design, k, init.length);
            calcCycles(v, k, initial, 2, init.length, designConsumer);
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
        Group group = new CyclicGroup(91);
        int k = 6;
        logAllCycles(group, k);
    }

    private static void logAllCycles(Group group, int k) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(v, k);
        int blocksNeeded = v / k / (k - 1);
        int[][] design = new int[blocksNeeded][k];
        State initial = State.forDesign(v, filter, design, k, 0);
        calcCycles(v, k, initial, 2, 0, cycle -> {
            System.out.println(Arrays.deepToString(cycle.design));
            System.out.flush();
        });
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

    private int compare(int[][] design, int[][] candidate) {
        for (int i = 0; i < design.length; i++) {
            int cmp = compare(design[i], candidate[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int[] multipliers(int v) {
        return IntStream.range(1, v).filter(m -> Group.gcd(m, v) == 1).toArray();
    }

    private static int[] minimalTuple(int[] tuple, int multiplier, int v, int k) {
        int[] arr = new int[k];
        int minDiff = Integer.MAX_VALUE;
        for (int j = 1; j < k; j++) {
            int mapped = (tuple[j] * multiplier) % v;
            arr[j] = mapped;
            if (mapped < minDiff) {
                minDiff = mapped;
            }
        }
        int[] min = arr;
        for (int j = 1; j < k; j++) {
            int[] cnd = new int[k];
            for (int i = 0; i < k; i++) {
                if (i == j) {
                    continue;
                }
                int diff = arr[i] - arr[j];
                diff = diff < 0 ? v + diff : diff;
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

    @Test
    public void refine() throws IOException {
        Group gr = new CyclicGroup(126);
        int v = gr.order();
        int k = 6;
        int[] multipliers = multipliers(v);
        File refined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "ref.txt");
        File unrefined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "nf.txt");
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
                int[][] bDes = Arrays.stream(sp).map(pt -> Arrays.stream(pt.split(", ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                int pow = 1 << bDes.length;
                IntStream.range(0, pow).forEach(i -> {
                    int[][] des = IntStream.range(0, bDes.length).mapToObj(j -> {
                        boolean keep = (i & (1 << j)) == 0;
                        int[] base = bDes[j];
                        if (keep) {
                            return base;
                        }
                        int[] res = new int[k];
                        res[1] = base[1];
                        for (int idx = 2; idx < k; idx++) {
                            res[k - idx + 1] = base[1] + v - base[idx];
                        }
                        return res;
                    }).toArray(int[][]::new);
                    int cnt = 0;
                    for (int m : multipliers) {
                        int[][] mapped = Arrays.stream(des).map(arr -> minimalTuple(arr, m, v, k)).toArray(int[][]::new);
                        Arrays.sort(mapped, Comparator.comparingInt(arr -> arr[1]));
                        int cmp = compare(des, mapped);
                        if (cmp > 0) {
                            return;
                        }
                        if (cmp == 0) {
                            cnt++;
                        }
                    }
                    System.out.println(cnt + " " + Arrays.deepToString(des));
                    ps.println(Arrays.deepToString(des));
                });
            });
        }
    }
}
