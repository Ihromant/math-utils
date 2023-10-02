package ua.ihromant.mathutils;

public enum NearField {
    ZERO, PL_1, MI_1, PL_I, MI_I, PL_J, MI_J, PL_K, MI_K;

    private static NearField[][] ADDITION_TABLE = {
            // 0     1    -1     i    -i     j    -j     k    -k
            {ZERO, PL_1, MI_1, PL_I, MI_I, PL_J, MI_J, PL_K, MI_K}, // 0
            {PL_1, MI_1, ZERO, PL_J, PL_K, MI_K, MI_I, MI_J, PL_I}, // 1
            {MI_1, ZERO, PL_1, MI_K, MI_J, PL_I, PL_K, MI_I, PL_J}, // -1
            {PL_I, PL_J, MI_K, MI_I, ZERO, PL_K, MI_1, PL_1, MI_J}, // i
            {MI_I, PL_K, MI_J, ZERO, PL_I, PL_1, MI_K, PL_J, MI_1}, // -i
            {PL_J, MI_K, PL_I, PL_K, PL_1, MI_J, ZERO, MI_1, MI_I}, // j
            {MI_J, MI_I, PL_K, MI_1, MI_K, ZERO, PL_J, PL_I, PL_1}, // -j
            {PL_K, MI_J, MI_I, PL_1, PL_J, MI_1, PL_I, MI_K, ZERO}, // k
            {MI_K, PL_I, PL_J, MI_J, MI_1, MI_I, PL_1, ZERO, PL_K}  // -k
    };

    private static NearField[][] MULTIPLICATION_TABLE = {
            // 0     1    -1     i    -i     j    -j     k    -k
            {ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO}, // 0
            {ZERO, PL_1, MI_1, PL_I, MI_I, PL_J, MI_J, PL_K, MI_K}, // 1
            {ZERO, MI_1, PL_1, MI_I, PL_I, MI_J, PL_J, MI_K, PL_K}, // -1
            {ZERO, PL_I, MI_I, MI_1, PL_1, PL_K, MI_K, MI_J, PL_J}, // i
            {ZERO, MI_I, PL_I, PL_1, MI_1, MI_K, PL_K, PL_J, MI_J}, // -i
            {ZERO, PL_J, MI_J, MI_K, PL_K, MI_1, PL_1, PL_I, MI_I}, // j
            {ZERO, MI_J, PL_J, PL_K, MI_K, PL_1, MI_1, MI_I, PL_I}, // -j
            {ZERO, PL_K, MI_K, PL_J, MI_J, MI_I, PL_I, MI_1, PL_1}, // k
            {ZERO, MI_K, PL_K, MI_J, PL_J, PL_I, MI_I, PL_1, MI_1}  // -k
    };

    private static NearField[] NEGATIONS =
            {ZERO, MI_1, PL_1, MI_I, PL_I, MI_J, PL_J, MI_K, PL_K};

    public NearField add(NearField that) {
        return ADDITION_TABLE[this.ordinal()][that.ordinal()];
    }

    public NearField mul(NearField that) {
        return MULTIPLICATION_TABLE[this.ordinal()][that.ordinal()];
    }

    public NearField neg() {
        return NEGATIONS[this.ordinal()];
    }

    public NearField sub(NearField that) {
        return this.add(that.neg());
    }

    public NearPoint mul(NearPoint p) {
        return new NearPoint(this.mul(p.x()), this.mul(p.y()));
    }

    @Override
    public String toString() {
        return switch (this) {
            case ZERO -> "0";
            case PL_1 -> "1";
            case MI_1 -> "-1";
            case PL_I -> "i";
            case MI_I -> "-i";
            case PL_J -> "j";
            case MI_J -> "-j";
            case PL_K -> "k";
            case MI_K -> "-k";
        };
    }
}
