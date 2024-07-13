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
            if ((k & (k - 1)) == 0) {
                filtered.xor(e.getValue());
            }
        }
        int v = filtered.cardinality();
        if (v == beams.length) {
            return this;
        } else {
            BitSet notSingle = new BitSet(b);
            long[] newBeams = IntStream.range(0, beams.length).filter(filtered::get).mapToLong(i -> beams[i]).toArray();
            out: for (int l = 0; l < b; l++) {
                for (int fst = 0; fst < newBeams.length; fst++) {
                    if ((newBeams[fst] & (1L << l)) == 0) {
                        continue;
                    }
                    for (int snd = fst + 1; snd < newBeams.length; snd++) {
                        if ((newBeams[snd] & (1L << l)) != 0) {
                            notSingle.set(l);
                            continue out;
                        }
                    }
                }
            }
            Inc res = new LInc(newBeams, b);
            if (notSingle.cardinality() == b) {
                return new LInc(newBeams, b);
            } else {
                Inc alt = new LInc(new long[newBeams.length], notSingle.cardinality());
                int newL = 0;
                for (int oldL = notSingle.nextSetBit(0); oldL >= 0; oldL = notSingle.nextSetBit(oldL + 1)) {
                    for (int pt = 0; pt < newBeams.length; pt++) {
                        if (res.inc(oldL, pt)) {
                            alt.set(newL, pt);
                        }
                    }
                    newL++;
                }
                return alt;
            }
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
