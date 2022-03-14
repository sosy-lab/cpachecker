// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive))
        .isEqualTo(zeroToFour.negate());
    assertThat(ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive.negate()))
        .isEqualTo(zeroToFour.negate());
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
    assertThat(ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ZERO)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ONE)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.TEN)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ZERO)).isEqualTo(one);
    assertThat(ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ONE)).isEqualTo(two);
    assertThat(ISIOperator.SHIFT_LEFT.apply(one, BigInteger.TEN)).isEqualTo(oneThousandTwentyFour);
    assertThat(ISIOperator.SHIFT_LEFT.apply(ten, BigInteger.ZERO)).isEqualTo(ten);
    assertThat(ISIOperator.SHIFT_LEFT.apply(zeroToFive, BigInteger.ONE)).isEqualTo(zeroToTen);
  }

  @Test
  public void testShiftRight() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval oneToTen = SimpleInterval.span(one, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    assertThat(ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ZERO)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ONE)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.TEN)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ZERO)).isEqualTo(one);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ONE)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.TEN)).isEqualTo(zero);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(ten, BigInteger.ZERO)).isEqualTo(ten);
    assertThat(ISIOperator.SHIFT_RIGHT.apply(oneToTen, BigInteger.ONE)).isEqualTo(zeroToFive);
  }
}
