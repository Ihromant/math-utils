package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GaloisField {
    private final int cardinality;
    private final int base;
    private final int power;
    private final int[] irreducible;
    private final int[][] additionTable;
    private final int[][] multiplicationTable;
    private final int[] addInverses;
    private final int[] mulInverses;

    public GaloisField(int base) {
        this.cardinality = base;
        int[] factors = Combinatorics.factorize(base);
        if (!Arrays.stream(factors).allMatch(f -> f == factors[0])) {
            throw new IllegalArgumentException(base + " contains different factors: " + Arrays.toString(factors));
        }
        this.base = factors[0];
        this.power = factors.length;
        this.irreducible = power == 1 ? new int[]{1, 0} : IntStream.range(cardinality, base * cardinality)
                .mapToObj(this::toPolynomial).filter(this::irreducible).findAny().orElseThrow();
        this.additionTable = new int[base][base];
        this.multiplicationTable = new int[base][base];
        this.mulInverses = new int[base];
        this.addInverses = new int[base];
        for (int i = 0; i < base; i++) {
            for (int j = i; j < base; j++) {
                int sum;
                int product;
                if (power > 1) {
                    int[] poly0 = toPolynomial(i);
                    int[] poly1 = toPolynomial(j);
                    sum = fromPolynomial(addPoly(poly0, poly1));
                    product = fromPolynomial(modPoly(mulPoly(poly0, poly1), irreducible));
                } else {
                    sum = baseAdd(i, j);
                    product = baseMul(i, j);
                }
                additionTable[i][j] = sum;
                additionTable[j][i] = sum;
                multiplicationTable[i][j] = product;
                multiplicationTable[j][i] = product;
            }
            int ii = i;
            addInverses[i] = IntStream.range(0, cardinality).filter(j -> additionTable[ii][j] == 0).findAny().orElseThrow();
            if (i != 0) {
                mulInverses[i] = IntStream.range(0, cardinality).filter(j -> multiplicationTable[ii][j] == 1).findAny().orElseThrow();
            }
        }
    }

    private int[] trim(int[] poly) {
        int zeros = Arrays.stream(poly).takeWhile(i -> i == 0).toArray().length;
        if (zeros == poly.length) {
            return new int[]{0};
        }
        return Arrays.stream(poly, zeros, poly.length).toArray();
    }

    private static int[] shift(int[] poly, int shift) {
        int[] result = new int[poly.length + shift];
        System.arraycopy(poly, 0, result, 0, poly.length);
        return result;
    }

    private int[] negPoly(int[] poly) {
        return Arrays.stream(poly).map(this::baseNeg).toArray();
    }

    private int[] mulPoly(int[] poly, int cff) {
        return Arrays.stream(poly).map(i -> baseMul(i, cff)).toArray();
    }

    private int[] modPoly(int[] dividend, int[] divisor) {
        int[] result = dividend;
        while (result.length >= divisor.length) {
            int fst = result[0];
            int multiplier = IntStream.range(1, base).filter(i -> baseMul(divisor[0], i) == fst).findAny().orElseThrow();
            int[] toSubtract = negPoly(mulPoly(shift(divisor, result.length - divisor.length), multiplier));
            result = trim(addPoly(result, toSubtract));
        }
        return result;
    }

    private boolean irreducible(int[] poly) {
        return IntStream.range(0, base).noneMatch(i -> evalPoly(poly, i) == 0) && IntStream.range(base, cardinality).noneMatch(i -> {
            int[] divisor = toPolynomial(i);
            int[] mod = modPoly(poly, divisor);
            return mod.length == 1 && mod[0] == 0;
        });
    }

    private int[] toPolynomial(int i) {
        if (i == 0) {
            return new int[]{0};
        }
        List<Integer> result = new ArrayList<>();
        while (i != 0) {
            result.add(0, i % base);
            i = i / base;
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private int fromPolynomial(int[] poly) {
        int result = 0;
        for (int i : poly) {
            result = result * base + i;
        }
        return result;
    }

    private int[] addPoly(int[] first, int[] second) {
        if (first.length < second.length) {
            return addPoly(second, first);
        }
        int[] result = new int[first.length];
        System.arraycopy(first, 0, result, 0, first.length - second.length);
        for (int i = first.length - second.length; i < first.length; i++) {
            result[i] = baseAdd(first[i], second[second.length - first.length + i]);
        }
        return result;
    }

    private int[] mulPoly(int[] first, int[] second) {
        int[] result = new int[first.length + second.length - 1];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < second.length; j++) {
                result[i + j] = baseAdd(result[i + j], baseMul(first[i], second[j]));
            }
        }
        return trim(result);
    }

    private int evalPoly(int[] poly, int number) {
        int result = 0;
        for (int cff : poly) {
            result = baseAdd(baseMul(result, number), cff);
        }
        return result;
    }

    private int baseAdd(int a, int b) {
        return (a + b) % base;
    }

    private int baseMul(int a, int b) {
        return (a * b) % base;
    }

    private int baseNeg(int a) {
        return (base - a) % base;
    }

    public int add(int a, int b) {
        return additionTable[a][b];
    }

    public int sub(int a, int b) {
        return additionTable[a][neg(b)];
    }

    public int add(int... vals) {
        return Arrays.stream(vals).reduce(0, this::add);
    }

    public int mul(int a, int b) {
        return multiplicationTable[a][b];
    }

    public int mul(int... vals) {
        return Arrays.stream(vals).reduce(1, this::mul);
    }

    public int mulOrder(int a) {
        int counter = 1;
        int from = 1;
        while ((from = mul(from, a)) != 1) {
            counter++;
        }
        return counter;
    }

    public IntStream primitives() {
        return IntStream.range(1, cardinality).filter(i -> mulOrder(i) == cardinality - 1);
    }

    public int cardinality() {
        return cardinality;
    }

    public IntStream elements() {
        return IntStream.range(0, cardinality);
    }

    public int power(int a, int b) {
        int result = 1;
        for (int i = 0; i < b; i++) {
            result = mul(result, a);
        }
        return result;
    }

    public int expOrder(int a) {
        int res = a;
        for (int i = 1; i < cardinality + 1; i++) {
            res = mul(res, a);
            if (res == a) {
                return i;
            }
        }
        return -1;
    }

    public int neg(int a) {
        return addInverses[a];
    }

    public int inverse(int a) {
        if (a == 0) {
            throw new IllegalArgumentException();
        }
        return mulInverses[a];
    }

    public int evalPolynomial(int[] poly, int number) {
        int result = 0;
        for (int cff : poly) {
            result = add(mul(result, number), cff);
        }
        return result;
    }

    public IntStream solve(int[] polynomial) {
        return IntStream.range(0, cardinality).filter(i -> evalPolynomial(polynomial, i) == 0);
    }

    public IntStream oneRoots(int degree) {
        return IntStream.range(2, cardinality).filter(i -> power(i, degree) == 1 && IntStream.range(1, degree).noneMatch(dg -> power(i, dg) == 1));
    }

    private int fromEuclideanCrd(int x, int y) {
        return x * cardinality + y;
    }

    private int mulSpace(int point, int cff) {
        int x = mul(point / cardinality / cardinality, cff);
        int y = mul(point / cardinality % cardinality, cff);
        int z = mul(point % cardinality, cff);
        return fromSpaceCrd(x, y, z);
    }

    private int mulPoint(int point, int cff) {
        int x = mul(x(point), cff);
        int y = mul(y(point), cff);
        return fromEuclideanCrd(x, y);
    }

    private int addPoints(int p1, int p2) {
        return fromEuclideanCrd(add(x(p1), x(p2)), add(y(p1), y(p2)));
    }

    private int addSpace(int p1, int p2) {
        int x1 = p1 / cardinality / cardinality;
        int y1 = p1 / cardinality % cardinality;
        int z1 = p1 % cardinality;
        int x2 = p2 / cardinality / cardinality;
        int y2 = p2 / cardinality % cardinality;
        int z2 = p2 % cardinality;
        return fromSpaceCrd(add(x1, x2), add(y1, y2), add(z1, z2));
    }

    private int x(int point) {
        return point / cardinality;
    }

    private int y(int point) {
        return point % cardinality;
    }

    public BitSet[] generatePlane() {
        int v = cardinality * cardinality + cardinality + 1;
        int[][] rays = Stream.concat(
                Stream.of(elements().skip(1)
                        .map(cf -> mulPoint(fromEuclideanCrd(0, 1), cf)).toArray()),
                elements().mapToObj(y -> elements().skip(1)
                        .map(cf -> mulPoint(fromEuclideanCrd(1, y), cf)).toArray())).toArray(int[][]::new);
        BitSet[] lines = new BitSet[v];
        for (int i = 0; i < cardinality; i++) {
            int start = fromEuclideanCrd(0, i);
            for (int j = 0; j < cardinality; j++) {
                int lineIdx = i * cardinality + j;
                BitSet line = new BitSet();
                line.set(start);
                IntStream.of(rays[j + 1]).forEach(p -> line.set(addPoints(p, start)));
                line.set(cardinality * cardinality + j);
                lines[lineIdx] = line;
            }
        }
        for (int i = 0; i < cardinality; i++) {
            int lineIdx = cardinality * cardinality + i;
            BitSet line = new BitSet();
            int start = fromEuclideanCrd(i, 0);
            line.set(start);
            IntStream.of(rays[0]).forEach(p -> line.set(addPoints(p, start)));
            line.set(cardinality * cardinality + cardinality);
            lines[lineIdx] = line;
        }
        BitSet infinity = new BitSet();
        for (int i = 0; i <= cardinality; i++) {
            infinity.set(cardinality * cardinality + i);
        }
        lines[cardinality * cardinality + cardinality] = infinity;
        return lines;
    }

    private int fromSpaceCrd(int x, int y, int z) {
        return (x * cardinality + y) * cardinality + z;
    }

    public BitSet[] generateSpace() {
        int v = cardinality * cardinality * cardinality * cardinality + cardinality * cardinality * cardinality
                + 2 * cardinality * cardinality + cardinality + 1;
        int[][] planeRays = Stream.concat(
                Stream.of(elements().skip(1)
                        .map(cf -> mulPoint(fromEuclideanCrd(0, 1), cf)).toArray()),
                elements().mapToObj(y -> elements().skip(1)
                        .map(cf -> mulPoint(fromEuclideanCrd(1, y), cf)).toArray())).toArray(int[][]::new);
        int[][] rays = Stream.of(Stream.of(elements().skip(1)
                        .map(cf -> mulSpace(fromSpaceCrd(0, 0, 1), cf)).toArray()),
                elements().mapToObj(z -> elements().skip(1)
                        .map(cf -> mulSpace(fromSpaceCrd(0, 1, z), cf)).toArray()),
                elements().boxed().flatMap(y -> elements().mapToObj(z -> elements().skip(1)
                        .map(cf -> mulSpace(fromSpaceCrd(1, y, z), cf)).toArray()))).flatMap(Function.identity()).toArray(int[][]::new);
        BitSet[] lines = new BitSet[v];
        int fromIdx = 0;
        for (int y = 0; y < cardinality; y++) {
            for (int z = 0; z < cardinality; z++) {
                int start = fromSpaceCrd(0, y, z);
                for (int yDir = 0; yDir < cardinality; yDir++) {
                    for (int zDir = 0; zDir < cardinality; zDir++) {
                        int lineIdx = fromIdx + ((y * cardinality + z) * cardinality + yDir) * cardinality + zDir;
                        BitSet line = new BitSet();
                        line.set(start);
                        IntStream.of(rays[1 + cardinality + yDir * cardinality + zDir]).forEach(p -> line.set(addSpace(p, start)));
                        line.set(cardinality * cardinality * cardinality + yDir * cardinality + zDir);
                        lines[lineIdx] = line;
                    }
                }
            }
        }
        fromIdx = fromIdx + cardinality * cardinality * cardinality * cardinality;
        for (int x = 0; x < cardinality; x++) {
            for (int z = 0; z < cardinality; z++) {
                int start = fromSpaceCrd(x, 0, z);
                for (int zDir = 0; zDir < cardinality; zDir++) {
                    int lineIdx = fromIdx + (x * cardinality + z) * cardinality + zDir;
                    BitSet line = new BitSet();
                    line.set(start);
                    IntStream.of(rays[1 + zDir]).forEach(p -> line.set(addSpace(p, start)));
                    line.set(cardinality * cardinality * cardinality + cardinality * cardinality + zDir);
                    lines[lineIdx] = line;
                }
            }
        }
        fromIdx = fromIdx + cardinality * cardinality * cardinality;
        for (int x = 0; x < cardinality; x++) {
            for (int y = 0; y < cardinality; y++) {
                int start = fromSpaceCrd(x, y, 0);
                int lineIdx = fromIdx + x * cardinality + y;
                BitSet line = new BitSet();
                line.set(start);
                IntStream.of(rays[0]).forEach(p -> line.set(addSpace(p, start)));
                line.set(cardinality * cardinality * cardinality + cardinality * cardinality + cardinality);
                lines[lineIdx] = line;
            }
        }
        fromIdx = fromIdx + cardinality * cardinality;
        for (int z = 0; z < cardinality; z++) {
            int start = fromEuclideanCrd(0, z);
            for (int zDir = 0; zDir < cardinality; zDir++) {
                int lineIdx = fromIdx + z * cardinality + zDir;
                BitSet line = new BitSet();
                line.set(cardinality * cardinality * cardinality  + start);
                IntStream.of(planeRays[zDir + 1]).forEach(p -> line.set(cardinality * cardinality * cardinality + addPoints(p, start)));
                line.set(cardinality * cardinality * cardinality + cardinality * cardinality + zDir);
                lines[lineIdx] = line;
            }
        }
        fromIdx = fromIdx + cardinality * cardinality;
        for (int y = 0; y < cardinality; y++) {
            int lineIdx = fromIdx + y;
            BitSet line = new BitSet();
            int start = fromEuclideanCrd(y, 0);
            line.set(cardinality * cardinality * cardinality + start);
            IntStream.of(planeRays[0]).forEach(p -> line.set(cardinality * cardinality * cardinality + addPoints(p, start)));
            line.set(cardinality * cardinality * cardinality + cardinality * cardinality + cardinality);
            lines[lineIdx] = line;
        }
        fromIdx = fromIdx + cardinality;
        BitSet infinity = new BitSet();
        for (int i = 0; i <= cardinality; i++) {
            infinity.set(cardinality * cardinality * cardinality + cardinality * cardinality + i);
        }
        lines[fromIdx] = infinity;
        return lines;
    }

    public int determinant(int[][] matrix) {
        int n = matrix.length;
        int det = 1;
        boolean sign = true;

        for (int i = 0; i < n; i++) {
            if (matrix[i][i] == 0) {
                boolean swapped = false;
                for (int j = i + 1; j < n; j++) {
                    if (matrix[j][i] != 0) {
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
                        matrix[j][k] = add(matrix[j][k], neg(mul(scale, matrix[i][k])));
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            det = mul(det, matrix[i][i]);
        }

        return sign ? det : neg(det);
    }

    private static void swapRows(int[][] matrix, int i, int j) {
        int[] temp = matrix[i];
        matrix[i] = matrix[j];
        matrix[j] = temp;
    }

    public int[][] multiply(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = add(sum, mul(first[i][k], second[k][j]));
                }
                result[i][j] = sum;
            }
        }
        return result;
    }
}
