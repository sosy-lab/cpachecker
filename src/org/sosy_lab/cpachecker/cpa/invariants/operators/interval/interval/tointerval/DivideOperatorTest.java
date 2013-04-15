/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Unit test for the operator used for dividing simple intervals by simple
 * intervals to produce simple intervals.
 */
public class DivideOperatorTest {

  @Test
  public void testApply() {
    SimpleInterval negFourToNegTwo = SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2));
    SimpleInterval negFourToNegOne = SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-1));
    SimpleInterval negTwoToNegOne = SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1));
    SimpleInterval oneToFour = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(4));
    SimpleInterval oneToTwo = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(2));
    SimpleInterval twoToFour = SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4));
    SimpleInterval negTwentyToTwenty = SimpleInterval.of(BigInteger.valueOf(-20), BigInteger.valueOf(20));
    SimpleInterval negTwoToTwo = SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(2));
    SimpleInterval negInfToTen = SimpleInterval.singleton(BigInteger.valueOf(10)).extendToNegativeInfinity();
    SimpleInterval negInfToFive = SimpleInterval.singleton(BigInteger.valueOf(5)).extendToNegativeInfinity();
    SimpleInterval negInfToNegFive = SimpleInterval.singleton(BigInteger.valueOf(-5)).extendToNegativeInfinity();
    SimpleInterval fiveToInf = SimpleInterval.singleton(BigInteger.valueOf(5)).extendToPositiveInfinity();
    SimpleInterval negTwentyToTen = SimpleInterval.of(BigInteger.valueOf(-20), BigInteger.valueOf(10));
    SimpleInterval negTwoToInf = SimpleInterval.singleton(BigInteger.valueOf(-2)).extendToPositiveInfinity();
    SimpleInterval zeroToInf = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
    SimpleInterval negInfToZero = zeroToInf.negate();
    SimpleInterval zeroToTwo = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(2));

    assertEquals(oneToFour, IIIOperator.DIVIDE_OPERATOR.apply(negFourToNegTwo, negTwoToNegOne));
    assertEquals(negFourToNegOne, IIIOperator.DIVIDE_OPERATOR.apply(negFourToNegTwo, oneToTwo));
    assertEquals(negFourToNegOne, IIIOperator.DIVIDE_OPERATOR.apply(twoToFour, negTwoToNegOne));
    assertEquals(oneToFour, IIIOperator.DIVIDE_OPERATOR.apply(twoToFour, oneToTwo));
    assertEquals(negTwentyToTwenty, IIIOperator.DIVIDE_OPERATOR.apply(negTwentyToTwenty, negTwoToTwo));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE_OPERATOR.apply(negInfToTen, negTwoToTwo));
    assertEquals(negInfToFive, IIIOperator.DIVIDE_OPERATOR.apply(negInfToTen, twoToFour));
    assertEquals(negInfToZero, IIIOperator.DIVIDE_OPERATOR.apply(negInfToNegFive, fiveToInf));
    assertEquals(negTwentyToTen, IIIOperator.DIVIDE_OPERATOR.apply(negTwentyToTen, negTwoToInf));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE_OPERATOR.apply(SimpleInterval.infinite(), SimpleInterval.infinite()));
    assertNull(IIIOperator.DIVIDE_OPERATOR.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.ZERO)));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE_OPERATOR.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(5))));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE_OPERATOR.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(-5))));
    assertEquals(zeroToInf, IIIOperator.DIVIDE_OPERATOR.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(5))));
    assertEquals(negInfToZero, IIIOperator.DIVIDE_OPERATOR.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(-5))));
    assertEquals(negTwentyToTwenty, IIIOperator.DIVIDE_OPERATOR.apply(negTwentyToTwenty, zeroToInf));
    assertEquals(oneToFour, IIIOperator.DIVIDE_OPERATOR.apply(twoToFour, zeroToTwo));
  }

}
