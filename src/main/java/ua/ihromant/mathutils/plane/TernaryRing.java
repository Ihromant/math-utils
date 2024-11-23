package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Triangle;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public interface TernaryRing {
    int op(int x, int a, int b);

    int order();

    int[][][] matrix();

    Quad base();

    default int op(Triangle tr) {
        return op(tr.o(), tr.u(), tr.w());
    }

    default int[][] addMatrix() {
        int[][] result = new int[order()][order()];
        for (int a : elements()) {
            for (int b : elements()) {
                result[a][b] = add(a, b);
            }
        }
        return result;
    }

    default int[][] pulsMatrix() {
        int[][] result = new int[order()][order()];
        for (int a : elements()) {
            for (int b : elements()) {
                result[a][b] = puls(a, b);
            }
        }
        return result;
    }

    default int[][] mulMatrix() {
        int[][] result = new int[order()][order()];
        for (int a : elements()) {
            for (int b : elements()) {
                result[a][b] = mul(a, b);
            }
        }
        return result;
    }

    default int add(int x, int b) {
        return op(x, 1, b);
    }

    default int puls(int x, int b) {
        return op(1, x, b);
    }

    default int mul(int x, int a) {
        return op(x, a, 0);
    }

    default boolean isLinear() {
        for (int x : elements()) {
            for (int a : elements()) {
                for (int b : elements()) {
                    if (op(x, a, b) != add(mul(x, a), b)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isPulsLinear() {
        for (int x : elements()) {
            for (int a : elements()) {
                for (int b : elements()) {
                    if (op(x, a, b) != puls(mul(x, a), b)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isConcurrent() {
        for (int a : elements()) {
            if (a == 1) {
                continue;
            }
            for (int b : elements()) {
                int x = IntStream.range(0, order()).filter(y -> mul(y, a) == add(y, b)).findAny().orElseThrow();
                for (int y : elements()) {
                    if (x == y) {
                        continue;
                    }
                    if (mul(y, a) == add(y, b)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isLeftDistributive() {
        for (int a : elements()) {
            for (int x : elements()) {
                for (int y : elements()) {
                    if (mul(a, add(x, y)) != add(mul(a, x), mul(a, y))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isRightDistributive() {
        for (int a : elements()) {
            for (int x : elements()) {
                for (int y : elements()) {
                    if (mul(add(x, y), a) != add(mul(x, a), mul(y, a))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isPulsLeftDistributive() {
        for (int a : elements()) {
            for (int x : elements()) {
                for (int y : elements()) {
                    if (mul(a, puls(x, y)) != puls(mul(a, x), mul(a, y))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean isPulsRightDistributive() {
        for (int a : elements()) {
            for (int x : elements()) {
                for (int y : elements()) {
                    if (mul(puls(x, y), a) != puls(mul(x, a), mul(y, a))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean addAssoc() {
        for (int a : elements()) {
            for (int b : elements()) {
                for (int c : elements()) {
                    if (add(add(a, b), c) != add(a, add(b, c))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean mulAssoc() {
        for (int a : elements()) {
            for (int b : elements()) {
                for (int c : elements()) {
                    if (mul(mul(a, b), c) != mul(a, mul(b, c))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean pulsAssoc() {
        for (int a : elements()) {
            for (int b : elements()) {
                for (int c : elements()) {
                    if (puls(puls(a, b), c) != puls(a, puls(b, c))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean addPowerAssoc() {
        for (int a : elements()) {
            if (add(add(a, a), a) != add(a, add(a, a))) {
                return false;
            }
        }
        return true;
    }

    default boolean mulPowerAssoc() {
        for (int a : elements()) {
            if (mul(mul(a, a), a) != mul(a, mul(a, a))) {
                return false;
            }
        }
        return true;
    }

    default boolean pulsPowerAssoc() {
        for (int a : elements()) {
            if (puls(puls(a, a), a) != puls(a, puls(a, a))) {
                return false;
            }
        }
        return true;
    }

    default boolean addComm() {
        for (int a : elements()) {
            for (int b : elements()) {
                if (add(a, b) != add(b, a)) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean pulsComm() {
        for (int a : elements()) {
            for (int b : elements()) {
                if (puls(a, b) != puls(b, a)) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean oneComm() {
        for (int x : elements()) {
            if (add(x, 1) != add(1, x)) {
                return false;
            }
        }
        return true;
    }

    default boolean pulsOneComm() {
        for (int x : elements()) {
            if (puls(x, 1) != puls(1, x)) {
                return false;
            }
        }
        return true;
    }

    default boolean mulComm() {
        for (int a : elements()) {
            for (int b : elements()) {
                if (mul(a, b) != mul(b, a)) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean addTwoSidedInverse() {
        for (int a : elements()) {
            OptionalInt x = IntStream.range(0, order()).filter(t -> add(t, a) == 0 && add(a, t) == 0).findAny();
            if (x.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    default boolean pulsTwoSidedInverse() {
        for (int a : elements()) {
            OptionalInt x = IntStream.range(0, order()).filter(t -> puls(t, a) == 0 && puls(a, t) == 0).findAny();
            if (x.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    default boolean addLeftInverse() {
        for (int a : elements()) {
            int x = IntStream.range(0, order()).filter(y -> add(y, a) == 0).findAny().orElseThrow();
            for (int y : elements()) {
                if (add(add(x, a), y) != add(x, add(a, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean pulsLeftInverse() {
        for (int a : elements()) {
            int x = IntStream.range(0, order()).filter(y -> puls(y, a) == 0).findAny().orElseThrow();
            for (int y : elements()) {
                if (puls(puls(x, a), y) != puls(x, puls(a, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean addRightInverse() {
        for (int a : elements()) {
            int x = IntStream.range(0, order()).filter(y -> add(a, y) == 0).findAny().orElseThrow();
            for (int y : elements()) {
                if (add(add(y, a), x) != add(y, add(a, x))) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean pulsRightInverse() {
        for (int a : elements()) {
            int x = IntStream.range(0, order()).filter(y -> puls(a, y) == 0).findAny().orElseThrow();
            for (int y : elements()) {
                if (puls(puls(y, a), x) != puls(y, puls(a, x))) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean mulTwoSidedInverse() {
        for (int a : elements()) {
            if (a == 0) {
                continue;
            }
            OptionalInt x = IntStream.range(1, order()).filter(t -> mul(t, a) == 1 && mul(a, t) == 1).findAny();
            if (x.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    default boolean mulLeftInverse() {
        for (int a : elements()) {
            if (a == 0) {
                continue;
            }
            int x = IntStream.range(1, order()).filter(y -> mul(y, a) == 1).findAny().orElseThrow();
            for (int y : elements()) {
                if (y == 0) {
                    continue;
                }
                if (mul(mul(x, a), y) != mul(x, mul(a, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    default boolean mulRightInverse() {
        for (int a : elements()) {
            if (a == 0) {
                continue;
            }
            int x = IntStream.range(1, order()).filter(y -> mul(a, y) == 1).findAny().orElseThrow();
            for (int y : elements()) {
                if (y == 0) {
                    continue;
                }
                if (mul(mul(y, a), x) != mul(y, mul(a, x))) {
                    return false;
                }
            }
        }
        return true;
    }

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

    default boolean biLoopEquals(TernaryRing that, boolean incAdd, boolean incMul) {
        if (this.order() != that.order()) {
            return false;
        }
        int[][] addMatrix = this.addMatrix();
        int[][] mulMatrix = this.mulMatrix();
        int[][] tAddMatrix = that.addMatrix();
        int[][] tMulMatrix = that.mulMatrix();
        int[][] permutations = GaloisField.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
        for (int[] perm : permutations) {
            int[][] permAddMatrix = new int[order()][order()];
            int[][] permMulMatrix = new int[order()][order()];
            for (int i = 0; i < order(); i++) {
                for (int j = 0; j < order(); j++) {
                    permAddMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tAddMatrix[i][j]);
                    permMulMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tMulMatrix[i][j]);
                }
            }
            if ((!incAdd || Arrays.deepEquals(permAddMatrix, addMatrix)) && (!incMul || Arrays.deepEquals(permMulMatrix, mulMatrix))) {
                return true;
            }
        }
        return false;
    }

    default boolean pulsEquals(TernaryRing that) {
        if (this.order() != that.order()) {
            return false;
        }
        int[][] pulsMatrix = this.pulsMatrix();
        int[][] tPulsMatrix = that.pulsMatrix();
        int[][] permutations = GaloisField.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
        for (int[] perm : permutations) {
            int[][] permPulsMatrix = new int[order()][order()];
            for (int i = 0; i < order(); i++) {
                for (int j = 0; j < order(); j++) {
                    permPulsMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tPulsMatrix[i][j]);
                }
            }
            if (Arrays.deepEquals(permPulsMatrix, pulsMatrix)) {
                return true;
            }
        }
        return false;
    }

    default boolean triLoopEquals(TernaryRing that) {
        if (this.order() != that.order()) {
            return false;
        }
        int[][] addMatrix = this.addMatrix();
        int[][] mulMatrix = this.mulMatrix();
        int[][] tAddMatrix = that.addMatrix();
        int[][] tMulMatrix = that.mulMatrix();
        int[][] pulsMatrix = this.pulsMatrix();
        int[][] tPulsMatrix = that.pulsMatrix();
        int[][] permutations = GaloisField.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
        for (int[] perm : permutations) {
            int[][] permAddMatrix = new int[order()][order()];
            int[][] permMulMatrix = new int[order()][order()];
            int[][] permPulsMatrix = new int[order()][order()];
            for (int i = 0; i < order(); i++) {
                for (int j = 0; j < order(); j++) {
                    permAddMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tAddMatrix[i][j]);
                    permMulMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tMulMatrix[i][j]);
                    permPulsMatrix[permute(perm, i)][permute(perm, j)] = permute(perm, tPulsMatrix[i][j]);
                }
            }
            if (Arrays.deepEquals(permAddMatrix, addMatrix) && Arrays.deepEquals(permMulMatrix, mulMatrix) && Arrays.deepEquals(permPulsMatrix, pulsMatrix)) {
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

    default OptionalInt orderTwoElem() {
        return IntStream.range(2, order()).filter(i -> add(i, i) == 0).findAny();
    }

    default OptionalInt pulsOrderTwoElem() {
        return IntStream.range(2, order()).filter(i -> puls(i, i) == 0).findAny();
    }

    default Iterable<Integer> elements() {
        return () -> IntStream.range(0, order()).boxed().iterator();
    }
}
