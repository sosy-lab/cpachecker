// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class PredicateOperatorUtilTest extends SolverViewBasedTest0 {
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

  @Test
  public void intermediateValuesAreIndependentAcrossConditionUses() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula = ints.equal(ints.makeVariable("x@1"), ints.makeVariable("x@2"));
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, 2).build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    BooleanFormula first =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    BooleanFormula second =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    IntegerFormula x = ints.makeVariable("x");
    first = mgrv.substitute(first, ImmutableMap.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, ImmutableMap.of(x, ints.makeNumber(1)));
    // Both instances are satisfiable with their own intermediate value. Reusing x.1 would
    // incorrectly require this private value to be both zero and one.
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }

  @Test
  public void alreadyUninstantiatedPrivateValuesAreStillFreshened() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula = ints.equal(ints.makeVariable("local!value"), ints.makeVariable("x"));
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    BooleanFormula first =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    BooleanFormula second =
        PredicateOperatorUtil.uninstantiate(
                path, mgrv, PredicateOperatorUtil.UniqueIndexProvider.withUUID())
            .booleanFormula();
    IntegerFormula x = ints.makeVariable("x");
    first = mgrv.substitute(first, ImmutableMap.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, ImmutableMap.of(x, ints.makeNumber(1)));
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }

  @Test
  public void fieldNamesDoNotCollapseDifferentSsaValues() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("record.field@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("record.field@2"), ints.makeNumber(1)));
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap()
                    .builder()
                    .setIndex("record.field", CNumericTypes.INT, 2)
                    .build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    BooleanFormula condition =
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
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula =
        bmgrv.and(
            ints.equal(ints.makeVariable("x@1"), ints.makeNumber(0)),
            ints.equal(ints.makeVariable("x!1"), ints.makeNumber(1)),
            ints.equal(ints.makeVariable("private!v@1"), ints.makeNumber(2)),
            ints.equal(ints.makeVariable("private!v@2"), ints.makeNumber(3)),
            ints.equal(ints.makeVariable("private!v#at2"), ints.makeNumber(4)),
            ints.equal(ints.makeVariable("x@2"), ints.makeNumber(5)));
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap().builder().setIndex("x", CNumericTypes.INT, 2).build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    BooleanFormula normalized = PredicateOperatorUtil.normalizeForComparison(path, mgrv);
    assertThat(solver.isUnsat(normalized)).isFalse();
    assertThat(solver.implies(normalized, ints.equal(ints.makeVariable("x"), ints.makeNumber(5))))
        .isTrue();
    assertThat(mgrv.extractVariableNames(normalized)).hasSize(6);
  }

  @Test
  public void latestNondeterministicValuesArePrivateOnEveryUse() throws Exception {
    IntegerFormulaManagerView ints = mgrv.getIntegerFormulaManager();
    BooleanFormula formula =
        ints.equal(ints.makeVariable("__VERIFIER_nondet_int@2"), ints.makeVariable("x@1"));
    PathFormula path =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(
                SSAMap.emptySSAMap()
                    .builder()
                    .setIndex("__VERIFIER_nondet_int", CNumericTypes.INT, 2)
                    .setIndex("x", CNumericTypes.INT, 1)
                    .build(),
                PointerTargetSet.emptyPointerTargetSet())
            .withFormula(formula);
    BooleanFormula first = PredicateOperatorUtil.uninstantiate(path, mgrv).booleanFormula();
    BooleanFormula second = PredicateOperatorUtil.uninstantiate(path, mgrv).booleanFormula();
    IntegerFormula x = ints.makeVariable("x");
    first = mgrv.substitute(first, ImmutableMap.of(x, ints.makeNumber(0)));
    second = mgrv.substitute(second, ImmutableMap.of(x, ints.makeNumber(1)));
    assertThat(solver.isUnsat(bmgrv.and(first, second))).isFalse();
  }
}
