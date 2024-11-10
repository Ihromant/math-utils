package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

public interface LinearSpace {
    int getN();

    int cardinality();

    int half();

    int add(int... numbers);

    FixBS hull(int... arr);

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
}
