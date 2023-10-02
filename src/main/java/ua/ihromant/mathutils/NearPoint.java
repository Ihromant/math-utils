package ua.ihromant.mathutils;

import java.util.Arrays;

public record NearPoint(NearField x, NearField y) {
    public NearPoint sub(NearPoint that) {
        return new NearPoint(this.x.sub(that.x), this.y.sub(that.y));
    }

    public boolean parallel(NearPoint that) {
        return Arrays.stream(NearField.values()).skip(1).anyMatch(nf -> this.equals(nf.mul(that)));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
