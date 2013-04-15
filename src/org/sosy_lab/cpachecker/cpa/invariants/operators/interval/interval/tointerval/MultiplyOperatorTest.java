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

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Unit test for the operator used to multiply simple intervals by simple
 * intervals to produce simple intervals.
 */
public class MultiplyOperatorTest {

  @Test
  public void testApply() {
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

    assertEquals(negHundredToZero, MultiplyOperator.INSTANCE.apply(negTenToZero, zeroToTen));
    assertEquals(negHundredToHundred, MultiplyOperator.INSTANCE.apply(negTenToZero, negTenToTen));
    assertEquals(zero, MultiplyOperator.INSTANCE.apply(SimpleInterval.infinite(), zero));
    assertEquals(SimpleInterval.infinite(), MultiplyOperator.INSTANCE.apply(SimpleInterval.infinite(), zeroToOne));
    assertEquals(SimpleInterval.infinite(), MultiplyOperator.INSTANCE.apply(negInfToFive, negInfToFive));
    assertEquals(twentyFiveToInf, MultiplyOperator.INSTANCE.apply(negInfToNegFive, negInfToNegFive));
    assertEquals(fortyToHundred, MultiplyOperator.INSTANCE.apply(twentyToTwentyFive, twoToFour));
  }

}
