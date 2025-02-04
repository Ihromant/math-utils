package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.NearField;

public class QuaternionGroup implements Group {
    @Override
    public int op(int a, int b) {
        return NearField.MULTIPLICATION_TABLE[a + 1][b + 1].ordinal() - 1;
    }

    @Override
    public int inv(int a) {
        return NearField.INVERSIONS[a + 1].ordinal() - 1;
    }

    @Override
    public int order() {
        return NearField.values().length - 1;
    }

    @Override
    public String name() {
        return "Q8";
    }

    @Override
    public String elementName(int a) {
        return NearField.values()[a + 1].name();
    }
}
