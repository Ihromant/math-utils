package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

public class TranslationPlane3Test {
    private static final int AZZA = 0;
    private static final int ZAAZ = 1;
    private static final int AAAZ = 2;
    private static final int AAZA = 3;
    private static final int AZAA = 4;
    private static final int ZAAA = 5;

    @Test
    public void translationPlanes() {
        int p = 2;
        int n = 4;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
        int[] stabilizers = suitableOperators(helper);
        int r = LinearSpace.pow(p, n) - 2;
        int[] init = helper.v();
        List<int[]> bases = new ArrayList<>();
        findBases(helper, stabilizers, init, new int[0], (s, stab) -> {
            if (stab.length > helper.p() && s.length < r) {
                return false;
            }
            bases.add(s);
            return true;
        });
        System.out.println(bases.size());
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        bases.parallelStream().forEach(arr -> {
            if (arr.length < r) {
                return;
            }
            Liner lnr = toAffine(helper, arr);
            if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                System.out.println(lnr.graphData().autCount());
            }
        });
    }

    private static Liner toAffine(ModuloMatrixHelper helper, int[] base) {
        int n = helper.n();
        int pow = LinearSpace.pow(2, n);
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
            act = a >>> sh;
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
        ex: for (int tr : transversal) {
            for (int st : stab) {
                int mapped = apply(helper, st, tr);
                if (mapped < tr) {
                    continue ex;
                }
            }
            minimals.add(tr);
        }
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
    }
}
