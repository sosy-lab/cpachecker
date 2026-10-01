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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

@RunWith(Parameterized.class)
public class ExistentialProjectionTest extends SolverViewBasedTest0 {

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

  private ExistentialProjection projection;
  private BitvectorFormulaManagerView bv;
  private BitvectorFormula y;
  private BitvectorFormula z;

  @Before
  public void setUp() {
    projection = new ExistentialProjection(solver);
    // DSS encodes C integers as bitvectors, like here
    bv = mgrv.getBitvectorFormulaManager();
    y = bv.makeVariable(WIDTH, "y");
    z = bv.makeVariable(WIDTH, "z");
  }

  private BitvectorFormula existential(String pName) {
    return bv.makeVariable(WIDTH, pName);
  }

  private BitvectorFormula number(long pValue) {
    return bv.makeBitvector(WIDTH, pValue);
  }

  private BooleanFormula eq(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.equal(pLeft, pRight);
  }

  private BitvectorFormula add(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.add(pLeft, pRight);
  }

  private BooleanFormula gt(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.greaterThan(pLeft, pRight, true);
  }

  private BooleanFormula lt(BitvectorFormula pLeft, BitvectorFormula pRight) {
    return bv.lessThan(pLeft, pRight, true);
  }

  private void assertEquivalent(BooleanFormula pActual, BooleanFormula pExpected) throws Exception {
    assertThat(solver.isUnsat(bmgrv.not(bmgrv.equivalence(pActual, pExpected)))).isTrue();
  }

  @Test
  public void definitionIsSubstituted() throws Exception {
    // ∃ e1. e1 = y + 1 ∧ z > e1  ≡  z > y + 1
    BitvectorFormula e1 = existential("e1");
    BooleanFormula projected =
        projection.project(
            bmgrv.and(eq(e1, add(y, number(1))), gt(z, e1)),
            ExistentialProjectionTest::isExistential);
    assertThat(mgrv.extractVariableNames(projected)).containsExactly("y", "z");
    assertEquivalent(projected, gt(z, add(y, number(1))));
  }

  @Test
  public void chainOfDefinitionsIsSubstituted() throws Exception {
    // ∃ e1, e2. e2 = e1 + 1 ∧ e1 = y ∧ z = e2  ≡  z = y + 1
    BitvectorFormula e1 = existential("e1");
    BitvectorFormula e2 = existential("e2");
    BooleanFormula projected =
        projection.project(
            bmgrv.and(eq(e2, add(e1, number(1))), eq(e1, y), eq(z, e2)),
            ExistentialProjectionTest::isExistential);
    assertThat(mgrv.extractVariableNames(projected)).containsExactly("y", "z");
    assertEquivalent(projected, eq(z, add(y, number(1))));
  }

  @Test
  public void cyclicDefinitionsStayEquivalent() throws Exception {
    // ∃ e1, e2. e1 = e2 ∧ e2 = e1 ∧ y = e1 ∧ y != e2: the definitions of e1 and e2 form a cycle, so
    // one of them has to stay as a constraint, and the result must be unsatisfiable like the input
    BitvectorFormula e1 = existential("e1");
    BitvectorFormula e2 = existential("e2");
    BooleanFormula formula = bmgrv.and(eq(e1, e2), eq(e2, e1), eq(y, e1), bmgrv.not(eq(y, e2)));
    assertThat(solver.isUnsat(formula)).isTrue();
    BooleanFormula projected =
        projection.project(formula, ExistentialProjectionTest::isExistential);
    assertWithMessage("%s projected to %s", formula, projected)
        .that(solver.isUnsat(projected))
        .isTrue();

    // ∃ e1, e2. e1 = e2 ∧ e2 = e1 ∧ y = e1 ∧ z > y  ≡  z > y
    assertEquivalent(
        projection.project(
            bmgrv.and(eq(e1, e2), eq(e2, e1), eq(y, e1), gt(z, y)),
            ExistentialProjectionTest::isExistential),
        gt(z, y));
  }

  @Test
  public void independentPartIsDropped() throws Exception {
    // ∃ e1. e1 != 0 ∧ y > 0  ≡  y > 0, like the result of a nondeterministic call in a branch
    BitvectorFormula e1 = existential("e1");
    BooleanFormula projected =
        projection.project(
            bmgrv.and(bmgrv.not(eq(e1, number(0))), gt(y, number(0))),
            ExistentialProjectionTest::isExistential);
    assertThat(projected).isEqualTo(gt(y, number(0)));
  }

  @Test
  public void connectedPartIsKept() throws Exception {
    // ∃ e1, e2. y > e1 ∧ e1 > 5 ∧ e2 > 0  ≡  ∃ e1. y > e1 ∧ e1 > 5: only e2 > 0 is independent
    BitvectorFormula e1 = existential("e1");
    BitvectorFormula e2 = existential("e2");
    BooleanFormula kept = bmgrv.and(gt(y, e1), gt(e1, number(5)));
    BooleanFormula projected =
        projection.project(
            bmgrv.and(kept, gt(e2, number(0))), ExistentialProjectionTest::isExistential);
    assertThat(mgrv.extractVariableNames(projected)).containsExactly("y", "e1");
    assertEquivalent(projected, kept);
  }

  @Test
  public void selfReferenceIsNotSubstituted() throws Exception {
    // ∃ e1. e1 = e1 + 1 ∧ y > 0 is unsatisfiable, so the formula is returned unchanged
    BitvectorFormula e1 = existential("e1");
    BooleanFormula formula = bmgrv.and(eq(e1, add(e1, number(1))), gt(y, number(0)));
    assertThat(projection.project(formula, ExistentialProjectionTest::isExistential))
        .isEqualTo(formula);
  }

  @Test
  public void booleanVariableIsDefined() throws Exception {
    // ∃ e1, e2. e1 ∧ ¬e2 ∧ (e1 ⇔ y > 0) ∧ (e2 ⇔ z > 0)  ≡  y > 0 ∧ ¬(z > 0)
    BooleanFormula e1 = bmgrv.makeVariable("e1");
    BooleanFormula e2 = bmgrv.makeVariable("e2");
    BooleanFormula projected =
        projection.project(
            bmgrv.and(
                e1,
                bmgrv.not(e2),
                bmgrv.equivalence(e1, gt(y, number(0))),
                bmgrv.equivalence(e2, gt(z, number(0)))),
            ExistentialProjectionTest::isExistential);
    assertThat(mgrv.extractVariableNames(projected)).containsExactly("y", "z");
    assertEquivalent(projected, bmgrv.and(gt(y, number(0)), bmgrv.not(gt(z, number(0)))));
  }

  @Test
  public void variableInDisjunctionIsNotSubstituted() throws Exception {
    // ∃ e1. e1 = y + 1 ∧ (e1 > 0 ∨ z > 0): substituting would copy the disjunction, so e1 stays
    BitvectorFormula e1 = existential("e1");
    BooleanFormula formula =
        bmgrv.and(eq(e1, add(y, number(1))), bmgrv.or(gt(e1, number(0)), gt(z, number(0))));
    BooleanFormula projected =
        projection.project(formula, ExistentialProjectionTest::isExistential);
    assertThat(mgrv.extractVariableNames(projected)).containsExactly("e1", "y", "z");
    assertEquivalent(projected, formula);
  }

  @Test
  public void disjunctsAreProjectedAndMerged() throws Exception {
    // (∃ e1. e1 = y ∧ z > e1) ∨ (∃ e2. e2 = y ∧ z > e2 ∧ e3 != 0), like two paths aligned by
    // makeOr: both disjuncts become z > y and are kept once
    BitvectorFormula e1 = existential("e1");
    BitvectorFormula e2 = existential("e2");
    BitvectorFormula e3 = existential("e3");
    BooleanFormula projected =
        projection.project(
            bmgrv.or(
                bmgrv.and(eq(e1, y), gt(z, e1)),
                bmgrv.and(eq(e2, y), gt(z, e2), bmgrv.not(eq(e3, number(0))))),
            ExistentialProjectionTest::isExistential);
    assertThat(projected).isEqualTo(gt(z, y));
  }

  @Test
  public void resultDoesNotDependOnConjunctOrder() throws Exception {
    BitvectorFormula e1 = existential("e1");
    BooleanFormula a = eq(e1, y);
    BooleanFormula b = gt(z, e1);
    BooleanFormula c = lt(y, number(10));
    assertThat(projection.project(bmgrv.and(a, b, c), ExistentialProjectionTest::isExistential))
        .isEqualTo(
            projection.project(bmgrv.and(c, b, a), ExistentialProjectionTest::isExistential));
  }

  @Test
  public void formulaWithoutExistentialsIsOnlySorted() throws Exception {
    BooleanFormula formula = bmgrv.and(gt(y, number(0)), lt(z, number(3)));
    assertEquivalent(
        projection.project(formula, ExistentialProjectionTest::isExistential), formula);
    assertThat(
            mgrv.extractVariableNames(
                projection.project(formula, ExistentialProjectionTest::isExistential)))
        .containsExactly("y", "z");
  }

  @Test
  public void nestedProjectionSubstitutesAcrossDisjunction() throws Exception {
    ExistentialProjection nested = new ExistentialProjection(solver, true);
    BitvectorFormula e = existential("e");
    BooleanFormula choice = bmgrv.or(gt(e, number(2)), lt(e, number(0)));
    assertEquivalent(
        nested.project(bmgrv.and(eq(e, y), choice), ExistentialProjectionTest::isExistential),
        bmgrv.or(gt(y, number(2)), lt(y, number(0))));
  }

  @Test
  public void nestedProjectionRetainsSharedExistentialCorrelation() throws Exception {
    ExistentialProjection nested = new ExistentialProjection(solver, true);
    BitvectorFormula e = existential("e");
    BooleanFormula choice =
        bmgrv.or(
            bmgrv.and(eq(e, number(0)), gt(y, number(0))),
            bmgrv.and(eq(e, number(1)), lt(y, number(0))));
    BooleanFormula constraint =
        bmgrv.or(
            bmgrv.and(eq(e, number(0)), lt(z, number(0))),
            bmgrv.and(eq(e, number(1)), gt(z, number(0))));
    BooleanFormula formula = bmgrv.and(choice, constraint);
    // Moving the quantifier separately into both disjunctions would lose their correlation.
    assertEquivalent(nested.project(formula, ExistentialProjectionTest::isExistential), formula);
  }

  @Test
  public void nestedProjectionRemovesContradictoryLocalBranch() throws Exception {
    ExistentialProjection nested = new ExistentialProjection(solver, true);
    BitvectorFormula e = existential("e");
    BooleanFormula formula =
        bmgrv.and(
            gt(y, number(0)),
            bmgrv.or(eq(z, number(0)), bmgrv.and(eq(e, number(1)), eq(e, number(2)))));
    assertEquivalent(
        nested.project(formula, ExistentialProjectionTest::isExistential),
        bmgrv.and(gt(y, number(0)), eq(z, number(0))));
  }
}
