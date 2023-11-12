package ua.ihromant.mathutils.group;

import java.util.Arrays;

public class CyclicProduct implements Group {
    public int[] base;

    public CyclicProduct(int... base) {
        this.base = base;
    }

    public int cardinality() {
        return Arrays.stream(base).reduce(1, (a, b) -> a * b);
    }

    public int fromArr(int... arr) {
        int result = 0;
        for (int i = 0; i < base.length; i++) {
            result = result * base[i] + arr[i];
        }
        return result;
    }

    public int[] toArr(int x) {
        int[] result = new int[base.length];
        for (int i = base.length - 1; i >= 0; i--) {
            result[i] = x % base[i];
            x = x / base[i];
        }
        return result;
    }

    private int[] arrAdd(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (a[i] + b[i]) % base[i];
        }
        return result;
    }

    private int[] invArr(int[] a) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = a[i] == 0 ? 0 : base[i] - a[i];
        }
        return result;
    }

    public int add(int a, int b) {
        return fromArr(arrAdd(toArr(a), toArr(b)));
    }

    public int[] arrMul(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (a[i] * b[i]) % base[i];
        }
        return result;
    }

    @Override
    public int op(int a, int b) {
        return fromArr(arrAdd(toArr(a), toArr(b)));
    }

    @Override
    public int inv(int a) {
        return fromArr(invArr(toArr(a)));
    }

    @Override
    public int order() {
        return cardinality();
    }
}
