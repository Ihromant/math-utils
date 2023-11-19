package ua.ihromant.mathutils.group;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record GroupProduct(List<Group> base) implements Group {
    public GroupProduct(int... base) {
        this(Arrays.stream(base).mapToObj(CyclicGroup::new).toArray(Group[]::new));
    }

    public GroupProduct(Group... base) {
        this(Arrays.asList(base));
    }

    public int fromArr(int... arr) {
        int result = 0;
        for (int i = 0; i < base.size(); i++) {
            result = result * base.get(i).order() + arr[i];
        }
        return result;
    }

    public int[] toArr(int x) {
        int[] result = new int[base.size()];
        for (int i = base.size() - 1; i >= 0; i--) {
            result[i] = x % base.get(i).order();
            x = x / base.get(i).order();
        }
        return result;
    }

    private int[] arrAdd(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = base.get(i).op(a[i], b[i]);
        }
        return result;
    }

    private int[] invArr(int[] a) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = base.get(i).inv(a[i]);
        }
        return result;
    }

    public int[] arrMul(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (a[i] * b[i]) % base.get(i).order();
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
        return base.stream().reduce(1, (pr, cg) -> pr * cg.order(), (pr1, pr2) -> pr1 * pr2);
    }

    @Override
    public String name() {
        return base.stream().map(Group::name).collect(Collectors.joining("x"));
    }

    @Override
    public String elementName(int a) {
        int[] arr = toArr(a);
        return IntStream.range(0, base.size()).mapToObj(i -> base.get(i).elementName(arr[i])).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int[][] auth() {
        throw new UnsupportedOperationException();
    }
}
