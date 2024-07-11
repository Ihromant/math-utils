package ua.ihromant.mathutils.nauty;

public record Squared(GraphWrapper base) implements GraphWrapper {
    @Override
    public int size() {
        return base.size() * base.size();
    }

    @Override
    public int color(int idx) {
        int f = idx / size();
        int s = idx % size();
        if (f == s) {
            return base.color(f);
        }
        return edge(f, s) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    @Override
    public boolean edge(int a, int b) {
        int a2 = a % base.size();
        int b1 = b / base.size();
        if (a2 != b1) {
            return false;
        }
        int a1 = a / base.size();
        int b2 = b % base.size();
        boolean aConn = a1 == a2 || base.edge(a1, a2);
        boolean bConn = b1 == b2 || base.edge(b1, b2);
        return aConn && bConn;
    }
}
