package ua.ihromant.mathutils.vector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public interface ModuloMatrixHelper {
    int p();

    int n();

    int unity();

    int matCount();

    int add(int i, int j);

    int sub(int i, int j);

    int mul(int i, int j);

    int mulVec(int a, int vec);

    int mulCff(int a, int cff);

    int inv(int i);

    boolean hasInv(int i);

    int[] gl();

    int[] v();

    default int fromVec(int[] vec) {
        int result = 0;
        for (int i = 0; i < n(); i++) {
            result = result * p() + vec[i];
        }
        return result;
    }

    default int[] toVec(int x) {
        int[] result = new int[n()];
        for (int i = n() - 1; i >= 0; i--) {
            result[i] = x % p();
            x = x / p();
        }
        return result;
    }

    default int[][] toMatrix(int a) {
        int n = n();
        int p = p();
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % p;
            a = a / p;
        }
        return result;
    }

    default int fromMatrix(int[][] matrix) {
        int n = n();
        int p = p();
        int result = 0;
        for (int i = n() * n() - 1; i >= 0; i--) {
            result = result * p + matrix[i / n][i % n];
        }
        return result;
    }

    static ModuloMatrixHelper of(int p, int n) {
        if (p == 2) {
            return new TwoMatrixHelper(n);
        }
        if (p == 3 && n > 3 || p == 2 && n > 3) {
            try {
                return readGl(p, n);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (p > 3 && n > 2 || p > 13 && n > 1) {
            return new CommonMatrixHelper(p, n);
        }
        return new TableMatrixHelper(p, n);
    }

    private static ModuloMatrixHelper readGl(int p, int n) throws IOException {
        int matCount = LinearSpace.pow(p, n * n);
        int[] mapGl = new int[matCount];
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/gl-" + p + "^" + n + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            br.lines().forEach(ln -> {
                String[] sp = ln.split("=");
                mapGl[Integer.parseInt(sp[0])] = Integer.parseInt(sp[1]);
            });
        }
        return p == 2 ? new TwoMatrixHelper(n, mapGl) : new CommonMatrixHelper(p, n, mapGl);
    }

    default BlockMatrix mul(BlockMatrix x, BlockMatrix y) {
        return new BlockMatrix(
                add(mul(x.a(), y.a()), mul(x.b(), y.c())),
                add(mul(x.a(), y.b()), mul(x.b(), y.d())),
                add(mul(x.c(), y.a()), mul(x.d(), y.c())),
                add(mul(x.c(), y.b()), mul(x.d(), y.d())));
    }

    default int apply(BlockMatrix bm, int m) {
        if (m == matCount()) {
            return bm.b() == 0 ? matCount() : mul(bm.d(), inv(bm.b()));
        }
        int den = add(bm.a(), mul(bm.b(), m));
        if (den == 0) {
            return matCount();
        }
        int nom = add(bm.c(), mul(bm.d(), m));
        return mul(nom, inv(den));
    }

    default BlockMatrix permutation(int a, int b, int c) {
        if (c == matCount()) {
            int ab = sub(b, a);
            return new BlockMatrix(unity(), 0, a, ab);
        }
        int ac = inv(sub(a, c));
        if (b == matCount()) {
            return new BlockMatrix(unity(), 0, a, ac);
        }
        int bc = inv(sub(b, c));
        int ab = sub(a, b);
        int rt = mul(ac, mul(ab, mul(bc, ac)));
        return new BlockMatrix(unity(), rt, a, mul(c, rt));
    }
}
