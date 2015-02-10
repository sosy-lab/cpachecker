package org.sosy_lab.cpachecker.util.rationals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing rationals library
 */
public class TestExtendedRational {
  @Test
  public void testTypes() {
    ExtendedRational x;

    x = new ExtendedRational(Rational.ofLongs(23, 7));
    Assert.assertEquals(true, x.isRational());

    x = ExtendedRational.INFTY;
    Assert.assertEquals(false, x.isRational());
  }

  @Test public void testInstantiation() {
    ExtendedRational x;
    x = new ExtendedRational(Rational.ofLongs(108, 96));
    Assert.assertEquals("9/8", x.toString());
  }

  @Test public void testAddition() {
    ExtendedRational a, b;
    a = new ExtendedRational(Rational.ofLongs(12, 8));
    b = new ExtendedRational(Rational.ofLongs(-54, 12));
    Assert.assertEquals("-3", a.plus(b).toString());

    b = ExtendedRational.INFTY;
    Assert.assertEquals("Infinity", a.plus(b).toString());

    b = ExtendedRational.NaN;
    Assert.assertEquals("NaN", a.plus(b).toString());

    a = ExtendedRational.INFTY;
    b = ExtendedRational.NEG_INFTY;
    Assert.assertEquals("NaN", a.plus(b).toString());


    a = ExtendedRational.ofString("2309820938409238490");
    b = ExtendedRational.NEG_INFTY;
    Assert.assertEquals("-Infinity", a.plus(b).toString());
  }

  @Test public void testSubtraction() {
    ExtendedRational a, b;

    a = ExtendedRational.ofString("5/2");
    b = ExtendedRational.ofString("3/2");

    Assert.assertEquals("1", a.minus(b).toString());
  }

  @Test public void testMultiplication() {
    ExtendedRational a, b;
    a = ExtendedRational.ofString("2/4");
    b = ExtendedRational.ofString("-1/3");
    Assert.assertEquals(ExtendedRational.ofString("-2/12"), a.times(b));

    a = ExtendedRational.ofString("100/4");
    b = ExtendedRational.ofString("1/100");
    Assert.assertEquals(ExtendedRational.ofString("1/4"), a.times(b));

    a = ExtendedRational.ofString("100/4");
    b = ExtendedRational.ofString("Infinity");
    Assert.assertEquals(ExtendedRational.ofString("Infinity"), a.times(b));
  }

  @Test public void testDivision() {
    ExtendedRational a, b;
    a = ExtendedRational.ofString("2/4");
    b = ExtendedRational.ofString("1/4");
    Assert.assertEquals("2", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.INFTY;
    Assert.assertEquals("0", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.NEG_INFTY;
    Assert.assertEquals("0", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.NaN;
    Assert.assertEquals("NaN", a.divides(b).toString());
  }

  @Test public void testComparison() {
    List<ExtendedRational> unsorted = Arrays.asList(
       ExtendedRational.NaN,
       ExtendedRational.NEG_INFTY,
       ExtendedRational.ofString("-2/4"),
       ExtendedRational.ofString("1/3"),
       ExtendedRational.ofString("2/3"),
       ExtendedRational.INFTY
    );
    Collections.shuffle(unsorted);

    List<ExtendedRational> sorted = Arrays.asList(
        ExtendedRational.NEG_INFTY,
        ExtendedRational.ofString("-2/4"),
        ExtendedRational.ofString("1/3"),
        ExtendedRational.ofString("2/3"),
        ExtendedRational.INFTY,
        ExtendedRational.NaN
    );

    Collections.sort(unsorted);

    Assert.assertEquals(sorted, unsorted);
  }

  @Test public void testOfString() {
    ExtendedRational a;
    a = ExtendedRational.ofString("Infinity");
    Assert.assertEquals(ExtendedRational.INFTY, a);

    a = ExtendedRational.ofString("-Infinity");
    Assert.assertEquals(ExtendedRational.NEG_INFTY, a);

    a = ExtendedRational.ofString("NaN");
    Assert.assertEquals(ExtendedRational.NaN, a);

    a = ExtendedRational.ofString("-2");
    Assert.assertEquals(new ExtendedRational(Rational.ofLong(-2)), a);
  }

}
