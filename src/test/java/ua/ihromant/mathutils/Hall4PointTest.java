package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Hall4PointTest {
    private final GaloisField fd = new GaloisField(4);
    private final int cube = fd.cardinality() * fd.cardinality() * fd.cardinality();
    @Test
    public void testHall() {
        BitSet[] lines = Stream.of(
                IntStream.range(0, cube).mapToObj(i -> {
                    BitSet result = new BitSet();
                    for (int j = 0; j < fd.cardinality(); j++) {
                        result.set(i * 4 + j);
                    }
                    return result;
                }),
                IntStream.range(0, cube).boxed().flatMap(x -> IntStream.range(0, cube).filter(v -> Arrays.stream(toArray(v)).anyMatch(i -> i > 1)).boxed()
                        .flatMap(v -> IntStream.range(0, fd.cardinality()).boxed().flatMap(a -> IntStream.range(0, fd.cardinality()).mapToObj(b -> {
                            BitSet result = new BitSet();
                            int[] xArr = toArray(x);
                            int[] vArr = toArray(v);
                            for (int t = 0; t < fd.cardinality(); t++) {
                                int sum = fromArray(linear(xArr, t, vArr));
                                result.set(sum * fd.cardinality() + fd.add(a, fd.mul(t, b)));
                            }
                            return result;
                        })))),
                IntStream.range(0, cube).boxed().flatMap(x -> IntStream.range(1, 8).map(v -> fromArray(new int[]{v / 4, v / 2 % 2, v % 2})).boxed()
                        .flatMap(v -> IntStream.range(0, fd.cardinality()).boxed().flatMap(a -> IntStream.range(0, fd.cardinality()).mapToObj(b -> {
                            BitSet result = new BitSet();
                            int[] xArr = toArray(x);
                            int[] vArr = toArray(v);
                            for (int t = 0; t < fd.cardinality(); t++) {
                                int sum = fromArray(linear(xArr, t, vArr));
                                result.set(sum * fd.cardinality() + fd.add(a, fd.mul(conjugate(t), b)));
                            }
                            return result;
                        }))))
        ).flatMap(Function.identity()).toArray(BitSet[]::new);
        HyperbolicPlane pl = new HyperbolicPlane(lines);
        assertEquals(cube * fd.cardinality(), pl.pointCount());
        assertEquals(cube * 1009, pl.lineCount());
        HyperbolicPlaneTest.testCorrectness(pl, of(4));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private int[] toArray(int idx) {
        return new int[]{idx / fd.cardinality() / fd.cardinality(), idx / fd.cardinality() % fd.cardinality(), idx % fd.cardinality()};
    }

    private int fromArray(int[] arr) {
        return (arr[0] * fd.cardinality() + arr[1]) * fd.cardinality() + arr[2];
    }

    private int[] linear(int[] arr1, int t, int[] arr2) {
        int[] result = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            result[i] = fd.add(arr1[i], fd.mul(t, arr2[i]));
        }
        return result;
    }

    private int conjugate(int par) {
        if (par < 2) {
            return par;
        }
        if (par == 2) {
            return 3;
        }
        if (par == 3) {
            return 2;
        }
        throw new IllegalArgumentException();
    }
}
