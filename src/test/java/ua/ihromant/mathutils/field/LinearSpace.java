package ua.ihromant.mathutils.field;

import java.util.BitSet;

public class LinearSpace {
    private final int p;
    private final int n;
    private final int pow;
    private final int bitCount;
    private final int mask;

    public LinearSpace(int p, int n) {
        this.p = p;
        this.n = n;
        int zeros = Integer.numberOfLeadingZeros(p - 1);
        this.bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(p - 1);
        this.pow = 1 << (bitCount * n);
        this.mask = 0xffffffff >>> zeros;
    }

    public int convert(int base) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int shift = bitCount * i;
            int acrd = base % p;
            base = base / p;
            res = res | (acrd << shift);
        }
        return res;
    }

    public int crd(int v, int crd) {
        return (v >>> (bitCount * crd)) & mask;
    }

    public int add(int a, int b) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int shift = bitCount * i;
            int acrd = (a >>> shift) & mask;
            int bcrd = (b >>> shift) & mask;
            res = res | (((acrd + bcrd) % p) << shift);
        }
        return res;
    }

    public int mul(int a, int cff) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int shift = bitCount * i;
            int acrd = (a >>> shift) & mask;
            res = res | (((acrd * cff) % p) << shift);
        }
        return res;
    }

    public int scalar(int a, int b) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int shift = bitCount * i;
            int acrd = (a >>> shift) & mask;
            int bcrd = (b >>> shift) & mask;
            res = res + (acrd * bcrd);
        }
        return res % p;
    }

    public int neg(int a) {
        return mul(a, p - 1);
    }

    public BitSet hull(int fst, int snd) {
        BitSet bs = new BitSet();
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < p; j++) {
                bs.set(add(mul(fst, i), mul(snd, j)));
            }
        }
        bs.set(0, false);
        return bs;
    }

    public BitSet hull(BitSet fst, BitSet snd) {
        BitSet bs = new BitSet();
        for (int a = fst.nextSetBit(0); a >= 0; a = fst.nextSetBit(a + 1)) {
            for (int b = snd.nextSetBit(0); b >= 0; b = snd.nextSetBit(b + 1)) {
                for (int i = 0; i < p; i++) {
                    for (int j = 0; j < p; j++) {
                        bs.set(add(mul(a, i), mul(b, j)));
                    }
                }
            }
        }
        bs.set(0, false);
        return bs;
    }

    public BitSet orthogonal(BitSet bs) {
        int a = bs.nextSetBit(0);
        int b = bs.stream().filter(c -> c != a && c != neg(a)).findFirst().orElseThrow();
        BitSet res = new BitSet();
        int pow = (int) Math.pow(p, n);
        for (int i = 1; i < pow; i++) {
            int c = convert(i);
            if (scalar(a, c) != 0 || scalar(b, c) != 0) {
                continue;
            }
            res.set(c);
        }
        return res;
    }
}
