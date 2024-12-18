package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.vector.ModuloMatrixHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record GroupProduct(int... base) implements Group {
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
        return Arrays.stream(base).reduce(1, (pr, cg) -> pr * cg);
    }

    @Override
    public String name() {
        return Arrays.stream(base).mapToObj(i -> "Z" + i).collect(Collectors.joining("x"));
    }

    @Override
    public String elementName(int a) {
        int[] arr = toArr(a);
        return IntStream.range(0, base.length).mapToObj(i -> String.valueOf(arr[i])).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int[][] auth() {
        if (!Arrays.equals(base, Arrays.stream(base).sorted().toArray())) {
            throw new UnsupportedOperationException();
        }
        int order = order();
        List<ModuloMatrixHelper> helpers = new ArrayList<>();
        int from = 0;
        int p = base[0];
        for (int i = 1; i < base.length; i++) {
            if (base[i] != p) {
                helpers.add(ModuloMatrixHelper.of(p, i - from));
                from = i;
                p = base[i];
            }
        }
        helpers.add(ModuloMatrixHelper.of(p, base.length - from));
        int[][] result = new int[helpers.stream().mapToInt(h -> h.gl().length).reduce(1, (a, b) -> a * b)][order];
        int[] idxes = new int[helpers.size()];
        calculateAuth(result, idxes, 0, 0, helpers);
        return result;
    }

    private void calculateAuth(int[][] result, int[] idxes, int hi, int ri, List<ModuloMatrixHelper> helpers) {
        if (hi == helpers.size()) {
            for (int el = 0; el < order(); el++) {
                int[] arr = toArr(el);
                int[] mapped = new int[arr.length];
                int shift = 0;
                for (int i = 0; i < helpers.size(); i++) {
                    ModuloMatrixHelper helper = helpers.get(i);
                    int[] vec = new int[helper.n()];
                    System.arraycopy(arr, shift, vec, 0, helper.n());
                    int[] vecMapped = helper.toVec(helper.mulVec(helper.gl()[idxes[i]], helper.fromVec(vec)));
                    System.arraycopy(vecMapped, 0, mapped, shift, helper.n());
                    shift = shift + helper.n();
                }
                result[ri][el] = fromArr(mapped);
            }
            return;
        }
        ModuloMatrixHelper helper = helpers.get(hi);
        int gl = helper.gl().length;
        for (int i = 0; i < gl; i++) {
            idxes[hi] = i;
            calculateAuth(result, idxes, hi + 1, ri * gl + i, helpers);
        }
    }
}
