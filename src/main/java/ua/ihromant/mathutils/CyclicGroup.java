package ua.ihromant.mathutils;

import java.util.Arrays;

public class CyclicGroup {
    public int[] base;

    public CyclicGroup(int... base) {
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
}
