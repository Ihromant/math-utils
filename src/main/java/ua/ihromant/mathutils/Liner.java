package ua.ihromant.mathutils;

import ua.ihromant.jnauty.GraphData;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.nauty.AutomorphismConsumer;
import ua.ihromant.mathutils.nauty.AutomorphismConsumerNew;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.CanonicalConsumerNew;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.nauty.NautyAlgoNew;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Liner implements NautyGraph {
    private final int pointCount;
    private final int[][] lines;
    private final FixBS[] flags;
    private final int[][] lookup;
    private final int[][] beams;
    private final int[][] intersections;
    private GraphData gd;

    public Liner(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, lines);
    }

    public Liner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = IntStream.range(0, lines.length).mapToObj(_ -> new FixBS(pointCount)).toArray(FixBS[]::new);
        int[] beamCounts = new int[pointCount];
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i].set(pt);
                beamCounts[pt]++;
            }
        }
        this.beams = new int[pointCount][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            beams[pt] = new int[bc];
            int idx = 0;
            for (int ln = 0; ln < lines.length; ln++) {
                if (flags[ln].get(pt)) {
                    beams[pt][idx++] = ln;
                }
            }
        }
        this.lookup = generateLookup();
        this.intersections = generateIntersections();
    }

    public Liner(FixBS[] incidence) {
        this.flags = incidence;
        int pointCount = 0;
        int lineCount = flags.length;
        this.lines = new int[lineCount][];
        for (int i = 0; i < lineCount; i++) {
            FixBS ln = flags[i];
            int sz = ln.cardinality();
            int[] line = new int[sz];
            int idx = 0;
            for (int pt = ln.nextSetBit(0); pt >= 0; pt = ln.nextSetBit(pt + 1)) {
                line[idx++] = pt;
                pointCount = Math.max(pointCount, pt);
            }
            lines[i] = line;
        }
        pointCount = pointCount + 1;
        this.pointCount = pointCount;
        int[] beamCounts = new int[pointCount];
        for (int i = 0; i < lineCount; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                beamCounts[pt]++;
            }
        }
        this.beams = new int[pointCount][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            beams[pt] = new int[bc];
            int idx = 0;
            for (int ln = 0; ln < lineCount; ln++) {
                if (flags[ln].get(pt)) {
                    beams[pt][idx++] = ln;
                }
            }
        }
        this.lookup = generateLookup();
        this.intersections = generateIntersections();
    }

    public Liner(BitSet[] lines) {
        this(Arrays.stream(lines).mapToInt(BitSet::length).max().orElseThrow(),
                Arrays.stream(lines).map(bs -> bs.stream().toArray()).toArray(int[][]::new));
    }

    public static Liner byGaloisPower(int pointCount, int[] base) {
        int k = base.length;
        int t = (pointCount - 1) / k / (k - 1);
        int m = (pointCount - 1) / 2 / t;
        GaloisField pf = new GaloisField(pointCount);
        int prim = pf.primitives().findAny().orElseThrow();
        int[][] lines = IntStream.range(0, t).boxed().flatMap(idx -> {
            int[] block = Arrays.stream(base).map(j -> pf.mul(j, pf.power(prim, idx * m))).toArray();
            return pf.elements().mapToObj(i -> {
                BitSet res = new BitSet();
                for (int shift : block) {
                    res.set(pf.add(i, shift));
                }
                return res;
            });
        }).map(bs -> bs.stream().toArray()).toArray(int[][]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(int[]... base) {
        int pointCount = Arrays.stream(base).mapToInt(arr -> arr.length * (arr.length - 1)).sum() + 1;
        int[][] lines = Stream.of(base).flatMap(arr -> IntStream.range(0, pointCount).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : arr) {
                res.set((idx + shift) % pointCount);
            }
            return res;
        })).map(bs -> bs.stream().toArray()).toArray(int[][]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(int pointCount, int[]... base) {
        int[] lastBlock = base[base.length - 1];
        int k = lastBlock.length; // assuming that difference family is correct
        boolean slanted = pointCount % k == 0;
        int groupCard = slanted && lastBlock[k - 1] == pointCount - 1 ? pointCount - 1 : pointCount;
        boolean hasFixed = groupCard != pointCount;
        int[][] lines = Stream.concat(Arrays.stream(base, 0, slanted ? base.length - 1 : base.length)
                .flatMap(arr -> IntStream.range(0, groupCard).mapToObj(idx -> {
                    BitSet res = new BitSet();
                    for (int shift : arr) {
                        res.set((idx + shift) % groupCard);
                    }
                    return res;
                })), slanted ? IntStream.range(0, hasFixed ? groupCard / (k - 1) : groupCard / k).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : lastBlock) {
                res.set(shift == groupCard ? shift : (idx + shift) % groupCard);
            }
            return res;
        }) : Stream.of()).map(bs -> bs.stream().toArray()).toArray(int[][]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(Group g, int[]... base) {
        int[] lastBlock = base[base.length - 1];
        int k = lastBlock.length; // assuming that difference family is correct
        int gOrd = g.order();
        int rest = gOrd % k;
        int pointCount = rest == k - 1 ? gOrd + 1 : gOrd;
        boolean slanted = rest == 0 || rest == k - 1;
        int[][] lines = Stream.concat(Arrays.stream(base, 0, slanted ? base.length - 1 : base.length)
                .flatMap(arr -> g.elements().mapToObj(el -> {
                    BitSet res = new BitSet();
                    for (int shift : arr) {
                        res.set(g.op(el, shift));
                    }
                    return res;
                })), slanted ? g.elements().mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : lastBlock) {
                res.set(shift == gOrd ? shift : g.op(idx, shift));
            }
            return res;
        }).distinct() : Stream.of()).map(bs -> bs.stream().toArray()).toArray(int[][]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byStrings(String... design) {
        int pointCount = Arrays.stream(design).flatMap(s -> s.chars().boxed()).collect(Collectors.toSet()).size();
        int[][] lines = IntStream.range(0, design[0].length()).mapToObj(idx -> {
            int[] res = new int[design.length];
            for (int i = 0; i < design.length; i++) {
                String s = design[i];
                res[i] = Character.digit(s.charAt(idx), 36);
            }
            return res;
        }).toArray((int[][]::new));
        return new Liner(pointCount, lines);
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

    private int[][] generateIntersections() {
        int[][] result = new int[lines.length][lines.length];
        for (int[] arr : result) {
            Arrays.fill(arr, -1);
        }
        for (int p = 0; p < pointCount; p++) {
            int[] beam = beams[p];
            for (int i = 0; i < beam.length; i++) {
                int l1 = beam[i];
                for (int j = i + 1; j < beam.length; j++) {
                    int l2 = beam[j];
                    result[l1][l2] = p;
                    result[l2][l1] = p;
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

    public int[][] lines() {
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
                FixBS fgs = flags[line(first, second)];
                return Arrays.stream(points, i + 1, points.length).allMatch(fgs::get);
            }
        }
        return true;
    }

    public String lineToString(int line) {
        return Arrays.toString(lines[line]);
    }

    public FixBS hull(int... points) {
        FixBS base = new FixBS(pointCount);
        for (int point : points) {
            base.set(point);
        }
        FixBS additional = base;
        while (!(additional = additional(base, additional)).isEmpty()) {
            base.or(additional);
        }
        return base;
    }

    public FixBS additional(FixBS base, FixBS add) {
        FixBS result = new FixBS(pointCount);
        for (int x = base.nextSetBit(0); x >= 0; x = base.nextSetBit(x + 1)) {
            for (int y = add.nextSetBit(0); y >= 0; y = add.nextSetBit(y + 1)) {
                int xy = line(x, y);
                if (xy < 0) {
                    continue;
                }
                result.or(flags[xy]);
            }
        }
        result.andNot(base);
        return result;
    }

    public Liner subPlane(int[] pointArray) {
        return new Liner(pointArray.length, Arrays.stream(lines).map(l -> Arrays.stream(l)
                        .map(p -> Arrays.binarySearch(pointArray, p)).filter(p -> p >= 0).toArray())
                .filter(bs -> bs.length > 1).toArray(int[][]::new));
    }

    public BitSet hyperbolicIndex() {
        int maximum = Arrays.stream(lines).mapToInt(arr -> arr.length).max().orElseThrow() - 1;
        BitSet result = new BitSet();
        for (int o = 0; o < pointCount; o++) {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                int ox = line(o, x);
                for (int y = 0; y < pointCount; y++) {
                    if (flag(ox, y)) {
                        continue;
                    }
                    for (int p : lines[line(x, y)]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int counter = 0;
                        for (int u : lines[line(o, y)]) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersection(line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        result.set(counter);
                    }
                    if (result.cardinality() == maximum) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public Map<Integer, Integer> hyperbolicFreq() {
        int maxLength = Arrays.stream(lines).mapToInt(l -> l.length).reduce(0, Math::max);
        AtomicInteger[] res = IntStream.range(0, maxLength).mapToObj(_ -> new AtomicInteger()).toArray(AtomicInteger[]::new);
        IntStream.range(0, pointCount).parallel().forEach(o -> {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                int ox = line(o, x);
                for (int y = 0; y < pointCount; y++) {
                    if (flag(ox, y)) {
                        continue;
                    }
                    for (int p : lines[line(x, y)]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int counter = 0;
                        for (int u : lines[line(o, y)]) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersection(line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        res[counter].incrementAndGet();
                    }
                }
            }
        });
        return IntStream.range(0, res.length).filter(i -> res[i].get() != 0).boxed().collect(Collectors.toMap(Function.identity(), i -> res[i].get()));
    }

    public BitSet playfairIndex() {
        BitSet result = new BitSet();
        for (int l = 0; l < lines.length; l++) {
            for (int p = 0; p < pointCount; p++) {
                if (flag(l, p)) {
                    continue;
                }
                int counter = 0;
                for (int par : beams[p]) {
                    if (intersection(par, l) == -1) {
                        counter++;
                    }
                }
                result.set(counter);
            }
        }
        return result;
    }

    public Map<Integer, Integer> cardSubPlanesFreq() {
        Map<Integer, Integer> result = new TreeMap<>();
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    if (line(x, y) == line(y, z)) {
                        continue;
                    }
                    int card = hull(x, y, z).cardinality();
                    result.compute(card, (_, v) -> v == null ? 1 : v + 1);
                }
            }
        }
        return result;
    }

    public FixBS cardSubPlanes(boolean full) {
        FixBS result = new FixBS(pointCount + 1);
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                int xy = line(x, y);
                for (int z = y + 1; z < pointCount; z++) {
                    if (xy == line(y, z)) {
                        continue;
                    }
                    int card = hull(x, y, z).cardinality();
                    result.set(card);
                    if (!full && card == pointCount) {
                        return result; // it's either plane or has no exchange property
                    }
                }
            }
        }
        return result;
    }

    public FixBS cardSubSpaces(boolean full) {
        FixBS result = new FixBS(pointCount + 1);
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    if (line(x, y) == line(y, z)) {
                        continue;
                    }
                    FixBS hull = hull(x, y, z);
                    for (int w = z + 1; w < pointCount; w++) {
                        if (hull.get(w)) {
                            continue;
                        }
                        int sCard = hull(x, y, z, w).cardinality();
                        result.set(sCard);
                        if (!full && sCard == pointCount) {
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    public Liner directProduct(Liner that) {
        int length = this.lines[0].length;
        if (Arrays.stream(this.lines).anyMatch(l -> l.length != length)) {
            throw new IllegalStateException("Not all lines of length " + length);
        }
        if (Arrays.stream(that.lines).anyMatch(l -> l.length != length)) {
            throw new IllegalArgumentException("Not all lines of length " + length);
        }
        Liner aff = new Liner(new GaloisField(length).generatePlane()).subPlane(IntStream.range(0, length * length).toArray());
        CyclicProduct cg = new CyclicProduct(this.pointCount(), that.pointCount());
        BitSet[] lines = Stream.of(IntStream.range(0, this.lineCount()).boxed().flatMap(l1 -> IntStream.range(0, that.pointCount()).mapToObj(p2 -> {
                    BitSet result = new BitSet();
                    for (int p1 : this.points(l1)) {
                        result.set(cg.fromArr(p1, p2));
                    }
                    return result;
                })),
                IntStream.range(0, that.lineCount()).boxed().flatMap(l2 -> IntStream.range(0, this.pointCount()).mapToObj(p1 -> {
                    BitSet result = new BitSet();
                    for (int p2 : that.points(l2)) {
                        result.set(cg.fromArr(p1, p2));
                    }
                    return result;
                })),
                IntStream.range(0, this.lineCount()).boxed().flatMap(l1 -> IntStream.range(0, that.lineCount()).boxed().flatMap(l2 -> {
                    int[] arr1 = this.line(l1);
                    int[] arr2 = that.line(l2);
                    return IntStream.range(0, aff.lineCount()).mapToObj(aff::line).filter(l -> {
                        int fst = l[0];
                        return Arrays.stream(l, 1, l.length).skip(1).allMatch(p -> p / length != fst / length && p % length != fst % length);
                    }).map(l -> {
                        BitSet result = new BitSet();
                        for (int i = 0; i < length; i++) {
                            result.set(cg.fromArr(arr1[l[i] / length], arr2[l[i] % length]));
                        }
                        return result;
                    });
                }))).flatMap(Function.identity()).toArray(BitSet[]::new);
        return new Liner(lines);
    }

    public boolean isRegular() {
        for (int i = 0; i < lines.length; i++) {
            for (int j = i + 1; j < lines.length; j++) {
                if (intersection(i, j) < 0) {
                    continue;
                }
                BitSet pts = new BitSet();
                for (int p1 : points(i)) {
                    for (int p2 : points(j)) {
                        if (p1 == p2) {
                            continue;
                        }
                        Arrays.stream(lines[line(p1, p2)]).forEach(pts::set);
                    }
                }
                if (pts.cardinality() != pointCount) {
                    return false;
                }
            }
        }
        return true;
    }

    public BitSet[] pointResidue(int p) {
        Set<FixBS> sets = new HashSet<>();
        BitSet points = new BitSet();
        for (int i = 0; i < lineCount(); i++) {
            if (flag(i, p)) {
                points.set(i);
            }
        }
        for (int i = 0; i < pointCount; i++) {
            if (i == p) {
                continue;
            }
            for (int j = i + 1; j < pointCount; j++) {
                if (j == p || line(i, p) == line(j, p)) {
                    continue;
                }
                FixBS hull = hull(p, i, j);
                FixBS line = new FixBS(pointCount);
                for (int pt = points.nextSetBit(0); pt >= 0; pt = points.nextSetBit(pt + 1)) {
                    int[] l = lines[pt];
                    if (Arrays.stream(l).allMatch(hull::get)) {
                        line.set(pt);
                    }
                }
                sets.add(line);
            }
        }
        int[] pointArray = points.stream().toArray();
        return sets.stream().map(l -> l.stream()
                        .map(pt -> Arrays.binarySearch(pointArray, pt)).filter(pt -> pt >= 0).collect(BitSet::new, BitSet::set, BitSet::or))
                .filter(bs -> bs.cardinality() > 1).toArray(BitSet[]::new);
    }

    public List<Liner> paraModifications() {
        GraphData gd = graphData();
        int b = lineCount();
        int v = pointCount;
        int k = lines[0].length;
        int r = (v - 1) / (k - 1);
        FixBS orbLines = new FixBS(b);
        for (int i = v; i < b + v; i++) {
            orbLines.set(gd.orbits()[i] - v);
        }
        List<Liner> result = new ArrayList<>();
        for (int ln : orbLines.toArray()) {
            int[] line = line(ln);
            int[] vert = new int[k * (r - 1)];
            int cnt = 0;
            FixBS[] comps = IntStream.range(0, k).mapToObj(_ -> new FixBS(vert.length)).toArray(FixBS[]::new);
            for (int i = 0; i < b; i++) {
                int inter = intersection(ln, i);
                if (inter >= 0) {
                    vert[cnt] = i;
                    comps[Arrays.binarySearch(line, inter)].set(cnt);
                    cnt++;
                }
            }
            Set<FixBS> original = Arrays.stream(comps).collect(Collectors.toSet());
            Graph g = new Graph(vert.length);
            for (int i = 0; i < vert.length; i++) {
                for (int j = i + 1; j < vert.length; j++) {
                    int inter = intersection(vert[i], vert[j]);
                    if (inter < 0 || flag(ln, inter)) {
                        g.connect(i, j);
                    }
                }
            }
            List<FixBS> cList = new ArrayList<>();
            g.bronKerbPivot(clq -> {
                if (clq.cardinality() == r - 1) {
                    cList.add(clq);
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
            List<FixBS> pList = new ArrayList<>();
            g1.bronKerbPivot(clq -> {
                if (clq.cardinality() == k) {
                    pList.add(clq);
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
                Liner altLnr = new Liner(altInc);
                result.add(altLnr);
            }
        }
        return result;
    }

    public int[][][] resolutions() {
        int k = lines[0].length;
        int v = pointCount;
        int b = lines.length;
        if (v % k != 0 || Arrays.stream(lines).anyMatch(l -> l.length != k)) {
            return new int[0][][];
        }
        int rs = v / k;
        int r = (v - 1) / (k - 1);
        Graph g = new Graph(b);
        for (int i = 0; i < b; i++) {
            for (int j = i + 1; j < b; j++) {
                if (intersection(i, j) < 0) {
                    g.connect(i, j);
                }
            }
        }
        List<FixBS> cList = new ArrayList<>();
        g.bronKerbPivot(clq -> {
            if (clq.cardinality() == rs) {
                cList.add(clq);
            }
        });
        int ncl = cList.size();
        Graph g1 = new Graph(ncl);
        for (int i = 0; i < ncl; i++) {
            for (int j = i + 1; j < ncl; j++) {
                if (!cList.get(i).intersects(cList.get(j))) {
                    g1.connect(i, j);
                }
            }
        }
        List<FixBS> pList = new ArrayList<>();
        g1.bronKerbPivot(clq -> {
            if (clq.cardinality() == r) {
                pList.add(clq);
            }
        });
        return pList.stream().map(p -> p.stream().mapToObj(i -> cList.get(i).toArray()).toArray(int[][]::new)).toArray(int[][][]::new);
    }

    public int triangleCount() {
        return pointCount * (pointCount - 1) * (pointCount - lines[0].length); // assuming that liner is uniform
    }

    public Triangle trOf(int idx) {
        int o = idx % pointCount;
        idx = idx / pointCount;
        int uIdx = idx % (pointCount - 1);
        int u = o > uIdx ? uIdx : uIdx + 1;
        int wIdx = idx / (pointCount - 1);
        BitSet bs = new BitSet();
        bs.set(0, pointCount);
        int[] line = lines[line(o, u)];
        for (int p : line) {
            bs.clear(p);
        }
        for (int w = bs.nextSetBit(0); w >= 0; w = bs.nextSetBit(w + 1)) {
            if (--wIdx < 0) {
                return new Triangle(o, u, w);
            }
        }
        throw new IllegalArgumentException();
    }

    public int trIdx(Triangle tr) {
        int uIdx = tr.u() > tr.o() ? tr.u() - 1 : tr.u();
        BitSet bs = new BitSet();
        bs.set(0, pointCount);
        int[] line = lines[line(tr.o(), tr.u())];
        for (int p : line) {
            bs.set(p, false);
        }
        int wIdx = 0;
        for (int w = bs.nextSetBit(0); w >= 0; w = bs.nextSetBit(w + 1)) {
            if (w == tr.w()) {
                return (wIdx * (pointCount - 1) + uIdx) * pointCount + tr.o();
            }
            wIdx++;
        }
        throw new IllegalArgumentException();
    }

    public void automorphisms(Consumer<int[]> autConsumer) {
        int[] partialPoints = new int[pointCount];
        int[] partialLines = new int[lines.length];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        int[] perLineUnAss = new int[lines.length];
        Arrays.fill(perLineUnAss, lines[0].length);
        int[] perPointUnAss = new int[pointCount];
        Arrays.fill(perPointUnAss, beams[0].length);
        automorphisms(autConsumer, 0, partialPoints, new boolean[pointCount], perPointUnAss, partialLines, new boolean[lines.length], perLineUnAss);
    }

    private void automorphisms(Consumer<int[]> autConsumer, int mapped, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        int from = -1;
        boolean foundNotCrossing = false;
        ex: for (int l = 0; l < linesMap.length; l++) {
            if (linesMap[l] >= 0) {
                continue;
            }
            for (int p : lines[l]) {
                if (pointsMap[p] >= 0) {
                    continue ex;
                }
            }
            foundNotCrossing = true;
            from = l;
            break;
        }
        if (!foundNotCrossing) {
            for (int i = 0; i < lines.length; i++) {
                if (linesMap[i] < 0) {
                    from = i;
                    break;
                }
            }
        }
        BitSet toFilter = new BitSet();
        if (foundNotCrossing) {
            for (int p : pointsMap) {
                if (p < 0) {
                    continue;
                }
                for (int l : beams[p]) {
                    toFilter.set(l);
                }
            }
        } else {
            for (int l : linesMap) {
                if (l < 0) {
                    continue;
                }
                toFilter.set(l);
            }
        }
        for (int to = toFilter.nextClearBit(0); to < linesMap.length; to = toFilter.nextClearBit(to + 1)) {
            if (lnMapped[to]) {
                continue;
            }
            int[] newPointsMap = pointsMap.clone();
            int[] newLinesMap = linesMap.clone();
            boolean[] newPtMapped = ptMapped.clone();
            boolean[] newLnMapped = lnMapped.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            int added = mapLine(this, from, to, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == lines.length) {
                autConsumer.accept(newPointsMap);
                continue;
            }
            automorphisms(autConsumer, newMapped, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
        }
    }

    private int mapPoint(Liner second, int from, int to, int[] newPointsMap, boolean[] newPtMapped, int[] newPerPointUnAss, int[] newLinesMap, boolean[] newLnMapped, int[] newPerLineUnAss) {
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
        if (newPtMapped[to]) {
            return -1;
        }
        newPointsMap[from] = to;
        newPtMapped[to] = true;
        int result = 0;
        for (int line : beams[from]) {
            newPerLineUnAss[line]--;
            for (int p : lines[line]) {
                int pMap = newPointsMap[p];
                if (p == from || pMap < 0) {
                    continue;
                }
                int lineTo = second.lookup[to][pMap];
                int added = mapLine(second, line, lineTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                if (newPerLineUnAss[line] == 1) {
                    int ptFrom = -1;
                    int ptTo = -1;
                    for (int p1 : lines[line]) {
                        if (newPointsMap[p1] < 0) {
                            ptFrom = p1;
                            break;
                        }
                    }
                    for (int p1 : second.lines[lineTo]) {
                        if (!newPtMapped[p1]) {
                            ptTo = p1;
                            break;
                        }
                    }
                    added = mapPoint(second, ptFrom, ptTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                    if (added < 0) {
                        return -1;
                    }
                    result = result + added;
                }
                break;
            }
        }
        return result;
    }

    private int mapLine(Liner second, int from, int to, int[] newPointsMap, boolean[] newPtMapped, int[] newPerPointUnAss, int[] newLinesMap, boolean[] newLnMapped, int[] newPerLineUnAss) {
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
        if (newLnMapped[to]) {
            return -1;
        }
        newLinesMap[from] = to;
        newLnMapped[to] = true;
        int result = 1;
        for (int pt : lines[from]) {
            newPerPointUnAss[pt]--;
            for (int line : beams[pt]) {
                int lineMap = newLinesMap[line];
                if (line == from || lineMap < 0) {
                    continue;
                }
                int ptTo = second.intersections[to][lineMap];
                int added = mapPoint(second, pt, ptTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                if (newPerPointUnAss[pt] == 1) {
                    int lineFrom = -1;
                    int lineTo = -1;
                    for (int l1 : beams[pt]) {
                        if (newLinesMap[l1] < 0) {
                            lineFrom = l1;
                            break;
                        }
                    }
                    for (int l1 : second.beams[ptTo]) {
                        if (!newLnMapped[l1]) {
                            lineTo = l1;
                            break;
                        }
                    }
                    added = mapLine(second, lineFrom, lineTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                    if (added < 0) {
                        return -1;
                    }
                    result = result + added;
                }
                break;
            }
        }
        return result;
    }

    public long autCountNew() {
        AtomicLong counter = new AtomicLong();
        Consumer<int[]> cons = _ -> counter.incrementAndGet();
        AutomorphismConsumerNew aut = new AutomorphismConsumerNew(this, cons);
        NautyAlgoNew.search(this, aut);
        return counter.get();
    }

    public long autCountOld() {
        AtomicLong counter = new AtomicLong();
        Consumer<int[]> cons = _ -> counter.incrementAndGet();
        AutomorphismConsumer aut = new AutomorphismConsumer(this, cons);
        NautyAlgo.search(this, aut);
        return counter.get();
    }

    public PermutationGroup automorphisms() {
        List<int[]> res = new ArrayList<>();
        Consumer<int[]> cons = res::add;
        AutomorphismConsumer aut = new AutomorphismConsumer(this, cons);
        NautyAlgo.search(this, aut);
        return new PermutationGroup(res.toArray(int[][]::new));
    }

    public FixBS getCanonical() {
        CanonicalConsumerNew cons = new CanonicalConsumerNew(this);
        NautyAlgoNew.search(this, cons);
        return cons.canonicalForm();
    }

    public FixBS getCanonicalOld() {
        CanonicalConsumer cons = new CanonicalConsumer(this);
        NautyAlgo.search(this, cons);
        return cons.canonicalForm();
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
