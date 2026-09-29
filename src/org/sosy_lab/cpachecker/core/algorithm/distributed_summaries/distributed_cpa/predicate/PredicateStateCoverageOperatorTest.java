// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;

@SuppressWarnings("deprecation") // Deliberately choose the SSA interface of a condition.
public class PredicateStateCoverageOperatorTest extends SolverViewBasedTest0 {
  @Override
  protected Solvers solverToUse() {
    return Solvers.SMTINTERPOL;
  }

  private PredicateAbstractState condition(BooleanFormula formula, int index) {
    var path =
        PathFormula.createManually(
            formula,
            SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, index).build(),
            PointerTargetSet.emptyPointerTargetSet(),
            0);
    var abstraction =
        new AbstractionFormula(
            mgrv,
            new SymbolicRegionManager(solver).makeTrue(),
            bmgrv.makeTrue(),
            bmgrv.makeTrue(),
            path,
            ImmutableSet.of());
    var initial =
        PredicateAbstractState.mkAbstractionState(
            path, abstraction, PathCopyingPersistentTreeMap.of());
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(path, initial);
  }

  @Test
  public void equalRawFormulasWithDifferentBoundaryValuesAreNotEqual() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("x@2"), ints.makeNumber(1)));
    var first = condition(formula, 1);
    var second = condition(formula, 2);
    var coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isFalse();
    assertThat(coverage.isSubsumed(first, second)).isFalse();
    assertThat(coverage.isSubsumed(second, first)).isFalse();
  }

  @Test
  public void equalBoundaryConditionsWithDifferentSsaIndicesAreRecognizedSyntactically()
      throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var first = condition(ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)), 1);
    var second = condition(ints.equal(ints.makeVariable("x@2"), ints.makeNumber(0)), 2);
    var coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isTrue();
    assertThat(coverage.areStatesEqual(first, second)).isTrue();
  }

  @Test
  public void uninstantiatedBoundaryVariablesStayPublic() throws Exception {
    var ints = mgrv.getIntegerFormulaManager();
    var first = condition(ints.equal(ints.makeVariable("x"), ints.makeNumber(0)), 1);
    var second = condition(ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)), 1);
    var coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isTrue();
  }
}
