package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.group.Loop;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CheinExtension implements Loop {
    private final Loop g;

    public CheinExtension(Loop g) {
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
}
