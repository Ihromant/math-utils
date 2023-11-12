package ua.ihromant.mathutils.group;

import java.util.stream.IntStream;

public interface Group {
    int op(int a, int b);

    int inv(int a);

    int order();

    String name();

    String elementName(int a);

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default Group asTable() {
        return new TableGroup(
                elements().mapToObj(i -> elements().map(j -> op(i, j)).toArray()).toArray(int[][]::new),
                elements().map(this::inv).toArray());
    }
}
