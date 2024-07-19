package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.stream.IntStream;

public record FixInc(FixBS[] lines, int v) implements Inc {
    @Override
    public int b() {
        return lines.length;
    }

    @Override
    public boolean inc(int l, int pt) {
        return lines[l].get(pt);
    }

    @Override
    public void set(int l, int pt) {
        lines[l].set(pt);
    }

    @Override
    public Inc removeTwins() {
        int[] beamCounts = new int[v];
        for (FixBS line : lines) {
            for (int pt = line.nextSetBit(0); pt >= 0; pt = line.nextSetBit(pt + 1)) {
                beamCounts[pt]++;
            }
        }
        FixBS filtered = new FixBS(v);
        IntStream.range(0, v).filter(i -> beamCounts[i] > 1).forEach(filtered::set);
        int pCard = filtered.cardinality();
        if (v == pCard) {
            return this;
        } else {
            FixBS[] newLines = IntStream.range(0, lines.length).mapToObj(i -> new FixBS(pCard)).toArray(FixBS[]::new);
            int idx = 0;
            for (int pt = filtered.nextSetBit(0); pt >= 0; pt = filtered.nextSetBit(pt + 1)) {
                for (int l = 0; l < lines.length; l++) {
                    if (inc(l, pt)) {
                        newLines[l].set(idx);
                    }
                }
                idx++;
            }
            FixBS filteredLines = new FixBS(lines.length);
            for (int l = 0; l < pCard; l++) {
                if (newLines[l].cardinality() > 1) {
                    filteredLines.set(l);
                }
            }
            int fCard = filteredLines.cardinality();
            if (fCard == lines.length) {
                return new FixInc(newLines, pCard);
            } else {
                FixBS[] res = new FixBS[fCard];
                int lIdx = 0;
                for (int ln = filteredLines.nextSetBit(0); ln >= 0; ln = filteredLines.nextSetBit(ln + 1)) {
                    res[ln] = newLines[lIdx++];
                }
                return new FixInc(res, pCard);
            }
        }
    }

    @Override
    public Inc addLine(int[] line) {
        FixBS[] next = new FixBS[lines.length + 1];
        System.arraycopy(lines, 0, next, 0, lines.length);
        FixBS ln = new FixBS(v);
        for (int pt : line) {
            ln.set(pt);
        }
        next[lines.length] = ln;
        return new FixInc(next, v);
    }
}
