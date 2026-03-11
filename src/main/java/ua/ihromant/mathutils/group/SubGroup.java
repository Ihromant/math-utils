package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;

public class SubGroup implements Group {
    private final Group group;
    private final FixBS elems;
    private final int[] arr;
    private final int[] reverse;

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

    public SubGroup normalizer() {
        FixBS nEls = new FixBS(group.order());
        for (int i = 0; i < group.order(); i++) {
            FixBS conj = new FixBS(group.order());
            for (int el : arr) {
                conj.set(group.conjugate(el, i));
            }
            if (conj.equals(elems)) {
                nEls.set(i);
            }
        }
        return new SubGroup(group, nEls);
    }

    @Override
    public String toString() {
        return "SubGroup[" + group.name() + ", " + elems + ']';
    }

    public List<FixBS> leftCosets() {
        int ord = group.order();
        List<FixBS> result = new ArrayList<>();
        ex: for (int g = 0; g < ord; g++) {
            FixBS cos = new FixBS(ord);
            for (int h : arr) {
                int mul = group.op(g, h);
                if (mul < g) {
                    continue ex;
                }
                cos.set(mul);
            }
            result.add(cos);
        }
        return result;
    }

    public List<FixBS> rightCosets() {
        int ord = group.order();
        List<FixBS> result = new ArrayList<>();
        ex: for (int g = 0; g < ord; g++) {
            FixBS cos = new FixBS(ord);
            for (int h : arr) {
                int mul = group.op(h, g);
                if (mul < g) {
                    continue ex;
                }
                cos.set(mul);
            }
            result.add(cos);
        }
        return result;
    }
}
