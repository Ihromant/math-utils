package ua.ihromant.mathutils.group;

import java.util.stream.IntStream;

public class SemiDirectProduct implements Group {
    private final Group h;
    private final CyclicGroup k;
    private final PermutationGroup gr;
    private final int[] psi;
    private final Integer elem;

    public SemiDirectProduct(Group h, CyclicGroup k) {
        this.h = h;
        this.k = k;
        this.gr = new PermutationGroup(h.auth());
        this.psi = new int[k.order()];
        int elem = IntStream.range(1, gr.order()).filter(e -> k.order() == gr.order(e)).findAny()
                .orElseGet(() -> IntStream.range(1, gr.order()).filter(e -> k.order() % gr.order(e) == 0).findAny().orElseThrow());
        for (int i = 1; i < k.order(); i++) {
            psi[i] = gr.mul(elem, i);
        }
        this.elem = null;
    }

    public SemiDirectProduct(Group h, CyclicGroup k, int mul) {
        if (k.order() % mul != 0) {
            throw new IllegalArgumentException();
        }
        this.h = h;
        this.k = k;
        this.gr = new PermutationGroup(h.auth());
        this.psi = new int[k.order()];
        psi[0] = 0;
        this.elem = IntStream.range(1, gr.order()).filter(e -> k.order() / mul == gr.order(e)).findAny().orElseThrow();
        for (int i = 1; i < k.order(); i++) {
            psi[i] = gr.mul(elem, i);
        }
    }

    public SemiDirectProduct(Group h, CyclicGroup k, int idx, boolean b) {
        this.h = h;
        this.k = k;
        this.gr = new PermutationGroup(h.auth());
        this.psi = new int[k.order()];
        psi[0] = 0;
        int[] elems = IntStream.range(1, gr.order()).filter(e -> k.order() == gr.order(e)).toArray();
        this.elem = elems[idx];
        for (int i = 1; i < k.order(); i++) {
            psi[i] = gr.mul(elem, i);
        }
    }

    public int from(int hp, int kp) {
        return hp * k.order() + kp;
    }

    @Override
    public int op(int a, int b) {
        int h1 = a / k.order();
        int k1 = a % k.order();
        int h2 = b / k.order();
        int k2 = b % k.order();
        return from(h.op(h1, gr.permutation(psi[k1])[h2]), k.op(k1, k2));
    }

    @Override
    public int inv(int a) {
        int alpha = a / k.order();
        int beta = a % k.order();
        int ia = h.inv(alpha);
        int ib = k.inv(beta);
        return from(gr.permutation(psi[ib])[ia], ib);
    }

    @Override
    public int order() {
        return h.order() * k.order();
    }

    @Override
    public String name() {
        return h.name() + "⋊" + k.name() + (elem == null ? "" : "ord" + gr.order(elem));
    }

    @Override
    public String elementName(int a) {
        String alpha = h.elementName(a / k.order());
        int beta = a % k.order();
        return "(" + alpha + ", " + beta + ")";
    }

    public int[] permutation() {
        return gr.permutation(psi[1]);
    }
}
