package ua.ihromant.mathutils.group;

import java.io.IOException;

public class GroupIndex {
    public static Group group(int order, int index) throws IOException {
        System.out.println("Reading SmallGroup(" + order + "," + index + ")");
        return new GapInteractor().smallGroup(order, index);
    }

    public static String identify(Group g) throws IOException {
        return new GapInteractor().identifyGroup(g);
    }
}
