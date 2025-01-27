package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

public class SubGroup implements Group {
    private final Group group;
    private final FixBS elems;
    private final int[] map;
    private final int[] reverse;

    public SubGroup(Group group, FixBS elems) {
        this.group = group;
        this.elems = elems;
        this.map = new int[elems.cardinality()];
        this.reverse = new int[group.order()];
        int cnt = 0;
        for (int i = elems.nextSetBit(0); i >= 0; i = elems.nextSetBit(i+1)) {
            map[cnt] = i;
            reverse[i] = cnt++;
        }
    }

    @Override
    public int op(int a, int b) {
        return reverse[group.op(map[a], map[b])];
    }

    @Override
    public int inv(int a) {
        return reverse[group.inv(map[a])];
    }

    @Override
    public int order() {
        return map.length;
    }

    @Override
    public String name() {
        return "Subgroup of " + group.name() + " elems " + elems;
    }

    @Override
    public String elementName(int a) {
        return group.elementName(map[a]);
    }

    @Override
    public int[][] auth() {
        throw new UnsupportedOperationException();
    }

    public boolean isNormal() {
        for (int n : map) {
            for (int g = 0; g < group.order(); g++) {
                if (!elems.get(group.op(g, group.op(n, group.inv(g))))) {
                    return false;
                }
            }
        }
        return true;
    }
}
