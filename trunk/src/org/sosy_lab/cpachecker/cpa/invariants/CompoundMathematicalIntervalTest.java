// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import java.util.List;
import org.junit.Test;

public class CompoundMathematicalIntervalTest {

  private final SimpleInterval oneToFiveInterval =
      SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(5));

  private final SimpleInterval sixToTenInterval =
      SimpleInterval.of(BigInteger.valueOf(6), BigInteger.valueOf(10));

  private final SimpleInterval oneToTenInterval = SimpleInterval.of(BigInteger.ONE, BigInteger.TEN);

  private final SimpleInterval negInfToZeroInterval =
      SimpleInterval.singleton(BigInteger.ZERO).extendToNegativeInfinity();

  private final SimpleInterval zeroToPosInfInterval =
      SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();

  @Test
  public void testIsTop() {
    assertThat(CompoundMathematicalInterval.of(oneToTenInterval).isTop()).isFalse();
    assertThat(CompoundMathematicalInterval.of(negInfToZeroInterval).isTop()).isFalse();
    assertThat(CompoundMathematicalInterval.of(zeroToPosInfInterval).isTop()).isFalse();
    assertThat(CompoundMathematicalInterval.bottom().isTop()).isFalse();
    assertThat(CompoundMathematicalInterval.top().isTop()).isTrue();
  }

  @Test
  public void testIsBottom() {
    assertThat(CompoundMathematicalInterval.of(oneToTenInterval).isBottom()).isFalse();
    assertThat(CompoundMathematicalInterval.of(negInfToZeroInterval).isBottom()).isFalse();
    assertThat(CompoundMathematicalInterval.of(zeroToPosInfInterval).isBottom()).isFalse();
    assertThat(CompoundMathematicalInterval.top().isBottom()).isFalse();
    assertThat(CompoundMathematicalInterval.bottom().isBottom()).isTrue();
  }

  @Test
  public void testUnionWith() {
    assertThat(
            CompoundMathematicalInterval.of(negInfToZeroInterval)
                .unionWith(zeroToPosInfInterval)
                .isTop())
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.of(negInfToZeroInterval)
                .unionWith(CompoundMathematicalInterval.of(zeroToPosInfInterval))
                .isTop())
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.bottom()
                .unionWith(CompoundMathematicalInterval.top())
                .isTop())
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.of(negInfToZeroInterval)
                .unionWith(CompoundMathematicalInterval.of(oneToTenInterval))
                .isTop())
        .isFalse();
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
      assertThat(steps.contains(i)).isTrue();
    }
    CompoundMathematicalInterval stepsNegInf =
        steps.unionWith(
            CompoundMathematicalInterval.singleton(BigInteger.valueOf(-4)).extendToMinValue());
    for (int i = -6; i <= 6; i += 2) {
      assertThat(stepsNegInf.contains(i)).isTrue();
    }

    CompoundMathematicalInterval zeroOrTenToInf =
        CompoundMathematicalInterval.singleton(0)
            .unionWith(CompoundMathematicalInterval.singleton(10).extendToMaxValue());
    CompoundMathematicalInterval negOne = CompoundMathematicalInterval.singleton(-1);
    CompoundMathematicalInterval negOneOrZeroOrTenToInf =
        CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.valueOf(-1), BigInteger.ZERO))
            .unionWith(CompoundMathematicalInterval.singleton(10).extendToMaxValue());
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
    assertThat(
            CompoundMathematicalInterval.bottom()
                .intersectWith(CompoundMathematicalInterval.top())
                .isBottom())
        .isTrue();
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
    CompoundMathematicalInterval negInfToTen =
        CompoundMathematicalInterval.singleton(10).extendToMinValue();
    CompoundMathematicalInterval elevenToInf =
        CompoundMathematicalInterval.singleton(11).extendToMaxValue();
    assertThat(negInfToTen.invert()).isEqualTo(elevenToInf);
    assertThat(elevenToInf.invert()).isEqualTo(negInfToTen);
    assertThat(negInfToTen.invert().invert()).isEqualTo(negInfToTen);
    for (int i = -1; i < 2; ++i) {
      CompoundMathematicalInterval invertedState =
          CompoundMathematicalInterval.singleton(i).invert();
      assertThat(invertedState.contains(i)).isFalse();
      assertThat(invertedState.hasLowerBound()).isFalse();
      assertThat(invertedState.hasUpperBound()).isFalse();
      assertThat(invertedState.isTop()).isFalse();
      assertThat(invertedState.isBottom()).isFalse();
      assertThat(invertedState.contains(i - 1)).isTrue();
      assertThat(invertedState.contains(i + 1)).isTrue();
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
    CompoundMathematicalInterval twoToFour =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundMathematicalInterval negTwoToNegOne =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundMathematicalInterval negFourToNegTwo =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundMathematicalInterval oneToTwo =
        CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertThat(negTwoToNegOne.unionWith(twoToFour).negate())
        .isEqualTo(oneToTwo.unionWith(negFourToNegTwo));
  }

  @Test
  public void testIsSingleton() {
    CompoundMathematicalInterval negOne = CompoundMathematicalInterval.singleton(-1);
    CompoundMathematicalInterval zero = CompoundMathematicalInterval.singleton(0);
    CompoundMathematicalInterval ten = CompoundMathematicalInterval.singleton(10);
    assertThat(negOne.isSingleton()).isTrue();
    assertThat(zero.isSingleton()).isTrue();
    assertThat(CompoundMathematicalInterval.one().isSingleton()).isTrue();
    assertThat(ten.isSingleton()).isTrue();
    assertThat(
            CompoundMathematicalInterval.span(CompoundMathematicalInterval.one(), ten)
                .isSingleton())
        .isFalse();
    assertThat(zero.unionWith(ten).isSingleton()).isFalse();
    assertThat(
            negOne
                .unionWith(
                    CompoundMathematicalInterval.span(CompoundMathematicalInterval.one(), ten))
                .isSingleton())
        .isFalse();
  }

  @Test
  public void containsTest() {
    assertThat(CompoundMathematicalInterval.singleton(-1).contains(-1)).isTrue();
    assertThat(CompoundMathematicalInterval.singleton(0).contains(0)).isTrue();
    assertThat(CompoundMathematicalInterval.one().contains(1)).isTrue();
    assertThat(
            CompoundMathematicalInterval.singleton(-1)
                .contains(CompoundMathematicalInterval.singleton(-1)))
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.singleton(0)
                .contains(CompoundMathematicalInterval.singleton(0)))
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.singleton(1)
                .contains(CompoundMathematicalInterval.singleton(1)))
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))
                .contains(
                    CompoundMathematicalInterval.of(
                        SimpleInterval.of(BigInteger.ONE, BigInteger.TEN))))
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN))
                .contains(
                    CompoundMathematicalInterval.of(
                        SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))))
        .isFalse();
    assertThat(
            CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))
                .contains(5))
        .isTrue();
    assertThat(
            CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.TEN))
                .contains(-1))
        .isFalse();
    assertThat(
            CompoundMathematicalInterval.of(
                    SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(4)))
                .unionWith(SimpleInterval.of(BigInteger.valueOf(6), BigInteger.TEN))
                .contains(5))
        .isFalse();
  }

  @Test
  public void binaryNotTest() {
    CompoundMathematicalInterval.singleton(1).extendToMinValue().binaryNot();
  }

  @Test
  public void testMultiply() {
    CompoundMathematicalInterval topMultNeg2 =
        CompoundMathematicalInterval.top().multiply(BigInteger.valueOf(-2));
    List<SimpleInterval> intervals = topMultNeg2.getIntervals();
    int i = 0;
    BigInteger lastUpperBound = null;
    for (SimpleInterval interval : intervals) {
      if (i == 0) {
        assertThat(interval.hasLowerBound()).isFalse();
      } else {
        assertThat(interval.hasLowerBound()).isTrue();
        // Check that intervals to not overlap, touch or are in the wrong order
        assertThat(interval.getLowerBound().subtract(lastUpperBound).compareTo(BigInteger.ONE) > 0)
            .isTrue();
      }
      if (i == intervals.size() - 1) {
        assertThat(interval.hasUpperBound()).isFalse();
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
    CompoundMathematicalInterval zeroToOne =
        CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundMathematicalInterval one = CompoundMathematicalInterval.one();
    assertThat(zeroToOne.binaryXor(one)).isEqualTo(zeroToOne);
    assertThat(one.binaryXor(zeroToOne)).isEqualTo(zeroToOne);
  }

  @Test
  public void testBinaryAnd() {
    CompoundMathematicalInterval zeroToOne =
        CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    CompoundMathematicalInterval one = CompoundMathematicalInterval.one();
    assertThat(CompoundMathematicalInterval.top().binaryAnd(one)).isEqualTo(zeroToOne);
    assertThat(one.binaryAnd(CompoundMathematicalInterval.top())).isEqualTo(zeroToOne);
    CompoundMathematicalInterval.top().binaryAnd(CompoundMathematicalInterval.singleton(8));
  }

  @Test
  public void testAbsolute() {
    assertThat(CompoundMathematicalInterval.top().absolute().containsNegative()).isFalse();
    assertThat(CompoundMathematicalInterval.one().negate().absolute())
        .isEqualTo(CompoundMathematicalInterval.one());
    CompoundMathematicalInterval twoToFour =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(2), BigInteger.valueOf(4)));
    CompoundMathematicalInterval negTwoToNegOne =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(-2), BigInteger.valueOf(-1)));
    CompoundMathematicalInterval negFourToNegTwo =
        CompoundMathematicalInterval.of(
            SimpleInterval.of(BigInteger.valueOf(-4), BigInteger.valueOf(-2)));
    CompoundMathematicalInterval oneToTwo =
        CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(2)));
    assertThat(twoToFour.absolute().containsNegative()).isFalse();
    assertThat(negFourToNegTwo.absolute().containsNegative()).isFalse();
    assertThat(negFourToNegTwo.negate().absolute().containsNegative()).isFalse();
    assertThat(oneToTwo.absolute().containsNegative()).isFalse();
    assertThat(oneToTwo.negate().absolute().containsNegative()).isFalse();
    assertThat(negTwoToNegOne.unionWith(twoToFour).negate().absolute().containsNegative())
        .isFalse();
  }
}
