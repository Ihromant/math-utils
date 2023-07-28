package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class MeanTest {
    private static final Fraction HALF = new Fraction(new Polynomial(1), new Polynomial(2));
    @Test
    public void test() {
        double a = (ThreadLocalRandom.current().nextInt(10) + 5) * (ThreadLocalRandom.current().nextDouble() + 0.1);
        double b = (ThreadLocalRandom.current().nextInt(10) + 5) * (ThreadLocalRandom.current().nextDouble() + 0.1);
        double geom = geometric(a, b);
        if (a > b) {
            double c = a;
            a = b;
            b = c;
        }
        Deque<Double> first = new ArrayDeque<>();
        Deque<Double> second = new ArrayDeque<>();
        first.addLast(a);
        second.addLast(b);
        while (b - a > 1e-5) {
            double c = harmonic(a, b);
            b = arithm(a, b);
            a = c;
            second.addLast(b);
            first.addLast(a);
        }
        System.out.println(first);
        System.out.println(second);
        System.out.println(first.getLast() + " " + geom + " " + second.getLast());
    }

    private double arithm(double a, double b) {
        return 0.5 * (a + b);
    }

    private double harmonic(double a, double b) {
        return 2 * a * b / (a + b);
    }

    private Fraction harmonic(Fraction first, Fraction second) {
        return new Fraction(new Polynomial(2, 0), new Polynomial(1)).mul(first.add(second).inv());
    }

    private Fraction arithm(Fraction first, Fraction second) {
        return HALF.mul(first.add(second));
    }

    private double geometric(double a, double b) {
        return Math.sqrt(a * b);
    }

    @Test
    public void testPolynomials() {
        Fraction a = new Fraction(new Polynomial(1), new Polynomial(1));
        Fraction b = new Fraction(new Polynomial(1, 0), new Polynomial(1));
        int counter = 0;
        while (counter++ < 11) {
            Fraction c = harmonic(a, b);
            b = arithm(a, b);
            a = c;
            System.out.println(counter + " a: " + a);
            Fraction d = b.mul(b).add(new Fraction(new Polynomial(-1, 0), new Polynomial(1)));
            System.out.println(counter + " diff: " + d);
        }
    }

    @Test
    public void testSequence() {
        Polynomial[] arr = new Polynomial[10];
        arr[0] = new Polynomial(1.0);
        for (int i = 1; i < arr.length; i++) {
            Polynomial pr = new Polynomial(1.0);
            for (int j = 0; j < i - 1; j++) {
                pr = pr.mul(arr[j]);
            }
            arr[i] = arr[i - 1].mul(arr[i - 1]).add(new Polynomial(Math.pow(2, 2 * i - 2)).mul(pr).mul(pr).mul(new Polynomial(1, 0)));
        }
        IntStream.range(0, arr.length).forEach(i -> System.out.println(i + ": " + arr[i]));
    }
}

