package ua.ihromant.mathutils.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        int alpha = a / k.order();
        int beta = a % k.order();
        return "(" + alpha + ", " + beta + ")";
    }

    @Override
    public int[][] auth() { // this is subgroup of automorphism group that fixes H as set
        List<int[]> result = new ArrayList<>();
        int[][] alphas = h.auth();
        int[][] deltas = k.auth();
        int hOrd = h.order();
        int kOrd = k.order();
        for (int oneMap = 0; oneMap < hOrd; oneMap++) {
            ex: for (int[] delta : deltas) {
                int[] beta = new int[kOrd];
                beta[1] = from(oneMap, 0);
                for (int i = 2; i < kOrd; i++) {
                    int conj = conjugate(beta[1], delta[i - 1]);
                    beta[i] = op(beta[i - 1], conj);
                }
                beta[0] = op(beta[kOrd - 1], conjugate(beta[1], delta[kOrd - 1]));
                for (int k1 = 0; k1 < kOrd; k1++) {
                    for (int k2 = 0; k2 < kOrd; k2++) {
                        int conj = conjugate(beta[k2], delta[k1]);
                        if (beta[k.op(k1, k2)] != op(beta[k1], conj)) {
                            continue ex;
                        }
                    }
                }
                ex1: for (int[] alpha : alphas) {
                    for (int h1 = 0; h1 < hOrd; h1++) {
                        for (int k1 = 0; k1 < kOrd; k1++) {
                            int ahk = alpha[conjugate(from(h1, 0), from(0, k1)) / kOrd];
                            int ahbd = conjugate(from(alpha[h1], 0), op(beta[k1], delta[k1])) / kOrd;
                            if (ahk != ahbd) {
                                continue ex1;
                            }
                        }
                    }
                    int[] auth = new int[order()];
                    for (int h1 = 0; h1 < hOrd; h1++) {
                        for (int k1 = 0; k1 < kOrd; k1++) {
                            int el = from(h1, k1);
                            auth[el] = op(op(from(alpha[h1], 0), beta[k1]), from(0, delta[k1]));
                        }
                    }
                    result.add(auth);
                }
            }
        }
        int[][] res = result.toArray(int[][]::new);
        Arrays.parallelSort(res, Group::compareArr);
        return res;
    }
}
