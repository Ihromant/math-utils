package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.Liner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public interface TernaryRing {
    int op(int x, int a, int b);

    int order();

    int[][][] matrix();

    Quad base();

    TernaryRing toMatrix();

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

    default int add(int a, int b) {
        return op(a, 1, b);
    }

    default int puls(int a, int b) {
        return op(1, a, b);
    }

    default int mul(int a, int b) {
        return op(a, b, 0);
    }

    default boolean isLinear() {
        int order = order();
        for (int x = 0; x < order; x++) {
            for (int a = 0; a < order; a++) {
                for (int b = 0; b < order; b++) {
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
        int order = order();
        for (int a = 0; a < order; a++) {
            for (int b = 0; b < order; b++) {
                for (int c = 0; c < order; c++) {
                    if (add(add(a, b), c) != add(a, add(b, c))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    default boolean mulAssoc() {
        int order = order();
        for (int a = 0; a < order; a++) {
            for (int b = 0; b < order; b++) {
                for (int c = 0; c < order; c++) {
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
        int order = order();
        for (int a = 1; a < order; a++) {
            for (int b = a + 1; b < order; b++) {
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
        int order = order();
        for (int a = 2; a < order; a++) {
            for (int b = a + 1; b < order; b++) {
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
        int[][] permutations = Combinatorics.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
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
        int[][] permutations = Combinatorics.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
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

    default boolean isotopic(TernaryRing that, int[][] hBijections) {
        int ord = order();
        if (ord != that.order()) {
            return false;
        }
        int[] g1s = IntStream.range(1, ord).toArray();
        for (int[] h : hBijections) {
            for (int g1 : g1s) {
                int[] f = new int[ord];
                for (int x = 0; x < ord; x++) {
                    for (int i = 0; i < ord; i++) {
                        if (h[this.op(x, 1, 0)] == that.op(i, g1, h[0])) {
                            f[x] = i;
                            break;
                        }
                    }
                }
                int f1 = f[1];
                int[] g = new int[ord];
                for (int x = 0; x < ord; x++) {
                    for (int i = 0; i < ord; i++) {
                        if (h[this.op(1, x, 0)] == that.op(f1, i, h[0])) {
                            g[x] = i;
                            break;
                        }
                    }
                }
                boolean eq = true;
                ex: for (int x = 0; x < ord; x++) {
                    for (int y = 0; y < ord; y++) {
                        for (int z = 0; z < ord; z++) {
                            if (h[this.op(x, y, z)] != that.op(f[x], g[y], h[z])) {
                                eq = false;
                                break ex;
                            }
                        }
                    }
                }
                if (eq) {
                    return true;
                }
            }
        }
        return false;
    }

    default Map<Map<Integer, Integer>, Integer> characteristic() {
        int ord = order();
        int[][][] matrix = matrix();
        Map<Map<Integer, Integer>, Integer> result = new HashMap<>();
        for (int a = 1; a < ord; a++) {
            for (int b = 1; b < ord; b++) {
                result.compute(cycles(matrix[a][b]), (k, v) -> v == null ? 1 : v + 1);
            }
        }
        return result;
    }

    private static Map<Integer, Integer> cycles(int[] arr) {
        Map<Integer, Integer> result = new HashMap<>();
        ex: for (int i = 0; i < arr.length; i++) {
            int next = arr[i];
            int len = 1;
            while (next != i) {
                if (next < i) {
                    continue ex;
                }
                next = arr[next];
                len++;
            }
            result.compute(len, (k, v) -> v == null ? 1 : v + 1);
        }
        return result;
    }

    default boolean pulsEquals(TernaryRing that) {
        if (this.order() != that.order()) {
            return false;
        }
        int[][] pulsMatrix = this.pulsMatrix();
        int[][] tPulsMatrix = that.pulsMatrix();
        int[][] permutations = Combinatorics.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
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
        int[][] permutations = Combinatorics.permutations(IntStream.range(0, order() - 2).toArray()).toArray(int[][]::new);
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

    default Liner toProjective() {
        int order = order();
        List<BitSet> lines = new ArrayList<>(order * order + order + 1);
        for (int a = 0; a < order; a++) {
            for (int b = 0; b < order; b++) {
                BitSet line = new BitSet(order * order + order + 1);
                for (int x = 0; x < order; x++) {
                    int y = op(x, a, b);
                    line.set(y * order + x);
                }
                line.set(order * order + a);
                lines.add(line);
            }
        }
        for (int x = 0; x < order; x++) {
            BitSet line = new BitSet(order * order + order + 1);
            for (int y = 0; y < order; y++) {
                line.set(y * order + x);
            }
            line.set(order * order + order);
            lines.add(line);
        }
        BitSet last = new BitSet(order * order + order + 1);
        last.set(order * order, order * order + order + 1);
        lines.add(last);
        return new Liner(lines.toArray(BitSet[]::new));
    }
}
