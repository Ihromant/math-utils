package ua.ihromant.mathutils.polymino;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static Set<Polymino> extension(Set<Polymino> base, Set<Polymino> filter, Map<Polymino, Polymino> defaults) {
        Set<Polymino> result = new HashSet<>();
        base.stream().flatMap(Polymino::extended).forEach(ext -> {
            if (!filter.contains(ext)) {
                result.add(defaults.get(ext));
            }
        });
        return result;
    }

    private static Stream<Set<Polymino>> partials(Set<Polymino> base) {
        List<Polymino> list = base.stream().toList();
        return IntStream.range(0, list.size()).mapToObj(i ->
                IntStream.range(0, list.size()).filter(j -> j != i).mapToObj(list::get).collect(Collectors.toSet()));
    }

    private static Map<Polymino, Polymino> defaults(Collection<Polymino> base) {
        Map<Polymino, Polymino> defaults = new HashMap<>();
        base.forEach(big -> big.variations().forEach(var -> defaults.put(var, big)));
        return defaults;
    }

    public static void main(String[] args) {
        int size = 7;
        Set<Polymino> lesser = generate(size - 1);
        Set<Polymino> bigger = generate(size);
        Map<Polymino, Polymino> defaults = defaults(bigger);
        Map<Polymino, Polymino> ancestors = calculateSingleAncestors(lesser, defaults);
        Set<Polymino> necessary = new HashSet<>(ancestors.values());
        Set<Polymino> biggerFilter = extension(necessary, Set.of(), defaults).stream().flatMap(Polymino::variations).collect(Collectors.toSet());
        Set<Polymino> biggerFiltered = bigger.stream().filter(big -> !biggerFilter.contains(big)).collect(Collectors.toSet());
        Set<Polymino> lesserFiltered = lesser.stream().filter(l -> !necessary.contains(l)).collect(Collectors.toSet());
        //System.out.println(lesser.size() + " " + bigger.size());
        Set<Set<Polymino>> sets = Set.of(lesserFiltered);
        for (int i = lesserFiltered.size(); i > 1; i--) {
            Set<Set<Polymino>> checked = new HashSet<>();
            Set<Set<Polymino>> next = sets.stream().flatMap(Generator::partials)
                    .filter(part -> checked.add(part) && extension(part, biggerFilter, defaults).size() == biggerFiltered.size()).collect(Collectors.toSet());
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

    private static Map<Polymino, Polymino> calculateSingleAncestors(Set<Polymino> lesser, Map<Polymino, Polymino> defaults) {
        Map<Polymino, Set<Polymino>> result = new HashMap<>();
        List<Polymino> bigger = new ArrayList<>();
        for (Polymino single : lesser) {
            Set<Polymino> descendants = new HashSet<>();
            extension(Set.of(single), Set.of(), defaults).forEach(ext -> {
                if (ext.variations().noneMatch(descendants::contains)) {
                    Optional<Polymino> same = bigger.stream().filter(big -> ext.variations().anyMatch(big::equals)).findAny();
                    if (same.isPresent()) {
                        descendants.add(same.get());
                    } else {
                        bigger.add(ext);
                        descendants.add(ext);
                    }
                }
            });
            result.put(single, descendants);
        }
        Map<Polymino, Set<Polymino>> reversed = new HashMap<>();
        for (Map.Entry<Polymino, Set<Polymino>> e : result.entrySet()) {
            for (Polymino big : e.getValue()) {
                reversed.computeIfAbsent(big, kk -> new HashSet<>()).add(e.getKey());
            }
        }
        return reversed.entrySet().stream().filter(e -> e.getValue().size() == 1)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().iterator().next()));
    }
}
