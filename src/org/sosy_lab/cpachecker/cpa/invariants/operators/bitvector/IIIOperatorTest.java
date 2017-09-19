/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Unit test for the operator used to add simple intervals to simple
 * intervals to produce simple intervals.
 */
public class IIIOperatorTest {

  private static final BitVectorInfo INT = BitVectorInfo.from(32, true);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> ADD
    = IIIOperatorFactory.INSTANCE.getAdd(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> MULTIPLY
    = IIIOperatorFactory.INSTANCE.getMultiply(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> DIVIDE
    = IIIOperatorFactory.INSTANCE.getDivide(true, OverflowEventHandler.EMPTY);

  private static final Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> MODULO
    = IIIOperatorFactory.INSTANCE.getModulo(true, OverflowEventHandler.EMPTY);

  @Test
  public void testAdd() {
    BitVectorInterval zero = BitVectorInterval.singleton(INT, BigInteger.ZERO);
    BitVectorInterval one = BitVectorInterval.singleton(INT, BigInteger.ONE);
    assertEquals(one, ADD.apply(zero, one));
  }

  @Test
  public void testModulo() {
    BigInteger scalarFour = BigInteger.valueOf(4);
    BigInteger scalarFive = BigInteger.valueOf(5);
    BitVectorInterval five = BitVectorInterval.singleton(INT, scalarFive);
    BitVectorInterval negFourToFour = BitVectorInterval.of(INT, scalarFour.negate(), scalarFour);
    BitVectorInterval zeroToFour = BitVectorInterval.of(INT, BigInteger.ZERO, scalarFour);
    BitVectorInterval zeroToMax = BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMaxValue();
    BitVectorInterval minToZero = BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMinValue();
    BitVectorInterval tenToEleven = BitVectorInterval.of(INT, BigInteger.valueOf(10), BigInteger.valueOf(11));
    BitVectorInterval twoToThree = BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(3));
    BitVectorInterval zeroToTwo = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(2));
    BitVectorInterval eightToTen = BitVectorInterval.of(INT, BigInteger.valueOf(8), BigInteger.TEN);

    assertEquals(negFourToFour, MODULO.apply(INT.getRange(), five));
    assertEquals(zeroToFour, MODULO.apply(zeroToMax, five));
    assertEquals(zeroToFour, MODULO.apply(zeroToMax, five.negate(true, OverflowEventHandler.EMPTY)));
    assertEquals(twoToThree, MODULO.apply(tenToEleven, BitVectorInterval.singleton(INT, scalarFour)));
    assertEquals(zeroToTwo, MODULO.apply(eightToTen, BitVectorInterval.singleton(INT, scalarFour)));
    assertEquals(twoToThree, MODULO.apply(tenToEleven, BitVectorInterval.singleton(INT, scalarFour).negate(true, OverflowEventHandler.EMPTY)));
    assertEquals(zeroToTwo, MODULO.apply(eightToTen, BitVectorInterval.singleton(INT, scalarFour).negate(true, OverflowEventHandler.EMPTY)));
    assertEquals(twoToThree.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(tenToEleven.negate(true, OverflowEventHandler.EMPTY), BitVectorInterval.singleton(INT, scalarFour)));
    assertEquals(zeroToTwo.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(eightToTen.negate(true, OverflowEventHandler.EMPTY), BitVectorInterval.singleton(INT, scalarFour)));
    assertEquals(twoToThree.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(tenToEleven.negate(true, OverflowEventHandler.EMPTY), BitVectorInterval.singleton(INT, scalarFour).negate(true, OverflowEventHandler.EMPTY)));
    assertEquals(zeroToTwo.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(eightToTen.negate(true, OverflowEventHandler.EMPTY), BitVectorInterval.singleton(INT, scalarFour).negate(true, OverflowEventHandler.EMPTY)));
    assertEquals(zeroToFour, MODULO.apply(zeroToMax, BitVectorInterval.of(INT, BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour, MODULO.apply(zeroToMax, BitVectorInterval.of(INT, scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour, MODULO.apply(zeroToMax, BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)));
    assertEquals(zeroToFour.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(minToZero, BitVectorInterval.of(INT, BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(minToZero, BitVectorInterval.of(INT, scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour.negate(true, OverflowEventHandler.EMPTY), MODULO.apply(minToZero, BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)));
    assertEquals(BitVectorInterval.of(INT, scalarFour.negate(), scalarFour), MODULO.apply(INT.getRange(), BitVectorInterval.of(INT, scalarFive.negate(), scalarFour)));

    BitVectorInterval fiftyNine = BitVectorInterval.of(INT, BigInteger.valueOf(59), BigInteger.valueOf(59));
    BitVectorInterval zeroTo255 = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(255));
    assertEquals(
        BitVectorInterval.of(INT, BigInteger.valueOf(0), BigInteger.valueOf(59)),
        MODULO.apply(fiftyNine, zeroTo255));
  }

  @Test
  public void testMultiply() {
    BigInteger hundred = BigInteger.valueOf(100);
    BitVectorInterval negTenToZero = BitVectorInterval.of(INT, BigInteger.TEN.negate(), BigInteger.ZERO);
    BitVectorInterval zeroToTen = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.TEN);
    BitVectorInterval negHundredToHundred = BitVectorInterval.of(INT, hundred.negate(), hundred);
    BitVectorInterval negHundredToZero = BitVectorInterval.of(INT, hundred.negate(), BigInteger.ZERO);
    BitVectorInterval negTenToTen = BitVectorInterval.of(INT, BigInteger.TEN.negate(), BigInteger.TEN);
    BitVectorInterval zero = BitVectorInterval.singleton(INT, BigInteger.ZERO);
    BitVectorInterval zeroToOne = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.ONE);
    BitVectorInterval minToFive = BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMinValue();
    BitVectorInterval minToNegFive = BitVectorInterval.singleton(INT, BigInteger.valueOf(-5)).extendToMinValue();
    BitVectorInterval twentyToTwentyFive = BitVectorInterval.of(INT, BigInteger.valueOf(20), BigInteger.valueOf(25));
    BitVectorInterval twoToFour = BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(4));
    BitVectorInterval fortyToHundred = BitVectorInterval.of(INT, BigInteger.valueOf(40), BigInteger.valueOf(100));

    assertEquals(negHundredToZero, MULTIPLY.apply(negTenToZero, zeroToTen));
    assertEquals(negHundredToHundred, MULTIPLY.apply(negTenToZero, negTenToTen));
    assertEquals(zero, MULTIPLY.apply(INT.getRange(), zero));
    assertEquals(INT.getRange(), MULTIPLY.apply(INT.getRange(), zeroToOne));
    assertEquals(INT.getRange(), MULTIPLY.apply(minToFive, minToFive));
    assertEquals(INT.getRange(), MULTIPLY.apply(minToNegFive, minToNegFive));
    assertEquals(fortyToHundred, MULTIPLY.apply(twentyToTwentyFive, twoToFour));
  }

  @Test
  public void testDivide() {
    BitVectorInterval negFourToNegTwo = BitVectorInterval.of(INT, BigInteger.valueOf(-4), BigInteger.valueOf(-2));
    BitVectorInterval negFourToNegOne = BitVectorInterval.of(INT, BigInteger.valueOf(-4), BigInteger.valueOf(-1));
    BitVectorInterval negTwoToNegOne = BitVectorInterval.of(INT, BigInteger.valueOf(-2), BigInteger.valueOf(-1));
    BitVectorInterval oneToFour = BitVectorInterval.of(INT, BigInteger.valueOf(1), BigInteger.valueOf(4));
    BitVectorInterval oneToTwo = BitVectorInterval.of(INT, BigInteger.valueOf(1), BigInteger.valueOf(2));
    BitVectorInterval twoToFour = BitVectorInterval.of(INT, BigInteger.valueOf(2), BigInteger.valueOf(4));
    BitVectorInterval negTwentyToTwenty = BitVectorInterval.of(INT, BigInteger.valueOf(-20), BigInteger.valueOf(20));
    BitVectorInterval negTwoToTwo = BitVectorInterval.of(INT, BigInteger.valueOf(-2), BigInteger.valueOf(2));
    BitVectorInterval minToTen = BitVectorInterval.singleton(INT, BigInteger.valueOf(10)).extendToMinValue();
    BitVectorInterval minToFive = BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMinValue();
    BitVectorInterval minToNegFive = BitVectorInterval.singleton(INT, BigInteger.valueOf(-5)).extendToMinValue();
    BitVectorInterval fiveToMax = BitVectorInterval.singleton(INT, BigInteger.valueOf(5)).extendToMaxValue();
    BitVectorInterval negTwentyToTen = BitVectorInterval.of(INT, BigInteger.valueOf(-20), BigInteger.valueOf(10));
    BitVectorInterval negTwoToMax = BitVectorInterval.singleton(INT, BigInteger.valueOf(-2)).extendToMaxValue();
    BitVectorInterval zeroToMax = BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMaxValue();
    BitVectorInterval minToZero = BitVectorInterval.singleton(INT, BigInteger.ZERO).extendToMinValue();
    BitVectorInterval zeroToTwo = BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(2));

    assertEquals(INT.getRange(), DIVIDE.apply(minToZero, minToFive));
    assertEquals(oneToFour, DIVIDE.apply(negFourToNegTwo, negTwoToNegOne));
    assertEquals(negFourToNegOne, DIVIDE.apply(negFourToNegTwo, oneToTwo));
    assertEquals(negFourToNegOne, DIVIDE.apply(twoToFour, negTwoToNegOne));
    assertEquals(oneToFour, DIVIDE.apply(twoToFour, oneToTwo));
    assertEquals(negTwentyToTwenty, DIVIDE.apply(negTwentyToTwenty, negTwoToTwo));
    assertEquals(INT.getRange(), DIVIDE.apply(minToTen, negTwoToTwo));
    assertEquals(BitVectorInterval.of(INT, BigInteger.valueOf(-1073741824), BigInteger.valueOf(5)),
        DIVIDE.apply(minToTen, twoToFour));
    assertEquals(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.ZERO),
        DIVIDE.apply(minToNegFive, fiveToMax));
    assertEquals(negTwentyToTwenty, DIVIDE.apply(negTwentyToTen, negTwoToMax));
    assertEquals(INT.getRange(), DIVIDE.apply(INT.getRange(), INT.getRange()));
    assertNull(DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.ZERO)));
    assertEquals(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.valueOf(429496729)),
        DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.valueOf(5))));
    assertEquals(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.valueOf(429496729)),
        DIVIDE.apply(INT.getRange(), BitVectorInterval.singleton(INT, BigInteger.valueOf(-5))));
    assertEquals(BitVectorInterval.of(INT, BigInteger.ZERO, BigInteger.valueOf(429496729)),
        DIVIDE.apply(zeroToMax, BitVectorInterval.singleton(INT, BigInteger.valueOf(5))));
    assertEquals(BitVectorInterval.of(INT, BigInteger.valueOf(-429496729), BigInteger.ZERO),
        DIVIDE.apply(zeroToMax, BitVectorInterval.singleton(INT, BigInteger.valueOf(-5))));
    assertEquals(negTwentyToTwenty, DIVIDE.apply(negTwentyToTwenty, zeroToMax));
    assertEquals(oneToFour, DIVIDE.apply(twoToFour, zeroToTwo));
  }

}
