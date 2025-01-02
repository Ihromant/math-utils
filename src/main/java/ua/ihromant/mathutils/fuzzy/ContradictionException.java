package ua.ihromant.mathutils.fuzzy;

public class ContradictionException extends RuntimeException {
    private final Rel rel;
    public ContradictionException(Rel rel) {
        this.rel = rel;
    }

    public Rel rel() {
        return rel;
    }
}
