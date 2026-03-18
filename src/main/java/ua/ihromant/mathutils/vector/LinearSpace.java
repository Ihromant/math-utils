package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public interface LinearSpace {
    int p();

    int n();

    int cardinality();

    int half();

    int mul(int a, int x);

    int add(int... numbers);

    int neg(int a);

    default int sub(int a, int b) {
        return add(a, neg(b));
    }

    FixBS hull(int... arr);

    int scalar(int a, int b);

    int crd(int v, int crd);

    default int[] toCrd(int a) {
        return IntStream.range(0, n()).map(i -> crd(a, i)).toArray();
    }

    default int fromCrd(int[] crd) {
        int result = 0;
        for (int i = crd.length - 1; i >= 0; i--) {
            result = result * p() + crd[i];
        }
        return result;
    }

    static int pow(int a, int b) {
        if (b == 0) {
            return 1;
        }
        if (b == 1) {
            return a;
        }
        if ((b & 1) == 0) {
            return pow(a * a, b / 2);
        } else {
            return a * pow(a * a, b / 2);
        }
    }

    static LinearSpace of(int p, int n) {
        if (p == 2) {
            return new TwoLinearSpace(n);
        } else {
            return new PrimeLinearSpace(p, n);
        }
    }

    default List<FixBS> subSpaces(int dim) {
        List<FixBS> result = new ArrayList<>();
        FixBS base = FixBS.of(cardinality(), 0);
        subSpaces(base, dim, result::add);
        return result;
    }

    private void subSpaces(FixBS curr, int dim, Consumer<FixBS> cons) {
        if (dim == 0) {
            cons.accept(curr);
            return;
        }
        ex: for (int el = curr.nextClearBit(curr.previousSetBit(cardinality())); el >= 0 && el < cardinality(); el = curr.nextClearBit(el + 1)) {
            FixBS next = curr.copy();
            for (int bs = curr.nextSetBit(0); bs >= 0; bs = curr.nextSetBit(bs + 1)) {
                for (int a = 0; a < p(); a++) {
                    for (int b = 1; b < p(); b++) {
                        int res = add(mul(a, bs), mul(b, el));
                        if (res < el) {
                            continue ex;
                        }
                        next.set(res);
                    }
                }
            }
            subSpaces(next, dim - 1, cons);
        }
    }

    default List<FixBS> cosets(FixBS subSpace) {
        List<FixBS> result = new ArrayList<>();
        ex: for (int i = 0; i < cardinality(); i++) {
            FixBS sh = new FixBS(cardinality());
            for (int el = subSpace.nextSetBit(0); el >= 0; el = subSpace.nextSetBit(el + 1)) {
                int res = add(el, i);
                if (res < i) {
                    continue ex;
                }
                sh.set(res);
            }
            result.add(sh);
        }
        return result;
    }

    default int applyOper(long oper, int val) {
        int result = 0;
        for (int i = 0; i < n(); i++) {
            int rest = (int) (oper % cardinality());
            int crd = val % p();
            result = add(result, mul(crd, rest));
            val = val / p();
            oper = oper / cardinality();
        }
        return result;
    }

    default FixBS applyOper(long oper, FixBS subSpace) {
        FixBS result = new FixBS(cardinality());
        for (int val = subSpace.nextSetBit(0); val >= 0; val = subSpace.nextSetBit(val + 1)) {
            result.set(applyOper(oper, val));
        }
        return result;
    }

    default int[][] toMatrix(long a) {
        int[][] result = new int[n()][n()];
        for (int i = 0; i < n() * n(); i++) {
            result[i / n()][i % n()] = (int) (a % p());
            a = a / p();
        }
        return result;
    }

    default long fromMatrix(int[][] matrix) {
        long result = 0;
        for (int i = n() * n() - 1; i >= 0; i--) {
            result = result * p() + matrix[i / n()][i % n()];
        }
        return result;
    }

    default long mulOper(long a, long b) {
        int[][] first = toMatrix(a);
        int[][] second = toMatrix(b);
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = sum + first[i][k] * second[k][j];
                }
                result[i][j] = sum % p();
            }
        }
        return fromMatrix(result);
    }
}
