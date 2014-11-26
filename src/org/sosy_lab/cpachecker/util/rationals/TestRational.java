package org.sosy_lab.cpachecker.util.rationals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestRational {
  @Test
  public void testInstantiation() {
    Rational x;
    x = Rational.ofLongs(108, 96);
    Assert.assertEquals("9/8", x.toString());

    x = Rational.ofLong(50);
    Assert.assertEquals("50", x.toString());

    x = Rational.of(BigInteger.ZERO, BigInteger.ONE);
    Assert.assertEquals("0", x.toString());
  }

  @Test public void testAddition() {
    Rational a, b;
    a = Rational.ofLongs(12, 8);
    b = Rational.ofLongs(-54, 12);
    Assert.assertEquals("-3", a.plus(b).toString());
  }

  @Test public void testSubtraction() {
    Rational a, b;

    a = Rational.ofString("5/2");
    b = Rational.ofString("3/2");

    Assert.assertEquals("1", a.minus(b).toString());
  }

  @Test public void testMultiplication() {
    Rational a, b;
    a = Rational.ofString("2/4");
    b = Rational.ofString("-1/3");
    Assert.assertEquals(Rational.ofString("-2/12"), a.times(b));

    a = Rational.ofString("100/4");
    b = Rational.ofString("1/100");
    Assert.assertEquals(Rational.ofString("1/4"), a.times(b));
  }

  @Test public void testDivision() {
    Rational a, b;
    a = Rational.ofString("2/4");
    b = Rational.ofString("1/4");
    Assert.assertEquals("2", a.divides(b).toString());
  }

  @Test public void testComparison() {
    List<Rational> unsorted = Arrays.asList(
        Rational.ofLongs(-2, 4),
        Rational.ofLongs(1, 3),
        Rational.ofLongs(2, 3)
    );
    Collections.shuffle(unsorted);

    List<Rational> sorted = Arrays.asList(
        Rational.ofLongs(-2, 4),
        Rational.ofLongs(1, 3),
        Rational.ofLongs(2, 3)
    );

    Collections.sort(unsorted);

    Assert.assertEquals(sorted, unsorted);
  }

  @Test public void testOfString() {
    Rational a;
    a = Rational.ofString("6/8");
    Assert.assertEquals(Rational.ofLongs(3, 4), a);

    a = Rational.ofString("-2");
    Assert.assertEquals(Rational.ofLongs(-2, 1), a);
  }

  @Test public void testCanonicity() {
    Rational a, b;
    a = Rational.ofString("6/8");
    b = Rational.ofString("-6/8");
    Assert.assertTrue(Rational.ZERO == a.plus(b));

    a = Rational.ofString("2");
    b = Rational.ofString("-1");
    Assert.assertTrue(Rational.NEG_ONE == b);
    Assert.assertTrue(Rational.ONE == a.plus(b));

    a = Rational.ofString("-2");
    b = Rational.ofString("1");
    Assert.assertTrue(Rational.NEG_ONE == a.plus(b));
  }
}
