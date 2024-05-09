package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.stream.IntStream;

public interface TernaryRing {
    int op(int x, int a, int b);

    int order();

    int[][][] matrix();

    default boolean trEquals(TernaryRing that) {
        if (this.order() != that.order()) {
            return false;
        }
        int[][][] matrix = this.matrix();
        int[][][] tMatrix = that.matrix();
        int[][] permutations = GaloisField.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
        for (int[] perm : permutations) {
            int[][][] permMatrix = new int[order()][order()][order()];
            for (int i = 0; i < order(); i++) {
                for (int j = 0; j < order(); j++) {
                    for (int k = 0; k < order(); k++) {
                        permMatrix[permute(perm, i)][permute(perm, j)][permute(perm, k)] = permute(perm, tMatrix[i][j][k]);
                    }
                }
            }
            if (Arrays.deepEquals(permMatrix, matrix)) {
                return true;
            }
        }
        return false;
    }

    private static int permute(int[] perm, int idx) {
        if (idx == 0 || idx == 1) {
            return idx;
        }
        return 2 + perm[idx - 2];
    }

    default Iterable<Integer> elements() {
        return () -> IntStream.range(0, order()).boxed().iterator();
    }
}
