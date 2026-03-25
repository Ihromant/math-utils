package ua.ihromant.mathutils.vector;

import ua.ihromant.mathutils.util.FixBS;

public record TwoLinearSpace(int n) implements LinearSpace {
    @Override
    public int p() {
        return 2;
    }

    @Override
    public int cardinality() {
        return 1 << n;
    }

    @Override
    public int half() {
        return 1 << (n / 2);
    }

    @Override
    public int mul(int a, int x) {
        return (a & 1) != 0 ? x : 0;
    }

    @Override
    public int add(int... numbers) {
        int result = 0;
        for (int numb : numbers) {
            result = result ^ numb;
        }
        return result;
    }

    @Override
    public int neg(int a) {
        return a;
    }

    @Override
    public FixBS hull(int... arr) {
        int al = arr.length;
        int fin = 1 << al;
        FixBS res = new FixBS(cardinality());
        for (int i = 0; i < fin; i++) {
            int comb = 0;
            for (int j = 0; j < al; j++) {
                if ((i & (1 << j)) != 0) {
                    comb = comb ^ arr[j];
                }
            }
            res.set(comb);
        }
        res.clear(0);
        return res;
    }

    @Override
    public int scalar(int a, int b) {
        return Integer.bitCount(a & b) % 2;
    }

    @Override
    public int crd(int v, int crd) {
        return (v >>> crd) & 1;
    }

    @Override
    public int applyOper(long oper, int val) {
        int n = n();
        int mask = (1 << n) - 1;
        int result = 0;
        for (int i = 0; i < n; i++) {
            int rest = ((int) oper) & mask;
            if ((val & 1) != 0) {
                result = result ^ rest;
            }
            val = val >>> 1;
            oper = oper >>> n;
        }
        return result;
    }

    @Override
    public long mulOper(long a, long b) {
        return switch (n) {
            case 1 -> mulMagic1((int) a, (int) b);
            case 2 -> mulMagic2((int) a, (int) b);
            case 3 -> mulMagic3((int) a, (int) b);
            case 4 -> mulMagic4((int) a, (int) b);
            case 5 -> mulMagic5((int) a, (int) b);
            case 6 -> mulMagic6(a, b);
            case 7 -> mulMagic7(a, b);
            case 8 -> mulMagic8(a, b);
            default -> throw new IllegalStateException();
        };
    }

    public static int mulMagic1(int a, int b) {
        return a & b;
    }

    public static int mulMagic2(int a, int b) {
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

    public static int mulMagic3(int a, int b) {
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

    public static int mulMagic4(int a, int b) {
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

    public static int mulMagic5(int a, int b) {
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

    public static long mulMagic6(long a, long b) {
        long r = 0;
        for (int i = 0; i < 6; i++) {
            long x = (b >>> i) & 0x41041041L;
            x = x ^ (x >>> 5) ^ (x >>> 10) ^ (x >>> 15) ^ (x >>> 20) ^ (x >>> 25);
            x = x & 0x3FL;
            x = x * 0x41041041L;
            x = x & a;
            x = x ^ (x >>> 5) ^ (x >>> 4) ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x41041041L) << i);
        }
        return r;
    }

    private static long mulMagic7(long a, long b) {
        long r = 0;
        for (int i = 0; i < 7; i++) {
            long x = (b >>> i) & 0x40810204081L;
            x = x ^ (x >>> 6) ^ (x >>> 12) ^ (x >>> 18) ^ (x >>> 24) ^ (x >>> 30) ^ (x >>> 36);
            x = x & 0x7FL;
            x = x * 0x40810204081L;
            x = x & a;
            x = x ^ (x >>> 6) ^ (x >>> 5) ^ (x >>> 4) ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x40810204081L) << i);
        }
        return r;
    }

    private static long mulMagic8(long a, long b) {
        long r = 0;
        for (int i = 0; i < 8; ++i) {
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
}
