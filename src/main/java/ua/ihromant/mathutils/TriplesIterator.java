package ua.ihromant.mathutils;

import java.util.Iterator;
import java.util.List;

public class TriplesIterator<T> implements Iterable<Triple<T>> {
    private final List<T> list;
    private final int size;

    public TriplesIterator(List<T> list) {
        this.list = list;
        this.size = list.size();
    }

    @Override
    public Iterator<Triple<T>> iterator() {
        return new Iterator<>() {
            private int counter;
            @Override
            public boolean hasNext() {
                return counter < size * size * size;
            }

            @Override
            public Triple<T> next() {
                return new Triple<T>(list.get(counter / size / size), list.get((counter / size) % size), list.get(counter++ % size));
            }
        };
    }
}
