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
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

@RunWith(Parameterized.class)
public class BitvectorManagerTest {

  private LogManager logger;

  private RegionManager rmgr;
  private BitvectorManager bvmgr;
  private Region[] zero;
  private Region[] one;
  private Region[] two;
  private Region[] n5;
  private Region[] n7;
  private Region[] n15;
  private Region[] n16;
  private Region[] neg1;
  private Region[] neg3;
  private Region[] neg5;
  private Region[] neg25;
  private Region[] neg35;

  private ImmutableList<Region[]> numbers;

  private int bitsize;

  @Parameters
  public static Collection<Integer> bitsize() {
    return ImmutableList.of(4, 5, 6, 8, 10, 12, 16, 32);
  }

  public BitvectorManagerTest(int pBitsize) {
    bitsize = pBitsize;
  }

  @Before
  public void init() throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();
    logger = LogManager.createTestLogManager();

    rmgr = new BDDManagerFactory(config, logger).createRegionManager();
    bvmgr = new BitvectorManager(rmgr);

    zero = bvmgr.makeNumber(BigInteger.ZERO, bitsize);
    one = bvmgr.makeNumber(BigInteger.ONE, bitsize);
    two = bvmgr.makeNumber(BigInteger.valueOf(2), bitsize);
    n5 = bvmgr.makeNumber(BigInteger.valueOf(5), bitsize);
    n7 = bvmgr.makeNumber(BigInteger.valueOf(7), bitsize);
    n15 = bvmgr.makeNumber(BigInteger.valueOf(15), bitsize);
    n16 = bvmgr.makeNumber(BigInteger.valueOf(16), bitsize);
    neg1 = bvmgr.makeNumber(BigInteger.valueOf(-1), bitsize);
    neg3 = bvmgr.makeNumber(BigInteger.valueOf(-3), bitsize);
    neg5 = bvmgr.makeNumber(BigInteger.valueOf(-5), bitsize);
    neg25 = bvmgr.makeNumber(BigInteger.valueOf(-25), bitsize);
    neg35 = bvmgr.makeNumber(BigInteger.valueOf(-35), bitsize);

    numbers = ImmutableList.of(zero, one, two, n5, n7, n15, n16, neg1, neg3, neg5, neg25, neg35);
  }

  private void assertIsTrue(Region r) {
    Assert.assertTrue(r.isTrue());
  }

  private void assertIsFalse(Region r) {
    Assert.assertTrue(r.isFalse());
  }

  private void assertEqual(Region[] r1, Region[] r2) {
    Assert.assertTrue(r1.length == r2.length);
    Assert.assertArrayEquals(toString(r1) + " != " + toString(r2), r1, r2);
  }

  private void assertDistinct(Region[] r1, Region[] r2) {
    Assert.assertTrue(r1.length == r2.length);
    boolean distinct = false;
    for (int i = 0; i < r1.length; i++) {
      distinct |= r1[i].equals(r2[i]);
    }
    Assert.assertTrue(distinct);
  }

  @Test
  public void selfTest() {
    assertDistinct(zero, one);
    assertDistinct(neg1, one);

    for (int i = 0; i < bitsize; i++) {
      assertIsFalse(zero[i]);
    }
    assertIsTrue(one[0]);
    for (int i = 1; i < bitsize; i++) {
      assertIsFalse(one[i]);
    }
  }

  @Test(expected = AssertionError.class)
  public void differentLen() {
    bvmgr.makeAdd(bvmgr.makeNumber(BigInteger.ONE, 3), bvmgr.makeNumber(BigInteger.ONE, 4));
  }

  @Test
  public void addsub() {
    assertEqual(one, bvmgr.makeAdd(zero, one));
    assertEqual(two, bvmgr.makeAdd(one, one));
    assertEqual(two, bvmgr.makeSub(neg3, neg5));
    assertEqual(neg5, bvmgr.makeSub(neg3, two));

    for (Region[] m : numbers) {
      assertEqual(m, bvmgr.makeAdd(m, zero));
      assertEqual(zero, bvmgr.makeSub(m, m));
      for (Region[] n : numbers) {
        assertEqual(m, bvmgr.makeSub(bvmgr.makeAdd(m, n), n));
        assertEqual(m, bvmgr.makeSub(bvmgr.makeAdd(n, m), n));
        assertEqual(m, bvmgr.makeAdd(bvmgr.makeSub(m, n), n));
        assertEqual(m, bvmgr.makeAdd(n, bvmgr.makeSub(m, n)));
      }
    }
  }

  @Test
  public void bool() {
    for (Region[] n : ImmutableList.of(one, two, n15, neg1, neg3, neg5)) {
      assertIsFalse(bvmgr.makeNot(n));
    }
    assertIsTrue(bvmgr.makeNot(zero));

    assertEqual(zero, bvmgr.makeBinaryAnd(one, two));
    assertEqual(zero, bvmgr.makeBinaryAnd(one, zero));
    assertEqual(one, bvmgr.makeBinaryAnd(one, one));
    assertEqual(one, bvmgr.makeBinaryAnd(one, bvmgr.makeAdd(one, two)));

    assertIsTrue(bvmgr.makeLogicalEqual(one, one));
    assertIsTrue(bvmgr.makeLogicalEqual(two, two));
    assertEqual(bvmgr.makeBinaryEqual(one, one), bvmgr.makeSub(zero, one));
    assertEqual(bvmgr.makeBinaryEqual(zero, zero), bvmgr.makeSub(one, two));
    assertEqual(bvmgr.makeXor(one, two), bvmgr.makeAdd(one, two));
    assertEqual(bvmgr.makeXor(one, zero), one);
  }

  /** compares operands modulo 16 */
  private void assertEqualwithLen4(Region[] a, Region[] b) {
    assertEqual(bvmgr.toBitsize(4, true, a), bvmgr.toBitsize(4, true, b));
  }

  @Test
  public void signedLen4() {
    assert bitsize >= 4 : "test is designed for a minimal bitsize of 4";

    assertEqualwithLen4(zero, bvmgr.makeAdd(neg1, one));
    assertEqualwithLen4(one, bvmgr.makeAdd(neg1, two));

    assertEqualwithLen4(neg1, bvmgr.makeSub(zero, one));
    assertEqualwithLen4(neg1, bvmgr.makeSub(one, two));

    assertEqualwithLen4(neg1, n15);

    Region[] sum = one;
    for (int i = 0; i < 4; i++) {
      sum = bvmgr.makeAdd(sum, sum);
    }
    assertEqualwithLen4(zero, sum); // 0 == 16 == 1*2*2*2*2 modulo 16
  }

  @Test
  public void lessSigned() {
    for (Region[] n : numbers) {
      assertIsFalse(bvmgr.makeLess(n, n, true));
    }

    assertIsFalse(bvmgr.makeLess(one, zero, true));
    assertIsTrue(bvmgr.makeLess(zero, one, true));

    assertIsTrue(bvmgr.makeLess(neg1, zero, true));
    assertIsFalse(bvmgr.makeLess(zero, neg1, true));
  }

  @Test
  public void lessUnsigned() {
    for (Region[] n : numbers) {
      assertIsFalse(bvmgr.makeLess(n, n, false));
    }

    assertIsFalse(bvmgr.makeLess(one, zero, false));
    assertIsTrue(bvmgr.makeLess(zero, one, false));
    assertIsTrue(bvmgr.makeLess(zero, two, false));
    assertIsTrue(bvmgr.makeLess(zero, n5, false));
    assertIsTrue(bvmgr.makeLess(zero, n7, false));

    assertIsFalse(bvmgr.makeLess(neg1, zero, false));
    assertIsTrue(bvmgr.makeLess(zero, neg1, false));
  }

  @Test
  public void lessOrEqualSigned() {
    for (Region[] n : numbers) {
      assertIsTrue(bvmgr.makeLessOrEqual(n, n, true));
    }

    assertIsFalse(bvmgr.makeLessOrEqual(one, zero, true));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, one, true));

    assertIsTrue(bvmgr.makeLessOrEqual(neg1, zero, true));
    assertIsFalse(bvmgr.makeLessOrEqual(zero, neg1, true));
  }

  @Test
  public void lessOrEqualUnsigned() {
    for (Region[] n : numbers) {
      assertIsTrue(bvmgr.makeLessOrEqual(n, n, false));
    }

    assertIsFalse(bvmgr.makeLessOrEqual(one, zero, false));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, one, false));

    assertIsFalse(bvmgr.makeLessOrEqual(neg1, zero, false));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, neg1, false));
  }

  private static String toString(Region[] r) {
    StringBuilder str = new StringBuilder("[ ");
    for (int i = r.length - 1; i >= 0; i--) {
      Region bit = r[i];
      str.append(bit.isFalse() ? "0" : (bit.isTrue() ? "1" : bit));
    }
    return str.append(" ]").toString();
  }
}
