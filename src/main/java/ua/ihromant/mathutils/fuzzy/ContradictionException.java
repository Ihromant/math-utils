package ua.ihromant.mathutils.fuzzy;

import java.util.Map;

public class ContradictionException extends RuntimeException {
    private final Rel rel;

    public ContradictionException(Rel rel, Map<Rel, Update> updates) {
        this.rel = rel;
    }

    public Rel rel() {
        return rel;
    }
}
