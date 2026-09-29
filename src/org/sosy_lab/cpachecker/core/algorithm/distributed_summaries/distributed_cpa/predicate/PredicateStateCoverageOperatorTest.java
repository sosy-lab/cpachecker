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
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateStateCoverageOperatorTest extends SolverViewBasedTest0 {
  private PathFormulaManager pathFormulaManager;

  @Before
  public void createPathFormulaManager() throws InvalidConfigurationException {
    pathFormulaManager =
        new PathFormulaManagerImpl(
            mgrv,
            config,
            logger,
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD,
            Language.C);
  }

  @Override
  protected Solvers solverToUse() {
    return Solvers.SMTINTERPOL;
  }

  private PredicateAbstractState condition(BooleanFormula formula, int index) {
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, index).build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    AbstractionFormula abstraction =
        new AbstractionFormula(
            mgrv,
            new SymbolicRegionManager(solver).makeTrue(),
            bmgrv.makeTrue(),
            bmgrv.makeTrue(),
            path,
            ImmutableSet.of());
    PredicateAbstractState initial =
        PredicateAbstractState.mkAbstractionState(
            path, abstraction, PathCopyingPersistentTreeMap.of());
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(path, initial);
  }

  @Test
  public void equalRawFormulasWithDifferentBoundaryValuesAreNotEqual() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("x@2"), ints.makeNumber(1)));
    PredicateAbstractState first = condition(formula, 1);
    PredicateAbstractState second = condition(formula, 2);
    PredicateStateCoverageOperator coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isFalse();
    assertThat(coverage.isSubsumed(first, second)).isFalse();
    assertThat(coverage.isSubsumed(second, first)).isFalse();
  }

  @Test
  public void equalBoundaryConditionsWithDifferentSsaIndicesAreRecognizedSyntactically()
      throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    PredicateAbstractState first =
        condition(ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)), 1);
    PredicateAbstractState second =
        condition(ints.equal(ints.makeVariable("x@2"), ints.makeNumber(0)), 2);
    PredicateStateCoverageOperator coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isTrue();
    assertThat(coverage.areStatesEqual(first, second)).isTrue();
  }

  @Test
  public void uninstantiatedBoundaryVariablesStayPublic() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    PredicateAbstractState first =
        condition(ints.equal(ints.makeVariable("x"), ints.makeNumber(0)), 1);
    PredicateAbstractState second =
        condition(ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)), 1);
    PredicateStateCoverageOperator coverage = new PredicateStateCoverageOperator(solver);
    assertThat(coverage.areStatesSyntacticallyEqual(first, second)).isTrue();
  }
}
