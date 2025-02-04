package ua.ihromant.mathutils.group;

import java.util.Arrays;

public class BurnsideGroup implements Group {
    private final CyclicGroup c3 = new CyclicGroup(3);
    private final CyclicProduct mul = new CyclicProduct(3, 3, 3);

    @Override
    public int op(int a, int b) {
        int[] fArr = mul.toArr(a);
        int[] sArr = mul.toArr(b);
        return mul.fromArr(c3.op(fArr[0], sArr[0]), c3.op(c3.op(fArr[1], sArr[1]), c3.mul(fArr[0], sArr[2])), c3.op(fArr[2], sArr[2]));
    }

    @Override
    public int inv(int a) {
        int[] arr = mul.toArr(a);
        return mul.fromArr(c3.inv(arr[0]), c3.op(c3.inv(arr[1]), c3.mul(arr[0], arr[2])), c3.inv(arr[2]));
    }

    @Override
    public int order() {
        return mul.order();
    }

    @Override
    public String name() {
        return "B(2,3)";
    }

    @Override
    public String elementName(int a) {
        return Arrays.toString(mul.toArr(a));
    }
}
