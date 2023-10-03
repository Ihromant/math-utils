package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SemiFieldGen {
    private static final int GEN_SIZE = 3;
    public static final int SIZE = GEN_SIZE * GEN_SIZE * GEN_SIZE;
    public static final int ZR = toInt(new int[] {0, 0, 0});
    public static final int ONE = toInt(new int[] {1, 0, 0});

    public static String toString(int numb) {
        int r = (numb / GEN_SIZE / GEN_SIZE) - 1;
        int i = (numb / GEN_SIZE % GEN_SIZE) - 1;
        int j = (numb % GEN_SIZE) - 1;
        StringBuilder result = new StringBuilder();
        if (r != 0) {
            if (r < 0) {
                result.append('-');
            }
            result.append('1');
        }
        if (i != 0) {
            if (i < 0) {
                result.append('-');
            } else {
                if (!result.isEmpty()) {
                    result.append('+');
                }
            }
            result.append('i');
        }
        if (j != 0) {
            if (j < 0) {
                result.append('-');
            } else {
                if (!result.isEmpty()) {
                    result.append('+');
                }
            }
            result.append('j');
        }
        if (result.isEmpty()) {
            return "0";
        } else {
            return result.toString();
        }
    }

    private static int[] toBase(int numb) {
        return new int[]{(numb / GEN_SIZE / GEN_SIZE) - 1, (numb / GEN_SIZE % GEN_SIZE) - 1, (numb % GEN_SIZE) - 1};
    }

    private static int[] add(int[] f, int[] s) {
        int[] result = new int[GEN_SIZE];
        for (int i = 0; i < GEN_SIZE; i++) {
            int sum = f[i] + s[i];
            if (sum == 2) {
                sum = -1;
            }
            if (sum == -2) {
                sum = 1;
            }
            result[i] = sum;
        }
        return result;
    }

    private static int[] neg(int[] n) {
        int[] result = new int[n.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = -n[i];
        }
        return result;
    }

    private static final int[] ZERO = {0, 0, 0};
    private static final int[] I_MUL_J = {1, 1, 0};
    private static final int[] J_MUL_J = {-1, 1, 1};

    private static int[] mul(int[] f, int[] s) {
        int[][][] unreduced = new int[GEN_SIZE][GEN_SIZE][];
        int[][] inter = new int[GEN_SIZE][GEN_SIZE];
        for (int i = 0; i < f.length; i++) {
            for (int j = 0; j < s.length; j++) {
                inter[i][j] = f[i] * s[j];
            }
        }
        unreduced[0][0] = new int[]{inter[0][0], 0, 0};
        unreduced[1][1] = new int[]{0, 0, f[1] * s[1]}; // {0, 0, +-1}
        unreduced[2][2] = inter[2][2] == 0 ? ZERO.clone() : inter[2][2] == 1 ? J_MUL_J.clone() : neg(J_MUL_J);
        unreduced[0][1] = new int[]{0, inter[0][1], 0};
        unreduced[0][2] = new int[]{0, 0, inter[0][2]};
        unreduced[1][0] = new int[]{0, inter[1][0], 0};
        unreduced[2][0] = new int[]{0, 0, inter[2][0]};
        unreduced[1][2] = inter[1][2] == 0 ? ZERO.clone() : inter[1][2] == 1 ? I_MUL_J.clone() : neg(I_MUL_J);
        unreduced[2][1] = inter[2][1] == 0 ? ZERO.clone() : inter[2][1] == 1 ? I_MUL_J.clone() : neg(I_MUL_J);
        int[] result = new int[GEN_SIZE];
        for (int i = 0; i < GEN_SIZE; i++) {
            for (int j = 0; j < GEN_SIZE; j++) {
                result = add(result, unreduced[i][j]);
            }
        }
        return result;
    }

    private static int toInt(int[] n) {
        return ((n[0] + 1) * GEN_SIZE + n[1] + 1) * GEN_SIZE + n[2] + 1;
    }

    private static void generateAdditionTable() {
        IntStream.range(0, SIZE).forEach(i -> {
            int[] f = toBase(i);
            System.out.println(IntStream.range(0, SIZE).mapToObj(j -> {
                int[] s = toBase(j);
                int[] sum = add(f, s);
                return String.format("%2d", toInt(sum));
            }).collect(Collectors.joining(", ", "            {", "},")));
        });
    }

    private static void generateNegationTable() {
        System.out.println(IntStream.range(0, SIZE).mapToObj(i -> String.format("%2d", toInt(neg(toBase(i)))))
                .collect(Collectors.joining(", ", "{", "}")));
    }

    private static void generateMultiplicationTable() {
        IntStream.range(0, SIZE).forEach(i -> {
            int[] f = toBase(i);
            System.out.println(IntStream.range(0, SIZE).mapToObj(j -> {
                int[] s = toBase(j);
                int[] sum = mul(f, s);
                return String.format("%2d", toInt(sum));
            }).collect(Collectors.joining(", ", "            {", "},")));
        });
    }

    private static void generateInverseTable() {
        System.out.println(ONE);
        System.out.println(IntStream.range(0, SIZE).mapToObj(i -> {
            int[] n = toBase(i);
            int r = Arrays.equals(n, ZERO) ? ZR : IntStream.range(0, SIZE).filter(j -> {
                    int[] inv = toBase(j);
                    return Arrays.equals(toBase(ONE), mul(n, inv));
                }).findAny().orElseThrow();
            return String.format("%2d", r);
        }).collect(Collectors.joining(", ", "{", "}")));
    }

    public static void main(String[] args) {
        generateInverseTable();
    }
}
