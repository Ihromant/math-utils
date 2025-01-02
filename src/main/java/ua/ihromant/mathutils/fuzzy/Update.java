package ua.ihromant.mathutils.fuzzy;

public record Update(Rel base, String reasonName, Rel... reasons) {
}
