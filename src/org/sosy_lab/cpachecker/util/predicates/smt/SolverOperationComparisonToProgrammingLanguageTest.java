// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.TruthJUnit;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/*
 * Note: when using the ManagerViews here, we might end up using different theories
 * internally compared to what one would expect in the test if a solver does not support a method.
 * This is fine however, as if the solver does not support something, and we have a backup that
 * works as expected, we are good.
 */
@RunWith(Parameterized.class)
public class SolverOperationComparisonToProgrammingLanguageTest extends SolverViewBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solverToUse;

  @Override
  protected Solvers solverToUse() {
    return solverToUse;
  }

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    // Force the solver to emulate bitvector logic with integer formulas
    // We will use this to test that division/modula still give the same results as in C even if
    // integer logic is used.
    return super.createTestConfigBuilder().setOption("cpa.predicate.encodeBitvectorsAs", "INTEGER");
  }

  // TODO: find mismatching/matching examples and test only those compared to the -11 - +11 range,
  //  as this is expensive in some solvers.

  @Before
  public void setUp() {
    // TODO: by testing Views instead of the raw managers, we can test the automatic (CPAchecker
    // internal) translation of formulas from one theory into another. Test by raw managers and by
    // views.
    bvmgr = mgrv.getBitvectorFormulaManager();
  }

  /**
   * SMTLib2 Integer divide() dividend 0 test for C/Java division. C/Java does not allow div 0,
   * SMTLib2 allows arbitrary results for div zero. This tests that an unexpected result based on
   * divisor 0 is possible for otherwise impossible operations. This is not in line how C/Java
   * handle div by 0.
   */
  @Test
  public void testSmtIntegerDivisionZeroOperationToC()
      throws SolverException, InterruptedException {
    requireIntegers();
    requireNonLinearIntegerDivision();
    IntegerFormula one = imgr.makeNumber(1);
    IntegerFormula dividend = imgr.makeNumber(5);
    IntegerFormula divisor = imgr.makeVariable("divisor");
    IntegerFormula div = imgr.divide(dividend, divisor);
    BooleanFormula divEq = imgr.equal(div, one);
    // Limits the divisor such that all valid models not based on 0 are blocked (divisor is not
    // 1,2,3,4,5)
    BooleanFormula limitDivisor =
        bmgr.or(
            imgr.lessOrEquals(divisor, imgr.makeNumber(0)),
            imgr.greaterThan(divisor, imgr.makeNumber(5)));
    BooleanFormula assertion = bmgr.and(divEq, limitDivisor);
    // SMT returns only models based on divisor == 0
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(assertion);
      // SAT
      assertThat(!prover.isUnsat()).isTrue();
      assertThat(prover.getModel().evaluate(divisor)).isEqualTo(BigInteger.ZERO);
    }
  }

  /**
   * SMTLib2 Integer divide() base test for C/Java division. C/Java simply truncate integers,
   * SMTLib2 rounds towards TODO TODO: test edge-cases
   */
  @Test
  public void testSmtIntegerDivisionOperationToC() throws SolverException, InterruptedException {
    requireIntegers();
    int dividend = -11;
    int divisor = -10;
    int expectedRes = dividend / divisor;
    IntegerFormula expectedResFormula = imgr.makeNumber(expectedRes);
    IntegerFormula div = imgr.divide(imgr.makeNumber(dividend), imgr.makeNumber(divisor));
    BooleanFormula divEq = imgr.equal(div, expectedResFormula);
    // SMT returns 2, expected 1
    assertThatFormula(divEq).isUnsatisfiable();
  }

  /**
   * Our SMTLib2 Integer division implementation base test for C/Java division. C/Java simply
   * truncate integers, ... TODO TODO: test edge-cases
   */
  @Test
  public void testOurSmtIntegerDivisionOperationToC() throws SolverException, InterruptedException {
    requireIntegers();
    for (int dividend = -11; dividend <= 11; dividend++) {
      for (int divisor = -11; divisor <= 11; divisor++) {
        if (divisor == 0) {
          continue;
        }
        int expectedRes = dividend / divisor;
        BitvectorFormula expectedResFormula = bvmgrv.makeBitvector(32, expectedRes);
        BitvectorFormula div =
            bvmgrv.divide(
                bvmgrv.makeBitvector(32, dividend), bvmgrv.makeBitvector(32, divisor), true);
        BooleanFormula divEq = bvmgrv.equal(div, expectedResFormula);
        assertThatFormula(divEq).isSatisfiable();
      }
    }
  }

  /** BV division() comparison to C/Java division (/) operator. TODO: text TODO: test edge-cases */
  @Test
  public void testSmtBitvectorDivisionOperationToC() throws SolverException, InterruptedException {
    requireBitvectors();
    for (int dividend = -11; dividend <= 11; dividend++) {
      for (int divisor = -11; divisor <= 11; divisor++) {
        if (divisor == 0) {
          continue;
        }
        int expectedRes = dividend / divisor;
        BitvectorFormula expectedResFormula = bvmgr.makeBitvector(16, expectedRes);
        BooleanFormula divEq =
            bvmgr.equal(
                bvmgr.divide(
                    bvmgr.makeBitvector(16, dividend), bvmgr.makeBitvector(16, divisor), true),
                expectedResFormula);
        assertThatFormula(divEq).isSatisfiable();
      }
    }
  }

  /**
   * Integer remainder() is an internally constructed method based on Integer modulo() with the goal
   * of behaving like the % operator in Java/C as closely as possible. TODO: test edge-cases
   */
  @Test
  public void testSmtIntegerRemainderOperationToC() throws SolverException, InterruptedException {
    requireIntegers();
    // The remainder() operation is based on the modulo() operation for Ints
    TruthJUnit.assume()
        .withMessage(
            "Solver %s does not support the modulo operator for the theory of integers",
            this.solverToUse())
        .that(this.solverToUse())
        .isNotEqualTo(Solvers.MATHSAT5);
    for (int dividend = -11; dividend <= 11; dividend++) {
      for (int divisor = -11; divisor <= 11; divisor++) {
        if (divisor == 0) {
          continue;
        }
        int expectedRes = dividend % divisor;
        BitvectorFormula expectedResFormula = bvmgrv.makeBitvector(32, expectedRes);
        BooleanFormula modEq =
            bvmgrv.equal(
                bvmgrv.remainder(
                    bvmgrv.makeBitvector(32, dividend), bvmgrv.makeBitvector(32, divisor), true),
                expectedResFormula);
        assertThatFormula(modEq).isSatisfiable();
      }
    }
  }

  // TODO: ReplaceIntegerWithBitvectorTheory.modulo/div/remainder
  // TODO: ReplaceBitvectorWithNumeralAndFunctionTheory.modulo/div/remainder

  /** Int modulo does not equal the % operator in C/Java! */
  @Test
  public void testSmtIntegerModuloOperationToC() throws SolverException, InterruptedException {
    requireIntegers();
    // TODO: add a alternative implementation as backup based on modular congruence
    TruthJUnit.assume()
        .withMessage(
            "Solver %s does not support the modulo operator for the theory of integers",
            this.solverToUse())
        .that(this.solverToUse())
        .isNotEqualTo(Solvers.MATHSAT5);
    int dividend = -11;
    int divisor = 2;
    int expectedRes = dividend % divisor;
    IntegerFormula expectedResFormula = imgr.makeNumber(expectedRes);
    IntegerFormula mod = imgr.modulo(imgr.makeNumber(dividend), imgr.makeNumber(divisor));
    BooleanFormula modEq = imgr.equal(mod, expectedResFormula);
    assertThatFormula(modEq).isUnsatisfiable();
  }

  /**
   * SMTLib2 Bitvector signed remainder(..., true) does equal the C/Java % operator, except for some
   * edge-cases like divisor 0. TODO: add tests for those.
   */
  @Test
  public void testSmtBitvectorRemainderOperationToC() throws SolverException, InterruptedException {
    requireBitvectors();
    for (int dividend = -11; dividend <= 11; dividend++) {
      for (int divisor = -11; divisor <= 11; divisor++) {
        if (divisor == 0) {
          continue;
        }
        int expectedRes = dividend % divisor;
        BitvectorFormula expectedResFormula = bvmgr.makeBitvector(16, expectedRes);
        BooleanFormula remEq =
            bvmgr.equal(
                bvmgr.remainder(
                    bvmgr.makeBitvector(16, dividend), bvmgr.makeBitvector(16, divisor), true),
                expectedResFormula);
        assertThatFormula(remEq).isSatisfiable();
      }
    }
  }

  /** SMTLib2 Bitvector smodulo() (signed modulo) does not equal the C/Java % operator! */
  @Test
  public void testSmtBitvectorModuloOperationToC() throws SolverException, InterruptedException {
    requireBitvectors();
    // TODO: add a Integer translation that behaves equally to smodulo() in
    // ReplaceBitvectorWithNumeralAndFunctionTheory.class
    TruthJUnit.assume()
        .withMessage(
            "Solver %s does not support the smodulo operator for the theory of bitvectors",
            this.solverToUse())
        .that(this.solverToUse())
        .isNoneOf(Solvers.OPENSMT, Solvers.SMTINTERPOL, Solvers.PRINCESS);
    int dividend = -11;
    int divisor = 2;
    int expectedRes = dividend % divisor;
    BitvectorFormula expectedResFormula = bvmgr.makeBitvector(16, expectedRes);
    BooleanFormula remEq =
        bvmgr.equal(
            bvmgr.smodulo(bvmgr.makeBitvector(16, dividend), bvmgr.makeBitvector(16, divisor)),
            expectedResFormula);

    assertThatFormula(remEq).isUnsatisfiable();
  }
}
