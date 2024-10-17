// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.truth.TruthJUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverException;

/*
 * Note: since we are using the ManagerViews here, we might end up using different theories
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

  @Before
  public void setUp() {
    imgr = imgrv;
    bvmgr = mgrv.getBitvectorFormulaManager();
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
        .withMessage("Solver %s does not support the modulo operator for the theory of integers")
        .that(this.solverToUse())
        .isNotEqualTo(Solvers.MATHSAT5);
    for (int dividend = -11; dividend <= 11; dividend++) {
      for (int divisor = -11; divisor <= 11; divisor++) {
        if (divisor == 0) {
          continue;
        }
        int expectedRes = dividend % divisor;
        IntegerFormula expectedResFormula = imgr.makeNumber(expectedRes);
        BooleanFormula modEq =
            imgr.equal(
                imgrv.remainder(imgr.makeNumber(dividend), imgr.makeNumber(divisor)),
                expectedResFormula);
        assertThatFormula(modEq).isSatisfiable();
      }
    }
  }

  /** Int modulo does not equal the % operator in C/Java! */
  @Test
  public void testSmtIntegerModuloOperationToC() throws SolverException, InterruptedException {
    requireIntegers();
    // TODO: add a alternative implementation as backup based on modular congruence
    TruthJUnit.assume()
        .withMessage("Solver %s does not support the modulo operator for the theory of integers")
        .that(this.solverToUse())
        .isNotEqualTo(Solvers.MATHSAT5);
    int dividend = -11;
    int divisor = 2;
    int expectedRes = dividend % divisor;
    IntegerFormula expectedResFormula = imgr.makeNumber(expectedRes);
    // Some of those (e.g. for positive dividend and divisor) are correct, but not all
    BooleanFormula modEq =
        imgr.equal(
            imgrv.modulo(imgr.makeNumber(dividend), imgr.makeNumber(divisor)), expectedResFormula);
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
        .withMessage("Solver %s does not support the smodulo operator for the theory of bitvectors")
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
