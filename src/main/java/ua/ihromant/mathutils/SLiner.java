package ua.ihromant.mathutils;

import ua.ihromant.jnauty.GraphData;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SLiner implements NautyGraph {
    private final int pointCount;
    private final short[][] lines;
    private final FixBS[] flags;
    private final short[][] beams;
    private GraphData gd;

    public SLiner(short[][] lines) {
        this(Arrays.stream(lines).mapToInt(l -> l[l.length - 1]).max().orElseThrow() + 1, lines);
    }

    public SLiner(int pointCount, short[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = IntStream.range(0, lines.length).mapToObj(_ -> new FixBS(pointCount)).toArray(FixBS[]::new);
        int[] beamCounts = new int[pointCount];
        for (int i = 0; i < lines.length; i++) {
            short[] line = lines[i];
            for (int pt : line) {
                flags[i].set(pt);
                beamCounts[pt]++;
            }
        }
        this.beams = new short[pointCount][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            beams[pt] = new short[bc];
            int idx = 0;
            for (short ln = 0; ln < lines.length; ln++) {
                if (flags[ln].get(pt)) {
                    beams[pt][idx++] = ln;
                }
            }
        }
    }

    public SLiner(FixBS[] incidence) {
        this.flags = incidence;
        int pointCount = 0;
        int lineCount = flags.length;
        this.lines = new short[lineCount][];
        for (int i = 0; i < lineCount; i++) {
            FixBS ln = flags[i];
            int sz = ln.cardinality();
            short[] line = new short[sz];
            int idx = 0;
            for (int pt = ln.nextSetBit(0); pt >= 0; pt = ln.nextSetBit(pt + 1)) {
                line[idx++] = (short) pt;
                pointCount = Math.max(pointCount, pt);
            }
            lines[i] = line;
        }
        pointCount = pointCount + 1;
        this.pointCount = pointCount;
        int[] beamCounts = new int[pointCount];
        for (int i = 0; i < lineCount; i++) {
            short[] line = lines[i];
            for (int pt : line) {
                beamCounts[pt]++;
            }
        }
        this.beams = new short[pointCount][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            beams[pt] = new short[bc];
            int idx = 0;
            for (short ln = 0; ln < lineCount; ln++) {
                if (flags[ln].get(pt)) {
                    beams[pt][idx++] = ln;
                }
            }
        }
    }

    private short[][] generateLookup() {
        short[][] result = new short[pointCount][pointCount];
        for (short[] p : result) {
            Arrays.fill(p, (short) -1);
        }
        for (short l = 0; l < lines.length; l++) {
            short[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    if (result[p1][p2] >= 0) {
                        throw new IllegalStateException();
                    }
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

    public boolean flag(int line, int point) {
        return flags[line].get(point);
    }

    public short[] line(int line) {
        return lines[line];
    }

    public short[] lines(int point) {
        return beams[point];
    }

    public short[] point(int point) {
        return beams[point];
    }

    public short[] points(int line) {
        return lines[line];
    }

    public short[][] lines() {
        return lines;
    }

    public List<SLiner> paraModificationsAlt() {
        GraphData gd = graphData();
        int b = lineCount();
        int v = pointCount;
        int k = lines[0].length;
        int r = (v - 1) / (k - 1);
        FixBS orbLines = new FixBS(b);
        for (int i = v; i < b + v; i++) {
            orbLines.set(gd.orbits()[i] - v);
        }
        List<SLiner> result = new ArrayList<>();
        Arrays.stream(orbLines.toArray()).parallel().forEach(ln -> {
            short[] line = line(ln);
            int[] vert = new int[k * (r - 1)];
            int cnt = 0;
            FixBS[] comps = IntStream.range(0, k).mapToObj(_ -> new FixBS(vert.length)).toArray(FixBS[]::new);
            for (int i = 0; i < line.length; i++) {
                int pt = line[i];
                for (int l : lines(pt)) {
                    if (l == ln) {
                        continue;
                    }
                    vert[cnt] = l;
                    comps[i].set(cnt);
                    cnt++;
                }
            }
            Set<FixBS> original = Arrays.stream(comps).collect(Collectors.toSet());
            int[] idxes = new int[b];
            Graph g = new Graph(vert.length);
            Arrays.fill(idxes, -1);
            for (int i = 0; i < vert.length; i++) {
                idxes[vert[i]] = i;
                g.neighbors(i).set(0, vert.length);
                g.neighbors(i).clear(i);
            }
            for (int i = 0; i < vert.length; i++) {
                short[] vertLine = line(vert[i]);
                ex: for (int pt : vertLine) {
                    for (int lPt : line) {
                        if (lPt == pt) {
                            continue ex;
                        }
                    }
                    for (int oLine : lines(pt)) {
                        if (idxes[oLine] >= 0) {
                            g.disconnect(i, idxes[oLine]);
                        }
                    }
                }
            }
            List<FixBS> cList = Collections.synchronizedList(new ArrayList<>());
            g.bronKerbPivotPar((clq, sz) -> {
                if (sz == r - 1) {
                    cList.add(clq.copy());
                }
            });
            int ncl = cList.size();
            int[][] cl = cList.stream().map(FixBS::toArray).toArray(int[][]::new);
            Graph g1 = new Graph(ncl);
            for (int i = 0; i < ncl; i++) {
                for (int j = i + 1; j < ncl; j++) {
                    if (!cList.get(i).intersects(cList.get(j))) {
                        g1.connect(i, j);
                    }
                }
            }
            List<FixBS> pList = Collections.synchronizedList(new ArrayList<>());
            g1.bronKerbPivotPar((clq, sz) -> {
                if (sz == k) {
                    pList.add(clq.copy());
                }
            });
            for (FixBS p : pList) {
                int[] part = p.toArray();
                Set<FixBS> altComps = Arrays.stream(part).mapToObj(cList::get).collect(Collectors.toSet());
                if (altComps.equals(original)) {
                    continue;
                }
                FixBS[] altInc = Arrays.stream(flags).map(FixBS::copy).toArray(FixBS[]::new);
                for (int pt : line) {
                    for (int l : vert) {
                        altInc[l].clear(pt);
                    }
                }
                for (int i = 0; i < k; i++) {
                    for (int j = 0; j < r - 1; j++) {
                        altInc[vert[cl[part[i]][j]]].set(line[i]);
                    }
                }
                SLiner altLnr = new SLiner(altInc);
                synchronized (result) {
                    result.add(altLnr);
                }
            }
        });
        return result;
    }

    @Override
    public int vCount() {
        return pointCount + lines.length;
    }

    @Override
    public int vColor(int idx) {
        return idx < pointCount ? 0 : 1;
    }

    @Override
    public boolean edge(int a, int b) {
        if (a < pointCount) {
            return b >= pointCount && flags[b - pointCount].get(a);
        } else {
            return b < pointCount && flags[a - pointCount].get(b);
        }
    }

    public GraphData graphData() {
        if (gd == null) {
            gd = JNauty.instance().traces(this);
        }
        return gd;
    }
}
