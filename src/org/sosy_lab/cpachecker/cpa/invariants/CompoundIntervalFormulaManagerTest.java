// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class CompoundIntervalFormulaManagerTest {

  private static final TypeInfo INT = BitVectorInfo.from(32, true);

  private final CompoundIntervalManagerFactory factory =
      CompoundBitVectorIntervalManagerFactory.forbidSignedWrapAround();

  private final CompoundIntervalFormulaManager fmgr = new CompoundIntervalFormulaManager(factory);

  private final CompoundIntervalManager cim = factory.createCompoundIntervalManager(INT);

  private final NumeralFormula<CompoundInterval> x =
      InvariantsFormulaManager.INSTANCE.asVariable(INT, MemoryLocation.forIdentifier("x"));

  private NumeralFormula<CompoundInterval> constant(long pValue) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        INT, cim.singleton(BigInteger.valueOf(pValue)));
  }

  private BooleanFormula<CompoundInterval> xInRange(long pLower, long pUpper) {
    return InvariantsFormulaManager.INSTANCE.logicalAnd(
        fmgr.lessThanOrEqual(constant(pLower), x), fmgr.lessThanOrEqual(x, constant(pUpper)));
  }

  @Test
  public void testDefinitelyImpliesVariableOnBothSides() {
    // fails for x = 1
    assertThat(
            fmgr.definitelyImplies(
                fmgr.lessThan(constant(0), x),
                fmgr.lessThan(fmgr.subtract(constant(Integer.MAX_VALUE), x), x),
                true))
        .isFalse();
  }

  @Test
  public void testDefinitelyImpliesSingleOccurrence() {
    assertThat(
            fmgr.definitelyImplies(
                fmgr.lessThan(constant(0), x), fmgr.logicalNot(fmgr.equal(x, constant(0))), true))
        .isTrue();
    assertThat(
            fmgr.definitelyImplies(
                xInRange(1, 4), fmgr.lessThan(fmgr.add(x, constant(5)), constant(10)), true))
        .isTrue();
  }
}
