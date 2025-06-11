package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.g.OrbitConfig;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Applicator2Test {
    @Test
    public void findPossible() {
        OrbitConfig conf = new OrbitConfig(28, 4, 0, true, 3);
        System.out.println(conf + " " + conf.innerFilter() + " " + conf.outerFilter());
        Map<int[], List<int[][]>> res = conf.groupedSuitable();
        for (Map.Entry<int[], List<int[][]>> e : res.entrySet()) {
            System.out.println(Arrays.toString(e.getKey()) + " " + e.getValue().stream().map(Arrays::deepToString).collect(Collectors.joining(", ", "[", "]")));
        }
    }

    @Test
    public void calculateFile() throws IOException {
        OrbitConfig conf = new OrbitConfig(16, 4, 0, true, 3);
        ObjectMapper om = new ObjectMapper();
        File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + "all.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
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

    private static void calculate(List<int[][]> lefts, OrbitConfig conf, ChunkCallback cb) {
        System.out.println("Lefts size: " + lefts.size() + " for conf " + conf);
        Map<int[], List<int[][]>> freq = getFreq(conf);
        lefts.stream().parallel().forEach(left -> {
            int[] fr = new int[conf.k() + 1];
            for (int[] arr : left) {
                fr[arr.length]++;
            }
            for (int[][] variant : freq.get(fr)) {
                int ll = left.length;
                BiPredicate<Calc[], RightState[]> cons = (mids, rights) -> {
                    if (rights[ll - 1] == null) {
                        return false;
                    }
                    int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left[i], mids[i].block, rights[i].block.toArray()})
                            .toArray(int[][][]::new);
                    cb.onDesign(res);
                    return true;
                };
                Calc[] calcs = Arrays.stream(left).map(arr -> fromBlock(arr, conf.orbitSize())).toArray(Calc[]::new);
                Calc fstLeft = calcs[0];
                Calc[] mids = new Calc[ll];
                RightState[] rights = new RightState[ll];
                FixBS whiteList = new FixBS(conf.orbitSize());
                whiteList.set(0, conf.orbitSize());
                FixBS outerFilter = conf.outerFilter();
                for (int el : fstLeft.block()) {
                    whiteList.diffModuleShifted(outerFilter, conf.orbitSize(), conf.orbitSize() - el);
                }
                MidState state = new MidState(new IntList(conf.k()), conf.innerFilter(), outerFilter, whiteList, 0);
                if (outerFilter.isEmpty()) {
                    state = state.acceptElem(0, fstLeft, conf.orbitSize());
                }
                findMid(calcs, mids, rights, state, conf, variant, cons);
            }
            cb.onFinish(left);
        });
    }

    private static void findMid(Calc[] lefts, Calc[] mids, RightState[] rights, MidState currState, OrbitConfig conf, int[][] variant, BiPredicate<Calc[], RightState[]> cons) {
        int idx = currState.idx;
        Calc left = lefts[idx];
        int ol = conf.orbitSize();
        int leftSize = left.len;
        int midSize = currState.block().size();
        int[] freq = variant[leftSize];
        if (hasNext(freq, midSize + 1)) {
            FixBS whiteList = currState.whiteList;
            for (int el = whiteList.nextSetBit(currState.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                MidState nextState = currState.acceptElem(el, left, ol);
                findMid(lefts, mids, rights, nextState, conf, variant, cons);
            }
        }
        if (freq[midSize] > 0) {
            int[] nextFreq = freq.clone();
            nextFreq[midSize]--;
            int[][] nextVariant = variant.clone();
            nextVariant[leftSize] = nextFreq;
            Calc[] nextMids = mids.clone();
            Calc mid = fromBlock(currState.block.toArray(), ol);
            nextMids[idx] = mid;
            RightState prev = idx > 0 ? rights[idx - 1] : new RightState(null, conf.innerFilter(), conf.outerFilter(), conf.outerFilter(), null, -1);
            if (conf.k() == leftSize + midSize) {
                RightState[] nextRights = rights.clone();
                nextRights[idx] = new RightState(new IntList(0), prev.filter(), prev.leftOuterFilter(), prev.midOuterFilter(), null, idx);
                if (cons.test(nextMids, rights)) {
                    return;
                }
                int nextIdx = idx + 1;
                int[] nextLeft = lefts[nextIdx].block();
                FixBS whiteList = new FixBS(ol);
                whiteList.flip(0, ol);
                for (int el : nextLeft) {
                    whiteList.diffModuleShifted(currState.outerFilter, ol, ol - el);
                }
                MidState nextState = new MidState(new IntList(conf.k()), currState.filter, currState.outerFilter, whiteList, nextIdx);
                findMid(lefts, nextMids, nextRights, nextState, conf, variant, cons);
            } else {
                FixBS whiteList = new FixBS(ol);
                whiteList.flip(0, ol);
                for (int el : left.block()) {
                    whiteList.diffModuleShifted(prev.leftOuterFilter(), ol, ol - el);
                }
                for (int el : mid.block()) {
                    whiteList.diffModuleShifted(prev.midOuterFilter(), ol, ol - el);
                }
                RightState nextState = new RightState(new IntList(conf.k()), prev.filter(), prev.leftOuterFilter(), prev.midOuterFilter(), whiteList, idx);
                if (prev.idx() < 0 && prev.midOuterFilter().isEmpty()) {
                    nextState = nextState.acceptElem(0, left, mid, ol);
                }
                findRight(lefts, nextMids, rights, currState, nextState, conf, nextVariant, cons);
            }
        }
    }

    private static void findRight(Calc[] lefts, Calc[] mids, RightState[] rights, MidState currMid, RightState currState, OrbitConfig conf, int[][] variant, BiPredicate<Calc[], RightState[]> cons) {
        int idx = currState.idx;
        Calc left = lefts[idx];
        Calc mid = mids[idx];
        int ol = conf.orbitSize();
        if (currState.block().size() == conf.k() - left.len() - mid.len()) {
            RightState[] nextRights = rights.clone();
            nextRights[idx] = currState;
            if (cons.test(mids, nextRights)) {
                return;
            }
            int nextIdx = idx + 1;
            int[] nextLeft = lefts[nextIdx].block();
            FixBS nextWhitelist = new FixBS(ol);
            nextWhitelist.flip(0, ol);
            for (int el : nextLeft) {
                nextWhitelist.diffModuleShifted(currMid.outerFilter, ol, ol - el);
            }
            MidState nextState = new MidState(new IntList(conf.k()), currMid.filter, currMid.outerFilter, nextWhitelist, nextIdx);
            findMid(lefts, mids, nextRights, nextState, conf, variant, cons);
        } else {
            FixBS whiteList = currState.whiteList;
            for (int el = whiteList.nextSetBit(currState.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                RightState nextState = currState.acceptElem(el, left, mid, ol);
                findRight(lefts, mids, rights, currMid, nextState, conf, variant, cons);
            }
        }
    }

    private record Calc(int[] block, FixBS inv, FixBS diff, int len) {}

    private static Calc fromBlock(int[] block, int v) {
        FixBS inv = new FixBS(v);
        FixBS diff = new FixBS(v);
        for (int i : block) {
            inv.set((v - i) % v);
            for (int j : block) {
                diff.set((v + i - j) % v);
            }
        }
        return new Calc(block, inv, diff, block.length);
    }

    private static boolean hasNext(int[] freq, int from) {
        for (int i = from; i < freq.length; i++) {
            if (freq[i] > 0) {
                return true;
            }
        }
        return false;
    }

    private record MidState(IntList block, FixBS filter, FixBS outerFilter, FixBS whiteList, int idx) {
        private MidState acceptElem(int el, Calc left, int v) {
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
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newOuterFilter.orModuleShifted(left.inv(), v, invEl);
            newWhiteList.diffModuleShifted(left.diff(), v, invEl);
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new MidState(nextBlock, newFilter, newOuterFilter, newWhiteList, idx);
        }

        public int last() {
            return block.isEmpty() ? -1 : block.getLast();
        }
    }

    private record RightState(IntList block, FixBS filter, FixBS leftOuterFilter, FixBS midOuterFilter, FixBS whiteList, int idx) {
        private RightState acceptElem(int el, Calc left, Calc mid, int v) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newLeftOuterFilter = leftOuterFilter.copy();
            FixBS newMidOuterFilter = midOuterFilter.copy();
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
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newLeftOuterFilter.orModuleShifted(left.inv(), v, invEl);
            newMidOuterFilter.orModuleShifted(mid.inv(), v, invEl);
            newWhiteList.diffModuleShifted(left.diff(), v, invEl);
            newWhiteList.diffModuleShifted(mid.diff(), v, invEl);
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new RightState(nextBlock, newFilter, newLeftOuterFilter, newMidOuterFilter, newWhiteList, idx);
        }

        public int last() {
            return block.isEmpty() ? -1 : block.getLast();
        }
    }

    @Test
    public void test() {
        OrbitConfig conf = new OrbitConfig(91, 7, 0, true, 3);
        Map<int[], List<int[][]>> freq = getFreq(conf);
        System.out.println(freq);
    }

    private static Map<int[], List<int[][]>> getFreq(OrbitConfig conf) {
        int[][][] suitable = conf.suitable();
        Map<int[], List<int[][]>> freq = new TreeMap<>(Combinatorics::compareArr);
        for (int[][] tail : suitable) {
            int[] base = new int[conf.k() + 1];
            for (int[] arr : tail) {
                base[arr[0]]++;
            }
            int[][] fr = new int[conf.k() + 1][conf.k() + 1];
            for (int[] arr : tail) {
                fr[arr[0]][arr[1]]++;
            }
            freq.computeIfAbsent(base, k -> new ArrayList<>()).add(fr);
        }
        return freq;
    }
}
