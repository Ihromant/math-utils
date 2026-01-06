package ua.ihromant.mathutils.loop;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.group.CyclicGroup;

public record RajahElement(int delta, int alpha, int beta, int gamma) {
    public RajahElement op(RajahElement that, int p, int q, int mu, int phi) {
        GaloisField cq = new GaloisField(q);
        int exp = cq.exponent(mu, (p - 1) * that.delta);
        int resDelta = new CyclicGroup(p).op(this.delta, that.delta);
        int resAlpha = cq.add(cq.mul(exp, this.alpha), that.alpha);
        int resBeta = cq.add(cq.mul(exp, this.beta), that.beta);
        int epsilon = cq.sub(cq.mul(this.alpha, that.beta), cq.mul(that.alpha, this.beta));
        int muFst = cq.sub(cq.exponent(mu, that.delta), cq.exponent(mu, (p - 2) * that.delta));
        int muSnd = cq.sub(cq.exponent(mu, resDelta), exp);
        int mu1Inv = cq.inverse(cq.sub(mu, 1));
        int squareBrackets = cq.add(cq.mul(this.alpha, this.beta, muFst), cq.mul(epsilon, muSnd));
        int resGamma = cq.add(cq.mul(this.gamma, cq.exponent(mu, that.delta)),
                that.gamma,
                cq.mul(phi, that.alpha, this.beta, cq.exponent(mu, (p - 1) * that.delta)),
                cq.mul(squareBrackets, mu1Inv));
        return new RajahElement(resDelta, resAlpha, resBeta, resGamma);
    }
}
