package ua.ihromant.mathutils.group;

import java.util.stream.IntStream;

public class DihedralGroup implements Group {
    private final int order;
    private final int[][] operationTable;

    public DihedralGroup(int order) {
        this.order = order;
        this.operationTable = IntStream.range(0, order()).mapToObj(i ->
                IntStream.range(0, order()).map(j -> addByDef(i, j)).toArray()).toArray(int[][]::new);
    }

    private int addByDef(int v1, int v2) {
        int alpha1 = v1 % order;
        int beta1 = v1 / order;
        int alpha2 = v2 % order;
        int beta2 = v2 / order;
        if (beta2 == 0) {
            return fromAlphaBeta(alpha1 + alpha2, beta1);
        } else {
            return fromAlphaBeta((order - alpha1 + alpha2) % order, (beta1 + beta2) % 2);
        }
    }

    private int fromAlphaBeta(int alpha, int beta) {
        return (beta % order) * order + (alpha % order);
    }

    public int add(int a, int b) {
        return operationTable[a][b];
    }

    @Override
    public int op(int a, int b) {
        return addByDef(a, b);
    }

    @Override
    public int inv(int a) {
        int alpha = a % order;
        int beta = a / order;
        return fromAlphaBeta(alpha == 0 || beta != 0 ? alpha : order - alpha, beta);
    }

    @Override
    public int order() {
        return 2 * order;
    }
}
