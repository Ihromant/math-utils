package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.util.FixBS;

public interface LinearSpace {
    int p();

    int n();

    int cardinality();

    int half();

    int mul(int a, int x);

    int add(int... numbers);

    FixBS hull(int... arr);

    int scalar(int a, int b);

    default LinearSpace halfSpace() {
        return of(p(), n() / 2);
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
        }
        if (GaloisField.isPrime(p)) {
            return new PrimeLinearSpace(p, n);
        }
        throw new IllegalArgumentException();
    }
}
