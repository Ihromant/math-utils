package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class GaloisField {
    private final int cardinality;
    private final int base;
    private final int power;
    private final int[] irreducible;
    private final int[][] additionTable;
    private final int[][] multiplicationTable;
    private final int[] mulInverces;

    public GaloisField(int base) {
        this.cardinality = base;
        int[] factors = factorize(base);
        if (!Arrays.stream(factors).allMatch(f -> f == factors[0])) {
            throw new IllegalArgumentException(base + " contains different factors: " + Arrays.toString(factors));
        }
        this.base = factors[0];
        this.power = factors.length;
        this.irreducible = power == 1 ? new int[]{1, 0} : IntStream.range(base, Integer.MAX_VALUE)
                .mapToObj(this::toPolynomial).filter(this::irreducible).findAny().orElseThrow();
        this.additionTable = new int[base][base];
        this.multiplicationTable = new int[base][base];
        this.mulInverces = new int[base];
        for (int i = 0; i < base; i++) {
            for (int j = i; j < base; j++) {
                int sum;
                int product;
                if (power > 1) {
                    int[] poly0 = toPolynomial(i);
                    int[] poly1 = toPolynomial(j);
                    sum = fromPolynomial(addPoly(poly0, poly1));
                    product = fromPolynomial(mulPoly(poly0, poly1));
                } else {
                    sum = baseAdd(i, j);
                    product = baseMul(i, j);
                }
                additionTable[i][j] = sum;
                additionTable[j][i] = sum;
                multiplicationTable[i][j] = product;
                multiplicationTable[j][i] = product;
            }
            if (i != 0) {
                int ii = i;
                mulInverces[i] = IntStream.range(0, cardinality).filter(j -> multiplicationTable[ii][j] == 1).findAny().orElseThrow();
            }
        }
    }

    public static int[] factorize(int base) {
        List<Integer> result = new ArrayList<>();
        int from = 2;
        while (base != 1) {
            int factor = factor(from, base);
            from = factor;
            base = base / factor;
            result.add(factor);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] trim(int[] poly) {
        int zeros = Arrays.stream(poly).takeWhile(i -> i == 0).toArray().length;
        if (zeros == poly.length) {
            return new int[]{0};
        }
        return Arrays.stream(poly, zeros, poly.length).toArray();
    }

    private int[] shift(int[] poly, int shift) {
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

    private static int factor(int from, int base) {
        int sqrt = (int) Math.ceil(Math.sqrt(base + 1));
        for (int i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    private boolean irreducible(int[] poly) {
        return IntStream.range(0, base).noneMatch(i -> evalPoly(poly, i) == 0);
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
        result = trim(result);
        while (result.length > power) {
            int[] toSubtract = shift(irreducible, result.length - irreducible.length);
            toSubtract = negPoly(mulPoly(toSubtract, result[0]));
            result = trim(addPoly(result, toSubtract));
        }
        return result;
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

    public int mul(int a, int b) {
        return multiplicationTable[a][b];
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

    public int inverse(int a) {
        if (a == 0) {
            throw new IllegalArgumentException();
        }
        return mulInverces[a];
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

    public IntStream oneCubeRoots() {
        return IntStream.range(2, cardinality).filter(i -> power(i, 3) == 1);
    }
}
