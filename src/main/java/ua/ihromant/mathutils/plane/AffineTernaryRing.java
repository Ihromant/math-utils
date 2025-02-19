package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.Triangle;
import ua.ihromant.mathutils.vf2.IntPair;

import java.util.Arrays;

public class AffineTernaryRing implements TernaryRing {
    private final Liner liner;
    private final int order;
    private final int o;
    private final int u;
    private final int w;
    private final int e;
    private final int hor;
    private final int ver;
    private final int oe;
    private final int[] diagonalOrder;
    private final int[] idxes;
    private final int[][][] opMatrix;

    public AffineTernaryRing(Liner liner, Triangle reper) {
        this.liner = liner;
        this.o = reper.o();
        this.u = reper.u();
        this.w = reper.w();
        this.hor = liner.line(o, u);
        this.ver = liner.line(o, w);
        this.e = liner.intersection(parallel(hor, w), parallel(ver, u));
        this.oe = liner.line(o, e);
        this.diagonalOrder = liner.line(oe).clone();
        this.order = diagonalOrder.length;
        for (int i = 0; i < order; i++) {
            if (diagonalOrder[i] == o) {
                int d = diagonalOrder[0];
                diagonalOrder[0] = o;
                diagonalOrder[i] = d;
                break;
            }
        }
        for (int i = 1; i < order; i++) {
            if (diagonalOrder[i] == e) {
                int d = diagonalOrder[1];
                diagonalOrder[1] = e;
                diagonalOrder[i] = d;
                break;
            }
        }
        this.idxes = new int[liner.pointCount()];
        Arrays.fill(idxes, -1);
        for (int i = 0; i < order; i++) {
            idxes[diagonalOrder[i]] = i;
        }
        this.opMatrix = new int[order][order][order];
        for (int a = 0; a < order; a++) {
            int aPt = diagonalOrder[a];
            for (int b = 0; b < order; b++) {
                int bPt = diagonalOrder[b];
                for (int x = 0; x < order; x++) {
                    int xPt = diagonalOrder[x];
                    int ea = liner.intersection(parallel(hor, aPt), parallel(ver, e));
                    int lao = liner.line(o, ea);
                    int ob = liner.intersection(ver, parallel(hor, bPt));
                    int lab = parallel(lao, ob);
                    int xy = liner.intersection(lab, parallel(ver, xPt));
                    int y = liner.intersection(oe, parallel(hor, xy));
                    opMatrix[x][a][b] = diagIdx(y);
                }
            }
        }
    }

    @Override
    public int op(int x, int a, int b) {
        return opMatrix[x][a][b];
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public int[][][] matrix() {
        return opMatrix;
    }

    @Override
    public Quad base() {
        return new Quad(o, u, w, e);
    }

    @Override
    public TernaryRing toMatrix() {
        return new MatrixTernaryRing(opMatrix, base());
    }

    public int trIdx() {
        return liner.trIdx(new Triangle(o, u, w));
    }

    private int parallel(int line, int point) {
        if (liner.flag(line, point)) {
            return line;
        }
        int[] lines = liner.point(point);
        for (int l : lines) {
            if (liner.intersection(l, line) < 0) {
                return l;
            }
        }
        throw new IllegalStateException();
    }

    private int diagIdx(int p) {
        return idxes[p];
    }

    private int byIdx(int idx) {
        return diagonalOrder[idx];
    }

    public IntPair toCoordinates(int pt) {
        return new IntPair(idxes[liner.intersection(parallel(ver, pt), oe)], idxes[liner.intersection(parallel(hor, pt), oe)]);
    }

    public int withCrd(int a, int b) {
        return liner.intersection(parallel(ver, diagonalOrder[a]), parallel(hor, diagonalOrder[b]));
    }
}
