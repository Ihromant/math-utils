package ua.ihromant.mathutils.nautyx;

import ua.ihromant.mathutils.Liner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record NautyWrapper(Liner liner) {
    public int size() {
        return liner.pointCount() + liner.lineCount();
    }

    public List<List<NautyNode>> partition() {
        List<List<NautyNode>> result = new ArrayList<>();
        result.add(IntStream.range(0, liner.pointCount()).mapToObj(i -> new NautyNode(this, false, i)).collect(Collectors.toList()));
        result.add(IntStream.range(0, liner.lineCount()).mapToObj(i -> new NautyNode(this, true, i)).collect(Collectors.toList()));
        return result;
    }
}
