package ua.ihromant.mathutils;

public class Fraction {
    private final Polynomial numerator;
    private final Polynomial denominator;

    public Fraction(Polynomial numerator, Polynomial denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Fraction add(Fraction other) {
        return new Fraction(this.numerator.mul(other.denominator).add(this.denominator.mul(other.numerator)), this.denominator.mul(other.denominator));
    }

    public Fraction mul(Fraction other) {
        return new Fraction(this.numerator.mul(other.numerator), this.denominator.mul(other.denominator));
    }

    public Fraction inv() {
        return new Fraction(this.denominator, this.numerator);
    }

    @Override
    public String toString() {
        return numerator.toString() + "/" + denominator.toString();
    }
}
