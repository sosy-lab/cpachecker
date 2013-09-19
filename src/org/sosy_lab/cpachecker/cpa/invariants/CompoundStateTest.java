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
package org.sosy_lab.cpachecker.cpa.invariants;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;


public class CompoundStateTest {

  private final SimpleInterval oneToFiveInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5));

  private final SimpleInterval sixToTenInterval = SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));

  private final SimpleInterval oneToTenInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.TEN);

  private final SimpleInterval negInfToZeroInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToNegativeInfinity();

  private final SimpleInterval zeroToPosInfInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();

  @Test
  public void testIsTop() {
    assertFalse(CompoundState.of(oneToTenInterval).isTop());
    assertFalse(CompoundState.of(negInfToZeroInterval).isTop());
    assertFalse(CompoundState.of(zeroToPosInfInterval).isTop());
    assertFalse(CompoundState.bottom().isTop());
    assertTrue(CompoundState.top().isTop());
  }

  @Test
  public void testIsBottom() {
    assertFalse(CompoundState.of(oneToTenInterval).isBottom());
    assertFalse(CompoundState.of(negInfToZeroInterval).isBottom());
    assertFalse(CompoundState.of(zeroToPosInfInterval).isBottom());
    assertFalse(CompoundState.top().isBottom());
    assertTrue(CompoundState.bottom().isBottom());
  }

  @Test
  public void testUnionWith() {
    assertTrue(CompoundState.of(negInfToZeroInterval).unionWith(zeroToPosInfInterval).isTop());
    assertTrue(CompoundState.of(negInfToZeroInterval).unionWith(CompoundState.of(zeroToPosInfInterval)).isTop());
    assertTrue(CompoundState.bottom().unionWith(CompoundState.top()).isTop());
    assertFalse(CompoundState.of(negInfToZeroInterval).unionWith(CompoundState.of(oneToTenInterval)).isTop());
    assertEquals(CompoundState.of(oneToFiveInterval).unionWith(sixToTenInterval), CompoundState.of(oneToTenInterval));
    assertEquals(CompoundState.of(oneToTenInterval).unionWith(sixToTenInterval), CompoundState.of(oneToTenInterval));
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    assertEquals(1, CompoundState.of(zero.extendToNegativeInfinity()).unionWith(zero.extendToPositiveInfinity()).getIntervals().size());
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundState zeroToThree = CompoundState.of(zeroToThreeInterval);
    CompoundState six = CompoundState.singleton(6);
    assertEquals(2, zeroToThree.unionWith(six).getIntervals().size());
    assertEquals(zeroToThree.unionWith(six), six.unionWith(zeroToThree));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundState.singleton(0)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundState.singleton(1)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundState.singleton(2)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundState.singleton(3)));
    assertEquals(zeroToThree, zeroToThree.unionWith(zeroToThree));
  }

  @Test
  public void testIntersectWith() {
    assertEquals(CompoundState.of(SimpleInterval.singleton(BigInteger.ZERO)),
        CompoundState.of(negInfToZeroInterval).intersectWith(zeroToPosInfInterval));
    assertEquals(CompoundState.of(SimpleInterval.singleton(BigInteger.ZERO)),
        CompoundState.of(negInfToZeroInterval).intersectWith(CompoundState.of(zeroToPosInfInterval)));
    assertTrue(CompoundState.bottom().intersectWith(CompoundState.top()).isBottom());
    assertEquals(CompoundState.of(oneToTenInterval), CompoundState.top().intersectWith(CompoundState.of(oneToTenInterval)));
    SimpleInterval oneToTwo = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(2));
    CompoundState notTwo = CompoundState.singleton(2).invert();
    assertEquals(CompoundState.singleton(1), notTwo.intersectWith(CompoundState.of(oneToTwo)));
  }

  @Test
  public void testInvert() {
    assertEquals(CompoundState.bottom(), CompoundState.top().invert());
    assertEquals(CompoundState.top(), CompoundState.bottom().invert());
    CompoundState negInfToTen = CompoundState.singleton(10).extendToNegativeInfinity();
    CompoundState elevenToInf = CompoundState.singleton(11).extendToPositiveInfinity();
    assertEquals(elevenToInf, negInfToTen.invert());
    assertEquals(negInfToTen, elevenToInf.invert());
    assertEquals(negInfToTen, negInfToTen.invert().invert());
    for (int i = -1; i < 2; ++i) {
      CompoundState invertedState = CompoundState.singleton(i).invert();
      assertFalse(invertedState.contains(i));
      assertFalse(invertedState.hasLowerBound());
      assertFalse(invertedState.hasUpperBound());
      assertFalse(invertedState.isTop());
      assertFalse(invertedState.isBottom());
      assertTrue(invertedState.contains(i - 1));
      assertTrue(invertedState.contains(i + 1));
    }
    assertEquals(CompoundState.singleton(0).extendToNegativeInfinity().unionWith(CompoundState.singleton(6)),
        CompoundState.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5))).unionWith(CompoundState.singleton(7).extendToPositiveInfinity()).invert());
  }

  @Test
  public void moduloTest() {
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundState zeroToThree = CompoundState.of(zeroToThreeInterval);
    CompoundState six = CompoundState.singleton(6);
    SimpleInterval sixToTenInterval = SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));
    CompoundState sixToTen = CompoundState.of(sixToTenInterval);
    assertEquals(zeroToThree.unionWith(six), sixToTen.modulo(BigInteger.valueOf(7)));
    assertEquals(zeroToThree.unionWith(six).negate(), sixToTen.negate().modulo(BigInteger.valueOf(7)));
    assertEquals(zeroToThree.unionWith(six), sixToTen.modulo(BigInteger.valueOf(7).negate()));
    assertEquals(zeroToThree.unionWith(six).negate(), sixToTen.negate().modulo(BigInteger.valueOf(7).negate()));
  }

  @Test
  public void testNegate() {
    CompoundState one = CompoundState.singleton(1);
    assertEquals(CompoundState.singleton(-1), one.negate());
    CompoundState twoToFour = CompoundState.of(SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundState negTwoToNegOne = CompoundState.of(SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundState negFourToNegTwo = CompoundState.of(SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundState oneToTwo = CompoundState.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertEquals(oneToTwo.unionWith(negFourToNegTwo), negTwoToNegOne.unionWith(twoToFour).negate());
  }

  @Test
  public void testIsSingleton() {
    CompoundState negOne = CompoundState.singleton(-1);
    CompoundState zero = CompoundState.singleton(0);
    CompoundState one = CompoundState.singleton(1);
    CompoundState ten = CompoundState.singleton(10);
    assertTrue(negOne.isSingleton());
    assertTrue(zero.isSingleton());
    assertTrue(one.isSingleton());
    assertTrue(ten.isSingleton());
    assertFalse(CompoundState.span(one, ten).isSingleton());
    assertFalse(zero.unionWith(ten).isSingleton());
    assertFalse(negOne.unionWith(CompoundState.span(one, ten)).isSingleton());
  }

  @Test
  public void containsTest() {
    assertTrue(CompoundState.singleton(-1).contains(-1));
    assertTrue(CompoundState.singleton(0).contains(0));
    assertTrue(CompoundState.singleton(1).contains(1));
    assertTrue(CompoundState.singleton(-1).contains(CompoundState.singleton(-1)));
    assertTrue(CompoundState.singleton(0).contains(CompoundState.singleton(0)));
    assertTrue(CompoundState.singleton(1).contains(CompoundState.singleton(1)));
    assertTrue(CompoundState.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(CompoundState.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN))));
    assertFalse(CompoundState.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN)).contains(CompoundState.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))));
    assertTrue(CompoundState.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(5));
    assertFalse(CompoundState.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(-1));
    assertFalse(CompoundState.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(4))).unionWith(SimpleInterval.of(BigInteger.valueOf(6), BigInteger.TEN)).contains(5));
  }

}
