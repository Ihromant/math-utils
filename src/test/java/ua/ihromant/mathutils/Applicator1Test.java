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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Applicator1Test {
    @Test
    public void findPossible() {
        int v = 53;
        int k = 6;
        List<IntList> res = getSuitable(v, k);
        res.forEach(System.out::println);
    }

    private static List<IntList> getSuitable(int v, int k) {
        IntList il = new IntList(v);
        List<IntList> res = new ArrayList<>();
        find(v, k, v % k == 0 ? k - 1 : 0, v % k == 0 ? k - 1 : 0, 0, il, res::add);
        return res;
    }

    private static void find(int v, int k, int left, int right, int inter, IntList lst, Consumer<IntList> cons) {
        if (left == v - 1 && right == v - 1 && inter == v) {
            cons.accept(lst);
            return;
        }
        if (left >= v || right >= v || inter > v) {
            return;
        }
        int prev = lst.isEmpty() ? 0 : lst.get(lst.size() - 1);
        for (int i = prev; i <= k; i++) {
            IntList nextLst = lst.copy();
            nextLst.add(i);
            find(v, k, left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst, cons);
        }
    }

    private record State(IntList block, FixBS filter, FixBS whiteList) {
        public static State fromBlock(int[] block, int v, int k) {
            FixBS whiteList = new FixBS(v);
            whiteList.set(1, v);
            State result = new State(new IntList(k), new FixBS(v), whiteList);
            for (int el : block) {
                result = result.acceptElem(el, v);
            }
            return result;
        }

        private State acceptElem(int el, int v) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
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
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(nextBlock, newFilter, newWhiteList);
        }

        private int size() {
            return block.size();
        }
    }

    @Test
    public void generate() throws IOException {
        int v = 48;
        int k = 6;
        int[][] suitable = getSuitable(v, k).stream().map(IntList::toArray).toArray(int[][]::new);
        for (int[] chunks : suitable) {
            File f = new File("/home/ihromant/maths/g-spaces/chunks", k + "-" + v + "-"
                    + Arrays.stream(chunks).mapToObj(Integer::toString).collect(Collectors.joining("-")) + ".txt");
            try (FileOutputStream fos = new FileOutputStream(f);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 PrintStream ps = new PrintStream(bos)) {
                generateChunks(ps, chunks, v, k);
            }
        }
    }

    private void generateChunks(PrintStream ps, int[] chunks, int v, int k) {
        System.out.println("Generate for " + v + " " + k + " " + Arrays.toString(chunks));
        int[] freq = new int[k + 1];
        for (int val : chunks) {
            if (val > 1) {
                freq[val]++;
            }
        }
        int total = Arrays.stream(freq).sum();
        int[] multipliers = Combinatorics.multipliers(v);
        IntList newBlock = new IntList(k);
        newBlock.add(0);
        FixBS filter = baseFilter(v, k);
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        List<State[]> triples = new ArrayList<>();
        searchDesigns(new State[0], freq, new State(newBlock, filter, whiteList).acceptElem(1, v), v, k, des -> {
//            FixBS[] base = Arrays.stream(des).map(st -> FixBS.of(v, st.block.toArray())).toArray(FixBS[]::new);
//            for (int mul : multipliers) {
//                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, mul, v)).sorted().toArray(FixBS[]::new))) {
//                    return true;
//                }
//            }
            if (des.length < 3) {
                return false;
            }
            triples.add(des);
            return true;
        });
        System.out.println("Triples size: " + triples.size());
        triples.stream().parallel().forEach(des -> {
            int[] rem = freq.clone();
            for (State st : des) {
                rem[st.block.size()]--;
            }
            FixBS ftr = des[des.length - 1].filter;
            FixBS whL = ftr.copy();
            whL.flip(1, v);
            IntList nwb = new IntList(k);
            nwb.add(0);
            searchDesigns(des, rem, new State(nwb, ftr, whL).acceptElem(whL.nextSetBit(0), v), v, k, finDes -> {
                if (finDes.length < total) {
                    return false;
                }
//                FixBS[] base = Arrays.stream(finDes).map(st -> FixBS.of(v, st.block.toArray())).toArray(FixBS[]::new);
//                for (int mul : multipliers) {
//                    if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, mul, v)).sorted().toArray(FixBS[]::new))) {
//                        return true;
//                    }
//                }
                ps.println(Arrays.toString(Arrays.stream(finDes).map(State::block).toArray()));
                ps.flush();
                return true;
            });
        });
    }

    private static Stream<int[]> blocks(int[] block, int v) {
        Set<FixBS> set = new HashSet<>(2 * v);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set((el + i) % v);
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    private boolean bigger(FixBS[] base, FixBS[] cand) {
        for (int i = 0; i < base.length; i++) {
            int cmp = base[i].compareTo(cand[i]);
            if (cmp < 0) {
                return false;
            }
            if (cmp > 0) {
                return true;
            }
        }
        return false;
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        if (v % k == 0) {
            for (int i = 1; i < v; i++) {
                if (i * k % v == 0) {
                    filter.set(i);
                }
            }
        }
        return filter;
    }

    private static void searchDesigns(State[] currDesign, int[] freq, State state, int v, int k, Predicate<State[]> cons) {
        IntList block = state.block;
        if (freq[state.size()] > 0) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign)) {
                return;
            }
            IntList newBlock = new IntList(k);
            newBlock.add(0);
            FixBS whiteList = state.filter.copy();
            whiteList.flip(1, v);
            State nextState = new State(newBlock, state.filter, whiteList).acceptElem(whiteList.nextSetBit(0), v);
            int[] newFreq = freq.clone();
            newFreq[state.size()]--;
            searchDesigns(nextDesign, newFreq, nextState, v, k, cons);
        }
        if (hasNext(freq, state.size() + 1)) {
            for (int el = state.whiteList.nextSetBit(block.getLast()); el >= 0; el = state.whiteList.nextSetBit(el + 1)) {
                State nextState = state.acceptElem(el, v);
                searchDesigns(currDesign, freq, nextState, v, k, cons);
            }
        }
    }

    private static boolean hasNext(int[] freq, int from) {
        for (int i = from; i < freq.length; i++) {
            if (freq[i] > 0) {
                return true;
            }
        }
        return false;
    }

    private static FixBS minimalTuple(FixBS tuple, int multiplier, int v) {
        FixBS mapped = new FixBS(v);
        for (int i = tuple.nextSetBit(0); i >= 0; i = tuple.nextSetBit(i + 1)) {
            mapped.set((i * multiplier) % v);
        }
        FixBS result = mapped;
        for (int i = mapped.nextSetBit(1); i >= 0; i = mapped.nextSetBit(i + 1)) {
            FixBS cand = new FixBS(v);
            for (int j = mapped.nextSetBit(0); j >= 0; j = mapped.nextSetBit(j + 1)) {
                int dff = i - j;
                cand.set(dff < 0 ? v + dff : dff);
            }
            if (cand.compareTo(result) < 0) {
                result = cand;
            }
        }
        return result;
    }

    private record Pair(FixBS left, FixBS right) {
        private FixBS filter(int v) {
            FixBS res = new FixBS(v);
            for (int i = left.nextSetBit(0); i >= 0; i = left.nextSetBit(i + 1)) {
                for (int j = right.nextSetBit(0); j >= 0; j = right.nextSetBit(j + 1)) {
                    int diff = j - i;
                    int val = diff < 0 ? v + diff : diff;
                    if (res.get(val)) {
                        return null;
                    }
                    res.set(val);
                }
            }
            return res;
        }

        @Override
        public String toString() {
            return "[" + left + ", " + right + "]";
        }
    }

    @Test
    public void match() throws IOException {
        int v = 48;
        int k = 6;
        List<IntList> res = getSuitable(v, k);
        int idx = 0;
        int[] base = res.get(idx).toArray();
        int[] opposite = Arrays.stream(base).map(i -> k - i).sorted().toArray();
        List<List<FixBS>> lefts = readAndEnhance(v, k, base);
        List<List<FixBS>> rights = readAndEnhance(v, k, opposite);
        int[] multipliers = Combinatorics.multipliers(v);
        System.out.println(v + " " + k + " " + Arrays.toString(base) + " " + lefts.size() + " " + Arrays.toString(opposite) + " " + rights.size());
        for (List<FixBS> left : lefts) {
            //for (int mul : multipliers) {
                for (List<FixBS> right : rights) {
//                    List<FixBS> right = bRight.stream().map(bl -> {
//                        FixBS r = new FixBS(v);
//                        for (int el = bl.nextSetBit(0); el >= 0; el = bl.nextSetBit(el + 1)) {
//                            r.set((el * mul) % v);
//                        }
//                        return r;
//                    }).toList();
                    FixBS fst = left.getFirst();
                    int lCard = fst.cardinality();
                    IntStream.range(0, right.size()).forEach(i -> {
                        FixBS snd = right.get(i);
                        if (snd.cardinality() != k - lCard) {
                            return;
                        }
                        List<FixBS> rightCp = IntStream.range(0, right.size()).filter(j -> j != i).mapToObj(right::get).toList();
                        Pair p = new Pair(fst, snd);
                        FixBS filter = p.filter(v);
                        if (filter == null) {
                            return;
                        }
                        tryMatch(left.subList(1, left.size()), rightCp, List.of(new Pair(fst, snd)), filter, v, k);
                    });
               // }
            }
        }
    }

    private static void tryMatch(List<FixBS> left, List<FixBS> right, List<Pair> pairs, FixBS filter, int v, int k) {
        if (left.isEmpty()) {
            System.out.println(pairs);
            return;
        }
        FixBS fst = left.getFirst();
        int lCard = fst.cardinality();
        IntStream.range(0, right.size()).forEach(i -> {
            FixBS snd = right.get(i);
            if (snd.cardinality() != k - lCard) {
                return;
            }
            for (int sh = 0; sh < v; sh++) {
                FixBS sndSh = new FixBS(v);
                for (int el = snd.nextSetBit(0); el >= 0; el = snd.nextSetBit(el + 1)) {
                    sndSh.set((el + sh) % v);
                }
                Pair pair = new Pair(fst, sndSh);
                FixBS ftr = pair.filter(v);
                if (ftr == null || ftr.intersects(filter)) {
                    continue;
                }
                List<FixBS> rightCp = IntStream.range(0, right.size()).filter(j -> j != i).mapToObj(right::get).toList();
                List<Pair> next = Stream.concat(pairs.stream(), Stream.of(pair)).toList();
                tryMatch(left.subList(1, left.size()), rightCp, next, filter.union(ftr), v, k);
            }
        });
    }

    private static List<List<FixBS>> readAndEnhance(int v, int k, int[] chunks) throws IOException {
        File f = new File("/home/ihromant/maths/g-spaces/chunks", k + "-" + v + "-"
                + Arrays.stream(chunks).mapToObj(Integer::toString).collect(Collectors.joining("-")) + ".txt");
        int ones = Arrays.stream(chunks).filter(i -> i == 1).sum();
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            List<List<FixBS>> result = new ArrayList<>();
            br.lines().forEach(l -> {
                String[] spl = l.substring(2, l.length() - 2).split("], \\[");
                result.add(Stream.concat(
                        Arrays.stream(spl).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())),
                        IntStream.range(0, ones).mapToObj(i -> FixBS.of(v, 0))).toList());
            });
            return result;
        }
    }

    private static Pair[] example = new Pair[]{new Pair(FixBS.of(48, 0, 1, 14, 18), FixBS.of(48, 0, 6)),
            new Pair(FixBS.of(48, 0, 2, 21, 28), FixBS.of(48, 15, 45)),
            new Pair(FixBS.of(48, 0, 3, 39), FixBS.of(48, 14, 28, 41)),
            new Pair(FixBS.of(48, 0, 5, 11), FixBS.of(48, 9, 31, 32)),
            new Pair(FixBS.of(48, 0, 10, 33), FixBS.of(48, 1, 18, 29)),
            new Pair(FixBS.of(48, 0), FixBS.of(48, 3, 7, 10, 12, 22)),
            new Pair(FixBS.of(48, 0, 8, 16, 24, 32, 40), new FixBS(48)),
            new Pair(new FixBS(48), FixBS.of(48, 0, 8, 16, 24, 32, 40))};
    //[[{0, 1, 14, 18}, {0, 6}], [{0, 2, 21, 28}, {15, 45}], [{0, 3, 39}, {14, 28, 41}], [{0, 5, 11}, {9, 31, 32}], [{0, 10, 33}, {1, 18, 29}], [{0}, {3, 7, 10, 12, 22}]]
    //[[{0, 1, 31, 35}, {0, 6}], [{0, 2, 22, 29}, {10, 40}], [{0, 3, 12}, {15, 28, 42}], [{0, 5, 42}, {1, 26, 27}], [{0, 10, 25}, {14, 34, 45}], [{0}, {2, 31, 41, 43, 46}]]

    @Test
    public void testExample() {
        int[][] lines = Arrays.stream(example).flatMap(pr -> {
            return IntStream.range(0, 48).mapToObj(sh -> {
                FixBS base = new FixBS(96);
                for (int i = pr.left.nextSetBit(0); i >= 0; i = pr.left.nextSetBit(i + 1)) {
                    base.set((i + sh) % 48);
                }
                for (int i = pr.right.nextSetBit(0); i >= 0; i = pr.right.nextSetBit(i + 1)) {
                    base.set(((i + sh) % 48) + 48);
                }
                return base;
            }).distinct().map(FixBS::toArray);
        }).toArray(int[][]::new);
        System.out.println(new Liner(96, lines).hyperbolicFreq());
    }
}
