package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.util.FixBS;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

public interface Loop {
    int op(int a, int b);

    int inv(int a);

    int order();

    default int order(int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = op(a, pow);
        } while (pow != 0);
        return counter;
    }

    default int expOrder(int a) {
        int eul = Combinatorics.euler(order());
        int res = a;
        for (int i = 1; i < eul + 1; i++) {
            res = mul(res, a);
            if (res == a) {
                return i;
            }
        }
        return -1;
    }

    String name();

    String elementName(int a);

    default int mul(int a, int cff) {
        int result = 0;
        for (int i = 0; i < cff; i++) {
            result = op(a, result);
        }
        return result;
    }

    default int exponent(int base, int power) {
        int result = 1;
        while (power > 0) {
            if (power % 2 == 1) {
                result = mul(result, base);
            }
            base = mul(base, base);
            power = power / 2;
        }
        return result;
    }

    default int[] squareRoots(int from) {
        return IntStream.range(0, order()).filter(i -> op(i, i) == from).toArray();
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default TableGroup asTable() {
        int order = order();
        int[][] table = new int[order][order];
        if (order > 1000) {
            IntStream.range(0, order).parallel().forEach(i -> {
                for (int j = 0; j < order; j++) {
                    table[i][j] = op(i, j);
                }
            });
        } else {
            for (int i = 0; i < order; i++) {
                for (int j = 0; j < order; j++) {
                    table[i][j] = op(i, j);
                }
            }
        }
        return new TableGroup(name(), table);
    }

    default int conjugate(int fst, int snd) {
        return op(op(snd, fst), inv(snd));
    }

    default List<FixBS> conjugationClasses() {
        int order = order();
        QuickFind qf = new QuickFind(order);
        for (int x = 0; x < order; x++) {
            for (int g = 0; g < order; g++) {
                int conj = op(inv(g), op(x, g));
                if (conj < x) {
                    qf.union(x, conj);
                    break;
                }
            }
        }
        return qf.components();
    }

    default boolean isCommutative() {
        return IntStream.range(1, order()).allMatch(i -> IntStream.range(1, order()).allMatch(j -> op(i, j) == op(j, i)));
    }

    default FixBS cycle(int from) {
        FixBS result = new FixBS(order());
        int el = from;
        do {
            el = op(el, from);
            result.set(el);
        } while (el != from);
        return result;
    }

    default int[][] innerAuth() {
        Set<int[]> result = new TreeSet<>(Combinatorics::compareArr);
        for (int conj = 0; conj < order(); conj++) {
            int[] arr = new int[order()];
            int inv = inv(conj);
            for (int el = 0; el < order(); el++) {
                arr[el] = op(op(inv, el), conj);
            }
            result.add(arr);
        }
        return result.toArray(int[][]::new);
    }
}
