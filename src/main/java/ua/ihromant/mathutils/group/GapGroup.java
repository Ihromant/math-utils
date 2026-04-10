package ua.ihromant.mathutils.group;

import java.util.List;

public record GapGroup(Group group, List<List<List<Integer>>> cycles) {
}
