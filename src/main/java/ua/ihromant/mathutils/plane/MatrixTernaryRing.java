package ua.ihromant.mathutils.plane;

public record MatrixTernaryRing(int[][][] matrix, Quad base) implements TernaryRing {
    @Override
    public int op(int x, int a, int b) {
        return matrix[x][a][b];
    }

    @Override
    public int order() {
        return matrix.length;
    }
}
