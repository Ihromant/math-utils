package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

public class SubGroup implements Group {
    private final Group group;
    private final FixBS elems;
    private final int[] arr;
    private final int[] reverse;
    private FixBS whiteList;

    public SubGroup(Group group, FixBS elems) {
        this.group = group;
        this.elems = elems;
        this.arr = new int[elems.cardinality()];
        this.reverse = new int[group.order()];
        int cnt = 0;
        for (int i = elems.nextSetBit(0); i >= 0; i = elems.nextSetBit(i+1)) {
            arr[cnt] = i;
            reverse[i] = cnt++;
        }
    }

    public Group group() {
        return group;
    }

    public FixBS elems() {
        return elems;
    }

    public int[] arr() {
        return arr;
    }

    @Override
    public int op(int a, int b) {
        return reverse[group.op(arr[a], arr[b])];
    }

    @Override
    public int inv(int a) {
        return reverse[group.inv(arr[a])];
    }

    @Override
    public int order() {
        return arr.length;
    }

    @Override
    public String name() {
        return "Subgroup of " + group.name() + " elems " + elems;
    }

    @Override
    public String elementName(int a) {
        return group.elementName(arr[a]);
    }

    @Override
    public int[][] auth() {
        throw new UnsupportedOperationException();
    }

    public boolean isNormal() {
        for (int n : arr) {
            for (int g = 0; g < group.order(); g++) {
                if (!elems.get(group.op(g, group.op(n, group.inv(g))))) {
                    return false;
                }
            }
        }
        return true;
    }

    public FixBS whiteList(int v) {
        if (whiteList == null) {
            whiteList = new FixBS(v);
            whiteList.set(1, v);
            for (int i = 0; i < arr.length; i++) {
                int x = arr[i];
                whiteList.clear(x);
                for (int j = i + 1; j < arr.length; j++) {
                    int y = arr[j];
                    int sqr = group.op(y, group.inv(x));
                    for (int root : group.squareRoots(sqr)) {
                        whiteList.clear(group.op(root, x));
                    }
                    int invSqr = group.op(x, group.inv(y));
                    for (int root : group.squareRoots(invSqr)) {
                        whiteList.clear(group.op(root, y));
                    }
                }
            }
        }
        return whiteList;
    }
}
