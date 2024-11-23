package ua.ihromant.mathutils.plane;

import java.util.List;

public record AffineChar(String name, boolean translation, int dl, List<TernarMapping> ternars) {
    @Override
    public String toString() {
        return "AC(" + name + " " + dl + " " + translation + " " + ternars + ")";
    }
}
