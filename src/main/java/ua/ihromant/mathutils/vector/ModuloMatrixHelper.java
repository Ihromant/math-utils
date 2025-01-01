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
}
