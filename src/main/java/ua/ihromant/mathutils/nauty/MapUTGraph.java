package ua.ihromant.mathutils.nauty;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nl.peterbloem.kit.Series.series;

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
    protected Set<T> tags = new LinkedHashSet<T>();

    protected long numLinks = 0;
    protected long modCount = 0;

    protected int hash;
    protected Long hashMod = null;

    public MapUTGraph()
    {
    }

    private class MapUTNode implements UTNode<L, T>
    {
        private Map<T, List<MapUTLink>> links = new LinkedHashMap<T, List<MapUTLink>>();

        private Set<MapUTNode> neighbors = new LinkedHashSet<MapUTNode>();

        private L label;

        // * A node is dead when it is removed from the graph. Since there is no
        //   way to ensure that clients don't maintain copies of node objects we
        //   keep check of nodes that are no longer part of the graph.
        private boolean dead = false;

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
        public Collection<MapUTNode> neighbors()
        {
            checkDead();

            return Collections.unmodifiableSet(neighbors);
        }

        @Override
        public L label()
        {
            checkDead();

            return label;
        }

        public int id()
        {
            checkDead();

            return ((Object) this).hashCode();
        }

        /**
         * An id to identify this node among nodes with the same label.
         * @return
         */
        public int labelId()
        {
            checkDead();

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

        public String toString()
        {
            checkDead();

            boolean unique = nodes.get(label).size() <= 1;

            return label + (unique ? "" : "_" + labelId());
        }


        @Override
        public int index()
        {
            checkDead();

            return index;
        }

        @Override
        public int hashCode()
        {
            checkDead();

            // * We base the hashcode on just the label. If adding links changes
            //   the hashcode, our maps will get messed up.
            // * Even the node index is _not_ persistent.
            int hash = 1;

            hash = 31 * hash + (label == null ? 0 : label.hashCode());

            return hash;
        }

        @Override
        public Collection<? extends UTLink<L, T>> links(Node<L> other)
        {
            checkDead();
            List<UTLink<L, T>> result = new ArrayList<UTLink<L, T>>();

            for(T tag : links.keySet())
                for(UTLink<L, T> link : links.get(tag))
                    if(link.other(this).equals(other))
                        result.add(link);

            return result;
        }

        public boolean equals(Object other)
        {
            checkDead();
            return this == other;
        }

        /**
         * Checks if the node is dead, and fails if it is.
         */
        private void checkDead()
        {
            if(dead)
                throw new IllegalStateException("This node (last index "+index+") has been removed, accessing any of its methods (except dead()) will cause en exception.");
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

        @Override
        public T tag()
        {
            return tag;
        }

        @Override
        public UTNode<L, T> first()
        {
            return first;
        }

        @Override
        public UTNode<L, T> second()
        {
            return second;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<? extends UTNode<L, T>> nodes()
        {
            return Arrays.asList(first, second);
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
    public Set<? extends UTNode<L, T>> nodes(L label)
    {
        Set<MapUTNode> n = nodes.get(label);
        if(n == null)
            return Collections.emptySet();

        return Collections.unmodifiableSet(n);
    }

    @Override
    public long numLinks()
    {
        return numLinks;
    }

    @Override
    public List<? extends UTNode<L, T>> nodes()
    {
        return Collections.unmodifiableList(nodeList);
    }

    private class LinkCollection extends AbstractCollection<MapUTLink>
    {

        @Override
        public Iterator<MapUTLink> iterator()
        {
            return new LCIterator();
        }

        @Override
        public int size()
        {
            return (int)numLinks();
        }

        private class LCIterator implements Iterator<MapUTLink>
        {
            private static final int BUFFER_SIZE = 5;
            private LinkedList<MapUTLink> buffer = new LinkedList<MapUTLink>();
            private MapUTLink last = null;

            private MapUTNode current = null;
            private Iterator<MapUTNode> nodeIt = nodeList.iterator();
            private Iterator<MapUTNode> neighborIt;

            LCIterator() {}

            @Override
            public boolean hasNext()
            {
                buffer();
                return ! buffer.isEmpty();
            }

            @Override
            public MapUTLink next()
            {
                buffer();
                last = buffer.poll();
                return last;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Call remove on the link object to remove the link from the graph");
            }

            private void buffer()
            {
                while(buffer.size() < BUFFER_SIZE)
                {
                    if(! nodeIt.hasNext())
                        break;

                    current = nodeIt.next();

                    for(T tag : current.links.keySet())
                        for(MapUTLink link : current.links.get(tag))
                        {
                            int curr = current.index(),
                                    oth = link.other(current).index();

                            if(curr <= oth)
                                buffer.add(link);
                        }
                }
            }
        }
    }

    /**
     * Resets the indices of all nodes
     */
    protected void updateIndices()
    {
        for(int i : series(nodeList.size()))
            nodeList.get(i).index = i;
    }
}
