package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.ProjChar;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TranslationPlane1Test {
    @Test
    public void findBeginnings() {
        int p = 2;
        int n = 10;
        System.out.println(p + " " + n);
        int half = n / 2;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        int[] v = helper.v();
        QuickFind quickFind = new QuickFind(v.length);
        int[] idxes = new int[helper.matCount()];
        for (int i = 0; i < v.length; i++) {
            idxes[v[i]] = i;
        }
        for (int b : v) {
            if (quickFind.size(idxes[b]) > 1) {
                continue;
            }
            for (int a : helper.gl()) {
                quickFind.union(idxes[b], idxes[helper.mul(helper.mul(a, b), helper.inv(a))]);
            }
        }
        List<FixBS> components = quickFind.components();
        for (int i = 0; i < components.size(); i++) {
            System.out.println(i + " " + components.get(i).cardinality() + " " + Arrays.toString(components.get(i).stream().map(j -> v[j]).toArray()));
        }
    }

    @Test
    public void generate() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int mc = mini.cardinality();

        IntList[] orbits = readOrbits(mc);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

        FixBS first = new FixBS(sc);
        first.set(0, mc);
        FixBS second = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            second.set(i * mc);
        }
        FixBS third = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            third.set(mc * i + i);
        }
        FixBS[] base = new FixBS[mc + 1];
        base[0] = first;
        base[1] = second;
        base[2] = third;
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = TranslationPlaneTest.readKnown(mc);
        for (FixBS comp : find.components()) {
            int min = comp.nextSetBit(0);
            Callback cons = (state, subGl) -> {
                FixBS[] newBase = base.clone();
                int[] arr = state.partSpread;
                for (int i = 0; i < state.sum(); i++) {
                    FixBS ln = new FixBS(sc);
                    int a = arr[i];
                    for (int x = 1; x < mc; x++) {
                        int ax = helper.mulVec(a, x);
                        ln.set(ax * mc + x);
                    }
                    newBase[i + 3] = ln;
                }
                int[][] lines = TranslationPlaneTest.toProjective(sp, newBase);
                Liner l = new Liner(lines.length, lines);
                if (TranslationPlaneTest.isDesargues(l, mc)) {
                    System.out.println("Desargues " + Arrays.toString(arr) + " " + state);
                    return;
                }
                ProjChar chr = TranslationPlaneTest.newTranslation(counter.toString(), l, projData);
                if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                    projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                    counter.incrementAndGet();
                    System.out.println(chr);
                    System.out.println(Arrays.toString(arr) + " " + state);
                } else {
                    System.out.println("Existing " + chr.name() + " " + Arrays.toString(arr) + " " + state);
                }
            };
            int[] partSpread = new int[mini.cardinality() - 2];
            tree(helper, filterGl(helper, p), new State(orbits, partSpread, new int[]{min}, new int[]{0}, 1, 0,
                    Arrays.stream(orbits).mapToInt(IntList::size).sum()), cons);
        }
    }

    private IntList[] readOrbits(int pow) throws IOException {
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/orbits" + pow + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(line -> {
                int idx = line.indexOf('[');
                if (idx < 0) {
                    return null;
                }
                String[] split = line.substring(idx + 1, line.length() - 1).split(", ");
                return Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
            }).filter(Objects::nonNull).map(IntList::new).toArray(IntList[]::new);
        }
    }

    private static QuickFind orbitComponents(ModuloMatrixHelper helper, IntList[] orbits) {
        QuickFind find = new QuickFind(orbits.length);
        for (int i = 0; i < orbits.length; i++) {
            int first = orbits[i].get(0);
            int inv = helper.inv(first);
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j].toArray(), inv) >= 0) {
                    find.union(i, j);
                }
            }
            int added = helper.add(first, helper.unity());
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j].toArray(), added) >= 0) {
                    find.union(i, j);
                }
            }
        }
        return find;
    }

    private record State(IntList[] orbits, int[] partSpread, int[] dom, int[] rng, int len, int sum, int vCnt) {
        private FixBS possibleJumps() {
            FixBS possible = new FixBS(orbits.length);
            possible.set(0, orbits.length);
            int last = rng[len - 1];
            for (int used : dom) {
                possible.clear(used);
            }
            if (last == 1) {
                possible.clear(0, dom[len - 1]);
            }
            return possible;
        }

        private boolean cantContinue() {
            return vCnt + sum < partSpread.length;
        }

        private boolean canJump() {
            return rng[len - 1] * (orbits.length - len) + sum >= partSpread.length;
        }

        private boolean canStay() {
            if (len <= 1) {
                return true;
            }
            int last = rng[len - 1];
            int preLast = rng[len - 2];
            int diff = preLast - last;
            if (diff == 1) {
                for (int i = len - 2; i >= 0; i--) {
                    if (rng[i] != preLast) {
                        break;
                    }
                    if (dom[len - 1] < dom[i]) {
                        return false;
                    }
                }
                return true;
            }
            return diff > 0;
        }

        private State stay() {
            int[] nextRng = rng.clone();
            nextRng[len - 1]++;
            return new State(orbits, partSpread, dom, nextRng, len, sum, vCnt);
        }

        private State jump(int next) {
            IntList[] nextOrbits = orbits.clone();
            nextOrbits[dom[len - 1]] = null;
            int[] nextDom = Arrays.copyOf(dom, len + 1);
            nextDom[len] = next;
            int[] nextRng = Arrays.copyOf(rng, len + 1);
            nextRng[len] = 1;
            return new State(nextOrbits, partSpread, nextDom, nextRng, len + 1, sum, vCnt - orbits[dom[len - 1]].size());
        }

        private IntList currV() {
            return orbits[dom[len - 1]];
        }

        private State addOperatorToSpread(ModuloMatrixHelper helper, int elIdx) {
            int currOrbitIdx = dom[len - 1];
            int a = orbits[currOrbitIdx].get(elIdx);
            int[] newSpread = partSpread.clone();
            newSpread[sum] = a;
            IntList[] filteredOrbits = new IntList[orbits.length];
            int newCnt = 0;
            for (int i = 0; i < orbits.length; i++) {
                IntList oldOrbit = orbits[i];
                if (oldOrbit == null) {
                    continue;
                }
                IntList filteredOrbit = new IntList(oldOrbit.size());
                int begin = i == currOrbitIdx ? elIdx + 1 : 0;
                for (int j = begin; j < oldOrbit.size(); j++) {
                    int b = oldOrbit.get(j);
                    if (helper.hasInv(helper.sub(b, a))) {
                        filteredOrbit.add(b);
                    }
                }
                filteredOrbits[i] = filteredOrbit;
                newCnt = newCnt + filteredOrbit.size();
            }
            return new State(filteredOrbits, newSpread, dom, rng, len, sum + 1, newCnt);
        }

        private boolean isFull() {
            return partSpread.length == sum;
        }

        @Override
        public String toString() {
            return "Func{" +
                    "dom=" + Arrays.toString(dom) +
                    ", rng=" + Arrays.toString(rng) +
                    ", len=" + len + '}';
        }
    }

    private static IntList filterGl(ModuloMatrixHelper helper, int p) {
        int[] gl = helper.gl();
        IntList result = new IntList(gl.length);
        FixBS filter = new FixBS(helper.matCount());
        for (int i = 1; i < p; i++) {
            filter.set(helper.mulCff(helper.unity(), i));
        }
        for (int i : gl) {
            if (filter.get(i)) {
                continue;
            }
            for (int j = 2; j < p; j++) {
                filter.set(helper.mulCff(i, j));
            }
            result.add(i);
        }
        return result;
    }

    private static IntList filterOrbit(ModuloMatrixHelper helper, int[] partSpread, IntList orbit, int idx, int min) {
        IntList result = new IntList(orbit.size());
        ex: for (int j = 0; j < orbit.size(); j++) {
            int b = orbit.get(j);
            for (int i = 0; i < idx; i++) {
                int el = partSpread[i];
                if (b <= min || !helper.hasInv(helper.sub(b, el))) {
                    continue ex;
                }
            }
            result.add(b);
        }
        return result;
    }

    private void tree(ModuloMatrixHelper helper, IntList subGl, State state, Callback sink) {
        int idx = state.sum();
        if (state.cantContinue()) {
            return;
        }
        if (state.isFull()) {
            sink.accept(state, subGl);
            return;
        }
        boolean canJump = state.canJump();
        boolean canStay = state.canStay();
        if (canStay) {
            State baseNext = state.stay();
            FixBS filter = new FixBS(helper.matCount());
            IntList v = baseNext.currV();
            for (int j = 0; j < v.size(); j++) {
                int a = v.get(j);
                if (filter.get(a)) {
                    continue;
                }
                State next = baseNext.addOperatorToSpread(helper, j);
                IntList centralizer = new IntList(subGl.size());
                for (int i = 0; i < subGl.size(); i++) {
                    int el = subGl.get(i);
                    int invEl = helper.inv(el);
                    int prod = helper.mul(helper.mul(invEl, a), el);
                    filter.set(prod);
                    if (idx == 0) {
                        filter.set(helper.inv(prod));
                    }

                    int lMul = helper.mul(a, el);
                    int rMul = helper.mul(el, a);
                    if (lMul == rMul) {
                        centralizer.add(el);
                    }
                }
                if (centralizer.isEmpty()) {
                    treeSimple(helper, next, sink);
                } else {
                    tree(helper, centralizer, next, sink);
                }
            }
        }
        if (canJump) {
            FixBS possibleJumps = state.possibleJumps();
            for (int possible = possibleJumps.nextSetBit(0); possible >= 0; possible = possibleJumps.nextSetBit(possible + 1)) {
                State baseNext = state.jump(possible);
                IntList v = baseNext.currV();
                FixBS filter = new FixBS(helper.matCount());
                for (int j = 0; j < v.size(); j++) {
                    int a = v.get(j);
                    if (filter.get(a)) {
                        continue;
                    }
                    State next = baseNext.addOperatorToSpread(helper, j);
                    IntList centralizer = new IntList(subGl.size());
                    for (int i = 0; i < subGl.size(); i++) {
                        int el = subGl.get(i);
                        int invEl = helper.inv(el);
                        int prod = helper.mul(helper.mul(invEl, a), el);
                        filter.set(prod);
                        if (idx == 0) {
                            filter.set(helper.inv(prod));
                        }

                        int lMul = helper.mul(a, el);
                        int rMul = helper.mul(el, a);
                        if (lMul == rMul) {
                            centralizer.add(el);
                        }
                    }
                    if (centralizer.isEmpty()) {
                        treeSimple(helper, next, sink);
                    } else {
                        tree(helper, centralizer, next, sink);
                    }
                }
            }
        }
    }

    private void treeSimple(ModuloMatrixHelper helper, State state, Callback sink) {
        if (state.cantContinue()) {
            return;
        }
        if (state.isFull()) {
            sink.accept(state, new IntList(0));
            return;
        }
        boolean canJump = state.canJump();
        boolean canStay = state.canStay();
        if (canStay) {
            State baseNext = state.stay();
            IntList v = baseNext.currV();
            for (int j = 0; j < v.size(); j++) {
                State next = baseNext.addOperatorToSpread(helper, j);
                treeSimple(helper, next, sink);
            }
        }
        if (canJump) {
            FixBS possibleJumps = state.possibleJumps();
            for (int possible = possibleJumps.nextSetBit(0); possible >= 0; possible = possibleJumps.nextSetBit(possible + 1)) {
                State baseNext = state.jump(possible);
                IntList v = baseNext.currV();
                for (int j = 0; j < v.size(); j++) {
                    State next = baseNext.addOperatorToSpread(helper, j);
                    treeSimple(helper, next, sink);
                }
            }
        }
    }

    @FunctionalInterface
    private interface Callback {
        void accept(State state, IntList subGl);
    }

    @Test
    public void dumpGl() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/gl-" + p + "^" + n + ".txt");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            for (int el : helper.gl()) {
                ps.println(el + "=" + helper.inv(el));
            }
        }
    }

    private ModuloMatrixHelper readGl(int p, int n) throws IOException {
        if (p != 2) {
            return ModuloMatrixHelper.of(p, n);
        }
        int matCount = LinearSpace.pow(p, n * n);
        int[] mapGl = new int[matCount];
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/gl-" + p + "^" + n + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            br.lines().forEach(ln -> {
                String[] sp = ln.split("=");
                mapGl[Integer.parseInt(sp[0])] = Integer.parseInt(sp[1]);
            });
        }
        return new TwoMatrixHelper(n, mapGl);
    }

    @Test
    public void generateBegins() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);

        IntList[] orbits = readOrbits(pow);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

        File f = new File("/home/ihromant/maths/trans/", "begins-" + p + "^" + n + "x.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            for (FixBS comp : find.components()) {
                int min = comp.nextSetBit(0);
                Callback cons = (state, subGl) -> {
                    int[] arr = state.partSpread();
                    ps.println(Arrays.toString(Arrays.copyOf(arr, state.sum())) + " " + Arrays.toString(state.dom)
                            + " " + Arrays.toString(state.rng));
                    ps.flush();
                };
                int[] partSpread = new int[pow - 2];
                tree(helper, filterGl(helper, p), new State(orbits, partSpread, new int[]{min}, new int[]{0}, 1, 0,
                        Arrays.stream(orbits).mapToInt(IntList::size).sum()), cons);
            }
        }
    }

    @Test
    public void generateByBegins() throws IOException {
        String suffix = "breed";
        int p = 2;
        int n = 10;
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int mc = mini.cardinality();

        IntList[] orbits = readOrbits(mc);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

        FixBS first = new FixBS(sc);
        first.set(0, mc);
        FixBS second = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            second.set(i * mc);
        }
        FixBS third = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            third.set(mc * i + i);
        }
        FixBS[] base = new FixBS[mc + 1];
        base[0] = first;
        base[1] = second;
        base[2] = third;
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = TranslationPlaneTest.readKnown(mc);
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/begins-" + p + "^" + n + suffix + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/begins-" + p + "^" + n + suffix + "processed.txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream pris = new FileInputStream("/home/ihromant/maths/trans/begins-" + p + "^" + n + suffix + "processed.txt");
             InputStreamReader prisr = new InputStreamReader(pris);
             BufferedReader prbr = new BufferedReader(prisr)) {
            Set<List<Integer>> processed = new HashSet<>();
            prbr.lines().forEach(line -> processed.add(
                    Arrays.stream(line.substring(1, line.length() - 1).split(", ")).map(Integer::parseInt).toList()));
            int[][][] starts = br.lines().<int[][]>mapMulti((line, sink) -> {
                String[] split = line.substring(1, line.length() - 1).split("] \\[");
                int[] spr = Arrays.stream(split[0].split(", ")).mapToInt(Integer::parseInt).toArray();
                int[] dom = Arrays.stream(split[1].split(", ")).mapToInt(Integer::parseInt).toArray();
                int[] rng = Arrays.stream(split[2].split(", ")).mapToInt(Integer::parseInt).toArray();
                if (processed.contains(Arrays.stream(spr).boxed().toList())) {
                    return;
                }
                int[][] res = new int[3][];
                res[0] = spr;
                res[1] = dom;
                res[2] = rng;
                sink.accept(res);
            }).toArray(int[][][]::new);
            System.out.println("Remaining " + starts.length);
            Arrays.stream(starts).parallel().forEach(start -> {
                int[] spt = start[0];
                int[] dom = start[1];
                int[] rng = start[2];
                int[] partSpread = new int[mini.cardinality() - 2];
                int sz = spt.length;
                System.arraycopy(spt, 0, partSpread, 0, sz);
                FixBS nulls = new FixBS(orbits.length);
                int li = dom.length - 1;
                for (int i = 0; i < li; i++) {
                    nulls.set(dom[i]);
                }
                int last = dom[li];
                IntList[] filteredOrbits = new IntList[orbits.length];
                for (int idx = 0; idx < filteredOrbits.length; idx++) {
                    if (nulls.get(idx)) {
                        continue;
                    }
                    filteredOrbits[idx] = filterOrbit(helper, partSpread, orbits[idx], sz, idx == last ? spt[sz - 1] : 0);
                }
                State st = new State(filteredOrbits, partSpread, dom, rng, dom.length, sz,
                        Arrays.stream(filteredOrbits).filter(Objects::nonNull).mapToInt(IntList::size).sum());
                Callback cons = (state, subGl) -> {
                    FixBS[] newBase = base.clone();
                    int[] arr = state.partSpread();
                    for (int i = 0; i < arr.length; i++) {
                        FixBS ln = new FixBS(sc);
                        int a = arr[i];
                        for (int x = 1; x < mc; x++) {
                            int ax = helper.mulVec(a, x);
                            ln.set(ax * mc + x);
                        }
                        newBase[i + 3] = ln;
                    }
                    int[][] lines = TranslationPlaneTest.toProjective(sp, newBase);
                    Liner l = new Liner(lines.length, lines);
                    if (TranslationPlaneTest.isDesargues(l, mc)) {
                        System.out.println("Desargues " + Arrays.toString(arr) + " " + state);
                        return;
                    }
                    ProjChar chr = TranslationPlaneTest.newTranslation(counter.toString(), l, projData);
                    if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                        projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                        counter.incrementAndGet();
                        System.out.println(chr);
                        System.out.println(Arrays.toString(arr) + " " + state);
                    } else {
                        System.out.println("Existing " + chr.name() + " " + Arrays.toString(arr) + " " + state);
                    }
                };
                treeSimple(helper, st, cons);
                ps.println(Arrays.toString(start[0]));
                ps.flush();
            });
        }
    }

    @Test
    public void breed() throws IOException {
        int p = 2;
        int n = 10;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);

        IntList[] orbits = readOrbits(pow);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/begins-" + p + "^" + n + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/begins-" + p + "^" + n + "breed.txt");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            int[][][] starts = br.lines().<int[][]>mapMulti((line, sink) -> {
                String[] split = line.substring(1, line.length() - 1).split("] \\[");
                int[] spr = Arrays.stream(split[0].split(", ")).mapToInt(Integer::parseInt).toArray();
                int[] dom = Arrays.stream(split[1].split(", ")).mapToInt(Integer::parseInt).toArray();
                int[] rng = Arrays.stream(split[2].split(", ")).mapToInt(Integer::parseInt).toArray();
                if (spr.length != 4) {
                    return;
                }
                int[][] res = new int[3][];
                res[0] = spr;
                res[1] = dom;
                res[2] = rng;
                sink.accept(res);
            }).toArray(int[][][]::new);
            System.out.println("To breed " + starts.length);
            Arrays.stream(starts).forEach(start -> {
                int[] spt = start[0];
                int[] dom = start[1];
                int[] rng = start[2];
                int[] partSpread = new int[pow - 2];
                int sz = spt.length;
                System.arraycopy(spt, 0, partSpread, 0, sz);
                FixBS nulls = new FixBS(orbits.length);
                int li = dom.length - 1;
                for (int i = 0; i < li; i++) {
                    nulls.set(dom[i]);
                }
                int last = dom[li];
                IntList[] filteredOrbits = IntStream.range(0, orbits.length).mapToObj(idx -> {
                    if (nulls.get(idx)) {
                        return null;
                    }
                    return filterOrbit(helper, partSpread, orbits[idx], sz, idx == last ? spt[sz - 1] : 0);
                }).toArray(IntList[]::new);
                State st = new State(filteredOrbits, partSpread, dom, rng, dom.length, sz,
                        Arrays.stream(filteredOrbits).filter(Objects::nonNull).mapToInt(IntList::size).sum());
                Callback cons = (state, subGl) -> {
                    int[] arr = state.partSpread();
                    ps.println(Arrays.toString(Arrays.copyOf(arr, state.sum())) + " " + Arrays.toString(state.dom)
                            + " " + Arrays.toString(state.rng));
                    ps.flush();
                };
                treeSimple(helper, st, cons);
            });
        }
    }
}
