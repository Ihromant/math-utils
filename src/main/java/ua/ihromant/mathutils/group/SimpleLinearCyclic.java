package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.vector.LinearSpace;

import java.util.Arrays;
import java.util.stream.IntStream;

public class SimpleLinearCyclic implements Group {
    private final int dim;
    private final CyclicGroup gr;
    private final int matCount;
    private final int[] mapGl;
    private final int[] gl;

    public SimpleLinearCyclic(int dim, int order) {
        this.dim = dim;
        this.gr = new CyclicGroup(order);
        this.matCount = LinearSpace.pow(order, dim * dim);
        int one = unity();
        this.mapGl = generateMapGl();
        int idx = mapGl[one];
        int fst = IntStream.range(0, matCount).filter(i -> mapGl[i] == 0).findFirst().orElseThrow();
        mapGl[one] = 0;
        mapGl[fst] = idx;
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] >= 0).toArray();
        this.gl[0] = one;
        this.gl[idx] = fst;
    }

    private int unity() {
        int[][] result = new int[dim][dim];
        for (int i = 0; i < dim; i++) {
            result[i][i] = 1;
        }
        return fromMatrix(result);
    }

    public int[][] asMatrix(int a) {
        return toMatrix(gl[a]);
    }

    public int asElem(int[][] matrix) {
        return mapGl[fromMatrix(matrix)];
    }

    private int[][] toMatrix(int a) {
        int[][] result = new int[dim][dim];
        for (int i = 0; i < dim * dim; i++) {
            int row = i / dim;
            result[row][i % dim] = a % gr.order();
            a = a / gr.order();
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = dim * dim - 1; i >= 0; i--) {
            result = result * gr.order() + matrix[i / dim][i % dim];
        }
        return result;
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        int idx = 0;
        for (int i = 0; i < matCount; i++) {
            int[][] matrix = toMatrix(i);
            int det = gr.determinant(matrix);
            if (det == 1) {
                result[i] = idx++;
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    @Override
    public int op(int a, int b) {
        return mapGl[fromMatrix(gr.multiply(toMatrix(gl[a]), toMatrix(gl[b])))];
    }

    @Override
    public int inv(int a) {
        return mapGl[fromMatrix(gr.inverseMatrix(toMatrix(gl[a])))];
    }

    @Override
    public int order() {
        return gl.length;
    }

    @Override
    public String name() {
        return "SL(" + dim + "," + gr.order() + ")";
    }

    @Override
    public String elementName(int a) {
        return Arrays.deepToString(toMatrix(gl[a]));
    }
}