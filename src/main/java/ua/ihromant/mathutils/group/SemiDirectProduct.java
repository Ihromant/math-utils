package ua.ihromant.mathutils.group;

import java.util.Arrays;
import java.util.stream.IntStream;

public class SemiDirectProduct implements Group {
    private final Group h;
    private final CyclicGroup k;
    private final PermutationGroup gr;
    private final int[] psi;

    public SemiDirectProduct(Group h, CyclicGroup k) {
        this.h = h;
        this.k = k;
        int[][] auth = h.auth();
        Arrays.sort(auth, SemiDirectProduct::compare);
        this.gr = new PermutationGroup(auth);
        this.psi = new int[k.order()];
        psi[0] = 0;
        int elem = IntStream.range(1, gr.order()).filter(e -> k.order() == gr.order(e)).findAny()
                .orElseGet(() -> IntStream.range(1, gr.order()).filter(e -> k.order() % gr.order(e) == 0).findAny().orElseThrow());
        for (int i = 1; i < k.order(); i++) {
            psi[i] = gr.mul(elem, i);
        }
    }

    private static int compare(int[] fst, int[] snd) {
        for (int i = 1; i < fst.length; i++) {
            int dff = fst[i] - snd[i];
            if (dff != 0) {
                return dff;
            }
        }
        return 0;
    }

    public int fromAB(int alpha, int beta) {
        return beta * h.order() + alpha;
    }

    @Override
    public int op(int a, int b) {
        int alpha1 = a % h.order();
        int beta1 = a / h.order();
        int alpha2 = b % h.order();
        int beta2 = b / h.order();
        return fromAB(h.op(alpha1, gr.permutation(psi[beta1])[alpha2]), k.op(beta1, beta2));
    }

    @Override
    public int inv(int a) {
        int alpha = a % h.order();
        int beta = a / h.order();
        int ia = h.inv(alpha);
        int ib = k.inv(beta);
        return fromAB(gr.permutation(psi[ib])[ia], ib);
    }

    @Override
    public int order() {
        return h.order() * k.order();
    }

    @Override
    public String name() {
        return h.name() + "⋊" + k.name();
    }

    @Override
    public String elementName(int a) {
        int alpha = a % h.order();
        int beta = a / h.order();
        return "a" + alpha + "b" + beta;
    }

    @Override
    public int[][] auth() {
        throw new UnsupportedOperationException();
    }
}
