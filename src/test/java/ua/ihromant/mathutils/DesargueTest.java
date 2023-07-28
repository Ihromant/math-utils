package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class DesargueTest {
    private static final int HALF = 100;
    private static final Point SHIFT = Point.of(HALF, HALF);
    @Test
    public void generateDesargues() throws IOException {
        for (Triple<Rational> slopes : new TriplesIterator<>(Rational.LATEX_SLOPES)) {
            Rational f = slopes.f();
            Rational s = slopes.s();
            Rational t = slopes.t();
            if (f.equals(s) || f.equals(t) || s.equals(t)) {
                continue;
            }
            for (int fl = 5; fl < HALF / f.max(); fl++) {
                for (int sl = 10; sl < HALF / s.max(); sl++) {
                    Point fpl = Point.of(f.denom() * fl, f.numer() * fl);
                    Point spl = Point.of(s.denom() * sl, s.numer() * sl);
                    Rational fssl = fpl.sub(spl).slope();
                    if (!checkSlope(fssl)) {
                        continue;
                    }
                    for (int tl = 5; tl < HALF / t.max(); tl++) {
                        Point tpl = Point.of(t.denom() * tl, t.numer() * tl);
                        Rational ftsl = fpl.sub(tpl).slope();
                        Rational stsl = spl.sub(tpl).slope();
                        if (!checkSlope(ftsl) || !checkSlope(stsl) || ftsl.equals(stsl)) {
                            continue;
                        }
                        for (int fr = 5; fr < HALF / f.max(); fr++) {
                            for (int sr = 10; sr < HALF / s.max(); sr++) {
                                Point fpr = Point.of(-f.denom() * fr, -f.numer() * fr);
                                Point spr = Point.of(-s.denom() * sr, -s.numer() * sr);
                                Rational fssr = fpr.sub(spr).slope();
                                if (!checkSlope(fssr) || fssl.equals(fssr)) {
                                    continue;
                                }
                                for (int tr = 5; tr < HALF / t.max(); tr++) {
                                    Point tpr = Point.of(-t.denom() * tr, -t.numer() * tr);
                                    Rational ftsr = fpr.sub(tpr).slope();
                                    Rational stsr = spr.sub(tpr).slope();
                                    if (!checkSlope(ftsr) || !checkSlope(stsr) || ftsl.equals(ftsr) || stsl.equals(stsr) || ftsr.equals(stsr)) {
                                        continue;
                                    }
                                    Point fsi = intersection(fpl, spl, fpr, spr);
                                    if (!fsi.x().isInt() || !fsi.y().isInt() || fsi.x().max() > HALF || fsi.y().max() > HALF) {
                                        continue;
                                    }
                                    Point fti = intersection(fpl, tpl, fpr, tpr);
                                    if (!fti.x().isInt() || !fti.y().isInt() || fti.x().max() > HALF || fti.y().max() > HALF) {
                                        continue;
                                    }
                                    Point vec = fsi.sub(fti);
                                    if (!Rational.ZERO.equals(vec.x())) {
                                        Rational slope = vec.slope();
                                        if (!checkSlope(slope)) {
                                            continue;
                                        }
                                    }
                                    Point sti = intersection(spl, tpl, spr, tpr);
                                    if (!sti.x().isInt() || !sti.y().isInt() || sti.x().max() > HALF || sti.y().max() > HALF) {
                                        continue;
                                    }
                                    printConfig(SHIFT.add(fpl), SHIFT.add(spl), SHIFT.add(tpl), SHIFT.add(fpr), SHIFT.add(spr), SHIFT.add(tpr),
                                            SHIFT.add(fsi), SHIFT.add(fti), SHIFT.add(sti));
                                    drawImage(SHIFT.add(fpl), SHIFT.add(spl), SHIFT.add(tpl), SHIFT.add(fpr), SHIFT.add(spr), SHIFT.add(tpr),
                                            SHIFT.add(fsi), SHIFT.add(fti), SHIFT.add(sti));
                                    //return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String sName(Point p) {
        return p.x() + "_" + p.y();
    }

    private static void printConfig(Point fpl, Point spl, Point tpl, Point fpr, Point spr, Point tpr,
                                  Point fsi, Point fti, Point sti) throws IOException {
        String fName = sName(fpl) + "_" + sName(spl) + "_" + sName(tpl) + "_" + sName(fpr) + "_" + sName(spr) + "_" + sName(tpr);
        try (FileOutputStream fos = new FileOutputStream(new File("/tmp/img", fName + ".txt"));
             OutputStreamWriter osw = new OutputStreamWriter(fos);
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write("\\begin{picture}(" + (2 * HALF) + "," + (2 * HALF) + ")(0,0)\n");
            bw.write("\\linethickness{1pt}\n");
            drawLine(bw, fpl, fsi);
            drawLine(bw, spl, fsi);
            drawLine(bw, fpl, fti);
            drawLine(bw, tpl, fti);
            drawLine(bw, spl, sti);
            drawLine(bw, tpl, sti);

            drawLine(bw, fpr, fsi);
            drawLine(bw, spr, fsi);
            drawLine(bw, fpr, fti);
            drawLine(bw, tpr, fti);
            drawLine(bw, spr, sti);
            drawLine(bw, tpr, sti);

            drawLine(bw, fpr, fpl);
            drawLine(bw, spr, spl);
            drawLine(bw, tpr, tpl);

            drawLine(bw, fsi, fti);
            drawLine(bw, fsi, sti);
            drawLine(bw, fti, sti);

            drawLine(bw, fpl, spl);
            drawLine(bw, fpl, tpl);
            drawLine(bw, spl, tpl);

            drawLine(bw, fpr, spr);
            drawLine(bw, fpr, tpr);
            drawLine(bw, spr, tpr);

            bw.write("\\end{picture}");
        }
    }

    private static void drawLine(Graphics gr, Point first, Point second) {
        gr.drawLine(first.x().numer(), first.y().numer(), second.x().numer(), second.y().numer());
    }

    private static void drawLine(BufferedWriter wr, Point first, Point second) throws IOException {
        int dx = second.x().numer() - first.x().numer();
        int dy = second.y().numer() - first.y().numer();
        int gcd = Rational.gcd(Math.abs(dx), Math.abs(dy));
        int nX = dx / gcd;
        int nY = dy / gcd;
        wr.write("\\put(" + first.x() + "," + first.y() + ")" +
                "{\\color{" + "black" + "}" +
                "\\line(" + nX + "," + nY + "){" + Math.max(Math.abs(dx), Math.abs(dy)) + "}}\n");
    }

    private static void drawImage(Point fpl, Point spl, Point tpl, Point fpr, Point spr, Point tpr,
                                  Point fsi, Point fti, Point sti) throws IOException {
        String fName = sName(fpl) + "_" + sName(spl) + "_" + sName(tpl) + "_" + sName(fpr) + "_" + sName(spr) + "_" + sName(tpr);
        BufferedImage img = new BufferedImage(2 * HALF, 2 * HALF, BufferedImage.TYPE_INT_ARGB);
        Graphics gr = img.getGraphics();
        gr.setColor(Color.BLACK);
        drawLine(gr, fpl, fsi);
        drawLine(gr, spl, fsi);
        drawLine(gr, fpl, fti);
        drawLine(gr, tpl, fti);
        drawLine(gr, spl, sti);
        drawLine(gr, tpl, sti);

        gr.setColor(Color.BLACK);
        drawLine(gr, fpr, fsi);
        drawLine(gr, spr, fsi);
        drawLine(gr, fpr, fti);
        drawLine(gr, tpr, fti);
        drawLine(gr, spr, sti);
        drawLine(gr, tpr, sti);

        gr.setColor(Color.BLUE);
        drawLine(gr, fpr, fpl);
        drawLine(gr, spr, spl);
        drawLine(gr, tpr, tpl);

        gr.setColor(Color.RED);
        drawLine(gr, fsi, fti);
        drawLine(gr, fsi, sti);
        drawLine(gr, fti, sti);

        gr.setColor(Color.GREEN);
        drawLine(gr, fpl, spl);
        drawLine(gr, fpl, tpl);
        drawLine(gr, spl, tpl);

        gr.setColor(Color.ORANGE);
        drawLine(gr, fpr, spr);
        drawLine(gr, fpr, tpr);
        drawLine(gr, spr, tpr);

        ImageIO.write(img, "png", new File("/tmp/img", fName + ".png"));
    }

    private static Point intersection(Point p1, Point p2, Point p3, Point p4) {
        Rational x12 = p1.x().sub(p2.x());
        Rational x34 = p3.x().sub(p4.x());
        Rational y12 = p1.y().sub(p2.y());
        Rational y34 = p3.y().sub(p4.y());
        Rational xy12 = p1.x().mul(p2.y()).sub(p1.y().mul(p2.x()));
        Rational xy34 = p3.x().mul(p4.y()).sub(p3.y().mul(p4.x()));
        Rational xn = xy12.mul(x34).sub(x12.mul(xy34));
        Rational yn = xy12.mul(y34).sub(y12.mul(xy34));
        Rational d = x12.mul(y34).sub(y12.mul(x34));
        return new Point(xn.div(d), yn.div(d));
    }

    private boolean checkSlope(Rational slope) {
        return !Rational.ZERO.equals(slope) && slope.max() <= 6;
    }
}
