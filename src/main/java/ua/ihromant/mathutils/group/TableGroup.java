package ua.ihromant.mathutils.group;

public class TableGroup implements Group {
    private final int[][] operationTable;
    private final int[] inverses;

    public TableGroup(int[][] operationTable, int[] inverses) {
        this.operationTable = operationTable;
        this.inverses = inverses;
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
        return "Table" + order();
    }

    @Override
    public String elementName(int a) {
        return String.valueOf(a);
    }

    @Override
    public Group asTable() {
        return this;
    }
}
