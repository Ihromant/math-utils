package ua.ihromant.mathutils.vector;

import lombok.Getter;
import ua.ihromant.mathutils.util.FixBS;

import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
public class PrimeLinearSpace implements LinearSpace {
    private final int p;
    private final int n;
    private final int[] powList;

    public PrimeLinearSpace(int p, int n) {
        this.p = p;
        this.n = n;
        this.powList = IntStream.range(0, n + 1).map(i -> LinearSpace.pow(p, i)).toArray();
    }

    @Override
    public int cardinality() {
        return powList[n];
    }

    @Override
    public int half() {
        return powList[n / 2];
    }

    public int crd(int v, int crd) {
        return (v / powList[crd]) % p;
    }

    @Override
    public int add(int... numbers) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int number : numbers) {
                sum = sum + crd(number, i);
            }
            res = res + powList[i] * (sum % p);
        }
        return res;
    }

    public int mul(int a, int cff) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int acrd = crd(a, i);
            res = res + powList[i] * ((acrd * cff) % p);
        }
        return res;
    }

    public int scalar(int a, int b) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int acrd = crd(a, i);
            int bcrd = crd(b, i);
            res = res + (acrd * bcrd);
        }
        return res % p;
    }

    public int neg(int a) {
        return mul(a, p - 1);
    }

    @Override
    public FixBS hull(int... arr) {
        FixBS bs = new FixBS(cardinality());
        for (int i = 0; i < powList[arr.length]; i++) {
            int[] mul = new int[arr.length];
            for (int j = 0; j < arr.length; j++) {
                mul[j] = mul(arr[j], crd(i, j));
            }
            bs.set(add(mul));
        }
        bs.set(0, false);
        return bs;
    }

    public FixBS hull(FixBS fst, FixBS snd) {
        FixBS bs = new FixBS(cardinality());
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

    public FixBS orthogonal(FixBS bs) {
        int a = bs.nextSetBit(0);
        int b = bs.stream().filter(c -> c != a && c != neg(a)).findFirst().orElseThrow();
        FixBS res = new FixBS(cardinality());
        int pow = LinearSpace.pow(p, n);
        for (int i = 1; i < pow; i++) {
            if (scalar(a, i) != 0 || scalar(b, i) != 0) {
                continue;
            }
            res.set(i);
        }
        return res;
    }

    public List<Bijection> bijections() {
        return bijections(n, new BitSet(), new int[n]).distinct().toList();
    }

    private Stream<Bijection> bijections(int needed, BitSet hull, int[] baseMap) {
        return IntStream.range(1, powList[n]).filter(i -> !hull.get(i)).boxed().mapMulti((i, sink) -> {
            int[] nextBaseMap = baseMap.clone();
            nextBaseMap[nextBaseMap.length - needed] = i;
            if (needed == 1) {
                sink.accept(toBijection(nextBaseMap));
                return;
            }
            BitSet nextHull = (BitSet) hull.clone();
            for (int j = hull.nextSetBit(0); j >= 0; j = hull.nextSetBit(j + 1)) {
                for (int c = 1; c < p; c++) {
                    nextHull.set(add(j, mul(i, c)));
                }
            }
            for (int c = 1; c < p; c++) {
                nextHull.set(mul(i, c));
            }
            bijections(needed - 1, nextHull, nextBaseMap).forEach(sink);
        });
    }

    private Bijection toBijection(int[] baseMap) {
        int[] arr = new int[powList[n]];
        for (int i = 0; i < arr.length; i++) {
            int[] muls = new int[n];
            for (int j = 0; j < muls.length; j++) {
                muls[j] = mul(baseMap[j], crd(i, j));
            }
            arr[i] = add(muls);
        }
        return new Bijection(arr);
    }
}
