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

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;


public class ISIOperatorTest {

  @Test
  public void testModulo() {
    BigInteger scalarFour = BigInteger.valueOf(4);
    BigInteger scalarFive = BigInteger.valueOf(5);
    SimpleInterval zeroToFour = SimpleInterval.of(BigInteger.ZERO, scalarFour);
    SimpleInterval zeroToInf = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
    assertEquals(zeroToFour.negate(), ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive));
    assertEquals(zeroToFour.negate(), ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive.negate()));
  }

  @Test
  public void testShiftLeft() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval zeroToTen = SimpleInterval.span(zero, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    SimpleInterval two = SimpleInterval.singleton(BigInteger.valueOf(2));
    SimpleInterval oneThousandTwentyFour = SimpleInterval.singleton(BigInteger.valueOf(1024));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.TEN));
    assertEquals(one, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ZERO));
    assertEquals(two, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ONE));
    assertEquals(oneThousandTwentyFour, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.TEN));
    assertEquals(ten, ISIOperator.SHIFT_LEFT.apply(ten, BigInteger.ZERO));
    assertEquals(zeroToTen, ISIOperator.SHIFT_LEFT.apply(zeroToFive, BigInteger.ONE));
  }

  @Test
  public void testShiftRight() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval oneToTen = SimpleInterval.span(one, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.TEN));
    assertEquals(one, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.TEN));
    assertEquals(ten, ISIOperator.SHIFT_RIGHT.apply(ten, BigInteger.ZERO));
    assertEquals(zeroToFive, ISIOperator.SHIFT_RIGHT.apply(oneToTen, BigInteger.ONE));
  }

}
