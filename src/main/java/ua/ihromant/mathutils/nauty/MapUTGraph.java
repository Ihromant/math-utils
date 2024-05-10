package ua.ihromant.mathutils.nauty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * UTGraph (undirected tagged graph) implementation based on hashmaps. Each
 * node stores its neighbours in a hashmap.
 * </p><p>
 * Graph traversal by labels is close to linear (in the same sense that retrieval
 * from a hasmap is close to constant). Memory usage is relatively inefficient.
 * </p><p>
 * Because MapDTGraph inherits from Graph, it is possible to connect two nodes
 * without specifying a tag for the resulting link. In this case the tag gets
 * the value null.
 * </p><p>
 * Self loops and multiple edges are allowed.
 * </p>
 *
 * @author peter
 *
 * @param <L>
 */
public class MapUTGraph<L, T> implements UTGraph<L, T>
{
    protected List<MapUTNode> nodeList = new ArrayList<MapUTNode>();
    protected Map<L, Set<MapUTNode>> nodes = new LinkedHashMap<L, Set<MapUTNode>>();

    protected long modCount = 0;

    public MapUTGraph() {
    }

    private class MapUTNode implements UTNode<L, T>
    {
        private Map<T, List<MapUTLink>> links = new LinkedHashMap<T, List<MapUTLink>>();

        private Set<MapUTNode> neighbors = new LinkedHashSet<MapUTNode>();

        private L label;

        private Integer labelId = null;
        private Long labelIdMod;
        private int index;

        public MapUTNode(L label)
        {
            this.label = label;

            index = nodeList.size();

            // * The node adds itself to the graph's data structures
            nodeList.add(this);

            if(! nodes.containsKey(label))
                nodes.put(label, new LinkedHashSet<MapUTNode>());
            nodes.get(label).add(this);
        }

        @Override
        public Collection<MapUTNode> neighbors() {

            return Collections.unmodifiableSet(neighbors);
        }

        @Override
        public L label() {

            return label;
        }

        /**
         * An id to identify this node among nodes with the same label.
         * @return
         */
        public int labelId() {

            if(labelIdMod == null || labelIdMod != modCount)
            {
                Collection<MapUTNode> others = nodes.get(label);

                int i = 0;
                for(MapUTNode other : others)
                {
                    if(other.equals(this))
                    {
                        labelId = i;
                        break;
                    }
                    i++;
                }
                labelIdMod = modCount;
            }
            return labelId;

        }

        public String toString() {

            boolean unique = nodes.get(label).size() <= 1;

            return label + (unique ? "" : "_" + labelId());
        }


        @Override
        public int index() {

            return index;
        }

        @Override
        public int hashCode() {

            // * We base the hashcode on just the label. If adding links changes
            //   the hashcode, our maps will get messed up.
            // * Even the node index is _not_ persistent.
            int hash = 1;

            hash = 31 * hash + (label == null ? 0 : label.hashCode());

            return hash;
        }

        @Override
        public Collection<? extends UTLink<L, T>> links(Node<L> other) {
            List<UTLink<L, T>> result = new ArrayList<UTLink<L, T>>();

            for(T tag : links.keySet())
                for(UTLink<L, T> link : links.get(tag))
                    if(link.other(this).equals(other))
                        result.add(link);

            return result;
        }

        public boolean equals(Object other) {
            return this == other;
        }
    }

    private final class MapUTLink implements UTLink<L, T>
    {
        private T tag;
        private MapUTNode first, second;
        private boolean dead = false;

        public MapUTLink(T tag, MapUTNode first, MapUTNode second)
        {
            this.tag = tag;
            this.first = first;
            this.second = second;
        }

        public String toString()
        {
            return first + " -- " + second + (tag == null ? "" : " [label="+tag+"]");
        }

        @Override
        public UTNode<L, T> other(Node<L> current)
        {
            if(first != current)
                return first;
            return second;
        }

        @Override
        public int hashCode()
        {
            int result = 1;
            result = 31 * result + (dead ? 1231 : 1237);
            result = 31 * result + ((tag == null) ? 0 : tag.hashCode());
            return result;
        }
    }

    public int size()
    {
        return nodeList.size();
    }

    @Override
    public List<? extends UTNode<L, T>> nodes()
    {
        return Collections.unmodifiableList(nodeList);
    }
}
