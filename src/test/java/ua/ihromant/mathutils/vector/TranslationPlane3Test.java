package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.Graph;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class TranslationPlane3Test {
    private static final int AZZA = 0;
    private static final int ZAAZ = 1;
    private static final int AAAZ = 2;
    private static final int AAZA = 3;
    private static final int AZAA = 4;
    private static final int ZAAA = 5;

    @Test
    public void translationPlanes() throws IOException {
        int p = 2;
        int n = 4;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int[] stabilizers = suitableOperators(helper);
        int r = LinearSpace.pow(p, n) - 2;
        int[] init = helper.v();
        List<int[]> bases = new ArrayList<>();
        findBases(helper, stabilizers, init, new int[0], (s, stab) -> {
            if (stab.length >= helper.p() && s.length < r) {
                return false;
            }
            bases.add(s);
            return true;
        });
        System.out.println(bases.size() + " " + bases.stream().collect(Collectors.groupingBy(l -> l.length, Collectors.counting())));
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        bases.parallelStream().forEach(arr -> {
            if (arr.length == r) {
                Liner lnr = toAffine(helper, arr);
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
                return;
            }
            int[] left = Arrays.stream(init).filter(i -> Arrays.stream(arr).allMatch(j -> helper.hasInv(helper.sub(i, j)))).toArray();
            if (left.length == 0) {
                return;
            }
            Graph g = Graph.by(left, (a, b) -> helper.hasInv(helper.sub(a, b)));
            JNauty.instance().maximalCliques(g, r - arr.length, a -> {
                FixBS fbs = new FixBS(a);
                Liner lnr = toAffine(helper, IntStream.concat(Arrays.stream(arr), Arrays.stream(fbs.toArray()).map(i -> left[i])).toArray());
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
            });
        });
    }

    private static Liner toAffine(ModuloMatrixHelper helper, int[] base) {
        int n = helper.n();
        int pow = LinearSpace.pow(helper.p(), n);
        LinearSpace sp = LinearSpace.of(helper.p(), 2 * helper.n());
        FixBS hor = FixBS.of(sp.cardinality(), IntStream.range(0, pow).toArray());
        List<FixBS> lns = new ArrayList<>(sp.cosets(hor));
        FixBS ver = FixBS.of(sp.cardinality(), IntStream.range(0, pow).map(i -> i * pow).toArray());
        lns.addAll(sp.cosets(ver));
        FixBS diag = FixBS.of(sp.cardinality(), IntStream.range(0, pow).map(i -> i * pow + i).toArray());
        lns.addAll(sp.cosets(diag));
        for (int op : base) {
            FixBS set = new FixBS(sp.cardinality());
            for (int i = 0; i < pow; i++) {
                set.set(helper.mulVec(op, i) * pow + i);
            }
            lns.addAll(sp.cosets(set));
        }
        return new Liner(lns.toArray(FixBS[]::new));
    }

    private static int[] suitableOperators(ModuloMatrixHelper helper) {
        boolean two = helper.p() == 2;
        int cap = two ? 6 : 2;
        int sh = helper.n() * helper.n();
        int[] result = new int[cap * helper.gl().length];
        int cnt = 0;
        for (int cf = 0; cf < cap; cf++) {
            for (int op : helper.gl()) {
                result[cnt++] = two ? (cf << sh) | op : helper.matCount() * cf + op;
            }
        }
        return result;
    }

    private static int apply(ModuloMatrixHelper helper, int op, int b) {
        int sh = helper.n() * helper.n();
        int a;
        int act;
        if (helper.p() == 2) {
            int mask = (1 << sh) - 1;
            a = op & mask;
            act = op >>> sh;
        } else {
            a = op % helper.matCount();
            act = op / helper.matCount();
        }
        int mid = switch (act) {
            case AZZA -> b;
            case ZAAZ -> helper.inv(b);
            case AAAZ -> helper.inv(helper.add(b, helper.unity()));
            case AAZA -> helper.mul(b, helper.inv(helper.add(b, helper.unity())));
            case AZAA -> helper.add(b, helper.unity());
            case ZAAA -> helper.mul(helper.add(b, helper.unity()), helper.inv(b));
            default -> throw new IllegalArgumentException();
        };
        return helper.mul(helper.mul(a, mid), helper.inv(a));
    }

    private static void findBases(ModuloMatrixHelper helper, int[] stab, int[] transversal, int[] curr, BiPredicate<int[], int[]> cons) {
        if (cons.test(curr, stab)) {
            return;
        }
        IntList minimals = new IntList(transversal.length);
        if (transversal.length < 16384) {
            ex: for (int tr : transversal) {
                for (int st : stab) {
                    int mapped = apply(helper, st, tr);
                    if (mapped < tr) {
                        continue ex;
                    }
                }
                minimals.add(tr);
            }
        } else {
            IntStream.of(transversal).parallel().forEach(tr -> {
                for (int st : stab) {
                    int mapped = apply(helper, st, tr);
                    if (mapped < tr) {
                        return;
                    }
                }
                synchronized (minimals) {
                    minimals.add(tr);
                }
            });
        }
        if (minimals.size() < 16384) {
            for (int i = 0; i < minimals.size(); i++) {
                int tr = minimals.get(i);
                if (curr.length > 0 && tr <= curr[curr.length - 1]) {
                    continue;
                }
                int[] nextCurr = Arrays.copyOf(curr, curr.length + 1);
                nextCurr[curr.length] = tr;
                IntList nextStab = new IntList(stab.length);
                for (int op : stab) {
                    int[] mapped = new int[nextCurr.length];
                    IntList il = new IntList(mapped, 0);
                    for (int s : nextCurr) {
                        il.add(apply(helper, op, s));
                    }
                    Arrays.sort(mapped);
                    if (Arrays.equals(mapped, nextCurr)) {
                        nextStab.add(op);
                    }
                }
                IntList nextTransversal = new IntList(transversal.length);
                for (int s : transversal) {
                    if (helper.hasInv(helper.sub(s, tr))) {
                        nextTransversal.add(s);
                    }
                }
                findBases(helper, nextStab.toArray(), nextTransversal.toArray(), nextCurr, cons);
            }
        } else {
            IntStream.range(0, minimals.size()).parallel().forEach(i -> {
                int tr = minimals.get(i);
                if (curr.length > 0 && tr <= curr[curr.length - 1]) {
                    return;
                }
                int[] nextCurr = Arrays.copyOf(curr, curr.length + 1);
                nextCurr[curr.length] = tr;
                IntList nextStab = new IntList(stab.length);
                for (int op : stab) {
                    int[] mapped = new int[nextCurr.length];
                    IntList il = new IntList(mapped, 0);
                    for (int s : nextCurr) {
                        il.add(apply(helper, op, s));
                    }
                    Arrays.sort(mapped);
                    if (Arrays.equals(mapped, nextCurr)) {
                        nextStab.add(op);
                    }
                }
                IntList nextTransversal = new IntList(transversal.length);
                for (int s : transversal) {
                    if (helper.hasInv(helper.sub(s, tr))) {
                        nextTransversal.add(s);
                    }
                }
                findBases(helper, nextStab.toArray(), nextTransversal.toArray(), nextCurr, cons);
            });
        }
    }

    @Test
    public void testPermutation() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int[] arr = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), Arrays.stream(helper.v()).limit(1)).sorted().toArray();
        int[][] choices = Combinatorics.choices(arr.length, 3).toArray(int[][]::new);
        int[] gls = helper.gl();
        BlockMatrix[] permutations = new BlockMatrix[]{
                new BlockMatrix(helper.unity(), 0, 0, helper.unity()), new BlockMatrix(0, helper.unity(), helper.unity(), 0),
                new BlockMatrix(helper.unity(), helper.unity(), helper.unity(), 0), new BlockMatrix(helper.unity(), helper.unity(), 0, helper.unity()),
                new BlockMatrix(0, helper.unity(), helper.unity(), helper.unity()), new BlockMatrix(helper.unity(), 0, helper.unity(), helper.unity())};
        for (int[] choice : choices) {
            int[] abc = Arrays.stream(choice).map(i -> arr[i]).toArray();
            BlockMatrix oper = helper.permutator(abc[0], abc[1], abc[2]);
            for (int gl : gls) {
                BlockMatrix glBlock = new BlockMatrix(gl, 0, 0, gl);
                for (BlockMatrix perm : permutations) {
                    BlockMatrix bm = helper.mul(oper, helper.mul(perm, glBlock));
                    int[] result = new int[]{helper.apply(bm, 0), helper.apply(bm, helper.unity()), helper.apply(bm, helper.matCount())};
                    Arrays.sort(result);
                    assertArrayEquals(abc, result);
                }
            }
        }
    }

    @Test
    public void testApplication() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int[] gls = helper.gl();
        BlockMatrix[] permutations = new BlockMatrix[]{
                new BlockMatrix(helper.unity(), 0, 0, helper.unity()), new BlockMatrix(0, helper.unity(), helper.unity(), 0),
                new BlockMatrix(helper.unity(), helper.unity(), helper.unity(), 0), new BlockMatrix(helper.unity(), helper.unity(), 0, helper.unity()),
                new BlockMatrix(0, helper.unity(), helper.unity(), helper.unity()), new BlockMatrix(helper.unity(), 0, helper.unity(), helper.unity())};
        int[] abc = new int[]{0, helper.unity(), helper.matCount()};
        for (int gl : gls) {
            BlockMatrix glBlock = new BlockMatrix(gl, 0, 0, gl);
            for (BlockMatrix perm : permutations) {
                BlockMatrix bm = helper.mul(perm, glBlock);
                int[] result = new int[]{helper.apply(bm, abc[0]), helper.apply(bm, abc[1]), helper.apply(bm, abc[2])};
                Arrays.sort(result);
                assertArrayEquals(abc, result);
            }
        }
    }

    private static List<BlockMatrix> suitableOperators(ModuloMatrixHelper helper, int[] spread) {
        BlockMatrix[] permutations = new BlockMatrix[]{
                new BlockMatrix(helper.unity(), 0, 0, helper.unity()), new BlockMatrix(0, helper.unity(), helper.unity(), 0),
                new BlockMatrix(helper.unity(), helper.unity(), helper.unity(), 0), new BlockMatrix(helper.unity(), helper.unity(), 0, helper.unity()),
                new BlockMatrix(0, helper.unity(), helper.unity(), helper.unity()), new BlockMatrix(helper.unity(), 0, helper.unity(), helper.unity())};
        int[][] choices = Combinatorics.choices(spread.length, 3).toArray(int[][]::new);
        int[] gls = helper.gl();
        List<BlockMatrix> result = Collections.synchronizedList(new ArrayList<>());
        for (int[] choice : choices) {
            int[] abc = new int[]{spread[choice[0]], spread[choice[1]], spread[choice[2]]};
            BlockMatrix oper = helper.permutator(abc[0], abc[1], abc[2]);
            Arrays.stream(gls).parallel().forEach(gl -> {
                BlockMatrix glBlock = new BlockMatrix(gl, 0, 0, gl);
                ex: for (BlockMatrix perm : permutations) {
                    BlockMatrix bm = helper.mul(oper, helper.mul(perm, glBlock));
                    for (int el : spread) {
                        int applied = helper.apply(bm, el);
                        if (Arrays.binarySearch(spread, applied) < 0) {
                            continue ex;
                        }
                    }
                    result.add(bm);
                }
            });
        }
        return new ArrayList<>(result);
    }

    private static final int[] orbEls32 = new int[]{1119572, 1119606};
    private static final int[] orbEls16 = new int[]{4698, 4699, 4713};

    @Test
    public void testSuitable() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int[] arr = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), IntStream.of(orbEls32[0])).sorted().toArray();
        List<BlockMatrix> stabilizer = suitableOperators(helper, arr);
        System.out.println(stabilizer.size());
    }

    @Test
    public void translationPlanesAlt() throws IOException {
        int p = 2;
        int n = 4;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int r = LinearSpace.pow(p, n) + 1;
        int[] init = helper.v();
        List<int[]> bases = new ArrayList<>();
        int[] base = new int[]{0, helper.unity(), helper.matCount()};
        findBasesAlt(helper, init, base, s -> {
            if (s.length < 6) {
                return false;
            }
            bases.add(s);
            return true;
        });
        System.out.println(bases.size() + " " + bases.stream().collect(Collectors.groupingBy(l -> l.length, Collectors.counting())));
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        bases.parallelStream().forEach(barr -> {
            int[] arr = Arrays.stream(barr).filter(el -> Arrays.binarySearch(base, el) < 0).toArray();
            if (arr.length == r - 3) {
                Liner lnr = toAffine(helper, arr);
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
                return;
            }
            int[] left = Arrays.stream(init).filter(i -> Arrays.stream(arr).allMatch(j -> helper.hasInv(helper.sub(i, j)))).toArray();
            if (left.length == 0) {
                return;
            }
            Graph g = Graph.by(left, (a, b) -> helper.hasInv(helper.sub(a, b)));
            JNauty.instance().maximalCliques(g, r - arr.length - 3, a -> {
                FixBS fbs = new FixBS(a);
                Liner lnr = toAffine(helper, IntStream.concat(Arrays.stream(arr), Arrays.stream(fbs.toArray()).map(i -> left[i])).toArray());
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
            });
        });
    }

    private static void findBasesAlt(ModuloMatrixHelper helper, int[] transversal, int[] curr, Predicate<int[]> cons) {
        if (cons.test(curr)) {
            return;
        }
        List<BlockMatrix> stabilizers = suitableOperators(helper, curr);
        IntList minimals = new IntList(transversal.length);
        ex: for (int tr : transversal) {
            for (BlockMatrix st : stabilizers) {
                int mapped = helper.apply(st, tr);
                if (mapped < tr) {
                    continue ex;
                }
            }
            minimals.add(tr);
        }
        for (int i = 0; i < minimals.size(); i++) {
            int tr = minimals.get(i);
            int[] nextCurr = append(helper, curr, tr);
            if (nextCurr == null) {
                continue;
            }
            IntList nextTransversal = new IntList(transversal.length);
            for (int s : transversal) {
                if (helper.hasInv(helper.sub(s, tr))) {
                    nextTransversal.add(s);
                }
            }
            findBasesAlt(helper, nextTransversal.toArray(), nextCurr, cons);
        }
    }

    private static int[] append(ModuloMatrixHelper helper, int[] curr, int tr) {
        int idx = curr.length - 2;
        while (true) {
            int c = curr[idx];
            boolean more = tr > c;
            if (more) {
                idx++;
                break;
            } else {
                if (c != helper.unity()) {
                    return null;
                }
            }
            idx--;
        }
        int[] nextCurr = new int[curr.length + 1];
        System.arraycopy(curr, 0, nextCurr, 0, idx);
        System.arraycopy(curr, idx, nextCurr, idx + 1, curr.length - idx);
        nextCurr[idx] = tr;
        return nextCurr;
    }

    @Test
    public void tstInv() throws IOException {
        int p = 2;
        int n = 4;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        int[] v = helper.v();
        for (int i = 0; i < v.length; i++) {
            int a = v[i];
            int[] v1 = Arrays.stream(v, i, v.length).filter(b -> helper.hasInv(helper.sub(b, a))).toArray();
            for (int j = 0; j < v1.length; j++) {
                int b = v1[j];
                int[] v2 = Arrays.stream(v1, j, v1.length).filter(c -> helper.hasInv(helper.sub(c, b))).toArray();
                for (int c : v2) {
                    BlockMatrix bm = helper.permutator(a, b, c);
                    BlockMatrix inv = helper.inverse(bm);
                    BlockMatrix mul = helper.mul(bm, inv);
                    assertEquals(helper.unity(), mul.a());
                    assertEquals(0, mul.b());
                    assertEquals(0, mul.c());
                    assertEquals(helper.unity(), mul.d());
                }
            }
        }
    }

    @Test
    public void testFourStruct() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        List<OrbitInfo> orbitInfos = getOrbitInfos(helper);
        System.out.println(helper.v().length + " " + orbitInfos.size() + " " + Arrays.toString(orbitInfos.stream().mapToInt(OrbitInfo::minimal).toArray()));
        for (OrbitInfo oi : orbitInfos) {
            int m = oi.minimal();
            System.out.println(m + " " + oi.ops.size() + " " + oi.stabilizer.size());
            for (Map.Entry<Integer, BlockMatrix> e : oi.ops().entrySet()) {
                assertEquals(e.getKey(), helper.apply(e.getValue(), m));
            }
            int[] arr = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), IntStream.of(m)).sorted().toArray();
            for (BlockMatrix bm : oi.stabilizer()) {
                assertArrayEquals(arr, IntStream.of(arr).map(i -> helper.apply(bm, i)).sorted().toArray());
            }
        }
    }

    private static List<OrbitInfo> getOrbitInfos(ModuloMatrixHelper helper) {
        List<OrbitInfo> orbitInfos = new ArrayList<>();
        List<BlockMatrix> stabilizers = Collections.synchronizedList(new ArrayList<>());
        int[] gl = helper.gl();
        BlockMatrix[] permutations = helper.p() % 2 != 0 ? new BlockMatrix[]{
                new BlockMatrix(helper.unity(), 0, 0, helper.unity()), new BlockMatrix(0, helper.unity(), helper.unity(), 0)}
                : new BlockMatrix[]{
                new BlockMatrix(helper.unity(), 0, 0, helper.unity()), new BlockMatrix(0, helper.unity(), helper.unity(), 0),
                new BlockMatrix(helper.unity(), helper.unity(), helper.unity(), 0), new BlockMatrix(helper.unity(), helper.unity(), 0, helper.unity()),
                new BlockMatrix(0, helper.unity(), helper.unity(), helper.unity()), new BlockMatrix(helper.unity(), 0, helper.unity(), helper.unity())};
        for (BlockMatrix perm : permutations) {
            Arrays.stream(gl).parallel().forEach(el -> {
                BlockMatrix bm = new BlockMatrix(el, 0, 0, el);
                stabilizers.add(helper.mul(perm, bm));
            });
        }
        int[] v = helper.v();
        int min = v[0];
        while (min >= 0) {
            int m = min;
            Map<Integer, BlockMatrix> ops = new ConcurrentHashMap<>();
            stabilizers.parallelStream().forEach(st -> {
                int applied = helper.apply(st, m);
                ops.putIfAbsent(applied, st);
            });
            int[] arr = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), IntStream.of(min)).sorted().toArray();
            List<BlockMatrix> stabilizer = suitableOperators(helper, arr);
            orbitInfos.add(new OrbitInfo(min, new HashMap<>(ops), stabilizer));
            min = Arrays.stream(v).filter(el -> orbitInfos.stream().noneMatch(oi -> oi.ops.containsKey(el))).findFirst().orElse(-1);
        }
        return orbitInfos;
    }

    private record OrbitInfo(int minimal, Map<Integer, BlockMatrix> ops, List<BlockMatrix> stabilizer) {
    }

    @Test
    public void translationPlanesB() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        List<OrbitInfo> orbitInfos = getOrbitInfos(helper);
        List<int[]> bases = new ArrayList<>();
        int[] v = helper.v();
        int[] base = new int[]{0, helper.unity(), helper.matCount()};
        int r = LinearSpace.pow(p, n) + 1;
        for (OrbitInfo info : orbitInfos) {
            int min = info.minimal();
            int[] quad = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), IntStream.of(min)).sorted().toArray();
            int[] newV = Arrays.stream(v).filter(el -> helper.hasInv(helper.sub(min, el))).toArray();
            findBasesB(helper, orbitInfos, newV, quad, s -> {
                if (s.length < 7) {
                    return false;
                }
                bases.add(s);
                return true;
            });
        }
        System.out.println(bases.size() + " " + bases.stream().collect(Collectors.groupingBy(l -> l.length, Collectors.counting())));
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        bases.parallelStream().forEach(barr -> {
            int[] arr = Arrays.stream(barr).filter(el -> Arrays.binarySearch(base, el) < 0).toArray();
            if (arr.length == r - 3) {
                Liner lnr = toAffine(helper, arr);
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
                return;
            }
            int[] left = Arrays.stream(v).filter(i -> Arrays.stream(arr).allMatch(j -> helper.hasInv(helper.sub(i, j)))).toArray();
            if (left.length == 0) {
                return;
            }
            Graph g = Graph.by(left, (a, b) -> helper.hasInv(helper.sub(a, b)));
            JNauty.instance().maximalCliques(g, r - arr.length - 3, a -> {
                FixBS fbs = new FixBS(a);
                Liner lnr = toAffine(helper, IntStream.concat(Arrays.stream(arr), Arrays.stream(fbs.toArray()).map(i -> left[i])).toArray());
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    System.out.println(lnr.graphData().autCount());
                }
            });
        });
    }

    private static void findBasesB(ModuloMatrixHelper helper, List<OrbitInfo> infos, int[] transversal, int[] curr, Predicate<int[]> cons) {
        if (cons.test(curr)) {
            return;
        }
        int[][] choices = Combinatorics.choices(curr.length, 3).toArray(int[][]::new);
        List<BlockMatrix> stabilizers = new ArrayList<>();
        for (int[] choice : choices) {
            int a = curr[choice[0]];
            int b = curr[choice[1]];
            int c = curr[choice[2]];
            BlockMatrix x = helper.permutator(a, b, c);
            BlockMatrix xi = helper.inverse(x);
            for (int last = choice[2] + 1; last < curr.length; last++) {
                int d = curr[last];
                int xd = helper.apply(xi, d);
                OrbitInfo info = infos.stream().filter(i -> i.ops().containsKey(xd)).findAny().orElseThrow();
                BlockMatrix y = info.ops().get(xd);
                for (BlockMatrix z : info.stabilizer()) {
                    BlockMatrix xyz = helper.mul(helper.mul(x, y), z);
                    if (Arrays.stream(curr).anyMatch(el -> Arrays.binarySearch(curr, helper.apply(xyz, el)) < 0)) {
                        continue;
                    }
                    stabilizers.add(xyz);
                }
            }
        }
        IntList minimals = new IntList(transversal.length);
        ex: for (int tr : transversal) {
            for (BlockMatrix st : stabilizers) {
                int mapped = helper.apply(st, tr);
                if (mapped < tr) {
                    continue ex;
                }
            }
            minimals.add(tr);
        }
        for (int i = 0; i < minimals.size(); i++) {
            int tr = minimals.get(i);
            int[] nextCurr = append(helper, curr, tr);
            if (nextCurr == null) {
                continue;
            }
            IntList nextTransversal = new IntList(transversal.length);
            for (int s : transversal) {
                if (helper.hasInv(helper.sub(s, tr))) {
                    nextTransversal.add(s);
                }
            }
            findBasesB(helper, infos, nextTransversal.toArray(), nextCurr, cons);
        }
    }

    @Test
    public void generateInitial() throws IOException {
        int p = 2;
        int n = 4;
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        List<OrbitInfo> orbitInfos = getOrbitInfos(helper);
        Path base = Path.of("/home/ihromant/maths/trans/new/begins-" + p + "^" + n + "-4.txt");
        for (OrbitInfo info : orbitInfos) {
            int min = info.minimal();
            int[] quad = IntStream.concat(IntStream.of(0, helper.unity(), helper.matCount()), IntStream.of(min)).sorted().toArray();
            Files.writeString(base, Arrays.toString(quad) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    @Test
    public void expand() throws IOException {
        int p = 2;
        int n = 5;
        int len = 4;
        ObjectMapper om = new ObjectMapper();
        ModuloMatrixHelper helper = TranslationPlane2Test.readGl(p, n);
        List<OrbitInfo> orbitInfos = getOrbitInfos(helper);
        int[] v = helper.v();
        List<String> lines = Files.readAllLines(Path.of("/home/ihromant/maths/trans/new/begins-" + p + "^" + n + "-" + len + ".txt"));
        Path next = Path.of("/home/ihromant/maths/trans/new/begins-" + p + "^" + n + "-" + (len + 1) + ".txt");
        System.out.println(lines.size());
        int cnt = 0;
        for (String ln : lines) {
            int[] arr = om.readValue(ln, int[].class);
            int[] newV = Arrays.stream(v).filter(el -> Arrays.stream(arr)
                    .allMatch(el1 -> el1 == helper.matCount() || helper.hasInv(helper.sub(el, el1)))).toArray();
            findBasesB(helper, orbitInfos, newV, arr, s -> {
                if (s.length < len + 1) {
                    return false;
                }
                try {
                    Files.writeString(next, Arrays.toString(s) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            });
            System.out.println(++cnt);
        }
    }
}
