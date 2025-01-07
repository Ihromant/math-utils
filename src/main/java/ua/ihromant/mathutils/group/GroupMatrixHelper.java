package ua.ihromant.mathutils.group;

import lombok.Getter;
import lombok.experimental.Accessors;
import ua.ihromant.mathutils.Rational;
import ua.ihromant.mathutils.vector.LinearSpace;
import ua.ihromant.mathutils.vector.MatrixInverseFiniteField;

import java.util.Arrays;
import java.util.stream.IntStream;

@Getter
@Accessors(fluent = true)
public class GroupMatrixHelper {
    private final int[] sequence;
    private final int n;
    private final int mod;
    private final int p;
    private final int matCount;
    private final int[] mapGl;
    private final int[] gl;

    public GroupMatrixHelper(int... sequence) {
        if (!Arrays.equals(sequence, Arrays.stream(sequence).sorted().toArray())) {
            throw new IllegalArgumentException("Not sorted");
        }
        this.mod = sequence[0];
        this.n = sequence.length;
        this.p = Group.factorize(mod)[0];
        for (int i = 1; i < n; i++) {
            if (sequence[i] % mod != 0) {
                throw new IllegalArgumentException("Not multipliers");
            }
        }
        this.sequence = sequence;
        this.matCount = Arrays.stream(sequence).map(el -> LinearSpace.pow(el, n)).reduce(1, (a, b) -> a * b);
        this.mapGl = generateMapGl();
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        System.out.println(gl.length);
    }

    private int[][] toMatrix(int a) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            int row = i / n;
            result[row][i % n] = a % sequence[row];
            a = a / sequence[i / n];
        }
        return result;
    }

    public int[] toVec(int a) {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = a % sequence[i];
            a = a / sequence[i];
        }
        return result;
    }

    public int fromVec(int[] crd) {
        int result = 0;
        for (int i = crd.length - 1; i >= 0; i--) {
            result = result * sequence[i] + crd[i];
        }
        return result;
    }

    public int mulVec(int a, int vec) {
        return fromVec(multiply(toMatrix(a), toVec(vec)));
    }

    private int[] multiply(int[][] first, int[] arr) {
        int[] result = new int[first.length];
        for (int i = 0; i < first.length; i++) {
            int sum = 0;
            for (int j = 0; j < first.length; j++) {
                sum = sum + first[i][j] * arr[j];
            }
            result[i] = sum % sequence[i];
        }
        return result;
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        for (int i = 0; i < matCount; i++) {
            try {
                int[][] matrix = toMatrix(i);
                int[][] conjMatrix = new int[n][n];
                for (int j = 0; j < n; j++) {
                    conjMatrix[j][j] = sequence[j] / p;
                }
                int[][] multiplied = multiply(matrix, conjMatrix);
                int[][] divided = new int[n][n];
                for (int j = 0; j < n; j++) {
                    int rowMultiplier = sequence[j] / p;
                    for (int k = 0; k < n; k++) {
                        if (multiplied[j][k] % rowMultiplier != 0) {
                            throw new ArithmeticException();
                        }
                        divided[j][k] = multiplied[j][k] / rowMultiplier;
                    }
                }
                int det = determinant(divided);
                if (det % p == 0) {
                    throw new ArithmeticException();
                }
                result[i] = i;
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private static int determinant(int[][] intMatrix) {
        Rational[][] matrix = Arrays.stream(intMatrix).map(arr -> Arrays.stream(arr).mapToObj(Rational::of).toArray(Rational[]::new)).toArray(Rational[][]::new);
        int n = matrix.length;
        Rational det = Rational.of(1);
        boolean sign = true;

        for (int i = 0; i < n; i++) {
            if (matrix[i][i].isZero()) {
                boolean swapped = false;
                for (int j = i + 1; j < n; j++) {
                    if (!matrix[j][i].isZero()) {
                        swapRows(matrix, i, j);
                        sign = !sign;
                        swapped = true;
                        break;
                    }
                }
                if (!swapped) {
                    return 0;
                }
            }

            for (int j = i + 1; j < n; j++) {
                if (!matrix[j][i].isZero()) {
                    Rational scale = matrix[j][i].div(matrix[i][i]);
                    for (int k = i; k < n; k++) {
                        matrix[j][k] = matrix[j][k].sub(scale.mul(matrix[i][k]));
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            det = det.mul(matrix[i][i]);
        }

        return sign ? det.asInt() : det.neg().asInt();
    }

    private static void swapRows(Rational[][] matrix, int i, int j) {
        Rational[] temp = matrix[i];
        matrix[i] = matrix[j];
        matrix[j] = temp;
    }

    public static void main(String[] args) {
        int[][] matrix = {
                {4, 3, 2},
                {3, 5, 1},
                {2, 1, 3}
        };

        System.out.println(Arrays.deepToString(MatrixInverseFiniteField.inverseMatrix(matrix)));
        int det = determinant(matrix);
        System.out.println("Determinant: " + det);

        int[][] matrix1 = {
                {4, 3, 2},
                {3, 5, 1},
                {2, 1, 2}
        };

        System.out.println(Arrays.deepToString(MatrixInverseFiniteField.inverseMatrix(matrix1)));
        int det1 = determinant(matrix1);
        System.out.println("Determinant: " + det1);

        GroupMatrixHelper helper = new GroupMatrixHelper(3, 3, 9);
        Arrays.stream(helper.gl).forEach(i -> System.out.println(i + "=" + helper.mapGl[i]));
    }

    private int[][] multiply(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = sum + first[i][k] * second[k][j];
                }
                result[i][j] = sum % sequence[i];
            }
        }
        return result;
    }
}
