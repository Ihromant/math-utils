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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder6Test {
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
                    //newBlackList.set((nv + diff) % v);
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
        int nextVal = currBlock[idx - 1] + 1;
        boolean first = idx == 2;
        int midCnt = k - idx - 1;
        boolean last = midCnt == 0;
        int from = 0;
        int minMidSpace = 0;
        while (--midCnt >= 0) {
            from = state.filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + nextVal - minMidSpace) / 2 : v - currBlock[2] + currBlock[1] - minMidSpace;
        for (int el = blackList.nextClearBit(nextVal); el >= 0 && el < max; el = blackList.nextClearBit(el + 1)) {
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

    private static void calcFirstCycles(int v, int k, State state, int idx, Tst sink) {
        FixBS blackList = state.blackList();
        int[] currBlock = state.block;
        int lastVal = currBlock[idx - 1];
        boolean first = idx == 2;
        int midCnt = k - idx - 1;
        int from = 0;
        int minMidSpace = 0;
        while (--midCnt >= 0) {
            from = state.filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + lastVal - minMidSpace + 1) / 2 : v - currBlock[2] + currBlock[1] - minMidSpace;
        for (int el = blackList.nextClearBit(lastVal); el >= 0 && el < max; el = blackList.nextClearBit(el + 1)) {
            State next = state.acceptElem(el, v, idx);
            int nIdx = idx + 1;
            if (sink.test(new int[][]{currBlock}, nIdx)) {
                continue;
            }
            calcFirstCycles(v, k, next, nIdx, sink);
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
            if (v % k == 0 && v % (k - 1) == 0) {
                logResultsOther(ps, v, k, set.stream().map(st -> st.stream()
                        .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
            } else {
                logResultsDepth(ps, v, k, set.stream().map(st -> st.stream()
                        .map(bs -> bs.stream().toArray()).toArray(int[][]::new)).toList());
            }
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
        int[][] base = new int[blocksNeeded][k];
        State initial = State.forDesign(v, filter, base, k, 0);
        calcCycles(v, k, base, initial, 2, 0, (design, blockIdx) -> {
            if (blockIdx + 1 != blocksNeeded) {
                return false;
            }
            System.out.println(Arrays.deepToString(design));
            return true;
        });
    }

    @Test
    public void logFirstCycles() {
        int v = 91;
        int k = 6;
        logFirstCycles(v, k, System.out);
    }

    @Test
    public void firstCyclesToFile() throws IOException {
        Group group = new CyclicGroup(225);
        int k = 8;
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + group.name() + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logFirstCycles(group.order(), k, ps);
        }
    }

    private static void logFirstCycles(int v, int k, PrintStream ps) {
        System.out.println(v + " " + k);
        FixBS filter = baseFilter(v, k);
        int blocksNeeded = v / k / (k - 1);
        int[] mul = Combinatorics.multipliers(v);
        State initial = State.forDesign(v, filter, new int[blocksNeeded][k], k, 0);
        calcFirstCycles(v, k, initial, 2, (design, idx) -> {
            int[] block = design[0];
            for (int m : mul) {
                int[] min = minimalTuple(block, m, v, idx);
                if (compare(min, block) < 0) {
                    return true;
                }
            }
            if (idx != k) {
                return false;
            }
            ps.println(Arrays.toString(block));
            return true;
        });
    }

    @Test
    public void firstCyclesToFileOther() throws IOException {
        Group group = new CyclicGroup(60);
        int k = 5;
        int v = group.order();
        if (v % k != 0 || v % (k - 1) != 0) {
            throw new IllegalArgumentException();
        }
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + group.name() + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logFirstCyclesOther(group.order(), k, ps);
        }
    }

    private static void logFirstCyclesOther(int v, int k, PrintStream ps) {
        System.out.println(v + " " + k);
        FixBS filter = new FixBS(v);
        for (int i = 1; i < v; i++) {
            if (i * k % v == 0 || i * (k - 1) % v == 0) {
                filter.set(i);
            }
        }
        int[] mul = Combinatorics.multipliers(v);
        State initial = new State(new int[k - 1], filter, filter).acceptElem(1, v, 1);
        calcFirstCycles(v, k - 1, initial, 2, (design, idx) -> {
            int[] block = design[0];
            for (int m : mul) {
                int[] min = minimalTuple(block, m, v, idx);
                if (compare(min, block) < 0) {
                    return true;
                }
            }
            if (idx != k - 1) {
                return false;
            }
            FixBS rests = new FixBS(k);
            for (int val : block) {
                rests.set(val % k);
            }
            if (rests.cardinality() != k - 1) {
                return true;
            }
            ps.println(Arrays.toString(block));
            ps.flush();
            return true;
        });
    }

    private static void logResultsOther(PrintStream destination, int v, int k, List<int[][]> unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.size());
        int blocksNeeded = v / k / (k - 1);
        FixBS baseFilter = new FixBS(v);
        for (int i = 1; i < v; i++) {
            if (i * k % v == 0 || i * (k - 1) % v == 0) {
                baseFilter.set(i);
            }
        }
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
            design[0] = new int[k - 1];
            System.arraycopy(init[0], 0, design[0], 0, k - 1);
            State initial = State.forDesign(v, baseFilter, design, k - 1, init.length);
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

    private static int compare(int[] fst, int[] snd) {
        for (int i = 0; i < fst.length; i++) {
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
        return IntStream.range(1, v).filter(m -> Combinatorics.gcd(m, v) == 1).toArray();
    }

    private static int[] minimalTuple(int[] tuple, int multiplier, int v, int len) {
        FixBS base = new FixBS(v);
        for (int i = 0; i < len; i++) {
            base.set((tuple[i] * multiplier) % v);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(0); val >= 0; val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(v);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                int diff = oVal - val;
                cnd.set(diff < 0 ? v + diff : diff);
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min.toArray();
    }

    @Test
    public void refine() throws IOException {
        Group gr = new CyclicGroup(151);
        int v = gr.order();
        int k = 6;
        int[] multipliers = multipliers(v);
        int rest = v % (k * (k - 1));
        boolean slanted = rest > 1;
        boolean fixed = rest == k - 1;
        File refined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "ref.txt");
        File unrefined = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + ".txt");
        try (FileInputStream fis = new FileInputStream(unrefined);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream(refined);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Set<List<FixBS>> unique = ConcurrentHashMap.newKeySet();
            br.lines().parallel().forEach(l -> {
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
                    int[][] minimal = des;
                    for (int m : multipliers) {
                        int[][] mapped = Arrays.stream(des).map(arr -> minimalTuple(arr, m, v, k)).toArray(int[][]::new);
                        Arrays.sort(mapped, Comparator.comparingInt(arr -> arr[1]));
                        int cmp = compare(mapped, minimal);
                        if (cmp < 0) {
                            minimal = mapped;
                            cnt = 1;
                        }
                        if (cmp == 0) {
                            cnt++;
                        }
                    }
                    if (!unique.add(Arrays.stream(minimal).map(arr -> FixBS.of(v, arr)).toList())) {
                        return;
                    }
                    int[][] base = Stream.concat(Arrays.stream(minimal), slanted ? Stream.of(
                            IntStream.concat(IntStream.range(0, fixed ? (k - 1) : k)
                                            .map(idx -> idx * gr.order() / (fixed ? (k - 1) : k)),
                            fixed ? IntStream.of(gr.order()) : IntStream.empty()).toArray()) : Stream.empty()).toArray(int[][]::new);
                    System.out.println(cnt + " " + Liner.byDiffFamily(fixed ? gr.order() + 1 : gr.order(), base).hyperbolicFreq() + " " + Arrays.deepToString(base));
                    ps.println(Arrays.deepToString(minimal));
                });
            });
        }
    }
}
