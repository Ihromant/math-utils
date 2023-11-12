package ua.ihromant.mathutils.group;

public record DihedralGroup(int order) implements Group {
    private int addByDef(int v1, int v2) {
        int alpha1 = v1 % order;
        int beta1 = v1 / order;
        int alpha2 = v2 % order;
        int beta2 = v2 / order;
        if (beta2 == 0) {
            return fromAlphaBeta(alpha1 + alpha2, beta1);
        } else {
            return fromAlphaBeta((order - alpha1 + alpha2) % order, (beta1 + beta2) % 2);
        }
    }

    private int fromAlphaBeta(int alpha, int beta) {
        return (beta % order) * order + (alpha % order);
    }

    @Override
    public int op(int a, int b) {
        return addByDef(a, b);
    }

    @Override
    public int inv(int a) {
        int alpha = a % order;
        int beta = a / order;
        return fromAlphaBeta(alpha == 0 || beta != 0 ? alpha : order - alpha, beta);
    }

    @Override
    public int order() {
        return 2 * order;
    }

    @Override
    public String name() {
        return "D" + order;
    }

    @Override
    public String elementName(int a) {
        return String.valueOf(a); // TODO remake
    }
}
