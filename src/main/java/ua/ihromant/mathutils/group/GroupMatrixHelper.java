package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Rational;
import ua.ihromant.mathutils.vector.LinearSpace;
import ua.ihromant.mathutils.vector.MatrixInverseFiniteField;

import java.util.Arrays;
import java.util.stream.IntStream;

public class GroupMatrixHelper {
    private final int[] sequence;
    private final int n;
    private final int mod;
    private final int matCount;
    private final int[] mapGl;
    private final int[] gl;

    public GroupMatrixHelper(int... sequence) {
        if (!Arrays.equals(sequence, Arrays.stream(sequence).sorted().toArray())) {
            throw new IllegalArgumentException("Not sorted");
        }
        this.mod = sequence[0];
        this.n = sequence.length;
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

    private Rational[][] toMatrix(int a) {
        Rational[][] result = new Rational[n][n];
        for (int i = 0; i < n * n; i++) {
            int row = i / n;
            result[row][i % n] = Rational.of(a % sequence[row]);
            a = a / sequence[row];
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = n * n - 1; i >= 0; i--) {
            int row = i / n;
            result = result * sequence[row] + matrix[row][i % n];
        }
        return result;
    }

    private int[][] toIntMatrix(int a) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            int row = i / n;
            result[row][i % n] = a % sequence[row];
            a = a / sequence[i / n];
        }
        return result;
    }

    private int[] toVec(int a) {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = a % sequence[i];
            a = a / sequence[i];
        }
        return result;
    }

    private int fromVec(int[] crd) {
        int result = 0;
        for (int i = crd.length - 1; i >= 0; i--) {
            result = result * sequence[i] + crd[i];
        }
        return result;
    }

    public int mulVec(int a, int vec) {
        return fromVec(multiply(toIntMatrix(a), toVec(vec)));
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

    private int[] multiplierInverses() {
        int last = sequence[sequence.length - 1];
        int[] result = new int[last];
        for (int i = 0; i < last; i++) {
            for (int j = 0; j < last; j++) {
                if (i * j % last == 1) {
                    result[i] = j;
                }
            }
        }
        return result;
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        int[] inverses = multiplierInverses();
        for (int i = 0; i < matCount; i++) {
            if (result[i] > 0) {
                continue;
            }
            try {
                Rational[][] matrix = toMatrix(i);
                Rational[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix);
                int[][] intRev = new int[n][n];
                for (int row = 0; row < n; row++) {
                    for (int col = 0; col < n; col++) {
                        Rational r = rev[row][col];
                        int denom = (int) r.denom();
                        if (inverses[Math.floorMod(denom, inverses.length)] == 0) {
                            throw new ArithmeticException();
                        }
                        intRev[row][col] = Math.floorMod(r.numer() * inverses[denom % inverses.length], sequence[row]);
                    }
                }
                int inv = fromMatrix(intRev);
                result[i] = inv;
                result[inv] = i;
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    // TODO this mutates matrix, avoid that
    private static int determinant(Rational[][] matrix) {
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
        Rational[][] matrix = {
                {Rational.of(4), Rational.of(3), Rational.of(2)},
                {Rational.of(3), Rational.of(5), Rational.of(1)},
                {Rational.of(2), Rational.of(1), Rational.of(3)}
        };

        System.out.println(Arrays.deepToString(MatrixInverseFiniteField.inverseMatrix(matrix)));
        int det = determinant(matrix);
        System.out.println("Determinant: " + det);

        Rational[][] matrix1 = {
                {Rational.of(4), Rational.of(3), Rational.of(2)},
                {Rational.of(3), Rational.of(5), Rational.of(1)},
                {Rational.of(2), Rational.of(1), Rational.of(2)}
        };

        System.out.println(Arrays.deepToString(MatrixInverseFiniteField.inverseMatrix(matrix1)));
        int det1 = determinant(matrix1);
        System.out.println("Determinant: " + det1);

        GroupMatrixHelper helper = new GroupMatrixHelper(3, 3, 9);
        System.out.println(Arrays.toString(helper.multiplierInverses()));
        Arrays.stream(helper.gl).forEach(i -> System.out.println(i + "=" + helper.mapGl[i]));
        //System.out.println(Arrays.deepToString(helper.toIntMatrix(398853)));
    }
}
