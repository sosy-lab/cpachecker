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
package org.sosy_lab.cpachecker.cpa.invariants;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.List;

import org.junit.Test;


public class CompoundIntervalTest {

  private final SimpleInterval oneToFiveInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5));

  private final SimpleInterval sixToTenInterval = SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));

  private final SimpleInterval oneToTenInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.TEN);

  private final SimpleInterval negInfToZeroInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToNegativeInfinity();

  private final SimpleInterval zeroToPosInfInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();

  @Test
  public void testIsTop() {
    assertFalse(CompoundInterval.of(oneToTenInterval).isTop());
    assertFalse(CompoundInterval.of(negInfToZeroInterval).isTop());
    assertFalse(CompoundInterval.of(zeroToPosInfInterval).isTop());
    assertFalse(CompoundInterval.bottom().isTop());
    assertTrue(CompoundInterval.top().isTop());
  }

  @Test
  public void testIsBottom() {
    assertFalse(CompoundInterval.of(oneToTenInterval).isBottom());
    assertFalse(CompoundInterval.of(negInfToZeroInterval).isBottom());
    assertFalse(CompoundInterval.of(zeroToPosInfInterval).isBottom());
    assertFalse(CompoundInterval.top().isBottom());
    assertTrue(CompoundInterval.bottom().isBottom());
  }

  @Test
  public void testUnionWith() {
    assertTrue(CompoundInterval.of(negInfToZeroInterval).unionWith(zeroToPosInfInterval).isTop());
    assertTrue(CompoundInterval.of(negInfToZeroInterval).unionWith(CompoundInterval.of(zeroToPosInfInterval)).isTop());
    assertTrue(CompoundInterval.bottom().unionWith(CompoundInterval.top()).isTop());
    assertFalse(CompoundInterval.of(negInfToZeroInterval).unionWith(CompoundInterval.of(oneToTenInterval)).isTop());
    assertEquals(CompoundInterval.of(oneToFiveInterval).unionWith(sixToTenInterval), CompoundInterval.of(oneToTenInterval));
    assertEquals(CompoundInterval.of(oneToTenInterval).unionWith(sixToTenInterval), CompoundInterval.of(oneToTenInterval));
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    assertEquals(1, CompoundInterval.of(zero.extendToNegativeInfinity()).unionWith(zero.extendToPositiveInfinity()).getIntervals().size());
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundInterval zeroToThree = CompoundInterval.of(zeroToThreeInterval);
    CompoundInterval six = CompoundInterval.singleton(6);
    assertEquals(2, zeroToThree.unionWith(six).getIntervals().size());
    assertEquals(zeroToThree.unionWith(six), six.unionWith(zeroToThree));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundInterval.singleton(0)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundInterval.singleton(1)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundInterval.singleton(2)));
    assertEquals(zeroToThree, zeroToThree.unionWith(CompoundInterval.singleton(3)));
    assertEquals(zeroToThree, zeroToThree.unionWith(zeroToThree));

    CompoundInterval steps = CompoundInterval.bottom();
    for (int i = -6; i <= 6; i += 2) {
      steps = steps.unionWith(CompoundInterval.singleton(i));
    }
    steps = steps.extendToNegativeInfinity().extendToPositiveInfinity();
    for (int i = -6; i <= 6; i += 2) {
      assertTrue(steps.contains(i));
    }
    CompoundInterval stepsNegInf = steps.unionWith(CompoundInterval.singleton(BigInteger.valueOf(-4)).extendToNegativeInfinity());
    for (int i = -6; i <= 6; i += 2) {
      assertTrue(stepsNegInf.contains(i));
    }

    CompoundInterval zeroOrTenToInf = CompoundInterval.singleton(0).unionWith(CompoundInterval.singleton(10).extendToPositiveInfinity());
    CompoundInterval negOne = CompoundInterval.singleton(-1);
    CompoundInterval negOneOrZeroOrTenToInf = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-1), BigInteger.ZERO)).unionWith(CompoundInterval.singleton(10).extendToPositiveInfinity());
    assertEquals(negOneOrZeroOrTenToInf, zeroOrTenToInf.unionWith(negOne));

    assertEquals(CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-1), BigInteger.valueOf(1))), negOne.unionWith(CompoundInterval.one()).unionWith(zero));
    assertEquals(1, negOne.unionWith(CompoundInterval.one()).unionWith(zero).getIntervals().size());
  }

  @Test
  public void testIntersectWith() {
    assertEquals(CompoundInterval.of(SimpleInterval.singleton(BigInteger.ZERO)),
        CompoundInterval.of(negInfToZeroInterval).intersectWith(zeroToPosInfInterval));
    assertEquals(CompoundInterval.of(SimpleInterval.singleton(BigInteger.ZERO)),
        CompoundInterval.of(negInfToZeroInterval).intersectWith(CompoundInterval.of(zeroToPosInfInterval)));
    assertTrue(CompoundInterval.bottom().intersectWith(CompoundInterval.top()).isBottom());
    assertEquals(CompoundInterval.of(oneToTenInterval), CompoundInterval.top().intersectWith(CompoundInterval.of(oneToTenInterval)));
    SimpleInterval oneToTwo = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(2));
    CompoundInterval notTwo = CompoundInterval.singleton(2).invert();
    assertEquals(CompoundInterval.singleton(1), notTwo.intersectWith(CompoundInterval.of(oneToTwo)));
  }

  @Test
  public void testInvert() {
    assertEquals(CompoundInterval.bottom(), CompoundInterval.top().invert());
    assertEquals(CompoundInterval.top(), CompoundInterval.bottom().invert());
    CompoundInterval negInfToTen = CompoundInterval.singleton(10).extendToNegativeInfinity();
    CompoundInterval elevenToInf = CompoundInterval.singleton(11).extendToPositiveInfinity();
    assertEquals(elevenToInf, negInfToTen.invert());
    assertEquals(negInfToTen, elevenToInf.invert());
    assertEquals(negInfToTen, negInfToTen.invert().invert());
    for (int i = -1; i < 2; ++i) {
      CompoundInterval invertedState = CompoundInterval.singleton(i).invert();
      assertFalse(invertedState.contains(i));
      assertFalse(invertedState.hasLowerBound());
      assertFalse(invertedState.hasUpperBound());
      assertFalse(invertedState.isTop());
      assertFalse(invertedState.isBottom());
      assertTrue(invertedState.contains(i - 1));
      assertTrue(invertedState.contains(i + 1));
    }
    assertEquals(CompoundInterval.singleton(0).extendToNegativeInfinity().unionWith(CompoundInterval.singleton(6)),
        CompoundInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5))).unionWith(CompoundInterval.singleton(7).extendToPositiveInfinity()).invert());
  }

  @Test
  public void moduloTest() {
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundInterval zeroToThree = CompoundInterval.of(zeroToThreeInterval);
    CompoundInterval six = CompoundInterval.singleton(6);
    SimpleInterval sixToTenInterval = SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));
    CompoundInterval sixToTen = CompoundInterval.of(sixToTenInterval);
    assertEquals(zeroToThree.unionWith(six), sixToTen.modulo(BigInteger.valueOf(7)));
    assertEquals(zeroToThree.unionWith(six).negate(), sixToTen.negate().modulo(BigInteger.valueOf(7)));
    assertEquals(zeroToThree.unionWith(six), sixToTen.modulo(BigInteger.valueOf(7).negate()));
    assertEquals(zeroToThree.unionWith(six).negate(), sixToTen.negate().modulo(BigInteger.valueOf(7).negate()));
  }

  @Test
  public void testNegate() {
    assertEquals(CompoundInterval.singleton(-1), CompoundInterval.one().negate());
    CompoundInterval twoToFour = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundInterval negTwoToNegOne = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundInterval negFourToNegTwo = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundInterval oneToTwo = CompoundInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertEquals(oneToTwo.unionWith(negFourToNegTwo), negTwoToNegOne.unionWith(twoToFour).negate());
  }

  @Test
  public void testIsSingleton() {
    CompoundInterval negOne = CompoundInterval.singleton(-1);
    CompoundInterval zero = CompoundInterval.singleton(0);
    CompoundInterval ten = CompoundInterval.singleton(10);
    assertTrue(negOne.isSingleton());
    assertTrue(zero.isSingleton());
    assertTrue(CompoundInterval.one().isSingleton());
    assertTrue(ten.isSingleton());
    assertFalse(CompoundInterval.span(CompoundInterval.one(), ten).isSingleton());
    assertFalse(zero.unionWith(ten).isSingleton());
    assertFalse(negOne.unionWith(CompoundInterval.span(CompoundInterval.one(), ten)).isSingleton());
  }

  @Test
  public void containsTest() {
    assertTrue(CompoundInterval.singleton(-1).contains(-1));
    assertTrue(CompoundInterval.singleton(0).contains(0));
    assertTrue(CompoundInterval.one().contains(1));
    assertTrue(CompoundInterval.singleton(-1).contains(CompoundInterval.singleton(-1)));
    assertTrue(CompoundInterval.singleton(0).contains(CompoundInterval.singleton(0)));
    assertTrue(CompoundInterval.singleton(1).contains(CompoundInterval.singleton(1)));
    assertTrue(CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(CompoundInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN))));
    assertFalse(CompoundInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN)).contains(CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))));
    assertTrue(CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(5));
    assertFalse(CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(-1));
    assertFalse(CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(4))).unionWith(SimpleInterval.of(BigInteger.valueOf(6), BigInteger.TEN)).contains(5));
  }

  @Test
  public void binaryNotTest() {
    CompoundInterval.singleton(1).extendToNegativeInfinity().binaryNot();
  }

  @Test
  public void testMultiply() {
    CompoundInterval topMultNeg2 = CompoundInterval.top().multiply(BigInteger.valueOf(-2));
    List<SimpleInterval> intervals = topMultNeg2.getIntervals();
    int i = 0;
    BigInteger lastUpperBound = null;
    for (SimpleInterval interval : intervals) {
      if (i == 0) {
        assertFalse(interval.hasLowerBound());
      } else {
        assertTrue(interval.hasLowerBound());
        // Check that intervals to not overlap, touch or are in the wrong order
        assertTrue(interval.getLowerBound().subtract(lastUpperBound).compareTo(BigInteger.ONE) > 0);
      }
      if (i == intervals.size() - 1) {
        assertFalse(interval.hasUpperBound());
      }
      if (interval.hasUpperBound()) {
        lastUpperBound = interval.getUpperBound();
      }
      ++i;
    }
    assertEquals(topMultNeg2, topMultNeg2.unionWith(topMultNeg2));
  }

  @Test
  public void testBinaryXor() {
    CompoundInterval zeroToOne = CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundInterval one = CompoundInterval.one();
    assertEquals(zeroToOne, zeroToOne.binaryXor(one));
    assertEquals(zeroToOne, one.binaryXor(zeroToOne));
  }

  @Test
  public void testBinaryAnd() {
    CompoundInterval zeroToOne = CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundInterval one = CompoundInterval.one();
    assertEquals(zeroToOne, CompoundInterval.top().binaryAnd(one));
    assertEquals(zeroToOne, one.binaryAnd(CompoundInterval.top()));
    CompoundInterval.top().binaryAnd(CompoundInterval.singleton(8));
  }

  @Test
  public void testAbsolute() {
    assertFalse(CompoundInterval.top().absolute().containsNegative());
    assertEquals(CompoundInterval.one(), CompoundInterval.one().negate().absolute());
    CompoundInterval twoToFour = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundInterval negTwoToNegOne = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundInterval negFourToNegTwo = CompoundInterval.of(SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundInterval oneToTwo = CompoundInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertFalse(twoToFour.absolute().containsNegative());
    assertFalse(negFourToNegTwo.absolute().containsNegative());
    assertFalse(negFourToNegTwo.negate().absolute().containsNegative());
    assertFalse(oneToTwo.absolute().containsNegative());
    assertFalse(oneToTwo.negate().absolute().containsNegative());
    assertFalse(negTwoToNegOne.unionWith(twoToFour).negate().absolute().containsNegative());
  }

}
