package ua.ihromant.mathutils.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

public interface Group {
    int op(int a, int b);

    int inv(int a);

    int order();

    default int order(int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = op(a, pow);
        } while (pow != 0);
        return counter;
    }

    default int expOrder(int a) {
        int eul = euler(order());
        int res = a;
        for (int i = 1; i < eul + 1; i++) {
            res = mul(res, a);
            if (res == a) {
                return i;
            }
        }
        return -1;
    }

    String name();

    String elementName(int a);

    default int mul(int a, int cff) {
        int result = 0;
        for (int i = 0; i < cff; i++) {
            result = op(a, result);
        }
        return result;
    }

    default int exponent(int base, int power) {
        int result = 1;
        while (power > 0) {
            if (power % 2 == 1) {
                result = mul(result, base);
            }
            base = mul(base, base);
            power = power / 2;
        }
        return result;
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default Group asTable() {
        return new TableGroup(
                elements().mapToObj(i -> elements().map(j -> op(i, j)).toArray()).toArray(int[][]::new),
                elements().map(this::inv).toArray());
    }

    default BitSet difference(int[] basic) {
        return Arrays.stream(basic).flatMap(i -> Arrays.stream(basic).filter(j -> i != j).map(j -> op(i, inv(j))))
                .collect(BitSet::new, BitSet::set, BitSet::or);
    }

    static int[] factorize(int base) {
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

    private static int factor(int from, int base) {
        int sqrt = (int) Math.ceil(Math.sqrt(base + 1));
        for (int i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    static int euler(int base) {
        int[] factors = factorize(base);
        if (factors.length == 0) {
            return 0;
        }
        int result = 1;
        int idx = 0;
        while (idx < factors.length) {
            int curr = factors[idx];
            result = result * (curr - 1);
            while (++idx < factors.length && factors[idx] == curr) {
                result = result * curr;
            }
        }
        return result;
    }
}
