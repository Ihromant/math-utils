package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

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
}
