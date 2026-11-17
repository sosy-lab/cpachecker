// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.testing.EquivalenceTester;
import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/**
 * Tests that {@link ValueWrapper} is an equivalence relation (reflexive, symmetric, transitive),
 * that equivalent values have equal hash codes, and that it distinguishes the intended values.
 */
public class ValueWrapperTest {

  @Test
  public void testEquivalenceGroups() {
    BigInteger twoToThe64 = BigInteger.ONE.shiftLeft(64);
    Rational oneHalf = Rational.of(BigInteger.ONE, BigInteger.TWO);
    SymbolicIdentifier pointer = SymbolicValueFactory.getInstance().newIdentifier(null);

    EquivalenceTester.of(new ValueWrapper())
        // All integer zeros are equivalent, regardless of their Java type
        .addEquivalenceGroup(
            new NumericValue(0),
            new NumericValue(0L),
            new NumericValue((short) 0),
            new NumericValue((byte) 0),
            new NumericValue(BigInteger.ZERO))
        // Floating-point zeros are neither equivalent to integer zero nor to each other
        .addEquivalenceGroup(new NumericValue(0.0f), new NumericValue(0.0f))
        .addEquivalenceGroup(new NumericValue(-0.0f))
        .addEquivalenceGroup(new NumericValue(0.0), new NumericValue(0.0))
        .addEquivalenceGroup(new NumericValue(-0.0))
        .addEquivalenceGroup(
            new NumericValue(FloatValue.zero(FloatValue.Format.Float32)),
            new NumericValue(FloatValue.zero(FloatValue.Format.Float32)))
        .addEquivalenceGroup(new NumericValue(FloatValue.zero(FloatValue.Format.Float64)))
        .addEquivalenceGroup(new NumericValue(Double.NaN), new NumericValue(Double.NaN))
        .addEquivalenceGroup(
            new NumericValue(FloatValue.nan(FloatValue.Format.Float64)),
            new NumericValue(FloatValue.nan(FloatValue.Format.Float64).negate()))
        // Rational numbers are compared with equals(), also if they are 0 or between -1 and 1
        .addEquivalenceGroup(new NumericValue(Rational.ZERO))
        .addEquivalenceGroup(
            new NumericValue(oneHalf),
            new NumericValue(Rational.of(BigInteger.ONE, BigInteger.TWO)))
        .addEquivalenceGroup(new NumericValue(Rational.of(BigInteger.ONE, BigInteger.valueOf(3))))
        // Non-zero integers of different Java types are not equivalent
        .addEquivalenceGroup(new NumericValue(1), new NumericValue(1))
        .addEquivalenceGroup(new NumericValue(1L))
        // Large integers whose lower 64 bits are zero are neither zero nor different from equal
        // values
        .addEquivalenceGroup(
            new NumericValue(twoToThe64), new NumericValue(BigInteger.ONE.shiftLeft(64)))
        .addEquivalenceGroup(new NumericValue(twoToThe64.negate()))
        // Symbolic values are compared with equals()
        .addEquivalenceGroup(pointer, new SymbolicIdentifier(pointer.getId(), null))
        .addEquivalenceGroup(SymbolicValueFactory.getInstance().newIdentifier(null))
        .test();
  }
}
