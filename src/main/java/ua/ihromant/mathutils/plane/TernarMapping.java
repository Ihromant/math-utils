package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.TernaryRing;
import ua.ihromant.mathutils.Triangle;
import ua.ihromant.mathutils.util.FixBS;

import java.util.List;

public record TernarMapping(TernaryRing ring, List<FixBS> xl, Triangle[] function) {
    public boolean isInduced() {
        return xl.getLast().cardinality() == ring.order();
    }
}
