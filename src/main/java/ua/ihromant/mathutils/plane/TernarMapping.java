package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Triangle;
import ua.ihromant.mathutils.util.FixBS;

import java.util.List;

public record TernarMapping(TernaryRing ring, List<FixBS> xl, Triangle[] function, Characteristic chr) {
    public boolean isInduced() {
        return xl.getLast().cardinality() == ring.order();
    }

    public boolean onePlus() {
        return chr.a1() == ring.order();
    }

    public boolean pulsOne() {
        return chr.a3() == ring.order();
    }

    public boolean plusOne() {
        return chr.a2() == ring.order();
    }

    public boolean oneGen() {
        return plusOne() || pulsOne() || onePlus();
    }

    public boolean twoMul() {
        return chr.mul1() == ring.order() - 1;
    }

    public boolean mulTwo() {
        return chr.mul2() == ring.order() - 1;
    }

    public boolean twoGen() {
        return twoMul() || mulTwo();
    }

    public boolean generated() {
        return oneGen() || twoGen();
    }

    @Override
    public String toString() {
        return "TM(" + ring + " " + chr + ")";
    }
}
