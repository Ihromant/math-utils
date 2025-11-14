package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;

import java.util.stream.IntStream;

public record CyclicGroup(int order) implements Group {
    @Override
    public int op(int a, int b) {
        return (a + b) % order;
    }

    @Override
    public int inv(int a) {
        return a == 0 ? 0 : order - a;
    }

    @Override
    public String name() {
        return "Z" + order;
    }

    @Override
    public String elementName(int a) {
        return String.valueOf(a);
    }

    @Override
    public int[][] auth() {
        return IntStream.range(1, order).filter(i -> Combinatorics.gcd(i, order) == 1)
                .mapToObj(i -> IntStream.range(0, order).map(j -> mul(i, j)).toArray()).toArray(int[][]::new);
    }

    public int determinant(int[][] matrix) {
        if (matrix.length == 2) {
            int res = (matrix[0][0] * matrix[1][1] - matrix[1][0] * matrix[0][1] + order * order) % order;
            if (invertible(res)) {
                return res;
            }
            return 0;
        } // TODO general algorithm is wrong for cyclic groups
        int n = matrix.length;
        int det = 1;
        boolean sign = true;

        for (int i = 0; i < n; i++) {
            if (!invertible(matrix[i][i])) {
                boolean swapped = false;
                for (int j = i + 1; j < n; j++) {
                    if (invertible(matrix[j][i])) {
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
                if (matrix[j][i] != 0) {
                    int scale = mul(matrix[j][i], inverse(matrix[i][i]));
                    for (int k = i; k < n; k++) {
                        matrix[j][k] = op(matrix[j][k], inv(mul(scale, matrix[i][k])));
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            det = mul(det, matrix[i][i]);
        }

        return sign ? det : inv(det);
    }

    private static void swapRows(int[][] matrix, int i, int j) {
        int[] temp = matrix[i];
        matrix[i] = matrix[j];
        matrix[j] = temp;
    }

    private boolean invertible(int a) {
        return Combinatorics.gcd(a, order) == 1;
    }

    private int inverse(int a) {
        return IntStream.range(0, order).filter(i -> mul(a, i) == 1).findAny().orElseThrow();
    }

    public int[][] multiply(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = op(sum, mul(first[i][k], second[k][j]));
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    public int[][] inverseMatrix(int[][] matrix) {
        int n = matrix.length;
        int[][] augmented = new int[n][2 * n];

        // Create the augmented matrix [A | I]
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1; // Identity matrix on the right
        }

        // Perform Gaussian elimination
        for (int i = 0; i < n; i++) {
            // Find the pivot element
            if (!invertible(augmented[i][i])) {
                // Swap with a row below if pivot is zero
                boolean swapped = false;
                for (int k = i + 1; k < n; k++) {
                    if (invertible(augmented[k][i])) {
                        int[] temp = augmented[i];
                        augmented[i] = augmented[k];
                        augmented[k] = temp;
                        swapped = true;
                        break;
                    }
                }
                if (!swapped) {
                    throw new ArithmeticException("Matrix is not invertible modulo " + order);
                }
            }

            // Normalize the pivot row
            int pivot = augmented[i][i];
            int pivotInverse = inverse(pivot);
            for (int j = 0; j < 2 * n; j++) {
                augmented[i][j] = (augmented[i][j] * pivotInverse) % order;
            }

            // Eliminate other rows
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    int factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] = (augmented[k][j] - factor * augmented[i][j] % order + order) % order;
                    }
                }
            }
        }

        // Extract the inverse matrix from the augmented matrix
        int[][] inverse = new int[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }

        return inverse;
    }
}
