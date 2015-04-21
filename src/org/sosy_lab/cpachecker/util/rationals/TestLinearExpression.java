package org.sosy_lab.cpachecker.util.rationals;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.testing.EqualsTester;


public class TestLinearExpression {

  @Test public void testInstantiation() {
    LinearExpression<String> x;
    x = LinearExpression.empty();
    assertThat(x.size()).isEqualTo(0);

    x = LinearExpression.pair("x", Rational.ofString("5"));
    assertThat(x.size()).isEqualTo(1);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("5"));

    x = LinearExpression.ofVariable("y");
    assertThat(x.size()).isEqualTo(1);
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ONE);

    x = LinearExpression.pair("x", Rational.ofString("0"));
    assertThat(x.size()).isEqualTo(0);
  }

  @Test public void testAdd() {
    LinearExpression<String> x;
    x = LinearExpression.pair("x", Rational.ofString("5"));
    x = x.add(LinearExpression.pair("x", Rational.ofString("8")));
    x = x.add(LinearExpression.pair("y", Rational.ofString("2")));
    x = x.add(LinearExpression.pair("z", Rational.ofString("3")));
    assertThat(x.size()).isEqualTo(3);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("13"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("2"));
    assertThat(x.getCoeff("z")).isEqualTo(Rational.ofString("3"));

    assertThat(x.isIntegral()).isTrue();
  }

  @Test public void testSub() {
    LinearExpression<String> x;
    x = LinearExpression.pair("x", Rational.ofString("5"));
    x = x.add(LinearExpression.pair("y", Rational.ofString("3")));
    x = x.sub(LinearExpression.pair("x", Rational.ofString("5")));
    x = x.sub(LinearExpression.pair("y", Rational.ofString("2")));
    x = x.sub(LinearExpression.pair("z", Rational.ofString("1")));

    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ZERO);
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ONE);
    assertThat(x.getCoeff("z")).isEqualTo(Rational.NEG_ONE);
  }

  @Test public void testMultiplication() {
    LinearExpression<String> x;
    x = LinearExpression.pair("x", Rational.ofString("5"));
    x = x.multByConst(Rational.ZERO);
    assertThat(x.size()).isEqualTo(0);

    x = LinearExpression.pair("x", Rational.ofString("5"));
    x = x.add(LinearExpression.pair("y", Rational.ofString("3")));
    x = x.multByConst(Rational.ofString("2"));
    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("10"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("6"));
  }

  @Test public void testNegation() {
    LinearExpression<String> x;
    x = LinearExpression.pair("x", Rational.ofString("5"));
    x = x.add(LinearExpression.pair("y", Rational.ofString("3")));
    x = x.negate();
    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("-5"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("-3"));
  }

  @Test public void testEquality() {
    LinearExpression<String> x, y;
    x = LinearExpression.pair("x", Rational.ofString("6"));
    y = LinearExpression.pair("x", Rational.ofString("3"));
    y = y.add(LinearExpression.pair("x", Rational.ofString("3")));
    y = y.add(LinearExpression.pair("z", Rational.ofString("3")));
    y = y.sub(LinearExpression.pair("z", Rational.ofString("3")));

    new EqualsTester().addEqualityGroup(x, y);
  }
}
