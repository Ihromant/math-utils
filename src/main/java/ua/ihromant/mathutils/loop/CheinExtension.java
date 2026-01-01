package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CheinExtension implements Loop {
    private final Group g;

    public CheinExtension(Group g) {
        this.g = g;
    }

    @Override
    public int op(int a, int b) {
        int i = a % 2;
        int j = b % 2;
        int ij = (i + j) % 2;
        int x = a / 2;
        int y = b / 2;
        return pInv(g.op(pInv(x, j), pInv(y, ij)), j) * 2 + ij;
    }

    private int pInv(int t, int inv) {
        return inv == 0 ? t : g.inv(t);
    }

    @Override
    public int inv(int a) {
        int r = a % 2;
        if (r == 0) {
            return g.inv(a / 2) * 2;
        } else {
            return a;
        }
    }

    @Override
    public int order() {
        return g.order() * 2;
    }

    @Override
    public String name() {
        return "M(" + g.name() + ",2)";
    }

    @Override
    public String elementName(int a) {
        return "(" + g.elementName(a / 2) + "," + (a % 2) + ")";
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
}
