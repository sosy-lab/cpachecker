// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class CompoundIntervalFormulaManagerTest {

  private final TypeInfo intType = BitVectorInfo.from(32, true);

  private final CompoundIntervalManagerFactory factory =
      CompoundBitVectorIntervalManagerFactory.forbidSignedWrapAround();

  private final CompoundIntervalFormulaManager manager =
      new CompoundIntervalFormulaManager(factory);

  private final CompoundIntervalManager cim = factory.createCompoundIntervalManager(intType);

  private final NumeralFormula<CompoundInterval> a =
      InvariantsFormulaManager.INSTANCE.asVariable(intType, MemoryLocation.forIdentifier("a"));

  private final NumeralFormula<CompoundInterval> zero =
      InvariantsFormulaManager.INSTANCE.asConstant(intType, cim.singleton(0));

  private final NumeralFormula<CompoundInterval> intMax =
      InvariantsFormulaManager.INSTANCE.asConstant(intType, cim.singleton(intType.getMaxValue()));

  @Test
  public void definitelyImplies_doesNotClaimSelfReferentialFormulaIsImplied() {
    BooleanFormula<CompoundInterval> zeroLessThanA = manager.lessThan(zero, a);
    BooleanFormula<CompoundInterval> intMaxMinusALessThanA =
        manager.lessThan(manager.subtract(intMax, a), a);

    // 0 < a does NOT imply INT_MAX - a < a: e.g. a == 1 satisfies the premise (0 < 1)
    // but not the conclusion (INT_MAX - 1 is not less than 1). The two occurrences of `a`
    // are not independent, so treating this as a simple single-variable implication (as
    // ContainsOnlyEnvInfoVisitor's variable-count check used to) is unsound.
    assertThat(manager.definitelyImplies(zeroLessThanA, intMaxMinusALessThanA, true)).isFalse();
  }

  @Test
  public void definitelyImplies_stillProvesSimpleSingleOccurrenceImplication() {
    BooleanFormula<CompoundInterval> zeroLessThanA = manager.lessThan(zero, a);
    BooleanFormula<CompoundInterval> aNotEqualZero = manager.logicalNot(manager.equal(a, zero));

    // 0 < a does imply a != 0: this is the simple, single-occurrence case the
    // environment-based reasoning is actually sound for, and must keep working.
    assertThat(manager.definitelyImplies(zeroLessThanA, aNotEqualZero, true)).isTrue();
  }

  @Test
  public void equal_ofBinaryOrAgainstZero_isConjunctionOfNegatedOperands() {
    NumeralFormula<CompoundInterval> t1 =
        InvariantsFormulaManager.INSTANCE.asVariable(intType, MemoryLocation.forIdentifier("t1"));
    NumeralFormula<CompoundInterval> t3 =
        InvariantsFormulaManager.INSTANCE.asVariable(intType, MemoryLocation.forIdentifier("t3"));
    NumeralFormula<CompoundInterval> disjunction = manager.binaryOr(t1, t3);

    BooleanFormula<CompoundInterval> equalsZero = manager.equal(disjunction, zero);

    // (t1 | t3) == 0 must be the conjunction "t1 == 0 && t3 == 0", built without
    // CompoundIntervalFormulaManager.logicalAnd's unsound implication-based simplification --
    // not the imprecise, unstructured result that falling through to the generic equal() would
    // give for a formula whose bitwise-or structure it doesn't otherwise recognize.
    BooleanFormula<CompoundInterval> expected =
        InvariantsFormulaManager.INSTANCE.logicalAnd(
            manager.equal(t1, zero), manager.equal(t3, zero));
    assertThat(equalsZero).isEqualTo(expected);
  }
}
