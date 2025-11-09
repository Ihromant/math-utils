package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.CommonMatrixHelper;

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
}
