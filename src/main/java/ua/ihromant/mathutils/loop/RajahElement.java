package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.CyclicGroup;

public record RajahElement(int delta, int alpha, int beta, int gamma) {
    public RajahElement op(RajahElement that, int p, GaloisField q, int mu, int phi) {
        int exp = q.exponent(mu, (p - 1) * that.delta);
        int resDelta = new CyclicGroup(p).op(this.delta, that.delta);
        int resAlpha = q.add(q.mul(exp, this.alpha), that.alpha);
        int resBeta = q.add(q.mul(exp, this.beta), that.beta);
        int epsilon = q.sub(q.mul(this.alpha, that.beta), q.mul(that.alpha, this.beta));
        int muFst = q.sub(q.exponent(mu, that.delta), q.exponent(mu, (p - 2) * that.delta));
        int muSnd = q.sub(q.exponent(mu, resDelta), exp);
        int mu1Inv = q.inverse(q.sub(mu, 1));
        int squareBrackets = q.add(q.mul(this.alpha, this.beta, muFst), q.mul(epsilon, muSnd));
        int resGamma = q.add(q.mul(this.gamma, q.exponent(mu, that.delta)),
                that.gamma,
                q.mul(phi, that.alpha, this.beta, q.exponent(mu, (p - 1) * that.delta)),
                q.mul(squareBrackets, mu1Inv));
        return new RajahElement(resDelta, resAlpha, resBeta, resGamma);
    }
}
