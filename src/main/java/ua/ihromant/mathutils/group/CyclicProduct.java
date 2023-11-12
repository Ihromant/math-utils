package ua.ihromant.mathutils.group;

import java.util.Arrays;

public class CyclicProduct implements Group {
    public CyclicGroup[] base;

    public CyclicProduct(int... base) {
        this.base = Arrays.stream(base).mapToObj(CyclicGroup::new).toArray(CyclicGroup[]::new);
    }

    public int fromArr(int... arr) {
        int result = 0;
        for (int i = 0; i < base.length; i++) {
            result = result * base[i].order() + arr[i];
        }
        return result;
    }

    public int[] toArr(int x) {
        int[] result = new int[base.length];
        for (int i = base.length - 1; i >= 0; i--) {
            result[i] = x % base[i].order();
            x = x / base[i].order();
        }
        return result;
    }

    private int[] arrAdd(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = base[i].op(a[i], b[i]);
        }
        return result;
    }

    private int[] invArr(int[] a) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = base[i].inv(a[i]);
        }
        return result;
    }

    public int[] arrMul(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (a[i] * b[i]) % base[i].order();
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
        return Arrays.stream(base).reduce(1, (pr, cg) -> pr * cg.order(), (pr1, pr2) -> pr1 * pr2);
    }
}
