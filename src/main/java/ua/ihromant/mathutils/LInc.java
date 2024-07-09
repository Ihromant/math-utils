package ua.ihromant.mathutils;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public record LInc(long[] beams, int b) implements Inc {
    @Override
    public int v() {
        return beams.length;
    }

    @Override
    public boolean inc(int l, int pt) {
        return (beams[pt] & (1L << l)) != 0;
    }

    @Override
    public void set(int l, int pt) {
        beams[pt] |= (1L << l);
    }

    @Override
    public Inc removeTwins() {
        Map<Long, BitSet> dist = new HashMap<>();
        int bl = beams.length;
        for (int i = 0; i < beams.length; i++) {
            long bin = beams[i];
            dist.computeIfAbsent(bin, k -> new BitSet(bl)).set(i);
        }
        BitSet filtered = new BitSet(bl);
        filtered.set(0, bl);
        for (Map.Entry<Long, BitSet> e : dist.entrySet()) {
            long k = e.getKey();
            if (k != 0) {
                BitSet v = e.getValue();
                v.clear(v.nextSetBit(0));
                filtered.xor(v);
            } else {
                filtered.xor(e.getValue());
            }
        }
        int v = filtered.cardinality();
        if (v == beams.length) {
            return this;
        } else {
            return new LInc(IntStream.range(0, this.b).filter(filtered::get).mapToLong(i -> beams[i]).toArray(), b);
        }
    }

    @Override
    public Inc addLine(int[] line) {
        if (b < Long.SIZE) {
            LInc res = new LInc(beams.clone(), b + 1);
            for (int pt : line) {
                res.set(b, pt);
            }
            return res;
        } else {
            int v = v();
            BSInc res = new BSInc(new BitSet(), v, Long.SIZE + 1);
            for (int pt = 0; pt < v; pt++) {
                for (int l = 0; l < Long.SIZE; l++) {
                    if (inc(l, pt)) {
                        res.set(l, pt);
                    }
                }
            }
            for (int pt : line) {
                res.set(Long.SIZE, pt);
            }
            return res;
        }
    }
}
