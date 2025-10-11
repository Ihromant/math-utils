package ua.ihromant.mathutils.group;

import java.io.IOException;

public class GroupIndex {
    public static Group group(int order, int index) throws IOException {
        System.out.println("Reading SmallGroup(" + order + "," + index + ")");
        return new GapInteractor().smallGroup(order, index);
    }

    public static String identify(Group g) throws IOException {
        if (g.order() > 1500) {
            return "Large order " + g.order();
        }
        return new GapInteractor().identifyGroup(g);
    }

    public static int groupId(Group g) throws IOException {
        if (g.order() > 1500) {
            throw new IllegalArgumentException("More than 1500");
        }
        return new GapInteractor().groupId(g);
    }

    public static int groupCount(int order) throws IOException {
        return new GapInteractor().groupCount(order);
    }
}
