package ua.ihromant.mathutils;

import ua.ihromant.mathutils.plane.Quad;

import java.util.Arrays;

public class ProjectiveTernaryRing implements TernaryRing {
    private final Liner plane;
    private final int order;
    private final int o;
    private final int u;
    private final int w;
    private final int e;
    private final int dl;
    private final int hor;
    private final int ver;
    private final int oe;
    private final int[] diagonalOrder;
    private final int[] idxes;

    public ProjectiveTernaryRing(Liner plane, Quad q) {
        this.plane = plane;
        this.o = q.a();
        this.u = q.b();
        this.w = q.c();
        this.e = q.d();
        this.hor = plane.line(o, u);
        this.ver = plane.line(o, w);
        this.oe = plane.line(o, e);
        this.dl = plane.line(plane.intersection(hor, plane.line(w, e)), plane.intersection(ver, plane.line(u, e)));
        this.diagonalOrder = plane.line(oe).clone();
        this.order = diagonalOrder.length - 1;
        for (int i = 0; i < diagonalOrder.length; i++) {
            int infty = diagonalOrder[i];
            if (plane.flag(dl, infty)) {
                diagonalOrder[i] = diagonalOrder[order];
                diagonalOrder[order] = infty;
                break;
            }
        }
        for (int i = 0; i < order; i++) {
            int zero = diagonalOrder[i];
            if (zero == o) {
                diagonalOrder[i] = diagonalOrder[0];
                diagonalOrder[0] = zero;
                break;
            }
        }
        for (int i = 1; i < order; i++) {
            int unit = diagonalOrder[i];
            if (unit == e) {
                diagonalOrder[i] = diagonalOrder[1];
                diagonalOrder[1] = unit;
                break;
            }
        }
        this.idxes = new int[plane.pointCount()];
        Arrays.fill(idxes, -1);
        for (int i = 0; i < order; i++) {
            idxes[diagonalOrder[i]] = i;
        }
    }

    @Override
    public int op(int x, int a, int b) {
        int aPt = diagonalOrder[a];
        int bPt = diagonalOrder[b];
        int xPt = diagonalOrder[x];
        int ea = plane.intersection(parallel(hor, aPt), parallel(ver, e));
        int lao = plane.line(o, ea);
        int ob = plane.intersection(ver, parallel(hor, bPt));
        int lab = parallel(lao, ob);
        int xy = plane.intersection(lab, parallel(ver, xPt));
        int y = plane.intersection(oe, parallel(hor, xy));
        return diagIdx(y);
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public int[][][] matrix() {
        int[][][] result = new int[order][order][order];
        for (int a = 0; a < order; a++) {
            int aPt = diagonalOrder[a];
            for (int b = 0; b < order; b++) {
                int bPt = diagonalOrder[b];
                for (int x = 0; x < order; x++) {
                    int xPt = diagonalOrder[x];
                    int ea = plane.intersection(parallel(hor, aPt), parallel(ver, e));
                    int lao = plane.line(o, ea);
                    int ob = plane.intersection(ver, parallel(hor, bPt));
                    int lab = parallel(lao, ob);
                    int xy = plane.intersection(lab, parallel(ver, xPt));
                    int y = plane.intersection(oe, parallel(hor, xy));
                    result[x][a][b] = diagIdx(y);
                }
            }
        }
        return result;
    }

    @Override
    public Quad base() {
        return new Quad(o, u, w, e);
    }

    private int parallel(int l, int p) {
        if (l == dl || plane.flag(dl, p)) {
            throw new IllegalArgumentException();
        }
        return plane.line(p, plane.intersection(l, dl));
    }

    private int diagIdx(int p) {
        return idxes[p];
    }

    private static long quadIdx(int order, int a, int b, int c, int d) {
        return (((long) order * a + b) * order + c) * order + d;
    }

    public static long quadIdx(Liner plane, Quad q) {
        return quadIdx(plane.pointCount(), q.a(), q.b(), q.c(), q.d());
    }

    public long quadIdx() {
        return quadIdx(plane.pointCount(), o, u, w, e);
    }
}
