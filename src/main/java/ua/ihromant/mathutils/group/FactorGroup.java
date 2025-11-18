package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FactorGroup implements Group {
    private final Group g;
    private final FixBS elems;
    private final FixBS[] cosets;
    private final TableGroup table;

    public FactorGroup(Group g, FixBS elems) {
        this.g = g;
        this.elems = elems;
        SubGroup sg = new SubGroup(g, elems);
        if (!sg.isNormal()) {
            throw new IllegalArgumentException("Not normal");
        }
        Set<FixBS> set = new HashSet<>();
        for (int i = 0; i < g.order(); i++) {
            FixBS fbs = new FixBS(g.order());
            for (int el = elems.nextSetBit(0); el >= 0; el = elems.nextSetBit(el + 1)) {
                fbs.set(g.op(el, i));
            }
            set.add(fbs);
        }
        this.cosets = set.toArray(FixBS[]::new);
        Arrays.sort(cosets);
        int[] idxes = new int[g.order()];
        for (int i = 0; i < cosets.length; i++) {
            FixBS coset = cosets[i];
            for (int el = coset.nextSetBit(0); el >= 0; el = coset.nextSetBit(el + 1)) {
                idxes[el] = i;
            }
        }
        int[][] table = new int[cosets.length][cosets.length];
        for (int i = 0; i < cosets.length; i++) {
            for (int j = 0; j < cosets.length; j++) {
                table[i][j] = idxes[g.op(cosets[i].nextSetBit(0), cosets[j].nextSetBit(0))];
            }
        }
        this.table = new TableGroup(table);
    }

    @Override
    public int op(int a, int b) {
        return table.op(a, b);
    }

    @Override
    public int inv(int a) {
        return table.inv(a);
    }

    @Override
    public int[] squareRoots(int from) {
        return table.squareRoots(from);
    }

    @Override
    public int order(int a) {
        return table.order(a);
    }

    @Override
    public int mul(int a, int cff) {
        return table.mul(a, cff);
    }

    @Override
    public TableGroup asTable() {
        return table;
    }

    @Override
    public int[][] auth() {
        return table.auth();
    }

    @Override
    public Map<Integer, List<SubGroup>> groupedSubGroups() {
        return table.groupedSubGroups();
    }

    @Override
    public int order() {
        return cosets.length;
    }

    public FixBS coset(int a) {
        return cosets[a];
    }

    @Override
    public String name() {
        return "Fac(" + g.name() + "," + elems + ")";
    }

    @Override
    public String elementName(int a) {
        return Arrays.toString(cosets[a].stream().mapToObj(g::elementName).toArray(String[]::new));
    }
}
