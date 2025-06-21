package ua.ihromant.mathutils.runner;

import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.Liner;
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
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Runner1 {
    private record ArrWrap(int[][] arr) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[][] arr1))) return false;

            return Arrays.deepEquals(arr, arr1);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(arr);
        }
    }

    public static void main(String[] args) throws IOException {
        OrbitConfig conf = new OrbitConfig(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        ObjectMapper om = new ObjectMapper();
        File f = new File(conf + "all.txt");
        File beg = new File(conf + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<ArrWrap> set = allBr.lines().map(s -> new ArrWrap(om.readValue(s, int[][].class))).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    int[][][] design = om.readValue(l, int[][][].class);
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                } else {
                    set.remove(new ArrWrap(om.readValue(l, int[][].class)));
                }
            });
            AtomicInteger ai = new AtomicInteger();
            ChunkCallback cb = new ChunkCallback() {
                @Override
                public void onDesign(int[][][] design) {
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                    ps.println(Arrays.deepToString(design));
                    ps.flush();
                }

                @Override
                public void onFinish(int[][] chunk) {
                    ps.println(Arrays.deepToString(chunk));
                    ps.flush();
                    int val = ai.incrementAndGet();
                    if (val % 100 == 0) {
                        System.out.println(val);
                    }
                }
            };
            calculate(set.stream().map(ArrWrap::arr).collect(Collectors.toList()), conf, cb);
        }
    }

    private interface ChunkCallback {
        void onDesign(int[][][] design);
        void onFinish(int[][] chunk);
    }

    private static void calculate(List<int[][]> lefts, OrbitConfig conf, ChunkCallback cb) {
        System.out.println("Lefts size: " + lefts.size() + " for conf " + conf);
        lefts.stream().parallel().forEach(left -> {
            int ll = left.length;
            Predicate<RightState[]> cons = arr -> {
                if (arr[ll - 1] == null) {
                    return false;
                }
                int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left[i], arr[i].block.toArray()}).toArray(int[][][]::new);
                cb.onDesign(res);
                return true;
            };
            LeftCalc[] calcs = Arrays.stream(left).map(arr -> fromBlock(arr, conf.orbitSize())).toArray(LeftCalc[]::new);
            LeftCalc fstLeft = calcs[0];
            RightState[] rights = new RightState[ll];
            FixBS whiteList = new FixBS(conf.orbitSize());
            whiteList.set(0, conf.orbitSize());
            FixBS outerFilter = conf.outerFilter();
            for (int el : fstLeft.block()) {
                whiteList.diffModuleShifted(outerFilter, conf.orbitSize(), conf.orbitSize() - el);
            }
            RightState state = new RightState(new IntList(conf.k()), conf.innerFilter(), outerFilter, whiteList, 0);
            if (outerFilter.isEmpty()) {
                state = state.acceptElem(0, fstLeft, conf.orbitSize());
            }
            find(calcs, rights, state, conf, cons);
            cb.onFinish(left);
        });
    }

    private static void find(LeftCalc[] lefts, RightState[] rights, RightState currState, OrbitConfig conf, Predicate<RightState[]> cons) {
        int idx = currState.idx;
        LeftCalc left = lefts[idx];
        int ol = conf.orbitSize();
        if (currState.block().size() == conf.k() - left.len()) {
            RightState[] nextDesign = rights.clone();
            nextDesign[idx] = currState;
            if (cons.test(nextDesign)) {
                return;
            }
            int nextIdx = idx + 1;
            int[] nextLeft = lefts[nextIdx].block();
            FixBS nextWhitelist = new FixBS(ol);
            nextWhitelist.flip(0, ol);
            for (int el : nextLeft) {
                nextWhitelist.diffModuleShifted(currState.outerFilter, ol, ol - el);
            }
            RightState nextState = new RightState(new IntList(conf.k()), currState.filter, currState.outerFilter, nextWhitelist, nextIdx);
            find(lefts, nextDesign, nextState, conf, cons);
        } else {
            FixBS whiteList = currState.whiteList;
            for (int el = whiteList.nextSetBit(currState.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                RightState nextState = currState.acceptElem(el, left, ol);
                find(lefts, rights, nextState, conf, cons);
            }
        }
    }

    private record LeftCalc(int[] block, FixBS inv, FixBS diff, int len) {}

    private static LeftCalc fromBlock(int[] block, int v) {
        FixBS inv = new FixBS(v);
        FixBS diff = new FixBS(v);
        for (int i : block) {
            inv.set((v - i) % v);
            for (int j : block) {
                diff.set((v + i - j) % v);
            }
        }
        return new LeftCalc(block, inv, diff, block.length);
    }

    private record RightState(IntList block, FixBS filter, FixBS outerFilter, FixBS whiteList, int idx) {
        private RightState acceptElem(int el, LeftCalc left, int v) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newOuterFilter = outerFilter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < sz; i++) {
                int val = nextBlock.get(i);
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = nextBlock.get(j);
                    //newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newOuterFilter.orModuleShifted(left.inv(), v, invEl);
            newWhiteList.diffModuleShifted(left.diff(), v, invEl);
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new RightState(nextBlock, newFilter, newOuterFilter, newWhiteList, idx);
        }

        public int last() {
            return block.isEmpty() ? -1 : block.getLast();
        }
    }

    private record OrbitConfig(int v, int k, int traceLength) {
        public OrbitConfig {
            if ((v - 1) % (k - 1) != 0 || (v * v - v) % (k * k - k) != 0) {
                throw new IllegalArgumentException();
            }
            int ol = v / 2;
            if (ol % 2 == 0 && (traceLength == 0 || traceLength % 2 != 0)) {
                throw new IllegalArgumentException();
            }
            if (traceLength != 0) {
                if (ol % traceLength != 0) {
                    throw new IllegalArgumentException();
                }
                int div = k / traceLength;
                if (k % 2 == 1 && (k % traceLength != 1 || div != 1 && div != 2)) {
                    throw new IllegalArgumentException();
                }
            }
        }

        public FixBS innerFilter() {
            FixBS filter = new FixBS(orbitSize());
            if (traceLength != 0) {
                for (int i = 1; i < orbitSize(); i++) {
                    if (i * traceLength % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
                if ((k == traceLength * 2 || k == traceLength) && infinity()) {
                    for (int i = 1; i < orbitSize(); i++) {
                        if (i * (k - 1) % orbitSize() == 0) {
                            filter.set(i);
                        }
                    }
                }
            }
            if (traceLength == 0 && infinity()) {
                for (int i = 1; i < orbitSize(); i++) {
                    if (i * (k - 1) % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
            }
            return filter;
        }

        public FixBS outerFilter() {
            FixBS filter = new FixBS(orbitSize());
            if (traceLength != 0 && k / traceLength != 1) {
                for (int i = 0; i < orbitSize(); i++) {
                    if (i * traceLength % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
            }
            return filter;
        }

        public int orbitSize() {
            return v / 2;
        }

        public boolean infinity() {
            return v % 2 == 1;
        }

        @Override
        public String toString() {
            return traceLength == 0 ? v + "-" + k : v + "-" + k + "-" + traceLength;
        }

        public Liner fromChunks(int[][][] chunks) {
            Set<BitSet> result = new HashSet<>();
            int ol = orbitSize();
            if (v % 2 == 1) {
                int div = k / traceLength;
                boolean exact = div * traceLength == k;
                if (div == 1) {
                    for (int i = 0; i < ol; i++) {
                        BitSet lBlock = new BitSet(v);
                        BitSet rBlock = new BitSet(v);
                        if (!exact) {
                            lBlock.set(v - 1);
                            rBlock.set(v - 1);
                        }
                        for (int j = 0; j < traceLength; j++) {
                            int sh = (j * ol / traceLength + i) % ol;
                            lBlock.set(sh);
                            rBlock.set(sh + ol);
                        }
                        result.add(lBlock);
                        result.add(rBlock);
                    }
                }
                if (div == 2) {
                    for (int i = 0; i < ol; i++) {
                        BitSet block = new BitSet(v);
                        if (!exact) {
                            block.set(v - 1);
                        }
                        for (int j = 0; j < traceLength; j++) {
                            int sh = (j * ol / traceLength + i) % ol;
                            block.set(sh);
                            block.set(sh + ol);
                        }
                        result.add(block);
                    }
                }
                if (exact) {
                    for (int i = 0; i < ol; i++) {
                        BitSet lBlock = new BitSet(v);
                        BitSet rBlock = new BitSet(v);
                        lBlock.set(v - 1);
                        rBlock.set(v - 1);
                        for (int j = 0; j < k - 1; j++) {
                            int sh = (j * ol / (k - 1) + i) % ol;
                            lBlock.set(sh);
                            rBlock.set(sh + ol);
                        }
                        result.add(lBlock);
                        result.add(rBlock);
                    }
                }
            } else {
                if (traceLength != 0) {
                    if (traceLength == k) {
                        for (int i = 0; i < ol; i++) {
                            BitSet lBlock = new BitSet(v);
                            BitSet rBlock = new BitSet(v);
                            for (int j = 0; j < traceLength; j++) {
                                int sh = (j * ol / traceLength + i) % ol;
                                lBlock.set(sh);
                                rBlock.set(sh + ol);
                            }
                            result.add(lBlock);
                            result.add(rBlock);
                        }
                    }
                    if (traceLength == k / 2) {
                        for (int i = 0; i < ol; i++) {
                            BitSet block = new BitSet(v);
                            for (int j = 0; j < traceLength; j++) {
                                int sh = (j * ol / traceLength + i) % ol;
                                block.set(sh);
                                block.set(sh + ol);
                            }
                            result.add(block);
                        }
                    }
                }
            }
            for (int[][] chunk : chunks) {
                int[] left = chunk[0];
                int[] right = chunk[1];
                for (int i = 0; i < ol; i++) {
                    BitSet block = new BitSet(v);
                    for (int l : left) {
                        block.set((l + i) % ol);
                    }
                    for (int r : right) {
                        block.set((r + i) % ol + ol);
                    }
                    result.add(block);
                }
            }
            return new Liner(result.toArray(BitSet[]::new));
        }
    }
}
