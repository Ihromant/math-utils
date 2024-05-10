package ua.ihromant.mathutils.nauty;

import nl.peterbloem.kit.Order;

import static nl.peterbloem.kit.Series.series;

public class Graphs {
    public static <L> Graph<L> reorder(Graph<L> graph, Order order)
    {
        if(graph instanceof UGraph<?>)
            return reorder((UGraph<L>)graph, order);

        throw new RuntimeException("Type of graph ("+graph.getClass()+") not recognized");
    }

    public static <L> UGraph<L> reorder(UGraph<L> graph, Order order)
    {
        assert(graph.size() == order.size());

        UTGraph<L, String> out = new MapUTGraph<L, String>();
        for(int newIndex : series(order.size()))
            out.add(graph.get(order.originalIndex(newIndex)).label());

        for(ULink<L> link : graph.links())
        {
            int originalIndexFirst = link.first().index();
            int originalIndexSecond = link.second().index();

            UNode<L> first = out.get(order.newIndex(originalIndexFirst));
            UNode<L> second = out.get(order.newIndex(originalIndexSecond));

            first.connect(second);
        }

        return out;
    }
}
