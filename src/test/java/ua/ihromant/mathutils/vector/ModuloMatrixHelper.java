package ua.ihromant.mathutils.vector;

public interface ModuloMatrixHelper {
    int unity();

    int matCount();

    int sub(int i, int j);

    int mul(int i, int j);

    int mulVec(int a, int vec);

    int mulCff(int a, int cff);

    int inv(int i);

    boolean hasInv(int i);

    int[] gl();

    int[] v();

    static ModuloMatrixHelper of(int p, int n) {
        if (p == 2) {
            return new TwoMatrixHelper(n);
        }
        return new CommonMatrixHelper(p, n);
    }
}
