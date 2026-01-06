package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.group.TableGroup;

import java.util.stream.IntStream;

public class RajahLoop implements Loop {
    private final int p;
    private final GaloisField q;
    private final int mu;
    private final int phi;
    private final CyclicProduct cp;
    private TableGroup tg;

    public RajahLoop(int p, int q, int mu, int phi) {
        this.p = p;
        this.q = new GaloisField(q);
        this.mu = mu;
        this.phi = phi;
        this.cp = new CyclicProduct(p, q, q, q);
    }

    private TableGroup tg() {
        if (this.tg == null) {
            int[][] table = new int[cp.order()][cp.order()];
            IntStream.range(0, cp.order()).parallel().forEach(a -> {
                for (int b = 0; b < cp.order(); b++) {
                    table[a][b] = opByDef(a, b);
                }
            });
            this.tg = new TableGroup(table);
        }
        return this.tg;
    }

    @Override
    public int op(int a, int b) {
        return tg().op(a, b);
    }

    public int opByDef(int a, int b) {
        int[] fst = cp.toArr(a);
        int[] snd = cp.toArr(b);
        RajahElement elem = new RajahElement(fst[0], fst[1], fst[2], fst[3]).op(
                new RajahElement(snd[0], snd[1], snd[2], snd[3]), p, q, mu, phi);
        return cp.fromArr(elem.delta(),elem.alpha(), elem.beta(), elem.gamma());
    }

    @Override
    public int inv(int a) {
        return tg().inv(a);
    }

    @Override
    public int order() {
        return cp.order();
    }

    @Override
    public String name() {
        return tg().name();
    }

    @Override
    public String elementName(int a) {
        return tg.elementName(a);
    }
}
