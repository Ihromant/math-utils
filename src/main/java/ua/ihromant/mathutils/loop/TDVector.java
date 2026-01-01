package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;

public record TDVector(GaloisField fd, int f, int s, int t) {
    public TDVector add(TDVector other) {
        return new TDVector(fd, fd.add(f, other.f), fd.add(s, other.s), fd.add(t, other.t));
    }

    public TDVector sub(TDVector other) {
        return new TDVector(fd, fd.sub(f, other.f), fd.sub(s, other.s), fd.sub(t, other.t));
    }

    public TDVector cross(TDVector other) {
        return new TDVector(fd, fd.sub(fd.mul(s, other.t), fd.mul(t, other.s)),
                fd.sub(fd.mul(t, other.f), fd.mul(f, other.t)),
                fd.sub(fd.mul(f, other.s), fd.mul(s, other.f)));
    }

    public int scalar(TDVector other) {
        return fd.add(fd.mul(f, other.f), fd.mul(s, other.s), fd.mul(t, other.t));
    }

    public TDVector mul(int c) {
        return new TDVector(fd, fd.mul(c, f), fd.mul(c, s), fd.mul(c, t));
    }
}
