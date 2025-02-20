package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.IntList;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TableGroup implements Group {
    private final int[][] operationTable;
    private final int[] inverses;
    private final int[][] squareRoots;
    private final String label;

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
}
