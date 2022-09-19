// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Unit test for the operator used to add simple intervals to simple intervals to produce simple
 * intervals.
 */
public class IIIOperatorTest {

  @Test
  public void testAdd() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    assertThat(IIIOperator.ADD.apply(zero, one)).isEqualTo(one);
  }

  @Test
  public void testModulo() {
    BigInteger scalarFour = BigInteger.valueOf(4);
    BigInteger scalarFive = BigInteger.valueOf(5);
    SimpleInterval five = SimpleInterval.singleton(scalarFive);
    SimpleInterval negFourToFour = SimpleInterval.of(scalarFour.negate(), scalarFour);
    SimpleInterval zeroToFour = SimpleInterval.of(BigInteger.ZERO, scalarFour);
    SimpleInterval zeroToInf = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
    SimpleInterval tenToEleven = SimpleInterval.of(BigInteger.valueOf(10), BigInteger.valueOf(11));
    SimpleInterval twoToThree = SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(3));
    SimpleInterval zeroToTwo = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(2));
    SimpleInterval eightToTen = SimpleInterval.of(BigInteger.valueOf(8), BigInteger.TEN);

    assertThat(IIIOperator.MODULO.apply(SimpleInterval.infinite(), five)).isEqualTo(negFourToFour);
    assertThat(IIIOperator.MODULO.apply(zeroToInf, five)).isEqualTo(zeroToFour);
    assertThat(IIIOperator.MODULO.apply(zeroToInf, five.negate())).isEqualTo(zeroToFour);
    assertThat(IIIOperator.MODULO.apply(tenToEleven, SimpleInterval.singleton(scalarFour)))
        .isEqualTo(twoToThree);
    assertThat(IIIOperator.MODULO.apply(eightToTen, SimpleInterval.singleton(scalarFour)))
        .isEqualTo(zeroToTwo);
    assertThat(IIIOperator.MODULO.apply(tenToEleven, SimpleInterval.singleton(scalarFour).negate()))
        .isEqualTo(twoToThree);
    assertThat(IIIOperator.MODULO.apply(eightToTen, SimpleInterval.singleton(scalarFour).negate()))
        .isEqualTo(zeroToTwo);
    assertThat(IIIOperator.MODULO.apply(tenToEleven.negate(), SimpleInterval.singleton(scalarFour)))
        .isEqualTo(twoToThree.negate());
    assertThat(IIIOperator.MODULO.apply(eightToTen.negate(), SimpleInterval.singleton(scalarFour)))
        .isEqualTo(zeroToTwo.negate());
    assertThat(
            IIIOperator.MODULO.apply(
                tenToEleven.negate(), SimpleInterval.singleton(scalarFour).negate()))
        .isEqualTo(twoToThree.negate());
    assertThat(
            IIIOperator.MODULO.apply(
                eightToTen.negate(), SimpleInterval.singleton(scalarFour).negate()))
        .isEqualTo(zeroToTwo.negate());
    assertThat(IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(BigInteger.ZERO, scalarFive)))
        .isEqualTo(zeroToFour);
    assertThat(
            IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(scalarFour.negate(), scalarFive)))
        .isEqualTo(zeroToFour);
    assertThat(
            IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(scalarFive.negate(), scalarFour)))
        .isEqualTo(zeroToFour);
    assertThat(
            IIIOperator.MODULO.apply(
                zeroToInf.negate(), SimpleInterval.of(BigInteger.ZERO, scalarFive)))
        .isEqualTo(zeroToFour.negate());
    assertThat(
            IIIOperator.MODULO.apply(
                zeroToInf.negate(), SimpleInterval.of(scalarFour.negate(), scalarFive)))
        .isEqualTo(zeroToFour.negate());
    assertThat(
            IIIOperator.MODULO.apply(
                zeroToInf.negate(), SimpleInterval.of(scalarFive.negate(), scalarFour)))
        .isEqualTo(zeroToFour.negate());
    assertThat(
            IIIOperator.MODULO.apply(
                SimpleInterval.infinite(), SimpleInterval.of(scalarFive.negate(), scalarFour)))
        .isEqualTo(SimpleInterval.of(scalarFour.negate(), scalarFour));

    SimpleInterval fiftyNine = SimpleInterval.of(BigInteger.valueOf(59), BigInteger.valueOf(59));
    SimpleInterval zeroTo255 = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(255));
    assertThat(IIIOperator.MODULO.apply(fiftyNine, zeroTo255))
        .isEqualTo(SimpleInterval.of(BigInteger.valueOf(0), BigInteger.valueOf(59)));
  }

  @Test
  public void testMultiply() {
    BigInteger hundred = BigInteger.valueOf(100);
    SimpleInterval negTenToZero = SimpleInterval.of(BigInteger.TEN.negate(), BigInteger.ZERO);
    SimpleInterval zeroToTen = SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN);
    SimpleInterval negHundredToHundred = SimpleInterval.of(hundred.negate(), hundred);
    SimpleInterval negHundredToZero = SimpleInterval.of(hundred.negate(), BigInteger.ZERO);
    SimpleInterval negTenToTen = SimpleInterval.of(BigInteger.TEN.negate(), BigInteger.TEN);
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval zeroToOne = SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE);
    SimpleInterval negInfToFive =
        SimpleInterval.singleton(BigInteger.valueOf(5)).extendToNegativeInfinity();
    SimpleInterval negInfToNegFive =
        SimpleInterval.singleton(BigInteger.valueOf(-5)).extendToNegativeInfinity();
    SimpleInterval twentyFiveToInf =
        SimpleInterval.singleton(BigInteger.valueOf(25)).extendToPositiveInfinity();
    SimpleInterval twentyToTwentyFive =
        SimpleInterval.of(BigInteger.valueOf(20), BigInteger.valueOf(25));
    SimpleInterval twoToFour = SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4));
    SimpleInterval fortyToHundred =
        SimpleInterval.of(BigInteger.valueOf(40), BigInteger.valueOf(100));

    assertThat(IIIOperator.MULTIPLY.apply(negTenToZero, zeroToTen)).isEqualTo(negHundredToZero);
    assertThat(IIIOperator.MULTIPLY.apply(negTenToZero, negTenToTen))
        .isEqualTo(negHundredToHundred);
    assertThat(IIIOperator.MULTIPLY.apply(SimpleInterval.infinite(), zero)).isEqualTo(zero);
    assertThat(IIIOperator.MULTIPLY.apply(SimpleInterval.infinite(), zeroToOne))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(IIIOperator.MULTIPLY.apply(negInfToFive, negInfToFive))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(IIIOperator.MULTIPLY.apply(negInfToNegFive, negInfToNegFive))
        .isEqualTo(twentyFiveToInf);
    assertThat(IIIOperator.MULTIPLY.apply(twentyToTwentyFive, twoToFour)).isEqualTo(fortyToHundred);
  }

  @Test
  public void testDivide() {
    SimpleInterval negFourToNegTwo =
        SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2));
    SimpleInterval negFourToNegOne =
        SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-1));
    SimpleInterval negTwoToNegOne =
        SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1));
    SimpleInterval oneToFour = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(4));
    SimpleInterval oneToTwo = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(2));
    SimpleInterval twoToFour = SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4));
    SimpleInterval negTwentyToTwenty =
        SimpleInterval.of(BigInteger.valueOf(-20), BigInteger.valueOf(20));
    SimpleInterval negTwoToTwo = SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(2));
    SimpleInterval negInfToTen =
        SimpleInterval.singleton(BigInteger.valueOf(10)).extendToNegativeInfinity();
    SimpleInterval negInfToFive =
        SimpleInterval.singleton(BigInteger.valueOf(5)).extendToNegativeInfinity();
    SimpleInterval negInfToNegFive =
        SimpleInterval.singleton(BigInteger.valueOf(-5)).extendToNegativeInfinity();
    SimpleInterval fiveToInf =
        SimpleInterval.singleton(BigInteger.valueOf(5)).extendToPositiveInfinity();
    SimpleInterval negTwentyToTen =
        SimpleInterval.of(BigInteger.valueOf(-20), BigInteger.valueOf(10));
    SimpleInterval negTwoToInf =
        SimpleInterval.singleton(BigInteger.valueOf(-2)).extendToPositiveInfinity();
    SimpleInterval zeroToInf = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
    SimpleInterval negInfToZero = zeroToInf.negate();
    SimpleInterval zeroToTwo = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(2));

    assertThat(IIIOperator.DIVIDE.apply(negInfToZero, negInfToFive))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(IIIOperator.DIVIDE.apply(negFourToNegTwo, negTwoToNegOne)).isEqualTo(oneToFour);
    assertThat(IIIOperator.DIVIDE.apply(negFourToNegTwo, oneToTwo)).isEqualTo(negFourToNegOne);
    assertThat(IIIOperator.DIVIDE.apply(twoToFour, negTwoToNegOne)).isEqualTo(negFourToNegOne);
    assertThat(IIIOperator.DIVIDE.apply(twoToFour, oneToTwo)).isEqualTo(oneToFour);
    assertThat(IIIOperator.DIVIDE.apply(negTwentyToTwenty, negTwoToTwo))
        .isEqualTo(negTwentyToTwenty);
    assertThat(IIIOperator.DIVIDE.apply(negInfToTen, negTwoToTwo))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(IIIOperator.DIVIDE.apply(negInfToTen, twoToFour)).isEqualTo(negInfToFive);
    assertThat(IIIOperator.DIVIDE.apply(negInfToNegFive, fiveToInf)).isEqualTo(negInfToZero);
    assertThat(IIIOperator.DIVIDE.apply(negTwentyToTen, negTwoToInf)).isEqualTo(negTwentyToTen);
    assertThat(IIIOperator.DIVIDE.apply(SimpleInterval.infinite(), SimpleInterval.infinite()))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(
            IIIOperator.DIVIDE.apply(
                SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.ZERO)))
        .isNull();
    assertThat(
            IIIOperator.DIVIDE.apply(
                SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(5))))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(
            IIIOperator.DIVIDE.apply(
                SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(-5))))
        .isEqualTo(SimpleInterval.infinite());
    assertThat(IIIOperator.DIVIDE.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(5))))
        .isEqualTo(zeroToInf);
    assertThat(
            IIIOperator.DIVIDE.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(-5))))
        .isEqualTo(negInfToZero);
    assertThat(IIIOperator.DIVIDE.apply(negTwentyToTwenty, zeroToInf)).isEqualTo(negTwentyToTwenty);
    assertThat(IIIOperator.DIVIDE.apply(twoToFour, zeroToTwo)).isEqualTo(oneToFour);
  }
}
