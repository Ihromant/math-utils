package ua.ihromant.mathutils.vector;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TwoMatrixHelper implements ModuloMatrixHelper {
    private final int n;
    private final int unity;
    private final int matCount;
    private final int[] gl;
    private final LinearSpace mini;
    private final int[] mapGl;
    private final int[] v;
    private final int[] vIdxes;

    public TwoMatrixHelper(int n) {
        this.n = n;
        this.unity = calcUnity();
        this.matCount = 1 << (this.n * this.n);
        this.mini = LinearSpace.of(2, this.n);
        this.mapGl = generateMapGl();
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        System.out.println(gl.length);
        this.v = Arrays.stream(gl).filter(a -> mapGl[sub(a, unity)] > 0).toArray();
        this.vIdxes = new int[matCount];
        for (int i = 0; i < v.length; i++) {
            vIdxes[v[i]] = i;
        }
        System.out.println(v.length);
    }

    public TwoMatrixHelper(int n, int[] mapGl) {
        this.n = n;
        this.unity = calcUnity();
        this.matCount = 1 << (this.n * this.n);
        this.mini = LinearSpace.of(2, this.n);
        this.mapGl = mapGl;
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        this.v = Arrays.stream(gl).filter(a -> mapGl[sub(a, unity)] > 0).toArray();
        this.vIdxes = new int[matCount];
        for (int i = 0; i < v.length; i++) {
            vIdxes[v[i]] = i;
        }
        System.out.println(v.length);
    }

    @Override
    public int p() {
        return 2;
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
        return i ^ j;
    }

    @Override
    public int sub(int i, int j) {
        return i ^ j;
    }

    @Override
    public int mul(int i, int j) {
        return fromMatrix(multiply(toMatrix(i), toMatrix(j)));
    }

    @Override
    public int mulVec(int a, int vec) {
        return mini.fromCrd(multiply(toMatrix(a), mini.toCrd(vec)));
    }

    @Override
    public int mulCff(int a, int cff) {
        return cff % 2 == 0 ? 0 : a;
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

    @Override
    public int[] vIdxes() {
        return vIdxes;
    }

    private int[][] toMatrix(int a) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % 2;
            a = a / 2;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = n * n - 1; i >= 0; i--) {
            result = result * 2 + matrix[i / n][i % n];
        }
        return result;
    }

    public int calcUnity() {
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = 1;
        }
        return fromMatrix(result);
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        for (int i = 0; i < matCount; i++) {
            if (result[i] > 0) {
                continue;
            }
            try {
                int[][] matrix = toMatrix(i);
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, 2);
                int inv = fromMatrix(rev);
                result[i] = inv;
                result[inv] = i;
            } catch (ArithmeticException e) {
                // ok
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
            result[i] = sum % 2;
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
                result[i][j] = sum % 2;
            }
        }
        return result;
    }
}