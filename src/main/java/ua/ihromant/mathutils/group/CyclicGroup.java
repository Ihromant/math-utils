package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;

import java.util.stream.IntStream;

public record CyclicGroup(int order) implements Group {
    @Override
    public int op(int a, int b) {
        return (a + b) % order;
    }

    @Override
    public int inv(int a) {
        return a == 0 ? 0 : order - a;
    }

    @Override
    public String name() {
        return "Z" + order;
    }

    @Override
    public String elementName(int a) {
        return String.valueOf(a);
    }

    @Override
    public int[][] auth() {
        return IntStream.range(1, order).filter(i -> Combinatorics.gcd(i, order) == 1)
                .mapToObj(i -> IntStream.range(0, order).map(j -> mul(i, j)).toArray()).toArray(int[][]::new);
    }
}
