package ua.ihromant.mathutils.fuzzy;

import java.util.Map;

public record LinerHistory(FuzzyLiner liner, Map<Rel, Update> updates) {
}
