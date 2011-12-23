/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import static junit.framework.Assert.assertNotNull;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.*;

import java.math.BigInteger;

import org.junit.Test;

public class SimpleIntervalTest {

  @Test
  public void testConstruction() {
    assertNotNull(singleton(BigInteger.ZERO));
    assertNotNull(singleton(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(singleton(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(lessOrEqual(BigInteger.ZERO));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(greaterOrEqual(BigInteger.ZERO));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(of(BigInteger.ZERO, BigInteger.ZERO));
    assertNotNull(of(BigInteger.ZERO, BigInteger.ONE));
    assertNotNull(of(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testInvalidConstruction1() {
    of(BigInteger.ONE, BigInteger.ZERO);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testInvalidConstruction2() {
    of(BigInteger.valueOf(Long.MAX_VALUE), BigInteger.valueOf(Long.MIN_VALUE));
  }
}
