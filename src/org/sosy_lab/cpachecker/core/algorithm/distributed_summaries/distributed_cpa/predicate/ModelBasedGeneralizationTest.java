// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

@RunWith(Parameterized.class)
public class ModelBasedGeneralizationTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getTestSolvers() {
    return new Object[] {Solvers.MATHSAT5, Solvers.SMTINTERPOL};
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  /** In these tests, exactly the variables whose name starts with "e" are existential. */
  private static boolean isExistential(String pName) {
    return pName.startsWith("e");
  }

  private static final int WIDTH = 32;
  private static final int MAX_CUBES = 4;

  private ModelBasedGeneralization generalization;
  private BitvectorFormulaManagerView bv;
  private BitvectorFormula y;
  private BitvectorFormula z;

  @Before
  public void setUp() {
    generalization =
        new ModelBasedGeneralization(solver, new ExistentialProjection(solver), MAX_CUBES, true);
    bv = mgrv.getBitvectorFormulaManager();
    y = bv.makeVariable(WIDTH, "y");
    z = bv.makeVariable(WIDTH, "z");
  }

  private BitvectorFormula number(long pValue) {
    return bv.makeBitvector(WIDTH, pValue);
  }

  private BooleanFormula eq(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.equal(pLeft, pRight);
  }

  private BooleanFormula gt(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.greaterThan(pLeft, pRight, true);
  }

  private BooleanFormula lt(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.lessThan(pLeft, pRight, true);
  }

  private BooleanFormula generalize(BooleanFormula pCondition) throws Exception {
    return generalization
        .generalize(pCondition, ModelBasedGeneralizationTest::isExistential, ImmutableSet.of())
        .orElseThrow();
  }

  private BooleanFormula generalizeWithPrecondition(
      BooleanFormula pCondition, BooleanFormula pPrecondition) throws Exception {
    PathFormulaManager pathFormulaManager =
        new PathFormulaManagerImpl(
            mgrv,
            config,
            logger,
            ShutdownNotifier.createDummy(),
            MachineModel.LINUX32,
            Optional.empty(),
            AnalysisDirection.FORWARD,
            Language.C);
    SSAMap ssa =
        SSAMap.emptySSAMap()
            .builder()
            .setIndex("y", CNumericTypes.INT, 1)
            .setIndex("z", CNumericTypes.INT, 1)
            .build();
    PathFormula condition =
        pathFormulaManager
            .makeEmptyPathFormulaWithContext(ssa, PointerTargetSet.emptyPointerTargetSet())
            .withFormula(mgrv.instantiate(pCondition, ssa));
    return mgrv.uninstantiate(
        generalization.generalize(condition, pPrecondition).orElseThrow().getFormula());
  }

  private void assertImplies(BooleanFormula pStronger, BooleanFormula pWeaker) throws Exception {
    assertWithMessage("%s implies %s", pStronger, pWeaker)
        .that(solver.isUnsat(bmgrv.and(pStronger, bmgrv.not(pWeaker))))
        .isTrue();
  }

  @Test
  public void localVariablesAreRemoved() throws Exception {
    // ∃ e1. e1 = y ∧ z > e1  becomes  z > y
    BitvectorFormula e1 = bv.makeVariable(WIDTH, "e1");
    BooleanFormula generalized = generalize(bmgrv.and(eq(e1, y), gt(z, e1)));
    assertThat(mgrv.extractVariableNames(generalized)).containsExactly("y", "z");
    assertImplies(generalized, gt(z, y));
    assertImplies(gt(z, y), generalized);
  }

  @Test
  public void everyDisjunctIsCoveredWithoutPrecondition() throws Exception {
    BooleanFormula first = bmgrv.and(gt(y, number(10)), eq(z, number(1)));
    BooleanFormula second = bmgrv.and(lt(y, number(0)), eq(z, number(2)));
    BooleanFormula condition = bmgrv.or(first, second);
    BooleanFormula generalized = generalize(condition);
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }

  @Test
  public void disjunctOutsideOfPreconditionIsPreserved() throws Exception {
    // Both disjuncts must survive even though only the first intersects the precondition.
    BooleanFormula first = bmgrv.and(gt(y, number(10)), eq(z, number(1)));
    BooleanFormula second = bmgrv.and(lt(y, number(0)), eq(z, number(2)));
    BooleanFormula condition = bmgrv.or(first, second);
    BooleanFormula generalized = generalizeWithPrecondition(condition, gt(y, number(5)));
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }

  @Test
  public void conditionOutsideOfPreconditionIsPreserved() throws Exception {
    BooleanFormula condition = lt(y, number(0));
    BooleanFormula generalized = generalizeWithPrecondition(condition, gt(y, number(5)));
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }

  @Test
  public void falsePreconditionDoesNotSuppressCondition() throws Exception {
    BooleanFormula condition = eq(y, number(1));
    BooleanFormula generalized = generalizeWithPrecondition(condition, bmgrv.makeFalse());
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }

  @Test
  public void tooManyCubesAreRejected() throws Exception {
    // every value of y needs its own cube
    List<BooleanFormula> points = new ArrayList<>();
    for (int i = 0; i <= MAX_CUBES; i++) {
      points.add(eq(y, number(i)));
    }
    assertThat(
            generalization.generalize(
                bmgrv.or(points), ModelBasedGeneralizationTest::isExistential, ImmutableSet.of()))
        .isEmpty();
  }

  @Test
  public void cubesAreRewrittenOverVocabulary() throws Exception {
    // y = 1 ∨ y = 2 ∨ y = 3 are three cubes, but y > 0 ∧ y < 4 covers them all at once
    BooleanFormula condition = bmgrv.or(eq(y, number(1)), eq(y, number(2)), eq(y, number(3)));
    BooleanFormula generalized =
        generalization
            .generalize(
                condition,
                ModelBasedGeneralizationTest::isExistential,
                ImmutableSet.of(gt(y, number(0)), lt(y, number(4))))
            .orElseThrow();
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
    assertThat(bmgrv.toDisjunctionArgs(generalized, true)).hasSize(1);
  }

  @Test
  public void cubeIsKeptIfVocabularyIsTooCoarse() throws Exception {
    // y > 0 does not imply y = 1, so the rewriting must not use it
    BooleanFormula condition = eq(y, number(1));
    BooleanFormula generalized =
        generalization
            .generalize(
                condition,
                ModelBasedGeneralizationTest::isExistential,
                ImmutableSet.of(gt(y, number(0))))
            .orElseThrow();
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }

  @Test
  public void negatedConjunctionPreservesBothAlternatives() throws Exception {
    // Both ways of falsifying the conjunction must survive a restrictive precondition.
    BooleanFormula condition = bmgrv.not(bmgrv.and(gt(y, number(0)), gt(z, number(0))));
    BooleanFormula generalized = generalizeWithPrecondition(condition, gt(y, number(0)));
    assertImplies(generalized, condition);
    assertImplies(condition, generalized);
  }
}
