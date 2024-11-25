package ua.ihromant.mathutils.vector;

public class MatrixInverseFiniteField {

    // Method to compute the modular multiplicative inverse of a number modulo p
    private static int modInverse(int a, int p) {
        a = a % p;
        for (int x = 1; x < p; x++) {
            if ((a * x) % p == 1) {
                return x;
            }
        }
        throw new ArithmeticException("No modular inverse exists");
    }

    // Method to find the inverse of a matrix modulo p
    public static int[][] inverseMatrix(int[][] matrix, int p) {
        int n = matrix.length;
        int[][] augmented = new int[n][2 * n];

        // Create the augmented matrix [A | I]
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = matrix[i][j] % p;
                if (augmented[i][j] < 0) augmented[i][j] += p; // Ensure non-negative
            }
            augmented[i][n + i] = 1; // Identity matrix on the right
        }

        // Perform Gaussian elimination
        for (int i = 0; i < n; i++) {
            // Find the pivot element
            if (augmented[i][i] == 0) {
                // Swap with a row below if pivot is zero
                boolean swapped = false;
                for (int k = i + 1; k < n; k++) {
                    if (augmented[k][i] != 0) {
                        int[] temp = augmented[i];
                        augmented[i] = augmented[k];
                        augmented[k] = temp;
                        swapped = true;
                        break;
                    }
                }
                if (!swapped) {
                    throw new ArithmeticException("Matrix is not invertible modulo " + p);
                }
            }

            // Normalize the pivot row
            int pivot = augmented[i][i];
            int pivotInverse = modInverse(pivot, p);
            for (int j = 0; j < 2 * n; j++) {
                augmented[i][j] = (augmented[i][j] * pivotInverse) % p;
            }

            // Eliminate other rows
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    int factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] = (augmented[k][j] - factor * augmented[i][j] % p + p) % p;
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