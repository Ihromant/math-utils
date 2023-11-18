package ua.ihromant.mathutils.group;

import java.util.stream.IntStream;

public class SemiDirectProduct implements Group {
    private final CyclicGroup left;
    private final CyclicGroup right;
    private final int i;

    public SemiDirectProduct(CyclicGroup left, CyclicGroup right) {
        this.left = left;
        this.right = right;
        int[] orders = left.elements().map(left::expOrder).toArray();
        this.i = IntStream.range(0, left.order()).filter(i -> orders[i] == right.order()).findAny()
                .orElseThrow(() -> new IllegalArgumentException(left + " " + right));
    }

    public SemiDirectProduct(CyclicGroup left, CyclicGroup right, int i) {
        this.left = left;
        this.right = right;
        int ord = left.expOrder(i);
        if (ord != right.order()) {
            throw new IllegalArgumentException(i + " " + left + " " + right);
        }
        this.i = i;
    }

    private int fromAlphaBeta(int alpha, int beta) {
        return beta * left.order() + alpha;
    }

    @Override
    public int op(int a, int b) {
        int alpha1 = a % left.order();
        int beta1 = a / left.order();
        int alpha2 = b % left.order();
        int beta2 = b / left.order();
        return fromAlphaBeta(left.op(alpha1, left.mul(left.exponent(i, beta1), alpha2)), right.op(beta1, beta2));
    }

    @Override
    public int inv(int a) {
        int alpha = a % left.order();
        int beta = a / left.order();
        int ia = left.inv(alpha);
        int ib = right.inv(beta);
        return fromAlphaBeta(left.mul(left.exponent(i, ib), ia), ib);
    }

    @Override
    public int order() {
        return left.order() * right.order();
    }

    @Override
    public String name() {
        return left.name() + "⋊" + right.name();
    }

    @Override
    public String elementName(int a) {
        int alpha = a % left.order();
        int beta = a / left.order();
        return "a" + alpha + "b" + beta;
    }
}
