package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.LinearSpace;

import java.util.ArrayList;
import java.util.Arrays;
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
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(init);
        find(init, new int[0], result::add);
        return result;
    }

    private void find(FixBS currLoop, int[] gens, Consumer<FixBS> cons) {
        int l = gens.length;
        int last = l == 0 ? 0 : gens[l - 1];
        ex: for (int gen = currLoop.nextClearBit(last); gen >= 0 && gen < order(); gen = currLoop.nextClearBit(gen + 1)) {
            int[] nextGens = Arrays.copyOf(gens, l + 1);
            nextGens[l] = gen;
            FixBS nextLoop = currLoop.copy();
            boolean added;
            do {
                added = false;
                for (int a : nextLoop.toArray()) {
                    for (int b : nextGens) {
                        int ab = op(a, b);
                        if (!currLoop.get(ab) && ab < gen) {
                            continue ex;
                        }
                        if (!nextLoop.get(ab)) {
                            added = true;
                            nextLoop.set(ab);
                        }
                        int ba = op(b, a);
                        if (!currLoop.get(ba) && ba < gen) {
                            continue ex;
                        }
                        if (!nextLoop.get(ba)) {
                            added = true;
                            nextLoop.set(ba);
                        }
                    }
                }
            } while (added);
            cons.accept(nextLoop);
            find(nextLoop, nextGens, cons);
        }
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
