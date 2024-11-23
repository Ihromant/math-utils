package ua.ihromant.mathutils.plane;

import java.util.List;

public record ProjChar(String name, List<TernarMapping> ternars) {
    @Override
    public String toString() {
        return "AC(" + name + " " + ternars + ")";
    }
}
