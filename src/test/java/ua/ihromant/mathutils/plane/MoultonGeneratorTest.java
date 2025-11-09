package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.CommonMatrixHelper;
import ua.ihromant.mathutils.vector.LinearSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class MoultonGeneratorTest {
    @Test
    public void andre() {
        int order = 27;
        GaloisField gf = new GaloisField(order);
        int generator = IntStream.range(1, order).filter(el -> gf.mulOrder(el) == 26).findAny().orElseThrow();
        System.out.println(generator);
        int[][] g = new int[][]{{0, 0, 2}, {1, 0, 1}, {0, 1, 0}};
        CommonMatrixHelper helper = new CommonMatrixHelper(3, 3);
        int gEl = helper.fromMatrix(g);
        int gSqr = helper.mul(gEl, gEl);
        FixBS fbs = new FixBS(order);
        for (int i = 1; i < order; i++) {
            fbs.set(gf.mul(i, i));
        }
        int[] positive = fbs.toArray();
        fbs.flip(1, order);
        int[] negative = fbs.toArray();
        System.out.println(Arrays.toString(positive) + " " + Arrays.toString(negative));
        int[] oddMuls = new int[13];
        int[] evenMuls = new int[13];
        oddMuls[0] = gEl;
        evenMuls[0] = helper.unity();
        for (int i = 1; i < oddMuls.length; i++) {
            oddMuls[i] = helper.mul(oddMuls[i - 1], gSqr);
            evenMuls[i] = helper.mul(evenMuls[i - 1], gSqr);
        }
        System.out.println(Arrays.toString(oddMuls));
        List<Pair> lst = new ArrayList<>();
        for (int a : helper.gl()) {
            ex: for (int b : helper.gl()) {
                for (int oddMul : oddMuls) {
                    for (int evenMul : evenMuls) {
                        int matr = helper.mul(helper.mul(a, oddMul), b);
                        for (int x = 1; x < order; x++) {
                            if (helper.mulVec(evenMul, x) == helper.mulVec(matr, x)) {
                                continue ex;
                            }
                        }
                    }
                }
                checkPair(new Pair(a, b), order, positive, gf, negative, helper);
                lst.add(new Pair(a, b));
            }
        }
        System.out.println(lst.size() + " " + lst);
        //System.out.println(Arrays.deepToString(helper.toMatrix(lst.get(0))));
//        for (Pair pr : lst) {
//            checkPair(pr, order, positive, gf, negative, helper);
//        }
    }

    private static void checkPair(Pair pr, int order, int[] positive, GaloisField gf, int[] negative, CommonMatrixHelper helper) {
        List<int[]> lns = new ArrayList<>();
        for (int i = 0; i < order; i++) {
            int fix = i;
            int[] horLine = IntStream.range(0, order).map(x -> fromXY(order, x, fix)).toArray();
            int[] verLine = IntStream.range(0, order).map(y -> fromXY(order, fix, y)).toArray();
            lns.add(horLine);
            lns.add(verLine);
        }
        for (int i = 0; i < order; i++) {
            int b = i;
            for (int a : positive) {
                int[] posLine = IntStream.range(0, order).map(x -> fromXY(order, x, gf.add(gf.mul(a, x), b))).toArray();
                lns.add(posLine);
            }
            for (int a : negative) {
                int[] negLine = IntStream.range(0, order).map(x -> {
                    int bMat = pr.b();
                    int xApplied = helper.mulVec(bMat, x);
                    int axb = gf.add(gf.mul(a, xApplied), b);
                    int mapped = helper.mulVec(pr.a(), axb);
                    return fromXY(order, x, mapped);
                }).toArray();
                lns.add(negLine);
            }
        }
        Liner lnr = new Liner(order * order, lns.toArray(int[][]::new));
        System.out.println(pr + " " + lnr.playfairIndex() + " " + TernaryAutomorphisms.isAffineTranslation(lnr) + " " + TernaryAutomorphisms.isAffineDesargues(lnr));
    }

    private static int fromXY(int order, int x, int y) {
        return x * order + y;
    }

    private static int[] toXY(int order, int pt) {
        return new int[]{pt / order, pt % order};
    }

    private record Pair(int a, int b) {}

    @Test
    public void test() {
        int order = 27;
        GaloisField gf = new GaloisField(order);
        int generator = IntStream.range(1, order).filter(el -> gf.mulOrder(el) == 26).findAny().orElseThrow();
        System.out.println(generator);
        int[][] g = new int[][]{{0, 0, 2}, {1, 0, 1}, {0, 1, 0}};
        CommonMatrixHelper helper = new CommonMatrixHelper(3, 3);
        int gEl = helper.fromMatrix(g);
        int gSqr = helper.mul(gEl, gEl);
        FixBS fbs = new FixBS(order);
        for (int i = 1; i < order; i++) {
            fbs.set(gf.mul(i, i));
        }
        int[] positive = fbs.toArray();
        fbs.flip(1, order);
        int[] negative = fbs.toArray();
        System.out.println(Arrays.toString(positive) + " " + Arrays.toString(negative));
        int[] oddMuls = new int[13];
        int[] evenMuls = new int[13];
        oddMuls[0] = gEl;
        evenMuls[0] = helper.unity();
        for (int i = 1; i < oddMuls.length; i++) {
            oddMuls[i] = helper.mul(oddMuls[i - 1], gSqr);
            evenMuls[i] = helper.mul(evenMuls[i - 1], gSqr);
        }
        System.out.println(Arrays.toString(oddMuls));
        //List<Pair> lst = new ArrayList<>();
        Arrays.stream(helper.gl()).parallel().forEach(a -> {
            for (int b : helper.gl()) {
                checkPairAlt(new Pair(a, b), order, positive, gf, FixBS.of(order, negative), helper);
            }
            System.out.println(a);
        });
        //System.out.println(lst.size() + " " + lst);
        //System.out.println(Arrays.deepToString(helper.toMatrix(lst.get(0))));
//        for (Pair pr : lst) {
//            checkPair(pr, order, positive, gf, negative, helper);
//        }
    }

    private static void checkPairAlt(Pair pr, int order, int[] positive, GaloisField gf, FixBS negative, CommonMatrixHelper helper) {
        List<int[]> lns = new ArrayList<>();
        for (int i = 0; i < order; i++) {
            int fix = i;
            int[] horLine = IntStream.range(0, order).map(x -> fromXY(order, x, fix)).toArray();
            int[] verLine = IntStream.range(0, order).map(y -> fromXY(order, fix, y)).toArray();
            lns.add(horLine);
            lns.add(verLine);
        }
        for (int i = 0; i < order; i++) {
            int b = i;
            for (int a : positive) {
                int[] posLine = IntStream.range(0, order).map(x -> fromXY(order, x, gf.add(gf.mul(a, x), b))).toArray();
                lns.add(posLine);
            }
            for (int j = negative.nextSetBit(0); j >= 0; j = negative.nextSetBit(j + 1)) {
                int a = j;
                int[] negLine = IntStream.range(0, order).map(x -> {
                    if (negative.get(x)) {
                        int bMat = pr.b();
                        int xApplied = helper.mulVec(bMat, x);
                        int axb = gf.add(gf.mul(a, xApplied), b);
                        int mapped = helper.mulVec(pr.a(), axb);
                        return fromXY(order, x, mapped);
                    } else {
                        return fromXY(order, x, gf.add(gf.mul(a, x), b));
                    }
                }).toArray();
                lns.add(negLine);
            }
        }
        try {
            Liner lnr = new Liner(order * order, lns.toArray(int[][]::new));
            System.out.println(pr + " " + lnr.playfairIndex() + " " + TernaryAutomorphisms.isAffineTranslation(lnr) + " " + TernaryAutomorphisms.isAffineDesargues(lnr));
        } catch (IllegalStateException e) {
            // ok
        }
    }

    @Test
    public void findMonotonous() {
        int order = 9;
        GaloisField fd = new GaloisField(order);
        FixBS ps = new FixBS(order);
        IntStream.range(1, fd.cardinality()).forEach(i -> ps.set(fd.mul(i, i)));
        int[] positive = ps.toArray();
        ps.flip(1, order);
        int[] negative = ps.toArray();
        ps.flip(1, order);
        int gen = IntStream.range(1, order).filter(i -> fd.mulOrder(i) == 8).findFirst().orElseThrow();
        System.out.println(gen);
        System.out.println(Arrays.toString(positive) + " " + Arrays.toString(negative));
        int[][] perms = Combinatorics.permutations(IntStream.range(0, 7).toArray()).map(perm -> {
            int[] res = new int[9];
            res[1] = 1;
            for (int i = 0; i < perm.length; i++) {
                res[i + 2] = perm[i] + 2;
            }
            return res;
        }).filter(f -> {
            for (int x = 0; x < fd.cardinality(); x++) {
                for (int p : positive) {
                    int xp = fd.add(x, p);
                    int sub = fd.sub(f[xp], f[x]);
                    if (!ps.get(sub)) {
                        return false;
                    }
                }
            }
            return true;
        }).toArray(int[][]::new);
        System.out.println(perms.length);
        Arrays.stream(perms).forEach(p -> System.out.println(Arrays.toString(p)));
        System.out.println(Arrays.toString(IntStream.range(0, 9).map(i -> fd.power(i, 3)).toArray()));
        for (int[] f : perms) {
            for (int mul : new int[]{1, gen}) {
                int[] funF = IntStream.of(f).map(i -> fd.mul(i, mul)).toArray();
                for (int[] g : perms) {
                    int[] funG = IntStream.of(g).map(i -> fd.mul(i, mul)).toArray();
                    List<int[]> lns = new ArrayList<>();
                    for (int i = 0; i < order; i++) {
                        int fix = i;
                        int[] horLine = IntStream.range(0, order).map(x -> fromXY(order, x, fix)).toArray();
                        int[] verLine = IntStream.range(0, order).map(y -> fromXY(order, fix, y)).toArray();
                        lns.add(horLine);
                        lns.add(verLine);
                    }
                    for (int i = 0; i < order; i++) {
                        int b = i;
                        for (int a : positive) {
                            int[] posLine = IntStream.range(0, order).map(x -> fromXY(order, x, fd.add(fd.mul(a, x), b))).toArray();
                            lns.add(posLine);
                        }
                        for (int a : negative) {
                            int[] negLine = IntStream.range(0, order).map(x -> {
                                int xApplied = funG[x];
                                int axb = fd.add(fd.mul(a, xApplied), b);
                                int mapped = funF[axb];
                                return fromXY(order, x, mapped);
                            }).toArray();
                            lns.add(negLine);
                        }
                    }
                    Liner lnr = new Liner(order * order, lns.toArray(int[][]::new));
                    System.out.println(Arrays.toString(f) + " " + Arrays.toString(g) + " " + lnr.playfairIndex() + " " + TernaryAutomorphisms.isAffineTranslation(lnr) + " " + TernaryAutomorphisms.isAffineDesargues(lnr));
                }
            }
        }
    }

    @Test
    public void andre1() {
        int base = 3;
        int pow = 3;
        int order = LinearSpace.pow(base, pow);
        GaloisField gf = new GaloisField(order);
        FixBS fbs = new FixBS(order);
        for (int i = 1; i < order; i++) {
            fbs.set(gf.mul(i, i));
        }
        int[] positive = fbs.toArray();
        fbs.flip(1, order);
        int[] negative = fbs.toArray();
        System.out.println(Arrays.toString(positive) + " " + Arrays.toString(negative));
        List<int[]> paley = new ArrayList<>();
        for (int p = 0; p < pow; p++) {
            int[] frobMap = new int[order];
            int cff = LinearSpace.pow(base, p);
            for (int i = 0; i < order; i++) {
                frobMap[i] = gf.power(i, cff);
            }
            for (int b : IntStream.range(0, order).toArray()) {
                for (int a : positive) {
                    int[] arr = new int[order];
                    for (int el = 0; el < order; el++) {
                        arr[el] = gf.add(gf.mul(a, frobMap[el]), b);
                    }
                    paley.add(arr);
                }
            }
        }
        System.out.println(paley.size());
        for (int[] a : paley) {
            for (int[] b : paley) {
                checkPair(a, b, order, positive, gf, negative);
            }
        }
        //System.out.println(Arrays.deepToString(helper.toMatrix(lst.get(0))));
//        for (Pair pr : lst) {
//            checkPair(pr, order, positive, gf, negative, helper);
//        }
    }

    private static void checkPair(int[] aFun, int[] bFun, int order, int[] positive, GaloisField gf, int[] negative) {
        List<int[]> lns = new ArrayList<>();
        for (int i = 0; i < order; i++) {
            int fix = i;
            int[] horLine = IntStream.range(0, order).map(x -> fromXY(order, x, fix)).toArray();
            int[] verLine = IntStream.range(0, order).map(y -> fromXY(order, fix, y)).toArray();
            lns.add(horLine);
            lns.add(verLine);
        }
        for (int i = 0; i < order; i++) {
            int b = i;
            for (int a : positive) {
                int[] posLine = IntStream.range(0, order).map(x -> fromXY(order, x, gf.add(gf.mul(a, x), b))).toArray();
                lns.add(posLine);
            }
            for (int a : negative) {
                int[] negLine = IntStream.range(0, order).map(x -> {
                    int xApplied = bFun[x];
                    int axb = gf.add(gf.mul(a, xApplied), b);
                    int mapped = aFun[axb];
                    return fromXY(order, x, mapped);
                }).toArray();
                lns.add(negLine);
            }
        }
        Liner lnr = new Liner(order * order, lns.toArray(int[][]::new));
        System.out.println(Arrays.toString(aFun) + " " + Arrays.toString(bFun) + " " + lnr.playfairIndex() + " " + TernaryAutomorphisms.isAffineTranslation(lnr) + " " + TernaryAutomorphisms.isAffineDesargues(lnr));
    }
}
