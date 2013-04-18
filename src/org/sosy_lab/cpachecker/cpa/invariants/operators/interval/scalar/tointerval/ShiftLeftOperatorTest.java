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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;


public class ShiftLeftOperatorTest {

  @Test
  public void test() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval zeroToTen = SimpleInterval.span(zero, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    SimpleInterval two = SimpleInterval.singleton(BigInteger.valueOf(2));
    SimpleInterval oneThousandTwentyFour = SimpleInterval.singleton(BigInteger.valueOf(1024));
    assertEquals(zero, ShiftLeftOperator.INSTANCE.apply(zero, BigInteger.ZERO));
    assertEquals(zero, ShiftLeftOperator.INSTANCE.apply(zero, BigInteger.ONE));
    assertEquals(zero, ShiftLeftOperator.INSTANCE.apply(zero, BigInteger.TEN));
    assertEquals(one, ShiftLeftOperator.INSTANCE.apply(one, BigInteger.ZERO));
    assertEquals(two, ShiftLeftOperator.INSTANCE.apply(one, BigInteger.ONE));
    assertEquals(oneThousandTwentyFour, ShiftLeftOperator.INSTANCE.apply(one, BigInteger.TEN));
    assertEquals(ten, ShiftLeftOperator.INSTANCE.apply(ten, BigInteger.ZERO));
    assertEquals(zeroToTen, ShiftLeftOperator.INSTANCE.apply(zeroToFive, BigInteger.ONE));
  }

}
