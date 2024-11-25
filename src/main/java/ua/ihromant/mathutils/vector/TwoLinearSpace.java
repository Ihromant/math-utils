package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

public record TwoLinearSpace(int n) implements LinearSpace {
    @Override
    public int p() {
        return 2;
    }

    @Override
    public int cardinality() {
        return 1 << n;
    }

    @Override
    public int half() {
        return 1 << (n / 2);
    }

    @Override
    public int mul(int a, int x) {
        return a % 2 == 0 ? 0 : x;
    }

    @Override
    public int add(int... numbers) {
        int result = 0;
        for (int numb : numbers) {
            result = result ^ numb;
        }
        return result;
    }

    @Override
    public FixBS hull(int... arr) {
        int al = arr.length;
        int fin = 1 << al;
        FixBS res = new FixBS(cardinality());
        for (int i = 0; i < fin; i++) {
            int comb = 0;
            for (int j = 0; j < al; j++) {
                if ((i & (1 << j)) != 0) {
                    comb = comb ^ arr[j];
                }
            }
            res.set(comb);
        }
        res.clear(0);
        return res;
    }

    @Override
    public int scalar(int a, int b) {
        return Integer.bitCount(a & b) % 2;
    }
}
