package ua.ihromant.mathutils.polymino;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Generator {
    public static Set<Polymino> generate(int n) {
        if (n == 1) {
            return Set.of(new Polymino(Set.of(new Point(0, 0))));
        }
        Set<Polymino> result = new HashSet<>();
        Set<Polymino> prev = generate(n - 1);
        prev.stream().flatMap(Polymino::extended).forEach(ext -> {
            if (ext.variations().noneMatch(result::contains)) {
                result.add(ext);
            }
        });
        return result;
    }

    private static Set<Polymino> extension(Set<Polymino> base) {
        Set<Polymino> result = new HashSet<>();
        base.stream().flatMap(Polymino::extended).forEach(ext -> {
            if (ext.variations().noneMatch(result::contains)) {
                result.add(ext);
            }
        });
        return result;
    }

    private static Stream<Set<Polymino>> partials(Set<Polymino> base) {
        List<Polymino> list = base.stream().toList();
        return IntStream.range(0, list.size()).mapToObj(i ->
                IntStream.range(0, list.size()).filter(j -> j != i).mapToObj(list::get).collect(Collectors.toSet()));
    }

    public static void main(String[] args) {
        int size = 7;
        Set<Polymino> lesser = generate(size - 1);
        Set<Polymino> bigger = generate(size);
        //System.out.println(lesser.size() + " " + bigger.size());
        Set<Set<Polymino>> sets = Set.of(lesser);
        for (int i = lesser.size(); i > 1; i--) {
            Set<Set<Polymino>> checked = new HashSet<>();
            Set<Set<Polymino>> next = sets.stream().flatMap(Generator::partials)
                    .filter(part -> checked.add(part) && extension(part).size() == bigger.size()).collect(Collectors.toSet());
            System.out.println((i - 1) + " " + next.size());
            if (next.isEmpty()) {
                for (Set<Polymino> polys : sets) {
                    System.out.println("Solution: ");
                    for (Polymino p : polys) {
                        System.out.println(p);
                    }
                }
                return;
            } else {
                sets = next;
            }
        }
//        generate(5).forEach(System.out::println);
    }
}
