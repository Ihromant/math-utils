package ua.ihromant.mathutils.auto;

import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.Triangle;
import ua.ihromant.mathutils.plane.AffineTernaryRing;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.ProjectiveTernaryRing;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.plane.TernarMapping;
import ua.ihromant.mathutils.plane.TernaryRing;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class TernaryAutomorphisms {
    public static List<int[]> automorphismsAffine(Liner proj, int dl) {
        List<int[]> result = new ArrayList<>();
        ProjectiveTernaryRing first = null;
        TernarMapping mapping = null;
        int pc = proj.pointCount();
        int order = proj.line(0).length - 1;
        int[] line = proj.line(dl);
        for (int h : line) {
            for (int v : line) {
                if (v == h) {
                    continue;
                }
                for (int o = 0; o < pc; o++) {
                    if (proj.flag(dl, o)) {
                        continue;
                    }
                    int oh = proj.line(o, h);
                    int ov = proj.line(o, v);
                    for (int e = 0; e < pc; e++) {
                        if (proj.flag(dl, e) || proj.flag(ov, e) || proj.flag(oh, e)) {
                            continue;
                        }
                        int w = proj.intersection(proj.line(e, h), ov);
                        int u = proj.intersection(proj.line(e, v), oh);
                        Quad base = new Quad(o, u, w, e);
                        ProjectiveTernaryRing ring = new ProjectiveTernaryRing("", proj, base);
                        int two = ring.op(1, 1, 1);
                        if (two == 0) {
                            continue;
                        }
                        CharVals cv = CharVals.of(ring, two, order);
                        if (!cv.induced()) {
                            continue;
                        }
                        if (mapping == null) {
                            first = ring;
                            mapping = fillTernarMapping(ring.toMatrix(), cv, two, order);
                            result.add(IntStream.range(0, proj.pointCount()).toArray());
                            continue;
                        }
                        if (!mapping.chr().equals(cv.chr())) {
                            continue;
                        }
                        TernaryRing matrix = ring.toMatrix();
                        int[] ringIsomorphism = ringIsomorphism(mapping, matrix);
                        if (ringIsomorphism == null) {
                            continue;
                        }
                        int[] map = new int[order * order + order + 1];
                        for (int i = 0; i < order; i++) {
                            for (int j = 0; j < order; j++) {
                                int fromPt = first.withCrd(i, j);
                                int toPt = ring.withCrd(ringIsomorphism[i], ringIsomorphism[j]);
                                map[fromPt] = toPt;
                            }
                            int fromDir = first.withDirection(i);
                            int toDir = ring.withDirection(ringIsomorphism[i]);
                            map[fromDir] = toDir;
                        }
                        map[first.horDir()] = ring.horDir();
                        result.add(map);
                    }
                }
            }
        }
        return result;
    }

    public static List<int[]> automorphismsProj(Liner proj) {
        List<int[]> result = new ArrayList<>();
        ProjectiveTernaryRing first = null;
        TernarMapping mapping = null;
        int pc = proj.pointCount();
        int order = proj.line(0).length - 1;
        for (int dl = 0; dl < proj.lineCount(); dl++) {
            int[] line = proj.line(dl);
            for (int h : line) {
                for (int v : line) {
                    if (v == h) {
                        continue;
                    }
                    for (int o = 0; o < pc; o++) {
                        if (proj.flag(dl, o)) {
                            continue;
                        }
                        int oh = proj.line(o, h);
                        int ov = proj.line(o, v);
                        for (int e = 0; e < pc; e++) {
                            if (proj.flag(dl, e) || proj.flag(ov, e) || proj.flag(oh, e)) {
                                continue;
                            }
                            int w = proj.intersection(proj.line(e, h), ov);
                            int u = proj.intersection(proj.line(e, v), oh);
                            Quad base = new Quad(o, u, w, e);
                            ProjectiveTernaryRing ring = new ProjectiveTernaryRing("", proj, base);
                            int two = ring.op(1, 1, 1);
                            if (two == 0) {
                                continue;
                            }
                            CharVals cv = CharVals.of(ring, two, order);
                            if (!cv.induced()) {
                                continue;
                            }
                            if (mapping == null) {
                                first = ring;
                                mapping = fillTernarMapping(ring.toMatrix(), cv, two, order);
                                result.add(IntStream.range(0, proj.pointCount()).toArray());
                                continue;
                            }
                            if (!mapping.chr().equals(cv.chr())) {
                                continue;
                            }
                            TernaryRing matrix = ring.toMatrix();
                            int[] ringIsomorphism = ringIsomorphism(mapping, matrix);
                            if (ringIsomorphism == null) {
                                continue;
                            }
                            int[] map = new int[order * order + order + 1];
                            for (int i = 0; i < order; i++) {
                                for (int j = 0; j < order; j++) {
                                    int fromPt = first.withCrd(i, j);
                                    int toPt = ring.withCrd(ringIsomorphism[i], ringIsomorphism[j]);
                                    map[fromPt] = toPt;
                                }
                                int fromDir = first.withDirection(i);
                                int toDir = ring.withDirection(ringIsomorphism[i]);
                                map[fromDir] = toDir;
                            }
                            map[first.horDir()] = ring.horDir();
                            result.add(map);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static final Triangle t111 = new Triangle(1, 1, 1);
    private static final FixBS base = FixBS.of(2, 0, 1);

    public static TernarMapping fillTernarMapping(TernaryRing ring, CharVals cv, int two, int order) {
        List<FixBS> xl = new ArrayList<>();
        xl.add(base);
        FixBS xi = base.copy();
        xi.set(two);
        xl.add(xi);
        int[][] vals = cv.vals();
        Triangle[] function = new Triangle[order];
        function[two] = t111;
        for (int i = 3; i < order; i++) {
            xi = xi.copy();
            if (!xi.get(vals[0][i])) {
                xi.set(vals[0][i]);
                function[vals[0][i]] = new Triangle(1, 1, vals[0][i - 1]);
            }
            if (!xi.get(vals[1][i])) {
                xi.set(vals[1][i]);
                function[vals[1][i]] = new Triangle(vals[1][i - 1], 1, 1);
            }
            if (!xi.get(vals[2][i])) {
                xi.set(vals[2][i]);
                function[vals[2][i]] = new Triangle(1, vals[2][i - 1], 1);
            }
            if (!xi.get(vals[3][i])) {
                xi.set(vals[3][i]);
                function[vals[3][i]] = new Triangle(two, vals[3][i - 1], 0);
            }
            if (!xi.get(vals[4][i])) {
                xi.set(vals[4][i]);
                function[vals[4][i]] = new Triangle(vals[4][i - 1], two, 0);
            }
            xl.add(xi);
            if (xi.cardinality() == order) {
                break;
            }
        }
        return new TernarMapping(ring, xl, function, cv.chr());
    }

    private static int[] ringIsomorphism(TernarMapping tm, TernaryRing second) {
        TernaryRing first = tm.ring();
        int[] function = new int[second.order()];
        Arrays.fill(function, -1);
        function[0] = 0;
        function[1] = 1;
        for (int i = 1; i < tm.xl().size(); i++) {
            FixBS xn1 = tm.xl().get(i);
            FixBS xn = tm.xl().get(i - 1);
            FixBS missing = xn1.copy().symDiff(xn);
            for (int x = missing.nextSetBit(0); x >= 0; x = missing.nextSetBit(x + 1)) {
                Triangle tr = tm.function()[x];
                int mappedX = second.op(function[tr.o()], function[tr.u()], function[tr.w()]);
                function[x] = mappedX;
            }
        }
        if (!isBijective(function)) {
            return null;
        }
        for (int a = 1; a < first.order(); a++) {
            for (int b = 0; b < first.order(); b++) {
                for (int c = 0; c < first.order(); c++) {
                    if (second.op(function[a], function[b], function[c]) != function[first.op(a, b, c)]) {
                        return null;
                    }
                }
            }
        }
        return function;
    }

    private static boolean isBijective(int[] partialFunc) {
        int[] idxes = new int[partialFunc.length];
        Arrays.fill(idxes, -1);
        for (int i = 0; i < partialFunc.length; i++) {
            int val = partialFunc[i];
            if (val >= 0) {
                if (idxes[val] >= 0) {
                    return false;
                }
                idxes[val] = i;
            } else {
                return false;
            }
        }
        return true;
    }

    public static TernarMapping findTernarMapping(TernaryRing ring) {
        int two = ring.op(1, 1, 1);
        int order = ring.order();
        if (two == 0) {
            return new TernarMapping(ring, List.of(base), new Triangle[order], Characteristic.simpleChr);
        }
        CharVals cv = CharVals.of(ring, two, order);
        TernarMapping result = TernaryAutomorphisms.fillTernarMapping(ring, cv, two, order);
        return finishTernarMapping(result);
    }

    private static TernarMapping finishTernarMapping(TernarMapping mapping) {
        TernaryRing ring = mapping.ring();
        int order = ring.order();
        FixBS xn = mapping.xl().getLast();
        if (xn.cardinality() == order) {
            return new TernarMapping(ring.toMatrix(), mapping.xl(), mapping.function(), mapping.chr());
        }
        FixBS xn1 = xn.copy();
        for (int a = xn.nextSetBit(0); a >= 0; a = xn.nextSetBit(a + 1)) {
            for (int b = xn.nextSetBit(0); b >= 0; b = xn.nextSetBit(b + 1)) {
                for (int c = xn.nextSetBit(0); c >= 0; c = xn.nextSetBit(c + 1)) {
                    int t = ring.op(a, b, c);
                    if (!xn1.get(t)) {
                        xn1.set(t);
                        mapping.function()[t] = new Triangle(a, b, c);
                    }
                }
            }
        }
        if (xn1.cardinality() == xn.cardinality()) {
            return mapping;
        }
        mapping.xl().add(xn1);
        return finishTernarMapping(mapping);
    }

    public static boolean isDesargues(Liner liner, int order) {
        int dl = 0;
        int o = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(dl, p)).findAny().orElseThrow();
        int u = IntStream.range(0, liner.pointCount()).filter(p -> p != o && !liner.flag(dl, p)).findAny().orElseThrow();
        int ou = liner.line(o, u);
        int w = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(ou, p) && !liner.flag(dl, p)).findAny().orElseThrow();
        int ow = liner.line(o, w);
        int e = liner.intersection(liner.line(u, liner.intersection(dl, ow)), liner.line(w, liner.intersection(dl, ou)));
        TernaryRing ring = new ProjectiveTernaryRing("", liner, new Quad(o, u, w, e)).toMatrix();
        return isDesargues(ring);
    }

    private static boolean isDesargues(TernaryRing ring) {
        for (int x = 1; x < ring.order(); x++) {
            for (int y = x + 1; y < ring.order(); y++) {
                int xy = ring.mul(x, y);
                if (xy != ring.mul(y, x)) {
                    return false;
                }
                if (ring.mul(ring.mul(x, x), y) != ring.mul(x, xy)) {
                    return false;
                }
                for (int z = 1; z < ring.order(); z++) {
                    int yz = ring.add(y, z);
                    if (ring.op(x, y, z) != ring.add(xy, z)) {
                        return false;
                    }
                    if (ring.add(ring.add(x, y), z) != ring.add(x, yz)) {
                        return false;
                    }
                    if (ring.mul(x, yz) != ring.add(xy, ring.mul(x, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static int findTranslationLine(Liner liner) {
        for (int dl : IntStream.range(0, liner.lineCount()).toArray()) {
            int o = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(dl, p)).findAny().orElseThrow();
            int[] dropped = liner.line(dl);
            int v = dropped[0];
            int h = dropped[1];
            int oh = liner.line(o, h);
            int ov = liner.line(o, v);
            int e = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(dl, p) && !liner.flag(ov, p) && !liner.flag(oh, p)).findAny().orElseThrow();
            int w = liner.intersection(liner.line(e, h), ov);
            int u = liner.intersection(liner.line(e, v), oh);
            Quad base = new Quad(o, u, w, e);
            TernaryRing ring = new ProjectiveTernaryRing("", liner, base).toMatrix();
            if (ring.isLinear() && ring.addAssoc() && ring.addComm() && ring.isRightDistributive()) {
                return dl;
            }
        }
        return -1;
    }

    public static boolean isAffineTranslation(Liner liner) {
        int o = 0;
        int u = 1;
        int ou = liner.line(o, u);
        int w = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(ou, p)).findAny().orElseThrow();
        TernaryRing ring = new AffineTernaryRing(liner, new Triangle(o, u, w)).toMatrix();
        return ring.isLinear() && ring.addAssoc() && ring.addComm() && ring.isRightDistributive();
    }

    public static boolean isAffineDesargues(Liner liner) {
        int o = 0;
        int u = 1;
        int ou = liner.line(o, u);
        int w = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(ou, p)).findAny().orElseThrow();
        TernaryRing ring = new AffineTernaryRing(liner, new Triangle(o, u, w)).toMatrix();
        return isDesargues(ring);
    }
}
