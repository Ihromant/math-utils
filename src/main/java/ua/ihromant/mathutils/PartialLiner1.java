package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

public class PartialLiner1 {
    private final int pointCount;
    private final int[][] lines;
    private final int[] beamCounts; // number of lines in beam
    private final int[] beamLengths; // distribution by beam count
    private final int[][] lookup;
    private final int[][] beams;
    private final int[][] beamDist; // points corresponding to specific beam count
    private final int[][] intersections;
    private final int[] lineInter; // number of other lines intersections
    private final int[] lineFreq; // distribution by line intersections count
    private int[] pointOrder;

    public PartialLiner1(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(arr -> arr[arr.length - 1]).max().orElseThrow() + 1, lines);
    }

    public PartialLiner1(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        boolean[][] flags = new boolean[lines.length][pointCount];
        this.beamCounts = new int[pointCount];
        int ll = lines[0].length;
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i][pt] = true;
                this.beamCounts[pt]++;
            }
        }
        int bmc = (pointCount - 1) / (ll - 1);
        int[] beamLengths = new int[bmc + 1];
        for (int beamCount : beamCounts) {
            beamLengths[beamCount]++;
        }
        this.beamLengths = beamLengths.clone();
        this.beams = new int[pointCount][];
        this.beamDist = new int[beamLengths.length][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            if (beamDist[bc] == null) {
                beamDist[bc] = new int[beamLengths[bc]];
            }
            beamDist[bc][beamDist[bc].length - beamLengths[bc]--] = pt;
            beams[pt] = new int[bc];
            int idx = 0;
            for (int ln = 0; ln < lines.length; ln++) {
                if (flags[ln][pt]) {
                    beams[pt][idx++] = ln;
                }
            }
        }
        this.lookup = generateLookup();
        this.intersections = new int[this.lines.length][this.lines.length];
        for (int[] arr : this.intersections) {
            Arrays.fill(arr, -1);
        }
        this.lineInter = new int[this.lines.length];
        for (int p = 0; p < this.pointCount; p++) {
            int[] beam = beams[p];
            for (int i = 0; i < beam.length; i++) {
                int l1 = beam[i];
                for (int j = i + 1; j < beam.length; j++) {
                    int l2 = beam[j];
                    this.intersections[l1][l2] = p;
                    this.intersections[l2][l1] = p;
                    lineInter[l1]++;
                    lineInter[l2]++;
                }
            }
        }
        this.lineFreq = new int[Math.min(lines.length, bmc * ll) + 1];
        for (int f : lineInter) {
            this.lineFreq[f]++;
        }
    }

    public PartialLiner1(PartialLiner1 prev, int[] newLine) {
        this.pointCount = prev.pointCount;
        int pll = prev.lines.length;
        this.lines = new int[pll + 1][];
        System.arraycopy(prev.lines, 0, this.lines, 0, pll);
        this.lines[pll] = newLine;
        this.lookup = prev.lookup.clone();
        this.beams = prev.beams.clone();
        this.beamCounts = prev.beamCounts.clone();
        this.beamLengths = prev.beamLengths.clone();
        this.lineInter = new int[pll + 1];
        int bl = beamLengths.length;
        this.lineFreq = new int[Math.min(lines.length, (bl - 1) * newLine.length) + 1];
        System.arraycopy(prev.lineInter, 0, this.lineInter, 0, pll);
        System.arraycopy(prev.lineFreq, 0, this.lineFreq, 0, prev.lineFreq.length);
        int ni = 0;
        this.intersections = new int[pll + 1][pll + 1];
        for (int i = 0; i < pll; i++) {
            System.arraycopy(prev.intersections[i], 0, this.intersections[i], 0, pll);
            intersections[i][pll] = -1;
        }
        Arrays.fill(intersections[pll], -1);
        for (int p : newLine) {
            int[] nl = this.lookup[p].clone(); // lookup
            for (int p1 : newLine) {
                if (p1 == p) {
                    continue;
                }
                nl[p1] = pll;
            }
            this.lookup[p] = nl;

            int[] ob = this.beams[p]; // beams
            int[] nb = new int[ob.length + 1];
            System.arraycopy(ob, 0, nb, 0, ob.length);
            nb[ob.length] = pll;
            this.beams[p] = nb;

            int pbc = beamCounts[p]; // beamcounts-beamlengths
            beamLengths[pbc++]--;
            beamCounts[p] = pbc;
            beamLengths[pbc]++;

            int[] beam = prev.beams[p];
            for (int l : beam) {
                int prInter = lineInter[l]; // lineinter-linefreq
                lineFreq[prInter++]--;
                lineInter[l] = prInter;
                lineFreq[prInter]++;

                intersections[l][pll] = p; // intersections
                intersections[pll][l] = p;
            }
            ni = ni + beam.length;
        }
        lineInter[pll] = ni;
        lineFreq[ni]++;

        this.beamDist = new int[bl][];
        int[] beamLengths = this.beamLengths.clone();
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            if (beamDist[bc] == null) {
                beamDist[bc] = new int[beamLengths[bc]];
            }
            beamDist[bc][beamDist[bc].length - beamLengths[bc]--] = pt;
        }
    }

    private int[][] generateLookup() {
        int[][] result = new int[pointCount][pointCount];
        for (int[] p : result) {
            Arrays.fill(p, -1);
        }
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    result[p1][p2] = l;
                    result[p2][p1] = l;
                }
            }
        }
        return result;
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public int[][] lines() {
        return lines;
    }

    public int[] beamCounts() {
        return beamCounts;
    }

    public int[] beamLengths() {
        return beamLengths;
    }

    public int[][] lookup() {
        return lookup;
    }

    public int[][] beams() {
        return beams;
    }

    public int[][] beamDist() {
        return beamDist;
    }

    public int[][] intersections() {
        return intersections;
    }

    public int[] lineInter() {
        return lineInter;
    }

    public int[] lineFreq() {
        return lineFreq;
    }

    private void initOrder() {
        if (pointOrder != null) {
            return;
        }
        int[] freq = new int[pointCount];
        for (int[] pts : beamDist) {
            if (pts == null) {
                continue;
            }
            for (int pt : pts) {
                freq[pt] = pts.length;
            }
        }
        Integer[] arr = new Integer[pointCount];
        for (int i = 0; i < pointCount; i++) {
            arr[i] = i;
        }
        Arrays.sort(arr, Comparator.<Integer>comparingInt(pt -> freq[pt])
                .thenComparingInt(pt -> -beams[pt].length));
        pointOrder = new int[pointCount];
        for (int i = 0; i < pointCount; i++) {
            pointOrder[i] = arr[i];
        }
    }

    public int[] lookup(int pt) {
        return lookup[pt];
    }

    public int[] line(int line) {
        return lines[line];
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public int[] lines(int point) {
        return beams[point];
    }

    public int[] point(int point) {
        return beams[point];
    }

    public int intersection(int l1, int l2) {
        return intersections[l1][l2];
    }

    public int[] points(int line) {
        return lines[line];
    }

    public boolean isomorphic(PartialLiner1 second) {
        if (!Arrays.equals(lineFreq, second.lineFreq())) {
            return false;
        }
        int[] partialPoints = new int[pointCount];
        int[] partialLines = new int[lines.length];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        int[] perLineUnAss = new int[lines.length];
        Arrays.fill(perLineUnAss, lines[0].length);
        int[] perPointUnAss = beamCounts.clone();
        initOrder();
        return isomorphic(0, 0, second, partialPoints, new BitSet(pointCount), perPointUnAss, partialLines, new BitSet(lines.length), perLineUnAss);
    }

    private boolean isomorphic(int mapped, int fromIdx, PartialLiner1 second, int[] oldPointsMap, BitSet oldPoints, int[] perPointUnAss, int[] oldLinesMap, BitSet oldLines, int[] perLineUnAss) {
        BitSet toMapped = new BitSet(pointCount);
        int from = pointOrder[fromIdx];
        for (int pp : oldPointsMap) {
            if (pp >= 0) {
                toMapped.set(pp);
            }
        }
        for (int to : second.beamDist()[beams[from].length]) {
            if (toMapped.get(to)) {
                continue;
            }
            int[] newPointsMap = oldPointsMap.clone();
            int[] newLinesMap = oldLinesMap.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            BitSet newPoints = (BitSet) oldPoints.clone();
            BitSet newLines = (BitSet) oldLines.clone();
            int added = mapPoint(second, from, to, newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == pointCount) {
                return true;
            }
            int newFrom = fromIdx + 1;
            while (newPoints.get(pointOrder[newFrom])) {
                newFrom++;
            }
            if (isomorphic(newMapped, newFrom, second, newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss)) {
                return true;
            }
        }
        return false;
    }

    private int mapPoint(PartialLiner1 second, int from, int to, int[] newPointsMap, BitSet newPoints, int[] newPerPointUnAss, int[] newLinesMap, BitSet newLines, int[] newPerLineUnAss) {
        if (from < 0) {
            return to >= 0 ? -1 : 0;
        } else {
            if (to < 0) {
                return -1;
            }
        }
        int oldPoint = newPointsMap[from];
        if (oldPoint >= 0) {
            return oldPoint != to ? -1 : 0;
        }
        newPointsMap[from] = to;
        newPoints.set(from);
        int result = 1;
        for (int line : beams[from]) {
            newPerLineUnAss[line]--;
            for (int p : lines[line]) {
                if (p == from || !newPoints.get(p)) {
                    continue;
                }
                int lineTo = second.lookup[to][newPointsMap[p]];
                int added = mapLine(second, line, lineTo, newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                break;
            }
            if (newPerLineUnAss[line] == 1) {
                int pointFrom = -1;
                BitSet values = new BitSet(pointCount);
                for (int p : lines[line]) {
                    int val = newPointsMap[p];
                    if (val < 0) {
                        pointFrom = p;
                    } else {
                        values.set(val);
                    }
                }
                for (int p : second.lines[newLinesMap[line]]) {
                    values.flip(p);
                }
                int added = mapPoint(second, pointFrom, values.nextSetBit(0), newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
            }
        }
        return result;
    }

    private int mapLine(PartialLiner1 second, int from, int to, int[] newPointsMap, BitSet newPoints, int[] newPerPointUnAss, int[] newLinesMap, BitSet newLines, int[] newPerLineUnAss) {
        if (from < 0) {
            return to >= 0 ? -1 : 0;
        } else {
            if (to < 0) {
                return -1;
            }
        }
        int oldLine = newLinesMap[from];
        if (oldLine >= 0) {
            return oldLine != to ? -1 : 0;
        }
        newLinesMap[from] = to;
        newLines.set(from);
        int result = 0;
        for (int pt : lines[from]) {
            newPerPointUnAss[pt]--;
            for (int line : beams[pt]) {
                if (line == from || !newLines.get(line)) {
                    continue;
                }
                int ptTo = second.intersections[to][newLinesMap[line]];
                int added = mapPoint(second, pt, ptTo, newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                break;
            }
            if (newPerPointUnAss[pt] == 1) {
                int lineFrom = -1;
                BitSet values = new BitSet(lines.length);
                for (int l : beams[pt]) {
                    int val = newLinesMap[l];
                    if (val < 0) {
                        lineFrom = l;
                    } else {
                        values.set(val);
                    }
                }
                for (int l : second.beams[newPointsMap[pt]]) {
                    values.flip(l);
                }
                int added = mapLine(second, lineFrom, values.nextSetBit(0), newPointsMap, newPoints, newPerPointUnAss, newLinesMap, newLines, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
            }
        }
        return result;
    }

    public boolean checkAP() {
        int last = lines.length - 1;
        int[] lLine = lines[last];
        int ll = lLine.length;
        for (int p : lLine) {
            int[] beam = beams[p];
            for (int ol : beam) {
                if (ol == last) {
                    continue;
                }
                int[] oLine = lines[ol];
                for (int a = 0; a < ll; a++) {
                    int pl1 = lLine[a];
                    if (pl1 == p) {
                        continue;
                    }
                    for (int b = a + 1; b < ll; b++) {
                        int pl2 = lLine[b];
                        if (pl2 == p) {
                            continue;
                        }
                        for (int c = 0; c < ll; c++) {
                            int po1 = oLine[c];
                            if (po1 == p) {
                                continue;
                            }
                            int l1 = lookup[pl1][po1];
                            int l2 = lookup[pl2][po1];
                            if (l1 < 0 && l2 < 0) {
                                continue;
                            }
                            for (int d = c + 1; d < ll; d++) {
                                int po2 = oLine[d];
                                if (po2 == p) {
                                    continue;
                                }
                                int l4 = lookup[pl2][po2];
                                if (l1 >= 0 && l4 >= 0 && intersections[l1][l4] >= 0) {
                                    return true;
                                }
                                int l3 = lookup[pl1][po2];
                                if (l2 >= 0 && l3 >= 0 && intersections[l2][l3] >= 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean hullsOverCap(int cap) {
        int[] ll = lines[lines.length - 1];
        BitSet last = new BitSet(pointCount);
        for (int point : ll) {
            last.set(point);
        }
        for (int pt = 0; pt < pointCount; pt++) {
            if (last.get(pt)) {
                continue;
            }
            BitSet base = (BitSet) last.clone();
            base.set(pt);
            BitSet additional = new BitSet(pointCount);
            additional.set(pt);
            while (!(additional = additional(base, additional)).isEmpty()) {
                base.or(additional);
                if (base.cardinality() > cap) {
                    return true;
                }
            }
        }
        return false;
    }

    private BitSet additional(BitSet first, BitSet second) {
        BitSet result = new BitSet(pointCount);
        for (int x = first.nextSetBit(0); x >= 0; x = first.nextSetBit(x + 1)) {
            for (int y = second.nextSetBit(0); y >= 0; y = second.nextSetBit(y + 1)) {
                int line = lookup[x][y];
                if (line < 0) {
                    continue;
                }
                for (int p : lines[line]) {
                    if (!first.get(p) && !second.get(p)) {
                        result.set(p);
                    }
                }
            }
        }
        return result;
    }

    public boolean hasGaps(int pt) {
        int[] arr = lookup[pt];
        for (int i = 0; i < pointCount; i++) {
            if (i == pt) {
                continue;
            }
            if (arr[i] < 0) {
                return true;
            }
        }
        return false;
    }
}
