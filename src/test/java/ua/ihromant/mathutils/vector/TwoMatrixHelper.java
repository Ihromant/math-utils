package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoMatrixHelper implements ModuloMatrixHelper {
    private final int half;
    private final int unity;
    private final int matCount;
    private final FixBS invertible;
    private final int[] gl;
    private final LinearSpace mini;
    private final Map<Integer, Integer> mapGl;
    private final int[] v;

    public TwoMatrixHelper(int n) {
        this.half = n / 2;
        this.unity = calcUnity();
        this.matCount = 1 << (half * half);
        this.invertible = generateInvertibleAlt();
        this.gl = invertible.stream().toArray();
        System.out.println(gl.length);
        this.mini = LinearSpace.of(2, half);
        this.mapGl = generateInvertibleGlAlt(gl);
        this.v = Arrays.stream(gl).filter(a -> !hasEigenOne(a)).toArray();
        System.out.println(v.length);
    }

    @Override
    public int matCount() {
        return matCount;
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
    public int inv(int i) {
        return mapGl.get(i);
    }

    @Override
    public boolean hasInv(int i) {
        return mapGl.containsKey(i);
    }

    @Override
    public int[] gl() {
        return gl;
    }

    @Override
    public int[] v() {
        return v;
    }

    private int[][] toMatrix(int a) {
        int[][] result = new int[half][half];
        for (int i = 0; i < half * half; i++) {
            result[i / half][i % half] = a % 2;
            a = a / 2;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = half * half - 1; i >= 0; i--) {
            result = result * 2 + matrix[i / half][i % half];
        }
        return result;
    }

    public int calcUnity() {
        int[][] result = new int[half][half];
        for (int i = 0; i < half; i++) {
            result[i][i] = 1;
        }
        return fromMatrix(result);
    }

    private boolean hasEigenOne(int a) {
        return !invertible.get(sub(a, unity));
    }

    private Map<Integer, Integer> generateInvertibleGlAlt(int[] gl) {
        Map<Integer, Integer> result = new HashMap<>();
        for (int i : gl) {
            int[][] matrix = toMatrix(i);
            if (result.containsKey(i)) {
                continue;
            }
            try {
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, 2);
                int inv = fromMatrix(rev);
                result.put(i, inv);
                result.put(inv, i);
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private FixBS generateInvertibleAlt() {
        FixBS result = new FixBS(matCount);
        for (int i = 0; i < matCount; i++) {
            int[][] matrix = toMatrix(i);
            try {
                MatrixInverseFiniteField.inverseMatrix(matrix, 2);
                result.set(i);
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
