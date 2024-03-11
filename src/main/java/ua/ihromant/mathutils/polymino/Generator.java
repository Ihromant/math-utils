package ua.ihromant.mathutils.polymino;

import ua.ihromant.mathutils.GaloisField;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static void main(String[] args) {
        List<Polymino> hexaminos = generate(6).stream().toList();
        Set<Polymino> heptominos = generate(7);
        System.out.println(hexaminos.size() + " " + heptominos.size());
        GaloisField.choices(hexaminos.size(), 25).parallel().forEach(ch -> {
            List<Polymino> choice = Arrays.stream(ch).mapToObj(hexaminos::get).toList();
            Set<Polymino> result = new HashSet<>();
            choice.stream().flatMap(Polymino::extended).forEach(ext -> {
                if (ext.variations().noneMatch(result::contains)) {
                    result.add(ext);
                }
            });
            if (heptominos.size() == result.size()) {
                synchronized (System.out) {
                    System.out.println("Solution: ");
                    for (Polymino polymino : choice) {
                        System.out.println(polymino);
                    }
                }
            }
        });
//        generate(5).forEach(System.out::println);
    }
}
