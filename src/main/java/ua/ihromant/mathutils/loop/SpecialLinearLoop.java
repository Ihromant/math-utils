package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.LinearSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class SpecialLinearLoop implements Loop {
    private final GaloisField fd;
    private final int matCount;
    private final int[] mapGl;
    private final int[] gl;
    private final int[][] mul;
    private final int[] inv;

    public SpecialLinearLoop(GaloisField fd) {
        this.fd = fd;
        this.matCount = LinearSpace.pow(fd.cardinality(), ZornMatrix.sz);
        int one = unity();
        this.mapGl = generateMapGl();
        int idx = mapGl[one];
        int fst = IntStream.range(0, matCount).filter(i -> mapGl[i] == 0).findFirst().orElseThrow();
        mapGl[one] = 0;
        mapGl[fst] = idx;
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] >= 0).toArray();
        this.gl[0] = one;
        this.gl[idx] = fst;
        this.mul = new int[gl.length][gl.length];
        for (int i = 0; i < gl.length; i++) {
            ZornMatrix f = ZornMatrix.fromInt(fd, gl[i]);
            for (int j = 0; j < gl.length; j++) {
                ZornMatrix s = ZornMatrix.fromInt(fd, gl[j]);
                mul[i][j] = mapGl[f.mul(s).toInt()];
            }
        }
        this.inv = new int[gl.length];
        for (int i = 0; i < gl.length; i++) {
            int el = i;
            inv[el] = IntStream.range(0, gl.length).filter(j -> mul[el][j] == 0).findFirst().orElseThrow();
        }
    }

    private int unity() {
        int[] arr = new int[ZornMatrix.sz];
        arr[0] = 1;
        arr[7] = 1;
        return ZornMatrix.fromArr(fd.cardinality(), arr);
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        int idx = 0;
        for (int i = 0; i < matCount; i++) {
            ZornMatrix matrix = ZornMatrix.fromInt(fd, i);
            int det = matrix.qNorm();
            if (det == 1) {
                result[i] = idx++;
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    @Override
    public int op(int a, int b) {
        return mul[a][b];
    }

    @Override
    public int inv(int a) {
        return inv[a];
    }

    @Override
    public int order() {
        return gl.length;
    }

    @Override
    public String name() {
        return "SLL(" + fd.cardinality() + ")";
    }

    @Override
    public String elementName(int a) {
        return ZornMatrix.fromInt(fd, gl[a]).toString();
    }

    public List<FixBS> subLoops() {
        List<FixBS> result = new ArrayList<>();
        int order = order();
        FixBS all = new FixBS(order);
        all.set(0, order);
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(init);
        find(init, 0, order, result::add);
        return result;
    }

    private void find(FixBS currGroup, int prev, int order, Consumer<FixBS> cons) {
        ex: for (int gen = currGroup.nextClearBit(prev + 1); gen >= 0 && gen < order; gen = currGroup.nextClearBit(gen + 1)) {
            FixBS nextGroup = currGroup.copy();
            FixBS additional = cycle(gen);
            additional.andNot(currGroup);
            do {
                if (additional.nextSetBit(0) < gen) {
                    continue ex;
                }
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, order)).isEmpty());
            cons.accept(nextGroup);
            find(nextGroup, gen, order, cons);
        }
    }

    private FixBS additional(FixBS currGroup, FixBS addition, int order) {
        FixBS result = new FixBS(order);
        for (int x = currGroup.nextSetBit(0); x >= 0; x = currGroup.nextSetBit(x + 1)) {
            for (int y = addition.nextSetBit(0); y >= 0; y = addition.nextSetBit(y + 1)) {
                result.set(op(x, y));
                result.set(op(y, x));
            }
        }
        result.andNot(currGroup);
        return result;
    }

//    public FactorGroup psl() {
//        FixBS els = new FixBS(order());
//        for (int i = 1; i < fd.cardinality(); i++) {
//            if (dim % fd.mulOrder(i) != 0) {
//                continue;
//            }
//            int[][] mat = new int[dim][dim];
//            for (int j = 0; j < dim; j++) {
//                mat[j][j] = i;
//            }
//            els.set(asElem(mat));
//        }
//        return new FactorGroup(this, els);
//    }
}
