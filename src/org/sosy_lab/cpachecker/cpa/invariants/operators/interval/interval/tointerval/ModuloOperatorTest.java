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
 * Unit test for the operator used to compute the remainder of the division
 * of one simple interval by another simple interval as a simple interval.
 */
public class ModuloOperatorTest {

  @Test
  public void testApply() {
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

    assertEquals(negFourToFour, IIIOperator.MODULO_OPERATOR.apply(SimpleInterval.infinite(), five));
    assertEquals(zeroToFour, IIIOperator.MODULO_OPERATOR.apply(zeroToInf, five));
    assertEquals(zeroToFour, IIIOperator.MODULO_OPERATOR.apply(zeroToInf, five.negate()));
    assertEquals(twoToThree, IIIOperator.MODULO_OPERATOR.apply(tenToEleven, SimpleInterval.singleton(scalarFour)));
    assertEquals(zeroToTwo, IIIOperator.MODULO_OPERATOR.apply(eightToTen, SimpleInterval.singleton(scalarFour)));
    assertEquals(twoToThree, IIIOperator.MODULO_OPERATOR.apply(tenToEleven, SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToTwo, IIIOperator.MODULO_OPERATOR.apply(eightToTen, SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(twoToThree.negate(), IIIOperator.MODULO_OPERATOR.apply(tenToEleven.negate(), SimpleInterval.singleton(scalarFour)));
    assertEquals(zeroToTwo.negate(), IIIOperator.MODULO_OPERATOR.apply(eightToTen.negate(), SimpleInterval.singleton(scalarFour)));
    assertEquals(twoToThree.negate(), IIIOperator.MODULO_OPERATOR.apply(tenToEleven.negate(), SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToTwo.negate(), IIIOperator.MODULO_OPERATOR.apply(eightToTen.negate(), SimpleInterval.singleton(scalarFour).negate()));
    assertEquals(zeroToFour, IIIOperator.MODULO_OPERATOR.apply(zeroToInf, SimpleInterval.of(BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour, IIIOperator.MODULO_OPERATOR.apply(zeroToInf, SimpleInterval.of(scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour, IIIOperator.MODULO_OPERATOR.apply(zeroToInf, SimpleInterval.of(scalarFive.negate(), scalarFour)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO_OPERATOR.apply(zeroToInf.negate(), SimpleInterval.of(BigInteger.ZERO, scalarFive)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO_OPERATOR.apply(zeroToInf.negate(), SimpleInterval.of(scalarFour.negate(), scalarFive)));
    assertEquals(zeroToFour.negate(), IIIOperator.MODULO_OPERATOR.apply(zeroToInf.negate(), SimpleInterval.of(scalarFive.negate(), scalarFour)));
    assertEquals(SimpleInterval.of(scalarFour.negate(), scalarFour), IIIOperator.MODULO_OPERATOR.apply(SimpleInterval.infinite(), SimpleInterval.of(scalarFive.negate(), scalarFour)));
  }

}
