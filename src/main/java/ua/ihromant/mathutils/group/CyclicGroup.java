package ua.ihromant.mathutils.group;

public record CyclicGroup(int order) implements Group {
    @Override
    public int op(int a, int b) {
        return (a + b) % order;
    }

    @Override
    public int inv(int a) {
        return a == 0 ? 0 : order - a;
    }
}
