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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;
import org.junit.Test;

public class CompoundMathematicalIntervalTest {

  private final SimpleInterval oneToFiveInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5));

  private final SimpleInterval sixToTenInterval = SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));

  private final SimpleInterval oneToTenInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.TEN);

  private final SimpleInterval negInfToZeroInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToNegativeInfinity();

  private final SimpleInterval zeroToPosInfInterval = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();

  @Test
  public void testIsTop() {
    assertFalse(CompoundMathematicalInterval.of(oneToTenInterval).isTop());
    assertFalse(CompoundMathematicalInterval.of(negInfToZeroInterval).isTop());
    assertFalse(CompoundMathematicalInterval.of(zeroToPosInfInterval).isTop());
    assertFalse(CompoundMathematicalInterval.bottom().isTop());
    assertTrue(CompoundMathematicalInterval.top().isTop());
  }

  @Test
  public void testIsBottom() {
    assertFalse(CompoundMathematicalInterval.of(oneToTenInterval).isBottom());
    assertFalse(CompoundMathematicalInterval.of(negInfToZeroInterval).isBottom());
    assertFalse(CompoundMathematicalInterval.of(zeroToPosInfInterval).isBottom());
    assertFalse(CompoundMathematicalInterval.top().isBottom());
    assertTrue(CompoundMathematicalInterval.bottom().isBottom());
  }

  @Test
  public void testUnionWith() {
    assertTrue(CompoundMathematicalInterval.of(negInfToZeroInterval).unionWith(zeroToPosInfInterval).isTop());
    assertTrue(CompoundMathematicalInterval.of(negInfToZeroInterval).unionWith(CompoundMathematicalInterval.of(zeroToPosInfInterval)).isTop());
    assertTrue(CompoundMathematicalInterval.bottom().unionWith(CompoundMathematicalInterval.top()).isTop());
    assertFalse(CompoundMathematicalInterval.of(negInfToZeroInterval).unionWith(CompoundMathematicalInterval.of(oneToTenInterval)).isTop());
    assertThat(CompoundMathematicalInterval.of(oneToTenInterval))
        .isEqualTo(CompoundMathematicalInterval.of(oneToFiveInterval).unionWith(sixToTenInterval));
    assertThat(CompoundMathematicalInterval.of(oneToTenInterval))
        .isEqualTo(CompoundMathematicalInterval.of(oneToTenInterval).unionWith(sixToTenInterval));
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    assertThat(
            CompoundMathematicalInterval.of(zero.extendToNegativeInfinity())
                .unionWith(zero.extendToPositiveInfinity())
                .getIntervals())
        .hasSize(1);
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundMathematicalInterval zeroToThree = CompoundMathematicalInterval.of(zeroToThreeInterval);
    CompoundMathematicalInterval six = CompoundMathematicalInterval.singleton(6);
    assertThat(zeroToThree.unionWith(six).getIntervals()).hasSize(2);
    assertThat(six.unionWith(zeroToThree)).isEqualTo(zeroToThree.unionWith(six));
    assertThat(zeroToThree.unionWith(CompoundMathematicalInterval.singleton(0)))
        .isEqualTo(zeroToThree);
    assertThat(zeroToThree.unionWith(CompoundMathematicalInterval.singleton(1)))
        .isEqualTo(zeroToThree);
    assertThat(zeroToThree.unionWith(CompoundMathematicalInterval.singleton(2)))
        .isEqualTo(zeroToThree);
    assertThat(zeroToThree.unionWith(CompoundMathematicalInterval.singleton(3)))
        .isEqualTo(zeroToThree);
    assertThat(zeroToThree.unionWith(zeroToThree)).isEqualTo(zeroToThree);

    CompoundMathematicalInterval steps = CompoundMathematicalInterval.bottom();
    for (int i = -6; i <= 6; i += 2) {
      steps = steps.unionWith(CompoundMathematicalInterval.singleton(i));
    }
    steps = steps.extendToMinValue().extendToMaxValue();
    for (int i = -6; i <= 6; i += 2) {
      assertTrue(steps.contains(i));
    }
    CompoundMathematicalInterval stepsNegInf = steps.unionWith(CompoundMathematicalInterval.singleton(BigInteger.valueOf(-4)).extendToMinValue());
    for (int i = -6; i <= 6; i += 2) {
      assertTrue(stepsNegInf.contains(i));
    }

    CompoundMathematicalInterval zeroOrTenToInf = CompoundMathematicalInterval.singleton(0).unionWith(CompoundMathematicalInterval.singleton(10).extendToMaxValue());
    CompoundMathematicalInterval negOne = CompoundMathematicalInterval.singleton(-1);
    CompoundMathematicalInterval negOneOrZeroOrTenToInf = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-1), BigInteger.ZERO)).unionWith(CompoundMathematicalInterval.singleton(10).extendToMaxValue());
    assertThat(zeroOrTenToInf.unionWith(negOne)).isEqualTo(negOneOrZeroOrTenToInf);

    assertThat(negOne.unionWith(CompoundMathematicalInterval.one()).unionWith(zero))
        .isEqualTo(
            CompoundMathematicalInterval.of(
                SimpleInterval.of(BigInteger.valueOf(-1), BigInteger.valueOf(1))));
    assertThat(negOne.unionWith(CompoundMathematicalInterval.one()).unionWith(zero).getIntervals())
        .hasSize(1);
  }

  @Test
  public void testIntersectWith() {
    assertThat(
            CompoundMathematicalInterval.of(negInfToZeroInterval)
                .intersectWith(zeroToPosInfInterval))
        .isEqualTo(CompoundMathematicalInterval.of(SimpleInterval.singleton(BigInteger.ZERO)));
    assertThat(
            CompoundMathematicalInterval.of(negInfToZeroInterval)
                .intersectWith(CompoundMathematicalInterval.of(zeroToPosInfInterval)))
        .isEqualTo(CompoundMathematicalInterval.of(SimpleInterval.singleton(BigInteger.ZERO)));
    assertTrue(CompoundMathematicalInterval.bottom().intersectWith(CompoundMathematicalInterval.top()).isBottom());
    assertThat(
            CompoundMathematicalInterval.top()
                .intersectWith(CompoundMathematicalInterval.of(oneToTenInterval)))
        .isEqualTo(CompoundMathematicalInterval.of(oneToTenInterval));
    SimpleInterval oneToTwo = SimpleInterval.of(BigInteger.valueOf(1), BigInteger.valueOf(2));
    CompoundMathematicalInterval notTwo = CompoundMathematicalInterval.singleton(2).invert();
    assertThat(notTwo.intersectWith(CompoundMathematicalInterval.of(oneToTwo)))
        .isEqualTo(CompoundMathematicalInterval.singleton(1));
  }

  @Test
  public void testInvert() {
    assertThat(CompoundMathematicalInterval.top().invert())
        .isEqualTo(CompoundMathematicalInterval.bottom());
    assertThat(CompoundMathematicalInterval.bottom().invert())
        .isEqualTo(CompoundMathematicalInterval.top());
    CompoundMathematicalInterval negInfToTen = CompoundMathematicalInterval.singleton(10).extendToMinValue();
    CompoundMathematicalInterval elevenToInf = CompoundMathematicalInterval.singleton(11).extendToMaxValue();
    assertThat(negInfToTen.invert()).isEqualTo(elevenToInf);
    assertThat(elevenToInf.invert()).isEqualTo(negInfToTen);
    assertThat(negInfToTen.invert().invert()).isEqualTo(negInfToTen);
    for (int i = -1; i < 2; ++i) {
      CompoundMathematicalInterval invertedState = CompoundMathematicalInterval.singleton(i).invert();
      assertFalse(invertedState.contains(i));
      assertFalse(invertedState.hasLowerBound());
      assertFalse(invertedState.hasUpperBound());
      assertFalse(invertedState.isTop());
      assertFalse(invertedState.isBottom());
      assertTrue(invertedState.contains(i - 1));
      assertTrue(invertedState.contains(i + 1));
    }
    assertThat(
            CompoundMathematicalInterval.of(
                    SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5)))
                .unionWith(CompoundMathematicalInterval.singleton(7).extendToMaxValue())
                .invert())
        .isEqualTo(
            CompoundMathematicalInterval.singleton(0)
                .extendToMinValue()
                .unionWith(CompoundMathematicalInterval.singleton(6)));
  }

  @Test
  public void moduloTest() {
    SimpleInterval zeroToThreeInterval = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(3));
    CompoundMathematicalInterval zeroToThree = CompoundMathematicalInterval.of(zeroToThreeInterval);
    CompoundMathematicalInterval six = CompoundMathematicalInterval.singleton(6);
    SimpleInterval sixToTenSimpleInterval =
        SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));
    CompoundMathematicalInterval sixToTen = CompoundMathematicalInterval.of(sixToTenSimpleInterval);
    assertThat(sixToTen.modulo(BigInteger.valueOf(7))).isEqualTo(zeroToThree.unionWith(six));
    assertThat(sixToTen.negate().modulo(BigInteger.valueOf(7)))
        .isEqualTo(zeroToThree.unionWith(six).negate());
    assertThat(sixToTen.modulo(BigInteger.valueOf(7).negate()))
        .isEqualTo(zeroToThree.unionWith(six));
    assertThat(sixToTen.negate().modulo(BigInteger.valueOf(7).negate()))
        .isEqualTo(zeroToThree.unionWith(six).negate());
  }

  @Test
  public void testNegate() {
    assertThat(CompoundMathematicalInterval.one().negate())
        .isEqualTo(CompoundMathematicalInterval.singleton(-1));
    CompoundMathematicalInterval twoToFour = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundMathematicalInterval negTwoToNegOne = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundMathematicalInterval negFourToNegTwo = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundMathematicalInterval oneToTwo = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertThat(negTwoToNegOne.unionWith(twoToFour).negate())
        .isEqualTo(oneToTwo.unionWith(negFourToNegTwo));
  }

  @Test
  public void testIsSingleton() {
    CompoundMathematicalInterval negOne = CompoundMathematicalInterval.singleton(-1);
    CompoundMathematicalInterval zero = CompoundMathematicalInterval.singleton(0);
    CompoundMathematicalInterval ten = CompoundMathematicalInterval.singleton(10);
    assertTrue(negOne.isSingleton());
    assertTrue(zero.isSingleton());
    assertTrue(CompoundMathematicalInterval.one().isSingleton());
    assertTrue(ten.isSingleton());
    assertFalse(CompoundMathematicalInterval.span(CompoundMathematicalInterval.one(), ten).isSingleton());
    assertFalse(zero.unionWith(ten).isSingleton());
    assertFalse(negOne.unionWith(CompoundMathematicalInterval.span(CompoundMathematicalInterval.one(), ten)).isSingleton());
  }

  @Test
  public void containsTest() {
    assertTrue(CompoundMathematicalInterval.singleton(-1).contains(-1));
    assertTrue(CompoundMathematicalInterval.singleton(0).contains(0));
    assertTrue(CompoundMathematicalInterval.one().contains(1));
    assertTrue(CompoundMathematicalInterval.singleton(-1).contains(CompoundMathematicalInterval.singleton(-1)));
    assertTrue(CompoundMathematicalInterval.singleton(0).contains(CompoundMathematicalInterval.singleton(0)));
    assertTrue(CompoundMathematicalInterval.singleton(1).contains(CompoundMathematicalInterval.singleton(1)));
    assertTrue(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN))));
    assertFalse(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN)).contains(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))));
    assertTrue(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(5));
    assertFalse(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN)).contains(-1));
    assertFalse(CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(4))).unionWith(SimpleInterval.of(BigInteger.valueOf(6), BigInteger.TEN)).contains(5));
  }

  @Test
  public void binaryNotTest() {
    CompoundMathematicalInterval.singleton(1).extendToMinValue().binaryNot();
  }

  @Test
  public void testMultiply() {
    CompoundMathematicalInterval topMultNeg2 = CompoundMathematicalInterval.top().multiply(BigInteger.valueOf(-2));
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
    assertThat(topMultNeg2.unionWith(topMultNeg2)).isEqualTo(topMultNeg2);
  }

  @Test
  public void testBinaryXor() {
    CompoundMathematicalInterval zeroToOne = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundMathematicalInterval one = CompoundMathematicalInterval.one();
    assertThat(zeroToOne.binaryXor(one)).isEqualTo(zeroToOne);
    assertThat(one.binaryXor(zeroToOne)).isEqualTo(zeroToOne);
  }

  @Test
  public void testBinaryAnd() {
    CompoundMathematicalInterval zeroToOne = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundMathematicalInterval one = CompoundMathematicalInterval.one();
    assertThat(CompoundMathematicalInterval.top().binaryAnd(one)).isEqualTo(zeroToOne);
    assertThat(one.binaryAnd(CompoundMathematicalInterval.top())).isEqualTo(zeroToOne);
    CompoundMathematicalInterval.top().binaryAnd(CompoundMathematicalInterval.singleton(8));
  }

  @Test
  public void testAbsolute() {
    assertFalse(CompoundMathematicalInterval.top().absolute().containsNegative());
    assertThat(CompoundMathematicalInterval.one().negate().absolute())
        .isEqualTo(CompoundMathematicalInterval.one());
    CompoundMathematicalInterval twoToFour = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundMathematicalInterval negTwoToNegOne = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundMathematicalInterval negFourToNegTwo = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundMathematicalInterval oneToTwo = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertFalse(twoToFour.absolute().containsNegative());
    assertFalse(negFourToNegTwo.absolute().containsNegative());
    assertFalse(negFourToNegTwo.negate().absolute().containsNegative());
    assertFalse(oneToTwo.absolute().containsNegative());
    assertFalse(oneToTwo.negate().absolute().containsNegative());
    assertFalse(negTwoToNegOne.unionWith(twoToFour).negate().absolute().containsNegative());
  }

}
