package ua.ihromant.mathutils.group;

import lombok.Setter;
import ua.ihromant.mathutils.IntList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class TableGroup implements Group {
    private final int[][] operationTable;
    private final int[] inverses;
    private final int[][] squareRoots;
    private final int[] orders;
    private final int[][] powers;
    private final String label;
    @Setter
    private int[][] cachedAuth;
    private Map<Integer, List<SubGroup>> cachedSubGroups;

    public TableGroup(int[][] operationTable) {
        this(null, operationTable);
    }

    public TableGroup(String label, int[][] operationTable) {
        this.operationTable = operationTable;
        this.inverses = Arrays.stream(operationTable).mapToInt(arr -> IntStream.range(0, operationTable.length).filter(j -> arr[j] == 0).findAny().orElseThrow()).toArray();
        IntList[] roots = IntStream.range(0, operationTable.length).mapToObj(i -> new IntList(operationTable.length)).toArray(IntList[]::new);
        for (int i = 0; i < operationTable.length; i++) {
            int sqr = operationTable[i][i];
            roots[sqr].add(i);
        }
        this.squareRoots = Arrays.stream(roots).map(IntList::toArray).toArray(int[][]::new);
        this.orders = new int[operationTable.length];
        this.powers = new int[operationTable.length][operationTable.length];
        for (int el = 0; el < operationTable.length; el++) {
            int comb = 0;
            for (int ord = 1; ord < operationTable.length; ord++) {
                comb = op(comb, el);
                if (comb == 0 && orders[el] == 0) {
                    orders[el] = ord;
                }
                powers[el][ord] = comb;
            }
        }
        this.label = label;
    }

    @Override
    public int op(int a, int b) {
        return operationTable[a][b];
    }

    @Override
    public int inv(int a) {
        return inverses[a];
    }

    @Override
    public int order() {
        return inverses.length;
    }

    @Override
    public int order(int a) {
        return orders[a];
    }

    @Override
    public int mul(int a, int cff) {
        return powers[a][cff];
    }

    @Override
    public String name() {
        return label == null ? "Table" + order() : label;
    }

    @Override
    public String elementName(int a) {
        return String.valueOf(a);
    }

    @Override
    public TableGroup asTable() {
        return this;
    }

    public int[][] table() {
        return operationTable;
    }

    @Override
    public int[] squareRoots(int from) {
        return squareRoots[from];
    }

    @Override
    public int[][] auth() {
        if (cachedAuth == null) {
            cachedAuth = Group.super.auth();
        }
        return cachedAuth;
    }

    @Override
    public Map<Integer, List<SubGroup>> groupedSubGroups() {
        if (cachedSubGroups == null) {
            cachedSubGroups = Group.super.groupedSubGroups();
        }
        return cachedSubGroups;
    }

    public TableGroup applyPerm(int[] perm) {
        if (perm[0] != 0) {
            throw new IllegalArgumentException();
        }
        int[][] opTable = new int[operationTable.length][operationTable.length];
        for (int i = 0; i < operationTable.length; i++) {
            for (int j = 0; j < operationTable.length; j++) {
                opTable[perm[i]][perm[j]] = perm[operationTable[i][j]];
            }
        }
        return new TableGroup(label, opTable);
    }
}
