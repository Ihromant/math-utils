package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;

public record ZornMatrix(int a, TDVector v, TDVector u, int b) {
    public static final int sz = 8;
    public ZornMatrix add(ZornMatrix other) {
        GaloisField fd = v.fd();
        return new ZornMatrix(fd.add(a, other.a), v.add(other.v()), u.add(other.u()), fd.add(b, other.b));
    }

    public ZornMatrix mul(ZornMatrix other) {
        GaloisField fd = v.fd();
        return new ZornMatrix(fd.add(fd.mul(a, other.a), v.scalar(other.u)),
                (other.v.mul(a)).add(v.mul(other.b)).sub(u.cross(other.u)),
                (other.u).mul(b).add(u.mul(other.a)).add(v.cross(other.v)),
                fd.add(fd.mul(b, other.b), u.scalar(other.v)));
    }

    public int qNorm() {
        GaloisField fd = v.fd();
        return fd.sub(fd.mul(a, b), v.scalar(u));
    }

    public int toInt() {
        return fromArr(v.fd().cardinality(), a, v.f(), v.s(), v.t(), u.f(), u.s(), u.t(), b);
    }

    public static ZornMatrix fromInt(GaloisField fd, int val) {
        int[] arr = toArr(fd.cardinality(), val);
        return new ZornMatrix(arr[0], new TDVector(fd, arr[1], arr[2], arr[3]), new TDVector(fd, arr[4], arr[5], arr[6]), arr[7]);
    }

    public static int fromArr(int crd, int... arr) {
        int result = 0;
        for (int i = 0; i < sz; i++) {
            result = result * crd + arr[i];
        }
        return result;
    }

    private static int[] toArr(int crd, int x) {
        int[] result = new int[sz];
        for (int i = sz - 1; i >= 0; i--) {
            result[i] = x % crd;
            x = x / crd;
        }
        return result;
    }
}
