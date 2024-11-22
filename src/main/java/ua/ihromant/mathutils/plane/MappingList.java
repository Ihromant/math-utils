package ua.ihromant.mathutils.plane;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MappingList(String name, boolean translation, int dl, Map<Characteristic, List<TernarMapping>> ternars) {
    @Override
    public String toString() {
        Map<Characteristic, Integer> freqs = freqs();
        return "ML(" + name + " " + dl + " " + translation + " " + freqs.values().stream().mapToInt(Integer::intValue).sum() + " " + freqs + ")";
    }

    private Map<Characteristic, Integer> freqs() {
        return ternars.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }
}
