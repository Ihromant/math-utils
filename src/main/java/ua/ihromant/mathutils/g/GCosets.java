package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GCosets {
    private final int[][] cosets;
    private final int[] idx;

    public GCosets(SubGroup sg) {
        Group gr = sg.group();
        Set<FixBS> set = new HashSet<>();
        int order = gr.order();
        for (int i = 0; i < order; i++) {
            FixBS coset = new FixBS(order);
            for (int el : sg.arr()) {
                coset.set(gr.op(i, el));
            }
            set.add(coset);
        }
        this.cosets = set.stream().map(FixBS::toArray).toArray(int[][]::new);
        Arrays.sort(cosets, Group::compareArr);
        this.idx = new int[order];
        for (int i = 0; i < cosets.length; i++) {
            for (int el : cosets[i]) {
                idx[el] = i;
            }
        }
    }

    public int xToG(int x) {
        return cosets[x][0];
    }

    public int gToX(int g) {
        return idx[g];
    }

    public int[] xToGs(int x) {
        return cosets[x];
    }

    public int cosetCount() {
        return cosets.length;
    }

    public int[][] cosets() {
        return cosets;
    }
}
