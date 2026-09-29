// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;

@SuppressWarnings("deprecation") // Tests need formulas with deliberately chosen SSA interfaces.
public class PredicateOperatorUtilTest extends SolverViewBasedTest0 {
  @Override
  protected Solvers solverToUse() {
    return Solvers.SMTINTERPOL;
  }

  @Test
  public void intermediateValuesAreIndependentAcrossConditionUses() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula = ints.equal(ints.makeVariable("x@1"), ints.makeVariable("x@2"));
    var path =
        PathFormula.createManually(
            formula,
            SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, 2).build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    var first =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    var second =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    var x = ints.makeVariable("x");
    first = mgrv.substitute(first, java.util.Map.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, java.util.Map.of(x, ints.makeNumber(1)));
    // Both instances are satisfiable with their own intermediate value. Reusing x.1 would
    // incorrectly require this private value to be both zero and one.
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }

  @Test
  public void alreadyUninstantiatedPrivateValuesAreStillFreshened() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula = ints.equal(ints.makeVariable("local!value"), ints.makeVariable("x"));
    var path =
        PathFormula.createManually(
            formula, SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), 0);
    var first =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    var second =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    var x = ints.makeVariable("x");
    first = mgrv.substitute(first, java.util.Map.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, java.util.Map.of(x, ints.makeNumber(1)));
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }

  @Test
  public void fieldNamesDoNotCollapseDifferentSsaValues() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("record.field@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("record.field@2"), ints.makeNumber(1)));
    var path =
        PathFormula.createManually(
            formula,
            SSAMap.emptySSAMap().builder().setIndex("record.field", CNumericTypes.INT, 2).build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    var condition =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    assertThat(solver.isUnsat(condition)).isFalse();
    assertThat(
            solver.implies(
                condition, ints.equal(ints.makeVariable("record.field"), ints.makeNumber(1))))
        .isTrue();
  }

  @Test
  public void comparisonNormalizationKeepsAllPrivateNamesAndVersionsDistinct() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("x!1"), ints.makeNumber(1)),
            ints.equal(ints.makeVariable("private!v@1"), ints.makeNumber(2)),
            ints.equal(ints.makeVariable("private!v@2"), ints.makeNumber(3)),
            ints.equal(ints.makeVariable("private!v#at2"), ints.makeNumber(4)),
            ints.equal(ints.makeVariable("x@2"), ints.makeNumber(5)));
    var path =
        PathFormula.createManually(
            formula,
            SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, 2).build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    var normalized = PredicateOperatorUtil.normalizeForComparison(path, mgrv);
    assertThat(solver.isUnsat(normalized)).isFalse();
    assertThat(solver.implies(normalized, ints.equal(ints.makeVariable("x"), ints.makeNumber(5))))
        .isTrue();
    assertThat(mgrv.extractVariableNames(normalized)).hasSize(6);
  }

  @Test
  public void latestNondeterministicValuesArePrivateOnEveryUse() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula =
        ints.equal(ints.makeVariable("__VERIFIER_nondet_int@2"), ints.makeVariable("x@1"));
    var path =
        PathFormula.createManually(
            formula,
            SSAMap.emptySSAMap()
                .builder()
                .setIndex("__VERIFIER_nondet_int", CNumericTypes.INT, 2)
                .setIndex("x", CNumericTypes.INT, 1)
                .build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    var first = PredicateOperatorUtil.uninstantiate(path, mgrv).booleanFormula();
    var second = PredicateOperatorUtil.uninstantiate(path, mgrv).booleanFormula();
    var x = ints.makeVariable("x");
    first = mgrv.substitute(first, java.util.Map.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, java.util.Map.of(x, ints.makeNumber(1)));
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }
}
