package ua.ihromant.mathutils;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegrator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PiCalculationTest {
    private record DiffFunction(double n) implements UnivariateRealFunction {
        private double diff(double x) {
            return Math.pow(x, n - 1) * Math.pow(1 - Math.pow(x, n), 1 / n - 1);
        }

        private double halfArc() {
            return Math.pow(0.5, 1 / n);
        }

        @Override
        public double value(double x) {
            double diff = diff(x);
            return Math.pow(1 + Math.pow(diff, n), 1 / n);
        }
    }

    @Test
    public void testCorrectnessAndTabulate() throws ConvergenceException, FunctionEvaluationException {
        DiffFunction common = new DiffFunction(2);
        UnivariateRealIntegrator integrator = new TrapezoidIntegrator();
        assertEquals(Math.PI / 4, integrator.integrate(common, 0, common.halfArc()), 0.000001);
        DiffFunction three = new DiffFunction(3);
        DiffFunction oneHalf = new DiffFunction(1.5);
        double threePiArc = integrator.integrate(three, 0, three.halfArc());
        assertEquals(1.629884, 2 * threePiArc, 0.000001);
        assertEquals(3.259768, 4 * threePiArc, 0.000001);
        assertEquals(threePiArc, integrator.integrate(oneHalf, 0, oneHalf.halfArc()), 0.0001);
        List<Double> doubles = new ArrayList<>();
        for (int i = 104; i <= 2600; i++) {
            double n = 0.01 * i;
            DiffFunction func = new DiffFunction(n);
            double part = 4 * integrator.integrate(func, 0, func.halfArc());
            doubles.add(part);
            System.out.println(n + ": " + part);
        }
        System.out.println(IntStream.range(0, doubles.size()).mapToObj(idx -> "[" + 0.01 * (idx + 104) + ", " + doubles.get(idx) + "]")
                .collect(Collectors.joining(", ", "[", "]")));
    }
}
