package ua.ihromant.mathutils.runner;

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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Runner {
    private record State(int[] block, FixBS filter, FixBS blackList) {
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
            State state = new State(baseDesign[blockIdx], filter, filter);
            return state.acceptElem(filter.nextClearBit(1), v, 1);
        }

        private State acceptElem(int el, int v, int idx) {
            block[idx] = el;
            FixBS newFilter = filter.copy();
            FixBS newBlackList = blackList.copy();
            int invEl = v - el;
            for (int i = 0; i < idx; i++) {
                int val = block[i];
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newBlackList.set((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= idx; j++) {
                    int nv = block[j];
                    newBlackList.set((nv + diff) % v);
                    newBlackList.set((nv + outDiff) % v);
                }
            }
            newBlackList.or_shifted(newFilter, el);
            return new State(block, newFilter, newBlackList);
        }

        private FixBS acceptLast(int el, int v, int idx) {
            FixBS newFilter = filter.copy();
            int invEl = v - el;
            for (int i = 0; i < idx; i++) {
                int val = block[i];
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
            }
            return newFilter;
        }
    }

    private static void calcCycles(int v, int k, int[][] design, State state, int idx, int blockIdx, Tst sink) {
        FixBS blackList = state.blackList();
        int[] currBlock = design[blockIdx];
        int lastVal = currBlock[idx - 1];
        boolean first = idx == 2;
        int midCnt = k - idx - 1;
        boolean last = midCnt == 0;
        int from = 0;
        int minMidSpace = 0;
        while (--midCnt >= 0) {
            from = state.filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + lastVal - minMidSpace + 1) / 2 : v - currBlock[2] + currBlock[1] - minMidSpace;
        for (int el = blackList.nextClearBit(lastVal); el >= 0 && el < max; el = blackList.nextClearBit(el + 1)) {
            if (last) {
                currBlock[idx] = el;
                if (sink.test(design, blockIdx)) {
                    continue;
                }
                FixBS newFilter = state.acceptLast(el, v, idx);
                State next = new State(design[blockIdx + 1], newFilter, newFilter)
                        .acceptElem(newFilter.nextClearBit(1), v, 1);
                calcCycles(v, k, design, next, 2, blockIdx + 1, sink);
            } else {
                State next = state.acceptElem(el, v, idx);
                calcCycles(v, k, design, next, idx + 1, blockIdx, sink);
            }
        }
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        int rest = v % (k * (k - 1));
        if (rest == k) {
            for (int i = 1; i < v; i++) {
                if (i * k % v == 0) {
                    filter.set(i);
                }
            }
        }
        if (rest == (k - 1)) {
            for (int i = 1; i < v; i++) {
                if (i * (k - 1) % v == 0) {
                    filter.set(i);
                }
            }
        }
        return filter;
    }

    public static void main(String[] args) throws IOException {
        int v = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        Group gr = new CyclicGroup(v);
        File f = new File(k + "-" + gr.name() + ".txt");
        File beg = new File(k + "-" + gr.name() + "beg.txt");
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

    @FunctionalInterface
    private interface Tst {
        boolean test(int[][] design, int idx);
    }

    private static void logResultsDepth(PrintStream destination, int v, int k, List<int[][]> unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.size());
        int blocksNeeded = v / k / (k - 1);
        FixBS baseFilter = baseFilter(v, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Tst designConsumer = (design, blockIdx) -> {
            if (blockIdx + 1 != blocksNeeded) {
                return false;
            }
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design));
            }
            return true;
        };
        AtomicInteger cnt = new AtomicInteger();
        unProcessed.stream().parallel().forEach(init -> {
            int[][] design = new int[blocksNeeded][k];
            for (int i = 0; i < init.length; i++) {
                System.arraycopy(init[i], 0, design[i], 0, k);
            }
            State initial = State.forDesign(v, baseFilter, design, k, init.length);
            calcCycles(v, k, design, initial, 2, init.length, designConsumer);
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
}
