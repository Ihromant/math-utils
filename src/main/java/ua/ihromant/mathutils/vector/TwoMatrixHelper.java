package ua.ihromant.mathutils.vector;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TwoMatrixHelper implements ModuloMatrixHelper {
    private final int n;
    private final int unity;
    private final int matCount;
    private final int mask;
    private final int[] gl;
    private final int[] mapGl;
    private final int[] v;

    public TwoMatrixHelper(int n) {
        this.n = n;
        this.unity = calcUnity();
        this.matCount = 1 << (this.n * this.n);
        this.mask = (1 << n) - 1;
        this.mapGl = generateMapGl();
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        System.out.println(gl.length);
        this.v = Arrays.stream(gl).filter(a -> mapGl[sub(a, unity)] > 0).toArray();
        System.out.println(v.length);
    }

    public TwoMatrixHelper(int n, int[] mapGl) {
        this.n = n;
        this.unity = calcUnity();
        this.matCount = 1 << (this.n * this.n);
        this.mask = (1 << n) - 1;
        this.mapGl = mapGl;
        this.gl = IntStream.range(0, matCount).filter(i -> mapGl[i] > 0).toArray();
        this.v = Arrays.stream(gl).filter(a -> mapGl[sub(a, unity)] > 0).toArray();
        System.out.println(v.length);
    }

    @Override
    public int p() {
        return 2;
    }

    @Override
    public int n() {
        return n;
    }

    @Override
    public int unity() {
        return unity;
    }

    @Override
    public int matCount() {
        return matCount;
    }

    @Override
    public int add(int i, int j) {
        return i ^ j;
    }

    @Override
    public int sub(int i, int j) {
        return i ^ j;
    }

    @Override
    public int mul(int a, int b) {
        return switch (n) {
            case 1 -> mulMagic1(a, b);
            case 2 -> mulMagic2(a, b);
            case 3 -> mulMagic3(a, b);
            case 4 -> mulMagic4(a, b);
            case 5 -> mulMagic5(a, b);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public int mulVec(int a, int vec) {
        int res = 0;
        for (int i = 0; i < n; i++) {
            int part = (a >>> i * n) & mask;
            res = res | ((Integer.bitCount(part & vec) & 1) << i);
        }
        return res;
    }

    @Override
    public int mulCff(int a, int cff) {
        return cff % 2 == 0 ? 0 : a;
    }

    @Override
    public int inv(int i) {
        return mapGl[i];
    }

    @Override
    public boolean hasInv(int i) {
        return mapGl[i] > 0;
    }

    @Override
    public int[] gl() {
        return gl;
    }

    @Override
    public int[] v() {
        return v;
    }

    private int mulMagic1(int a, int b) {
        return a & b;
    }

    private int mulMagic2(int a, int b) {
        int r = 0;
        for (int i = 0; i < 2; i++) {
            int x = (b >>> i) & 0x5;
            x = x ^ (x >>> 1);
            x = x & 0x3;
            x = x * 0x5;
            x = x & a;
            x = x ^ (x >>> 1);
            r = r | ((x & 0x5) << i);
        }
        return r;
    }

    private int mulMagic3(int a, int b) {
        int r = 0;
        for (int i = 0; i < 3; i++) {
            int x = (b >>> i) & 0x49;
            x = x ^ (x >>> 2) ^ (x >>> 4);
            x = x & 0x7;
            x = x * 0x49;
            x = x & a;
            x = x ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x49) << i);
        }
        return r;
    }

    private int mulMagic4(int a, int b) {
        int r = 0;
        for (int i = 0; i < 4; i++) {
            int x = (b >>> i) & 0x1111;
            x = x ^ (x >>> 3) ^ (x >>> 6) ^ (x >>> 9);
            x = x & 0xF;
            x = x * 0x1111;
            x = x & a;
            x = x ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x1111) << i);
        }
        return r;
    }

    private int mulMagic5(int a, int b) {
        int r = 0;
        for (int i = 0; i < 5; ++i) {
            int x = (b >>> i) & 0x108421;
            x = x ^ (x >>> 4) ^ (x >>> 8) ^ (x >>> 12) ^ (x >>> 16);
            x = x & 0x1F;
            x = x * 0x108421;
            x = x & a;
            x = x ^ (x >>> 4) ^ (x >>> 3) ^ (x >>> 2) ^ (x >>> 1);
            r = r | ((x & 0x108421) << i);
        }
        return r;
    }

    private int[][] toMatrix(int a) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % 2;
            a = a / 2;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix) {
        int result = 0;
        for (int i = n * n - 1; i >= 0; i--) {
            result = result * 2 + matrix[i / n][i % n];
        }
        return result;
    }

    private int calcUnity() {
        int res = 0;
        for (int i = 0; i < n; i++) {
            res = (res << (n + 1)) + 1;
        }
        return res;
    }

    private int[] generateMapGl() {
        int[] result = new int[matCount];
        for (int i = 0; i < matCount; i++) {
            if (result[i] > 0) {
                continue;
            }
            try {
                int[][] matrix = toMatrix(i);
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, 2);
                int inv = fromMatrix(rev);
                result[i] = inv;
                result[inv] = i;
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }
}
