package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record CyclicProduct(int... base) implements Group {
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

    private static class HelperInfo {
        private final int bs;
        private final List<Integer> elems;
        private final FixBS idx;
        private GroupMatrixHelper helper;

        public HelperInfo(int bs, int len) {
            this.bs = bs;
            this.elems = new ArrayList<>(len);
            this.idx = new FixBS(len);
        }
    }

    @Override
    public int[][] auth() {
        int[][] factors = Arrays.stream(base).mapToObj(Group::factorize).toArray(int[][]::new);
        if (Arrays.stream(factors).anyMatch(arr -> arr.length == 0 || arr[0] != arr[arr.length - 1])) {
            throw new IllegalArgumentException("Only prime powers are allowed"); // TODO also require to be sorted
        }
        List<HelperInfo> helpers = new ArrayList<>();
        for (int i = 0; i < base.length; i++) {
            int b = factors[i][0];
            Optional<HelperInfo> opt = helpers.stream().filter(h -> h.bs == b).findFirst();
            HelperInfo info;
            if (opt.isPresent()) {
                info = opt.get();
            } else {
                info = new HelperInfo(b, base.length);
                helpers.add(info);
            }
            info.elems.add(base[i]);
            info.idx.set(i);
        }
        for (HelperInfo info : helpers) {
            info.helper = new GroupMatrixHelper(info.elems.stream().mapToInt(Integer::intValue).toArray());
        }
        int order = order();
        int[][] result = new int[helpers.stream().mapToInt(h -> h.helper.gl().length).reduce(1, (a, b) -> a * b)][order];
        calculateAuth(result, helpers);
        Arrays.parallelSort(result, Group::compareArr);
        return result;
    }

    private void calculateAuth(int[][] result, List<HelperInfo> helpers) {
        for (int i = 0; i < result.length; i++) {
            int[] cff = new int[helpers.size()];
            int autIdx = i;
            for (int j = 0; j < helpers.size(); j++) {
                int len = helpers.get(j).helper.gl().length;
                cff[j] = autIdx % len;
                autIdx = autIdx / len;
            }
            for (int el = 0; el < order(); el++) {
                int[] arr = toArr(el);
                int[] mapped = new int[arr.length];
                for (int j = 0; j < helpers.size(); j++) {
                    HelperInfo info = helpers.get(j);
                    GroupMatrixHelper helper = info.helper;
                    int[] vec = new int[info.elems.size()];
                    int cnt = 0;
                    for (int k = info.idx.nextSetBit(0); k >= 0; k = info.idx.nextSetBit(k + 1)) {
                        vec[cnt++] = arr[k];
                    }
                    int[] vecMapped = helper.toVec(helper.mulVec(helper.gl()[cff[j]], helper.fromVec(vec)));
                    int cf = -1;
                    for (int k = 0; k < vec.length; k++) {
                        mapped[(cf = info.idx.nextSetBit(cf + 1))] = vecMapped[k];
                    }
                }
                result[i][el] = fromArr(mapped);
            }
        }
    }
}
