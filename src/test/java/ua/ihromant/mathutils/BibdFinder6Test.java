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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BibdFinder6Test {
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
        private static State forDesign(int v, FixBS baseFilter, int[][] baseDesign, int k, int blockIdx) {
            int[][] nextDesign = baseDesign.clone();
            nextDesign[blockIdx] = new int[k];
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
            Design curr = new Design(nextDesign, 1, blockIdx);
            State state = new State(curr, filter, whiteList);
            return state.acceptElem(whiteList.nextSetBit(0), v, st -> {});
        }

        private State acceptElem(int el, int v, Consumer<State> cons) {
            Design nextCurr = curr.simpleAdd(el);
            boolean tupleFinished = nextCurr.tupleFinished();
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int[] nextTuple = nextCurr.curr();
            int idx = curr.idx;
            for (int i = 0; i < idx; i++) {
                int val = nextTuple[i];
                int diff = el - val;
                int outDiff = v + val - el;
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
            if (!tupleFinished) {
                for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear((el + diff) % v);
                }
            }
            State result = new State(nextCurr, newFilter, newWhiteList);
            if (tupleFinished) {
                if (nextCurr.lastBlock()) {
                    cons.accept(result);
                    return null;
                }
                result = result.initiateNextTuple(newFilter, v)
                        .acceptElem(newFilter.nextClearBit(1), v, st -> {});
            }
            return result;
        }

        private State initiateNextTuple(FixBS filter, int v) {
            FixBS nextWhiteList = filter.copy();
            nextWhiteList.flip(1, v);
            return new State(new Design(curr.design, 1, curr.blockIdx + 1), filter, nextWhiteList);
        }
    }

    private static void calcCycles(int v, State state, Consumer<State> sink) {
        FixBS whiteList = state.whiteList();
        for (int idx = whiteList.nextSetBit(state.curr.lastVal()); idx >= 0; idx = whiteList.nextSetBit(idx + 1)) {
            State next = state.acceptElem(idx, v, sink);
            if (next != null) {
                calcCycles(v, next, sink);
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
            Set<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
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
            State initial = State.forDesign(v, baseFilter, design, k, init.length);
            calcCycles(v, initial, designConsumer);
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
        calcCycles(v, initial, cycle -> {
            System.out.println(Arrays.deepToString(cycle.curr.design));
            System.out.flush();
        });
    }
}
