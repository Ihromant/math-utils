package ua.ihromant.mathutils.plane;

import java.util.List;

public record MappingList(String name, boolean translation, int dl, List<TernarMapping> ternars) {
    @Override
    public String toString() {
        return "ML(" + name + " " + dl + " " + translation + " " + ternars.size() + ")";
    }
}
