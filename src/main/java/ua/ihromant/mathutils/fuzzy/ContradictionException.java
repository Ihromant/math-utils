package ua.ihromant.mathutils.fuzzy;

import java.util.Map;

public class ContradictionException extends RuntimeException {
    private final Rel rel;
    private final Map<Rel, Update> updates;

    public ContradictionException(Rel rel, Map<Rel, Update> updates) {
        this.rel = rel;
        this.updates = updates;
    }

    public Rel rel() {
        return rel;
    }

    public Map<Rel, Update> updates() {
        return updates;
    }
}
