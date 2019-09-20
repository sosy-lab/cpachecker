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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
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
  private ImmutableList<Region[]> numbersNotZero;

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
    numbersNotZero = ImmutableList.of(one, two, n5, n7, n15, neg1, neg3, neg5);
  }

  private void assertIsTrue(Region r) {
    assertThat(r.isTrue()).isTrue();
  }

  private void assertIsFalse(Region r) {
    assertThat(r.isFalse()).isTrue();
  }

  private void assertEqual(Region[] r1, Region[] r2) {
    assertThat(r1).hasLength(r2.length);
    assertWithMessage(toString(r1) + " != " + toString(r2)).that(r2).isEqualTo(r1);
  }

  private void assertDistinct(Region[] r1, Region[] r2) {
    assertThat(r1).hasLength(r2.length);
    boolean distinct = false;
    for (int i = 0; i < r1.length; i++) {
      distinct |= r1[i].equals(r2[i]);
    }
    assertThat(distinct).isTrue();
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
    for (Region[] n : ImmutableList.of(one, two, n5, n7, n15, neg1, neg3, neg5)) {
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
    assume().withMessage("test is designed for a minimal bitsize of 4").that(bitsize).isAtLeast(4);

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

  @Test
  public void shiftLeft() {
    for (Region[] n : numbers) {
      assertEqual(n, bvmgr.makeShiftLeft(n, zero));
      assertEqual(zero, bvmgr.makeShiftLeft(zero, n));
      Region[] twice = bvmgr.makeAdd(n, n);
      assertEqual(twice, bvmgr.makeShiftLeft(n, one));
      assertEqual(bvmgr.makeAdd(twice, twice), bvmgr.makeShiftLeft(n, two));
    }

    assertEqual(n16, bvmgr.makeShiftLeft(one, bvmgr.makeAdd(two, two)));
  }

  @Test
  public void shiftRightUnsigned() {
    for (Region[] n : numbers) {
      assertEqual(n, bvmgr.makeShiftRight(n, zero, false));
      assertEqual(zero, bvmgr.makeShiftRight(zero, n, false));
    }

    assertEqual(one, bvmgr.makeShiftRight(two, one, false));
    assertEqual(one, bvmgr.makeShiftRight(n5, two, false));
    assertEqual(one, bvmgr.makeShiftRight(n7, two, false));
  }

  @Test
  public void shiftRightSigned() {
    for (Region[] n : numbers) {
      assertEqual(n, bvmgr.makeShiftRight(n, zero, true));
      assertEqual(zero, bvmgr.makeShiftRight(zero, n, true));
      Region[] minus1 = bvmgr.makeSub(zero, one);
      assertEqual(minus1, bvmgr.makeShiftRight(minus1, n, true));
    }

    assertEqual(one, bvmgr.makeShiftRight(two, one, true));
    assertEqual(one, bvmgr.makeShiftRight(n5, two, true));
    assertEqual(one, bvmgr.makeShiftRight(n7, two, true));
  }

  @Test
  public void mult() {
    for (Region[] n : numbers) {
      assertEqual(zero, bvmgr.makeMult(n, zero));
      assertEqual(zero, bvmgr.makeMult(zero, n));
      assertEqual(n, bvmgr.makeMult(n, one));
      assertEqual(n, bvmgr.makeMult(one, n));
    }

    assertEqual(n16, bvmgr.makeMult(bvmgr.makeMult(two, two), bvmgr.makeMult(two, two)));

    assertEqual(n15, bvmgr.makeMult(neg3, neg5));
    assertEqual(n15, bvmgr.makeMult(neg5, neg3));

    assertEqual(bvmgr.makeMult(neg1, neg25), bvmgr.makeMult(neg5, neg5));

    assertEqual(neg35, bvmgr.makeMult(neg5, n7));
    assertEqual(neg35, bvmgr.makeMult(n7, neg5));
  }

  @Test
  public void divSigned() {
    for (Region[] n : numbersNotZero) {
      assertEqual(zero, bvmgr.makeDiv(zero, n, true));
      assertEqual(n, bvmgr.makeDiv(n, one, true));
      assertEqual(bvmgr.makeMult(neg1, n), bvmgr.makeDiv(n, neg1, true));
    }

    assertEqual(zero, bvmgr.makeDiv(two, neg3, true));
    assertEqual(neg1, bvmgr.makeDiv(bvmgr.makeAdd(two, two), neg3, true));
    assertEqual(zero, bvmgr.makeDiv(bvmgr.makeAdd(two, two), neg5, true));
    assertEqual(two, bvmgr.makeDiv(bvmgr.makeAdd(two, two), two, true));
    if (bitsize > 6) {
      assertEqual(neg3, bvmgr.makeDiv(n15, neg5, true));
      assertEqual(neg5, bvmgr.makeDiv(n15, neg3, true));
      assertEqual(neg5, bvmgr.makeDiv(neg25, n5, true));
      assertEqual(n5, bvmgr.makeDiv(neg25, neg5, true));
      assertEqual(n7, bvmgr.makeDiv(neg35, neg5, true));
    }
  }

  @Test
  public void divUnsigned() {
    for (Region[] n : numbersNotZero) {
      assertEqual(zero, bvmgr.makeDiv(zero, n, false));
      assertEqual(n, bvmgr.makeDiv(n, one, false));
    }

    assertEqual(zero, bvmgr.makeDiv(two, neg3, false));
    assertEqual(zero, bvmgr.makeDiv(bvmgr.makeAdd(two, two), neg5, false));
    assertEqual(two, bvmgr.makeDiv(bvmgr.makeAdd(two, two), two, false));
  }

  /** check some special values that are more or less undefined by the C99 standard. */
  @Test
  public void divModSpecial() {
    assertEqual(neg1, bvmgr.makeDiv(n5, zero, false));
    assertEqual(neg1, bvmgr.makeDiv(n5, zero, true));
    assertEqual(n5, bvmgr.makeMod(n5, zero, false));
    assertEqual(n5, bvmgr.makeMod(n5, zero, true));

    assertEqual(neg1, bvmgr.makeDiv(neg5, zero, false));
    assertEqual(one, bvmgr.makeDiv(neg5, zero, true));
    assertEqual(neg5, bvmgr.makeMod(neg5, zero, false));
    assertEqual(neg5, bvmgr.makeMod(neg5, zero, true));

    final Region[] big = bvmgr.makeNumber(1 << (bitsize - 1), bitsize);

    assertEqual(neg1, bvmgr.makeDiv(big, zero, false));
    assertEqual(one, bvmgr.makeDiv(big, zero, true));
    assertEqual(big, bvmgr.makeMod(big, zero, false));
    assertEqual(big, bvmgr.makeMod(big, zero, true));

    assertEqual(zero, bvmgr.makeDiv(big, neg1, false));
    assertEqual(big, bvmgr.makeDiv(big, neg1, true));
    assertEqual(big, bvmgr.makeMod(big, neg1, false));
    assertEqual(zero, bvmgr.makeMod(big, neg1, true));
  }

  @Test
  public void modSigned() {
    for (Region[] n : numbersNotZero) {
      assertEqual(zero, bvmgr.makeMod(n, n, true));
      assertEqual(zero, bvmgr.makeMod(zero, n, true));
      assertEqual(zero, bvmgr.makeMod(n, one, true));
    }
    if (bitsize > 6) {
      assertEqual(zero, bvmgr.makeMod(neg25, n5, true));
      assertEqual(zero, bvmgr.makeMod(neg35, n5, true));
      assertEqual(zero, bvmgr.makeMod(neg25, neg5, true));
      assertEqual(zero, bvmgr.makeMod(neg35, neg5, true));
    }
  }

  @Test
  public void modUnsigned() {
    for (Region[] n : numbersNotZero) {
      assertEqual(zero, bvmgr.makeMod(n, n, false));
      assertEqual(zero, bvmgr.makeMod(zero, n, false));
      assertEqual(zero, bvmgr.makeMod(n, one, false));
    }

    assertEqual(zero, bvmgr.makeMod(bvmgr.makeAdd(two, two), two, false));
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
