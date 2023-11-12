package ua.ihromant.mathutils.group;

import java.util.stream.IntStream;

public interface Group {
    int op(int a, int b);

    int inv(int a);

    int order();

    default int order(int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = op(a, pow);
        } while (pow != 0);
        return counter;
    }

    String name();

    String elementName(int a);

    default int power(int a, int pow) {
        int result = 0;
        for (int i = 0; i < pow; i++) {
            result = op(a, result);
        }
        return result;
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default Group asTable() {
        return new TableGroup(
                elements().mapToObj(i -> elements().map(j -> op(i, j)).toArray()).toArray(int[][]::new),
                elements().map(this::inv).toArray());
    }
}
