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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Unit test for the operator used to add simple intervals to simple
 * intervals to produce simple intervals.
 */
public class IIIOperatorTest {

  @Test
  public void testAdd() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    assertEquals(one, IIIOperator.ADD.apply(zero, one));
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

    assertEquals(negFourToFour, IIIOperator.MODULO.apply(SimpleInterval.infinite(), five));
    assertEquals(zeroToFour, IIIOperator.MODULO.apply(zeroToInf, five));
    assertEquals(zeroToFour, IIIOperator.MODULO.apply(zeroToInf, five.negate()));
    assertEquals(twoToThree, IIIOperator.MODULO.apply(tenToEleven, SimpleInterval.singleton(scalarFour)));
    assertEquals(zeroToTwo, IIIOperator.MODULO.apply(eightToTen, SimpleInterval.singleton(scalarFour)));
    assertEquals(twoToThree, IIIOperator.MODULO.apply(tenToEleven, SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToTwo, IIIOperator.MODULO.apply(eightToTen, SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(twoToThree.negate(), IIIOperator.MODULO.apply(tenToEleven.negate(), SimpleInterval.singleton(scalarFour)));
    assertEquals(zeroToTwo.negate(), IIIOperator.MODULO.apply(eightToTen.negate(), SimpleInterval.singleton(scalarFour)));
    assertEquals(twoToThree.negate(), IIIOperator.MODULO.apply(tenToEleven.negate(), SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToTwo.negate(), IIIOperator.MODULO.apply(eightToTen.negate(), SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToFour, IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour, IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour, IIIOperator.MODULO.apply(zeroToInf, SimpleInterval.of(scalarFive.negate(), scalarFour)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO.apply(zeroToInf.negate(), SimpleInterval.of(BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO.apply(zeroToInf.negate(), SimpleInterval.of(scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO.apply(zeroToInf.negate(), SimpleInterval.of(scalarFive.negate(), scalarFour)));
    assertEquals(SimpleInterval.of(scalarFour.negate(), scalarFour), IIIOperator.MODULO.apply(SimpleInterval.infinite(), SimpleInterval.of(scalarFive.negate(), scalarFour)));
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
    SimpleInterval negInfToFive = SimpleInterval.singleton(BigInteger.valueOf(5)).extendToNegativeInfinity();
    SimpleInterval negInfToNegFive = SimpleInterval.singleton(BigInteger.valueOf(-5)).extendToNegativeInfinity();
    SimpleInterval twentyFiveToInf = SimpleInterval.singleton(BigInteger.valueOf(25)).extendToPositiveInfinity();
    SimpleInterval twentyToTwentyFive = SimpleInterval.of(BigInteger.valueOf(20), BigInteger.valueOf(25));
    SimpleInterval twoToFour = SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4));
    SimpleInterval fortyToHundred = SimpleInterval.of(BigInteger.valueOf(40), BigInteger.valueOf(100));

    assertEquals(negHundredToZero, IIIOperator.MULTIPLY.apply(negTenToZero, zeroToTen));
    assertEquals(negHundredToHundred, IIIOperator.MULTIPLY.apply(negTenToZero, negTenToTen));
    assertEquals(zero, IIIOperator.MULTIPLY.apply(SimpleInterval.infinite(), zero));
    assertEquals(SimpleInterval.infinite(), IIIOperator.MULTIPLY.apply(SimpleInterval.infinite(), zeroToOne));
    assertEquals(SimpleInterval.infinite(), IIIOperator.MULTIPLY.apply(negInfToFive, negInfToFive));
    assertEquals(twentyFiveToInf, IIIOperator.MULTIPLY.apply(negInfToNegFive, negInfToNegFive));
    assertEquals(fortyToHundred, IIIOperator.MULTIPLY.apply(twentyToTwentyFive, twoToFour));
  }

  @Test
  public void testDivide() {
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

    assertEquals(oneToFour, IIIOperator.DIVIDE.apply(negFourToNegTwo, negTwoToNegOne));
    assertEquals(negFourToNegOne, IIIOperator.DIVIDE.apply(negFourToNegTwo, oneToTwo));
    assertEquals(negFourToNegOne, IIIOperator.DIVIDE.apply(twoToFour, negTwoToNegOne));
    assertEquals(oneToFour, IIIOperator.DIVIDE.apply(twoToFour, oneToTwo));
    assertEquals(negTwentyToTwenty, IIIOperator.DIVIDE.apply(negTwentyToTwenty, negTwoToTwo));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE.apply(negInfToTen, negTwoToTwo));
    assertEquals(negInfToFive, IIIOperator.DIVIDE.apply(negInfToTen, twoToFour));
    assertEquals(negInfToZero, IIIOperator.DIVIDE.apply(negInfToNegFive, fiveToInf));
    assertEquals(negTwentyToTen, IIIOperator.DIVIDE.apply(negTwentyToTen, negTwoToInf));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE.apply(SimpleInterval.infinite(), SimpleInterval.infinite()));
    assertNull(IIIOperator.DIVIDE.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.ZERO)));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(5))));
    assertEquals(SimpleInterval.infinite(), IIIOperator.DIVIDE.apply(SimpleInterval.infinite(), SimpleInterval.singleton(BigInteger.valueOf(-5))));
    assertEquals(zeroToInf, IIIOperator.DIVIDE.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(5))));
    assertEquals(negInfToZero, IIIOperator.DIVIDE.apply(zeroToInf, SimpleInterval.singleton(BigInteger.valueOf(-5))));
    assertEquals(negTwentyToTwenty, IIIOperator.DIVIDE.apply(negTwentyToTwenty, zeroToInf));
    assertEquals(oneToFour, IIIOperator.DIVIDE.apply(twoToFour, zeroToTwo));
  }

}
