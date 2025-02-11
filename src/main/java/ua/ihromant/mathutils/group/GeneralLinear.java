package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.vector.LinearSpace;
import ua.ihromant.mathutils.vector.MatrixInverseFiniteField;

import java.util.Arrays;
import java.util.stream.IntStream;

public class GeneralLinear implements Group {
    private final int dim;
    private final GaloisField fd;
    private final int matCount;
    private final int[] mapGl;
    private final int[] gl;

    public GeneralLinear(int dim, GaloisField fd) {
        this.dim = dim;
        this.fd = fd;
        this.matCount = LinearSpace.pow(fd.cardinality(), dim * dim);
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

    private int[][] toMatrix(int a) {
        int[][] result = new int[dim][dim];
        for (int i = 0; i < dim * dim; i++) {
            int row = i / dim;
            result[row][i % dim] = a % fd.cardinality();
            a = a / fd.cardinality();
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = dim * dim - 1; i >= 0; i--) {
            result = result * fd.cardinality() + matrix[i / dim][i % dim];
        }
        return result;
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        int idx = 0;
        for (int i = 0; i < matCount; i++) {
            int[][] matrix = toMatrix(i);
            int det = fd.determinant(matrix);
            if (det == 0) {
                result[i] = -1;
            } else {
                result[i] = idx++;
            }
        }
        return result;
    }

    @Override
    public int op(int a, int b) {
        return mapGl[fromMatrix(fd.multiply(toMatrix(gl[a]), toMatrix(gl[b])))];
    }

    @Override
    public int inv(int a) {
        return mapGl[fromMatrix(MatrixInverseFiniteField.inverseMatrix(toMatrix(gl[a]), fd))];
    }

    @Override
    public int order() {
        return gl.length;
    }

    @Override
    public String name() {
        return "GL(" + dim + "," + fd.cardinality() + ")";
    }

    @Override
    public String elementName(int a) {
        return Arrays.deepToString(toMatrix(gl[a]));
    }
}
