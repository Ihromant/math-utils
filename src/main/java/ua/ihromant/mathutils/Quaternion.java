package ua.ihromant.mathutils;

public record Quaternion(Rational r, Rational i, Rational j, Rational k) {
    public Quaternion(long v) {
        this(Rational.of(v), Rational.ZERO, Rational.ZERO, Rational.ZERO);
    }

    public Quaternion neg() {
        return new Quaternion(r.neg(), i.neg(), j.neg(), k.neg());
    }

    public Quaternion add(Quaternion that) {
        return new Quaternion(r.add(that.r), i.add(that.i), j.add(that.j), k.add(that.k));
    }

    public Quaternion sub(Quaternion that) {
        return new Quaternion(r.sub(that.r), i.sub(that.i), j.sub(that.j), k.sub(that.k));
    }

    public Quaternion mul(Quaternion that) {
        return new Quaternion(r.mul(that.r).sub(i.mul(that.i)).sub(j.mul(that.j)).sub(k.mul(that.k)),
                r.mul(that.i).add(i.mul(that.r)).add(j.mul(that.k)).sub(k.mul(that.j)),
                r.mul(that.j).add(j.mul(that.r)).add(k.mul(that.i)).sub(i.mul(that.k)),
                r.mul(that.k).add(k.mul(that.r)).add(i.mul(that.j)).sub(j.mul(that.i)));
    }

    public Quaternion inv() {
        Rational denom = r.mul(r).add(i.mul(i)).add(j.mul(j)).add(k.mul(k));
        return new Quaternion(r.div(denom), i.neg().div(denom), j.neg().div(denom), k.neg().div(denom));
    }

    @Override
    public String toString() {
        return toStr(r, "") + toStr(i, "i") + toStr(j, "j") + toStr(k, "k");
    }

    private String toStr(Rational r, String suffix) {
        if (r.numer() == 0) {
            return "";
        } else if (r.numer() > 0) {
            return "+" + r + suffix;
        } else {
            return r + suffix;
        }
    }
}
