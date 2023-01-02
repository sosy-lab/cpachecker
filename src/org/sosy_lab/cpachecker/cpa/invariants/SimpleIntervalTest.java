// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.greaterOrEqual;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.lessOrEqual;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.singleton;

import java.math.BigInteger;
import org.junit.Test;

public class SimpleIntervalTest {

  @Test
  public void testConstruction() {
    assertThat(singleton(BigInteger.ZERO)).isNotNull();
    assertThat(singleton(BigInteger.valueOf(Long.MAX_VALUE))).isNotNull();
    assertThat(singleton(BigInteger.valueOf(Long.MIN_VALUE))).isNotNull();

    assertThat(lessOrEqual(BigInteger.ZERO)).isNotNull();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE))).isNotNull();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE))).isNotNull();

    assertThat(greaterOrEqual(BigInteger.ZERO)).isNotNull();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE))).isNotNull();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE))).isNotNull();

    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ZERO)).isNotNull();
    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE)).isNotNull();
    assertThat(
            SimpleInterval.of(
                BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)))
        .isNotNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConstruction1() {
    SimpleInterval.of(BigInteger.ONE, BigInteger.ZERO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConstruction2() {
    SimpleInterval.of(BigInteger.valueOf(Long.MAX_VALUE), BigInteger.valueOf(Long.MIN_VALUE));
  }

  @Test
  public void testContains() {
    assertThat(singleton(BigInteger.ZERO).contains(BigInteger.ZERO)).isTrue();
    assertThat(singleton(BigInteger.TEN).contains(BigInteger.TEN)).isTrue();
    assertThat(singleton(BigInteger.ZERO).contains(BigInteger.TEN)).isFalse();
    assertThat(singleton(BigInteger.TEN).contains(BigInteger.ZERO)).isFalse();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.ONE)).isTrue();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.TEN)).isTrue();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(5)))
        .isTrue();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.ZERO))
        .isFalse();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(-5)))
        .isFalse();
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(-10)))
        .isFalse();
  }

  @Test
  public void testIsSingleton() {
    assertThat(singleton(BigInteger.ZERO).isSingleton()).isTrue();
    assertThat(singleton(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton()).isTrue();
    assertThat(singleton(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton()).isTrue();

    assertThat(lessOrEqual(BigInteger.ZERO).isSingleton()).isFalse();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton()).isFalse();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton()).isFalse();

    assertThat(greaterOrEqual(BigInteger.ZERO).isSingleton()).isFalse();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton()).isFalse();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton()).isFalse();

    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ZERO).isSingleton()).isTrue();
    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE).isSingleton()).isFalse();
    assertThat(
            SimpleInterval.of(
                    BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE))
                .isSingleton())
        .isFalse();
  }

  @Test
  public void testSize() {
    assertThat(singleton(BigInteger.ZERO).size()).isEqualTo(BigInteger.ONE);
    assertThat(singleton(BigInteger.valueOf(Long.MAX_VALUE)).size()).isEqualTo(BigInteger.ONE);
    assertThat(singleton(BigInteger.valueOf(Long.MIN_VALUE)).size()).isEqualTo(BigInteger.ONE);

    assertThat(lessOrEqual(BigInteger.ZERO).size()).isNull();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).size()).isNull();
    assertThat(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).size()).isNull();

    assertThat(greaterOrEqual(BigInteger.ZERO).size()).isNull();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).size()).isNull();
    assertThat(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).size()).isNull();

    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ZERO).size())
        .isEqualTo(BigInteger.ONE);
    assertThat(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE).size())
        .isEqualTo(BigInteger.valueOf(2L));
    assertThat(SimpleInterval.of(BigInteger.ONE, BigInteger.TEN).size()).isEqualTo(BigInteger.TEN);

    assertThat(SimpleInterval.of(BigInteger.valueOf(-100L), BigInteger.valueOf(100L)).size())
        .isEqualTo(BigInteger.valueOf(201L));

    assertThat(
            SimpleInterval.of(
                    BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE))
                .size())
        .isEqualTo(
            BigInteger.valueOf(Long.MAX_VALUE)
                .subtract(BigInteger.valueOf(Long.MIN_VALUE))
                .add(BigInteger.ONE));
  }

  @Test
  public void testIntersectsWith() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval two = SimpleInterval.singleton(BigInteger.valueOf(2));
    SimpleInterval negFiveToTen = SimpleInterval.of(BigInteger.valueOf(-5), BigInteger.TEN);
    SimpleInterval fiveToFifteen = SimpleInterval.of(BigInteger.valueOf(5), BigInteger.valueOf(15));
    SimpleInterval twentyToFifty =
        SimpleInterval.of(BigInteger.valueOf(20), BigInteger.valueOf(50));
    SimpleInterval oneToThousand = SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(1000));
    assertThat(zero.intersectsWith(one)).isFalse();
    assertThat(one.intersectsWith(zero)).isFalse();
    assertThat(zero.intersectsWith(zero)).isTrue();
    assertThat(one.intersectsWith(one)).isTrue();
    assertThat(zero.extendToNegativeInfinity().intersectsWith(zero.extendToPositiveInfinity()))
        .isTrue();
    assertThat(one.extendToNegativeInfinity().intersectsWith(one.extendToPositiveInfinity()))
        .isTrue();
    assertThat(zero.extendToNegativeInfinity().intersectsWith(one.extendToPositiveInfinity()))
        .isFalse();
    assertThat(one.extendToPositiveInfinity().intersectsWith(zero.extendToNegativeInfinity()))
        .isFalse();
    assertThat(one.extendToNegativeInfinity().intersectsWith(zero.extendToPositiveInfinity()))
        .isTrue();
    assertThat(zero.extendToPositiveInfinity().intersectsWith(one.extendToNegativeInfinity()))
        .isTrue();
    assertThat(negFiveToTen.intersectsWith(fiveToFifteen)).isTrue();
    assertThat(negFiveToTen.intersectsWith(twentyToFifty)).isFalse();
    assertThat(fiveToFifteen.intersectsWith(twentyToFifty)).isFalse();
    assertThat(oneToThousand.intersectsWith(two)).isTrue();
  }
}
