package ua.ihromant.mathutils.vector;

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

    int[] vIdxes();

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
        if (p == 2 && n > 4) {
            return new TwoMatrixHelper(n);
        }
        return new CommonMatrixHelper(p, n);
    }
}
