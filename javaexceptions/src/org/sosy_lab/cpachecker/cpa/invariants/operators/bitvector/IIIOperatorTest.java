// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Unit test for the operator used to add simple intervals to simple intervals to produce simple
 * intervals.
 */
public class IIIOperatorTest {

  private static final BitVectorInfo INT = BitVectorInfo.from(32, true);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> ADD =
      IIIOperatorFactory.INSTANCE.getAdd(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> MULTIPLY =
      IIIOperatorFactory.INSTANCE.getMultiply(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> DIVIDE =
      IIIOperatorFactory.INSTANCE.getDivide(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> MODULO =
      IIIOperatorFactory.INSTANCE.getModulo(true, OverflowEventHandler.EMPTY);

  @Test
  public void testAdd() {
    BitVectorInterval zero = BitVectorInterval.singleton(INT, BigInteger.ZERO);
    BitVectorInterval one = BitVectorInterval.singleton(INT, BigInteger.ONE);
    assertThat(ADD.apply(zero, one)).isEqualTo(one);
  }

  @Test
  public void testModulo() {
    BigInteger scalarFour = BigInteger.valueOf(4);
    BigInteger scalarFive = BigInteger.valueOf(5);
    BitVectorInterval five = BitVectorInterval.singleton(INT, scalarFive);
    BitVectorInterval negFourToFour = BitVectorInterval.of(INT, scalarFour.negate(), scalarFour);
    BitVectorInterval zeroToFour = BitVectorInterval.of(INT, BigInteger.ZERO, scalarFour);
    BitVectorInterval zeroToMax =
        BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMaxValue();
    BitVectorInterval minToZero =
        BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMinValue();
    BitVectorInterval tenToEleven =
        BitVectorInterval.of(INT, BigInteger.valueOf(10), BigInteger.valueOf(11));
    BitVectorInterval twoToThree =
        BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(3));
    BitVectorInterval zeroToTwo = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(2));
    BitVectorInterval eightToTen = BitVectorInterval.of(INT, BigInteger.valueOf(8), BigInteger.TEN);

    assertThat(MODULO.apply(INT.getRange(), five)).isEqualTo(negFourToFour);
    assertThat(MODULO.apply(zeroToMax, five)).isEqualTo(zeroToFour);
    assertThat(MODULO.apply(zeroToMax, five.negate(true, OverflowEventHandler.EMPTY)))
        .isEqualTo(zeroToFour);
    assertThat(MODULO.apply(tenToEleven, BitVectorInterval.singleton(INT, scalarFour)))
        .isEqualTo(twoToThree);
    assertThat(MODULO.apply(eightToTen, BitVectorInterval.singleton(INT, scalarFour)))
        .isEqualTo(zeroToTwo);
    assertThat(
            MODULO.apply(
                tenToEleven,
                BitVectorInterval.singleton(INT, scalarFour)
                    .negate(true, OverflowEventHandler.EMPTY)))
        .isEqualTo(twoToThree);
    assertThat(
            MODULO.apply(
                eightToTen,
                BitVectorInterval.singleton(INT, scalarFour)
                    .negate(true, OverflowEventHandler.EMPTY)))
        .isEqualTo(zeroToTwo);
    assertThat(
            MODULO.apply(
                tenToEleven.negate(true, OverflowEventHandler.EMPTY),
                BitVectorInterval.singleton(INT, scalarFour)))
        .isEqualTo(twoToThree.negate(true, OverflowEventHandler.EMPTY));
    assertThat(
            MODULO.apply(
                eightToTen.negate(true, OverflowEventHandler.EMPTY),
                BitVectorInterval.singleton(INT, scalarFour)))
        .isEqualTo(zeroToTwo.negate(true, OverflowEventHandler.EMPTY));
    assertThat(
            MODULO.apply(
                tenToEleven.negate(true, OverflowEventHandler.EMPTY),
                BitVectorInterval.singleton(INT, scalarFour)
                    .negate(true, OverflowEventHandler.EMPTY)))
        .isEqualTo(twoToThree.negate(true, OverflowEventHandler.EMPTY));
    assertThat(
            MODULO.apply(
                eightToTen.negate(true, OverflowEventHandler.EMPTY),
                BitVectorInterval.singleton(INT, scalarFour)
                    .negate(true, OverflowEventHandler.EMPTY)))
        .isEqualTo(zeroToTwo.negate(true, OverflowEventHandler.EMPTY));
    assertThat(MODULO.apply(zeroToMax, BitVectorInterval.of(INT, BigInteger.ZERO, scalarFive)))
        .isEqualTo(zeroToFour);
    assertThat(MODULO.apply(zeroToMax, BitVectorInterval.of(INT, scalarFour.negate(), scalarFive)))
        .isEqualTo(zeroToFour);
    assertThat(MODULO.apply(zeroToMax, BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)))
        .isEqualTo(zeroToFour);
    assertThat(MODULO.apply(minToZero, BitVectorInterval.of(INT, BigInteger.ZERO, scalarFive)))
        .isEqualTo(zeroToFour.negate(true, OverflowEventHandler.EMPTY));
    assertThat(MODULO.apply(minToZero, BitVectorInterval.of(INT, scalarFour.negate(), scalarFive)))
        .isEqualTo(zeroToFour.negate(true, OverflowEventHandler.EMPTY));
    assertThat(MODULO.apply(minToZero, BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)))
        .isEqualTo(zeroToFour.negate(true, OverflowEventHandler.EMPTY));
    assertThat(
            MODULO.apply(
                INT.getRange(), BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)))
        .isEqualTo(BitVectorInterval.of(INT, scalarFour.negate(), scalarFour));

    BitVectorInterval fiftyNine =
        BitVectorInterval.of(INT, BigInteger.valueOf(59), BigInteger.valueOf(59));
    BitVectorInterval zeroTo255 =
        BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(255));
    assertThat(MODULO.apply(fiftyNine, zeroTo255))
        .isEqualTo(BitVectorInterval.of(INT, BigInteger.valueOf(0), BigInteger.valueOf(59)));
  }

  @Test
  public void testMultiply() {
    BigInteger hundred = BigInteger.valueOf(100);
    BitVectorInterval negTenToZero =
        BitVectorInterval.of(INT, BigInteger.TEN.negate(), BigInteger.ZERO);
    BitVectorInterval zeroToTen = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.TEN);
    BitVectorInterval negHundredToHundred = BitVectorInterval.of(INT, hundred.negate(), hundred);
    BitVectorInterval negHundredToZero =
        BitVectorInterval.of(INT, hundred.negate(), BigInteger.ZERO);
    BitVectorInterval negTenToTen =
        BitVectorInterval.of(INT, BigInteger.TEN.negate(), BigInteger.TEN);
    BitVectorInterval zero = BitVectorInterval.singleton(INT, BigInteger.ZERO);
    BitVectorInterval zeroToOne = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.ONE);
    BitVectorInterval minToFive =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMinValue();
    BitVectorInterval minToNegFive =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(-5)).extendToMinValue();
    BitVectorInterval twentyToTwentyFive =
        BitVectorInterval.of(INT, BigInteger.valueOf(20), BigInteger.valueOf(25));
    BitVectorInterval twoToFour =
        BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(4));
    BitVectorInterval fortyToHundred =
        BitVectorInterval.of(INT, BigInteger.valueOf(40), BigInteger.valueOf(100));

    assertThat(MULTIPLY.apply(negTenToZero, zeroToTen)).isEqualTo(negHundredToZero);
    assertThat(MULTIPLY.apply(negTenToZero, negTenToTen)).isEqualTo(negHundredToHundred);
    assertThat(MULTIPLY.apply(INT.getRange(), zero)).isEqualTo(zero);
    assertThat(MULTIPLY.apply(INT.getRange(), zeroToOne)).isEqualTo(INT.getRange());
    assertThat(MULTIPLY.apply(minToFive, minToFive)).isEqualTo(INT.getRange());
    assertThat(MULTIPLY.apply(minToNegFive, minToNegFive)).isEqualTo(INT.getRange());
    assertThat(MULTIPLY.apply(twentyToTwentyFive, twoToFour)).isEqualTo(fortyToHundred);
  }

  @Test
  public void testDivide() {
    BitVectorInterval negFourToNegTwo =
        BitVectorInterval.of(INT, BigInteger.valueOf(-4), BigInteger.valueOf(-2));
    BitVectorInterval negFourToNegOne =
        BitVectorInterval.of(INT, BigInteger.valueOf(-4), BigInteger.valueOf(-1));
    BitVectorInterval negTwoToNegOne =
        BitVectorInterval.of(INT, BigInteger.valueOf(-2), BigInteger.valueOf(-1));
    BitVectorInterval oneToFour =
        BitVectorInterval.of(INT, BigInteger.valueOf(1), BigInteger.valueOf(4));
    BitVectorInterval oneToTwo =
        BitVectorInterval.of(INT, BigInteger.valueOf(1), BigInteger.valueOf(2));
    BitVectorInterval twoToFour =
        BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(4));
    BitVectorInterval negTwentyToTwenty =
        BitVectorInterval.of(INT, BigInteger.valueOf(-20), BigInteger.valueOf(20));
    BitVectorInterval negTwoToTwo =
        BitVectorInterval.of(INT, BigInteger.valueOf(-2), BigInteger.valueOf(2));
    BitVectorInterval minToTen =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(10)).extendToMinValue();
    BitVectorInterval minToFive =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMinValue();
    BitVectorInterval minToNegFive =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(-5)).extendToMinValue();
    BitVectorInterval fiveToMax =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMaxValue();
    BitVectorInterval negTwentyToTen =
        BitVectorInterval.of(INT, BigInteger.valueOf(-20), BigInteger.valueOf(10));
    BitVectorInterval negTwoToMax =
        BitVectorInterval.singleton(INT, BigInteger.valueOf(-2)).extendToMaxValue();
    BitVectorInterval zeroToMax =
        BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMaxValue();
    BitVectorInterval minToZero =
        BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMinValue();
    BitVectorInterval zeroToTwo = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(2));

    assertThat(DIVIDE.apply(minToZero, minToFive)).isEqualTo(INT.getRange());
    assertThat(DIVIDE.apply(negFourToNegTwo, negTwoToNegOne)).isEqualTo(oneToFour);
    assertThat(DIVIDE.apply(negFourToNegTwo, oneToTwo)).isEqualTo(negFourToNegOne);
    assertThat(DIVIDE.apply(twoToFour, negTwoToNegOne)).isEqualTo(negFourToNegOne);
    assertThat(DIVIDE.apply(twoToFour, oneToTwo)).isEqualTo(oneToFour);
    assertThat(DIVIDE.apply(negTwentyToTwenty, negTwoToTwo)).isEqualTo(negTwentyToTwenty);
    assertThat(DIVIDE.apply(minToTen, negTwoToTwo)).isEqualTo(INT.getRange());
    assertThat(DIVIDE.apply(minToTen, twoToFour))
        .isEqualTo(
            BitVectorInterval.of(INT, BigInteger.valueOf(-1073741824), BigInteger.valueOf(5)));
    assertThat(DIVIDE.apply(minToNegFive, fiveToMax))
        .isEqualTo(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.ZERO));
    assertThat(DIVIDE.apply(negTwentyToTen, negTwoToMax)).isEqualTo(negTwentyToTwenty);
    assertThat(DIVIDE.apply(INT.getRange(), INT.getRange())).isEqualTo(INT.getRange());
    assertThat(DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.ZERO)))
        .isNull();
    assertThat(
            DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.valueOf(5))))
        .isEqualTo(
            BitVectorInterval.of(
                INT, BigInteger.valueOf(-429496729), BigInteger.valueOf(429496729)));
    assertThat(
            DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.valueOf(-5))))
        .isEqualTo(
            BitVectorInterval.of(
                INT, BigInteger.valueOf(-429496729), BigInteger.valueOf(429496729)));
    assertThat(DIVIDE.apply(zeroToMax, BitVectorInterval.singleton(INT, BigInteger.valueOf(5))))
        .isEqualTo(BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(429496729)));
    assertThat(DIVIDE.apply(zeroToMax, BitVectorInterval.singleton(INT, BigInteger.valueOf(-5))))
        .isEqualTo(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.ZERO));
    assertThat(DIVIDE.apply(negTwentyToTwenty, zeroToMax)).isEqualTo(negTwentyToTwenty);
    assertThat(DIVIDE.apply(twoToFour, zeroToTwo)).isEqualTo(oneToFour);
  }
}
