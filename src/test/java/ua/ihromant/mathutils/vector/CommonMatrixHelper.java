package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class CommonMatrixHelper implements ModuloMatrixHelper {
    private final int p;
    private final int half;
    private final int unity;
    private final int matCount;
    private final FixBS invertible;
    private final int[] gl;
    private final int[][] mulMatrix;
    private final int[][] subMatrix;
    private final int[][] mulVecMatrix;
    private final int[] idxArr;
    private final Map<Integer, Integer> mapGl;
    private final int[] v;

    public CommonMatrixHelper(int p, int n) {
        this.p = p;
        this.half = n / 2;
        this.unity = calcUnity();
        this.matCount = LinearSpace.pow(p, half * half);
        this.invertible = generateInvertibleAlt();
        this.gl = invertible.stream().toArray();
        System.out.println(gl.length);
        this.idxArr = new int[matCount];
        LinearSpace mini = LinearSpace.of(p, half);
        Arrays.fill(idxArr, -1);
        for (int i = 0; i < gl.length; i++) {
            idxArr[gl[i]] = i;
        }
        this.mulMatrix = new int[gl.length][gl.length];
        this.subMatrix = new int[gl.length][gl.length];
        this.mulVecMatrix = new int[gl.length][mini.cardinality()];
        IntStream.range(0, gl.length).parallel().forEach(i -> {
            int[][] iMat = toMatrix(gl[i]);
            for (int j = 0; j < gl.length; j++) {
                int[][] jMat = toMatrix(gl[j]);
                mulMatrix[i][j] = fromMatrix(multiply(iMat, jMat));
                subMatrix[i][j] = fromMatrix(subtract(iMat, jMat));
            }
            for (int j = 0; j < mini.cardinality(); j++) {
                int[] vec = mini.toCrd(j);
                mulVecMatrix[i][j] = mini.fromCrd(multiply(iMat, vec));
            }
        });
        this.mapGl = generateInvertibleGlAlt(gl);
        this.v = Arrays.stream(gl).filter(a -> !hasEigenOne(a)).toArray();
        System.out.println(v.length);
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
    public int sub(int i, int j) {
        return subMatrix[idxArr[i]][idxArr[j]];
    }

    @Override
    public int mul(int i, int j) {
        return mulMatrix[idxArr[i]][idxArr[j]];
    }

    @Override
    public int mulVec(int a, int vec) {
        return mulVecMatrix[idxArr[a]][vec];
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
            result[i / half][i % half] = a % p;
            a = a / p;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = half * half - 1; i >= 0; i--) {
            result = result * p + matrix[i / half][i % half];
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
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, p);
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
                MatrixInverseFiniteField.inverseMatrix(matrix, p);
                result.set(i);
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
