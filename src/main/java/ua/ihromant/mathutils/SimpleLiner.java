package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public class SimpleLiner {
    private final int pointCount;
    private final FixBS[] lines;
    private int[][] lookup;

    public SimpleLiner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = Arrays.stream(lines).map(l -> {
            FixBS bs = new FixBS(pointCount);
            for (int pt : l) {
                bs.set(pt);
            }
            return bs;
        }).toArray(FixBS[]::new);
        this.lookup = new int[pointCount][pointCount];
        for (int[] p : lookup) {
            Arrays.fill(p, -1);
        }
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    if (lookup[p1][p2] >= 0) {
                        throw new IllegalStateException();
                    }
                    lookup[p1][p2] = l;
                    lookup[p2][p1] = l;
                }
            }
        }
    }

    public SimpleLiner(int pointCount, FixBS[] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.lookup = new int[pointCount][pointCount];
        for (int[] p : lookup) {
            Arrays.fill(p, -1);
        }
        for (int l = 0; l < lines.length; l++) {
            FixBS line = lines[l];
            for (int p1 = line.nextSetBit(0); p1 >= 0; p1 = line.nextSetBit(p1 + 1)) {
                for (int p2 = line.nextSetBit(p1 + 1); p2 >= 0; p2 = line.nextSetBit(p2 + 1)) {
                    if (lookup[p1][p2] >= 0) {
                        throw new IllegalStateException();
                    }
                    lookup[p1][p2] = l;
                    lookup[p2][p1] = l;
                }
            }
        }
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public int[] line(int line) {
        return lines[line].stream().toArray();
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public int intersection(int l1, int l2) {
        FixBS line1 = lines[l1];
        FixBS line2 = lines[l2];
        for (int i = line1.nextSetBit(0); i >= 0; i = line1.nextSetBit(i + 1)) {
            if (line2.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public int[] points(int line) {
        return lines[line].stream().toArray();
    }

    public FixBS[] lines() {
        return lines;
    }

    public boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                FixBS line = lines[line(first, second)];
                return Arrays.stream(points, i + 1, points.length).allMatch(line::get);
            }
        }
        return true;
    }

    public String lineToString(int line) {
        return lines[line].toString();
    }
}
