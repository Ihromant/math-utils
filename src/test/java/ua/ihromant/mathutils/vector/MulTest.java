package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MulTest {
    @Test
    public void test() {
        int n = 8;
//        int matCount = LinearSpace.pow(2, sz);
//        int[] mapGl = generateMapGl(n, matCount);
//        int[] gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
//        Arrays.stream(gl).forEach(i -> System.out.println(i + " " + transpose(i, n)
//                + " " + Arrays.deepToString(toMatrix(i, n)) + " " + Arrays.deepToString(toMatrix(transpose(i, n), n))));
//        ModuloMatrixHelper helper = ModuloMatrixHelper.of(2, n);
        for (int i = 0; i < 10000; i++) {
            long a = ThreadLocalRandom.current().nextLong();
            long b = ThreadLocalRandom.current().nextLong();
            assertEquals(mul(a, b, n), mulMagic(a, b, n));
        }
        long time = System.currentTimeMillis();
        long sum = 0;
        for (int i = 0; i < 10_000_000; i++) {
            long a = ThreadLocalRandom.current().nextLong();
            long b = ThreadLocalRandom.current().nextLong();
            sum = sum + mul(a, b, n);
        }
        System.out.println("Binary method " + (System.currentTimeMillis() - time) + " " + sum);
        time = System.currentTimeMillis();
        sum = 0;
        for (int i = 0; i < 10_000_000; i++) {
            long a = ThreadLocalRandom.current().nextLong();
            long b = ThreadLocalRandom.current().nextLong();
            sum = sum + mulMagic(a, b, n);
        }
        System.out.println("Magic method " + (System.currentTimeMillis() - time) + " " + sum);
    }

    private long mul(long a, long b, int n) {
        long tr = transpose(b, n);
        long res = 0;
        long mask = (1L << n) - 1;
        int idx = 0;
        for (int row = 0; row < n; row++) {
            long rowPart = (a >>> (row * n)) & mask;
            for (int col = 0; col < n; col++) {
                long colPart = (tr >>> (col * n)) & mask;
                long mul = Long.bitCount(colPart & rowPart) & 1;
                res = res | (mul << idx++);
            }
        }
        return res;
    }

    private long transpose(long a, int n) {
        long res = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int pos = i * n + j;
                int movedPos = j * n + i;
                long bt = (a >>> pos) & 1L;
                res = res | (bt << movedPos);
            }
        }
        return res;
    }

    private int[] generateMapGl(int n, int matCount) {
        int[] result = new int[matCount];
        for (int i = 0; i < matCount; i++) {
            if (result[i] > 0) {
                continue;
            }
            try {
                int[][] matrix = toMatrix(i, n);
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, 2);
                int inv = fromMatrix(rev, n);
                result[i] = inv;
                result[inv] = i;
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private int[][] toMatrix(int a, int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % 2;
            a = a / 2;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix, int n) {
        int result = 0;
        for (int i = n * n - 1; i >= 0; i--) {
            result = result * 2 + matrix[i / n][i % n];
        }
        return result;
    }

    private long mulMagic(long a, long b, int n) {
        long r = 0;
        for (int i = 0; i < n; ++i) {
            long x = (b >>> i) & 0x101010101010101L;
            x ^= x >>> 7;
            x ^= x >>> 14;
            x ^= x >>> 28;
            x &= 0xFF;
            x *= 0x101010101010101L;
            x &= a;
            x ^= x >>> 4;
            x ^= x >>> 2;
            x ^= x >>> 1;
            r |= (x & 0x101010101010101L) << i;
        }
        return r;
    }
}
