package ua.ihromant.mathutils.vector;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TableMatrixHelper implements ModuloMatrixHelper {
    private final int p;
    private final int n;
    private final int unity;
    private final int matCount;
    private final int[] gl;
    private final int[][] mulMatrix;
    private final int[][] addMatrix;
    private final int[][] subMatrix;
    private final int[][] mulVecMatrix;
    private final int[] mapGl;
    private final int[] v;

    public TableMatrixHelper(int p, int n) {
        this.p = p;
        this.n = n;
        this.unity = calcUnity();
        this.matCount = LinearSpace.pow(p, n * n);
        this.mapGl = generateMapGl();
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        LinearSpace mini = LinearSpace.of(p, n);
        this.mulMatrix = new int[matCount][matCount];
        this.addMatrix = new int[matCount][matCount];
        this.subMatrix = new int[matCount][matCount];
        this.mulVecMatrix = new int[matCount][mini.cardinality()];
        IntStream.range(0, matCount).parallel().forEach(i -> {
            int[][] iMat = toMatrix(i);
            for (int j = 0; j < matCount; j++) {
                int[][] jMat = toMatrix(j);
                mulMatrix[i][j] = fromMatrix(multiply(iMat, jMat));
                addMatrix[i][j] = fromMatrix(add(iMat, jMat));
                subMatrix[i][j] = fromMatrix(subtract(iMat, jMat));
            }
            for (int j = 0; j < mini.cardinality(); j++) {
                int[] vec = mini.toCrd(j);
                mulVecMatrix[i][j] = mini.fromCrd(multiply(iMat, vec));
            }
        });
        this.v = Arrays.stream(gl).filter(a -> mapGl[sub(a, unity)] > 0).toArray();
    }

    @Override
    public int p() {
        return p;
    }

    @Override
    public int n() {
        return n;
    }

    @Override
    public int unity() {
        return unity;
    }

    @Override
    public int matCount() {
        return matCount;
    }

    @Override
    public int add(int i, int j) {
        return addMatrix[i][j];
    }

    @Override
    public int sub(int i, int j) {
        return subMatrix[i][j];
    }

    @Override
    public int mul(int i, int j) {
        return mulMatrix[i][j];
    }

    @Override
    public int mulVec(int a, int vec) {
        return mulVecMatrix[a][vec];
    }

    @Override
    public int mulCff(int a, int cff) {
        int[][] matrix = toMatrix(a);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j] = (matrix[i][j] * cff) % p;
            }
        }
        return fromMatrix(matrix);
    }

    @Override
    public int inv(int i) {
        return mapGl[i];
    }

    @Override
    public boolean hasInv(int i) {
        return mapGl[i] > 0;
    }

    @Override
    public int[] gl() {
        return gl;
    }

    @Override
    public int[] v() {
        return v;
    }

    private int calcUnity() {
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = 1;
        }
        return fromMatrix(result);
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        for (int i  = 0; i < matCount; i++) {
            if (result[i] > 0) {
                continue;
            }
            try {
                int[][] matrix = toMatrix(i);
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, p);
                int inv = fromMatrix(rev);
                result[i] = inv;
                result[inv] = i;
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private int[][] subtract(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                result[i][j] = (p + first[i][j] - second[i][j]) % p;
            }
        }
        return result;
    }

    private int[][] add(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                result[i][j] = (p + first[i][j] + second[i][j]) % p;
            }
        }
        return result;
    }

    private int[] multiply(int[][] first, int[] arr) {
        int[] result = new int[first.length];
        for (int i = 0; i < first.length; i++) {
            int sum = 0;
            for (int j = 0; j < first.length; j++) {
                sum = sum + first[i][j] * arr[j];
            }
            result[i] = sum % p;
        }
        return result;
    }

    private int[][] multiply(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = sum + first[i][k] * second[k][j];
                }
                result[i][j] = sum % p;
            }
        }
        return result;
    }
}
