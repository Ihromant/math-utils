package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Triangle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MappingList(String name, boolean translation, int dl, Map<Triangle, List<TernarMapping>> ternars) {
    @Override
    public String toString() {
        Map<Triangle, Integer> freqs = freqs();
        return "ML(" + name + " " + dl + " " + translation + " " + freqs.values().stream().mapToInt(Integer::intValue).sum() + " " + freqs + ")";
    }

    private Map<Triangle, Integer> freqs() {
        return ternars.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }
}
