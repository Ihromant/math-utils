package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MulTest {
    @Test
    public void test() {
        int n = 8;
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

    private static int[][] toMatrix(int a, int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % 2;
            a = a / 2;
        }
        return result;
    }

    private static int fromMatrix(int[][] matrix, int n) {
        int result = 0;
        for (int i = n * n - 1; i >= 0; i--) {
            result = result * 2 + matrix[i / n][i % n];
        }
        return result;
    }

    private static long mulMagic(long a, long b, int n) {
        long r = 0;
        for (int i = 0; i < n; ++i) {
            long x = (b >>> i) & 0x101010101010101L;
            x = x ^ (x >>> 7);
            x = x ^ (x >>> 14);
            x = x ^ (x >>> 28);
            x = x & 0xFF;
            x = x * 0x101010101010101L;
            x = x & a;
            x = x ^ (x >>> 4);
            x = x ^ (x >>> 2);
            x = x ^ (x >>> 1);
            r = r | ((x & 0x101010101010101L) << i);
        }
        return r;
    }

    private static long generateConstant(int n) {
        long res = 0;
        for (int i = 0; i < n; i++) {
            res = (res << n) + 1;
        }
        return res;
    }

    @Test
    public void test1() {
        for (int n = 1; n <= 5; n++) {
            System.out.println("For " + n + " constant " + generateConstant(n));
            int cap = 1 << (n * n);
            for (int i = 0; i < 10000; i++) {
                int a = ThreadLocalRandom.current().nextInt(cap);
                int b = ThreadLocalRandom.current().nextInt(cap);
                assertEquals(mul(a, b, n), mulMagic(a, b, n));
            }
            long time = System.currentTimeMillis();
            long sum = 0;
            for (int i = 0; i < 10_000_000; i++) {
                int a = ThreadLocalRandom.current().nextInt(cap);
                int b = ThreadLocalRandom.current().nextInt(cap);
                sum = sum + mul(a, b, n);
            }
            System.out.println("Binary method " + (System.currentTimeMillis() - time) + " " + sum);
            time = System.currentTimeMillis();
            sum = 0;
            for (int i = 0; i < 10_000_000; i++) {
                int a = ThreadLocalRandom.current().nextInt(cap);
                int b = ThreadLocalRandom.current().nextInt(cap);
                sum = sum + mulMagic(a, b, n);
            }
            System.out.println("Magic method " + (System.currentTimeMillis() - time) + " " + sum);
            time = System.currentTimeMillis();
            sum = 0;
            for (int i = 0; i < 10_000_000; i++) {
                int a = ThreadLocalRandom.current().nextInt(cap);
                int b = ThreadLocalRandom.current().nextInt(cap);
                sum = sum + mulByMatrix(a, b, n);
            }
            System.out.println("Matrix method " + (System.currentTimeMillis() - time) + " " + sum);
        }
    }

    private static int mulByMatrix(int a, int b, int n) {
        return fromMatrix(multiply(toMatrix(a, n), toMatrix(b, n)), n);
    }

    private static int[][] multiply(int[][] first, int[][] second) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = sum + first[i][k] * second[k][j];
                }
                result[i][j] = sum % 2;
            }
        }
        return result;
    }

    private int mulMagic(int a, int b, int n) {
        return switch (n) {
            case 1 -> mulMagic1(a, b);
            case 2 -> mulMagic2(a, b);
            case 3 -> mulMagic3(a, b);
            case 4 -> mulMagic4(a, b);
            case 5 -> mulMagic5(a, b);
            default -> throw new IllegalStateException();
        };
    }

    private int mulMagic1(int a, int b) {
        return a & b;
    }

    private int mulMagic2(int a, int b) {
        int r = 0;
        for (int i = 0; i < 2; i++) {
            int x = (b >>> i) & 0x5;
            x = x ^ (x >>> 1);
            x = x & 0x3;
            x = x * 0x5;
            x = x & a;
            x = x ^ (x >>> 1);
            r = r | ((x & 0x5) << i);
        }
        return r;
    }

    private int mulMagic3(int a, int b) {
        int r = 0;
        for (int i = 0; i < 3; i++) {
            int x = (b >>> i) & 0x49;
            x = x ^ (x >>> 2) ^ (x >>> 4);
            x = x & 0x7;
            x = x * 0x49;
            x = x & a;
            x = x ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x49) << i);
        }
        return r;
    }

    private int mulMagic4(int a, int b) {
        int r = 0;
        for (int i = 0; i < 4; i++) {
            int x = (b >>> i) & 0x1111;
            x = x ^ (x >>> 3) ^ (x >>> 6) ^ (x >>> 9);
            x = x & 0xF;
            x = x * 0x1111;
            x = x & a;
            x = x ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x1111) << i);
        }
        return r;
    }

    private int mulMagic5(int a, int b) {
        int r = 0;
        for (int i = 0; i < 5; ++i) {
            int x = (b >>> i) & 0x108421;
            x = x ^ (x >>> 4) ^ (x >>> 8) ^ (x >>> 12) ^ (x >>> 16);
            x = x & 0x1F;
            x = x * 0x108421;
            x = x & a;
            x = x ^ (x >>> 4) ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x108421) << i);
        }
        return r;
    }
}
