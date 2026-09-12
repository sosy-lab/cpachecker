// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Tests for {@link FormulaToCVisitor}. Every test is a round trip C -> SMT -> C: the formula of a C
 * expression is converted back to a C expression, whose formula must be equivalent to the one we
 * started with. Comparing the formulas instead of the C expressions keeps the tests independent of
 * how the solvers restructure a formula. A round trip mirrors exporting an invariant of a witness
 * and validating it.
 */
@RunWith(Parameterized.class)
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class FormulaToCVisitorTest extends SolverViewBasedTest0 {

  /** The variables that the C expressions of the tests may use. */
  private static final String DECLARATIONS = "int x; int y; unsigned int u; long long ll;";

  @Parameters(name = "{0} {1}")
  public static ImmutableList<Object[]> getAllSolversAndMachineModels() {
    ImmutableList.Builder<Object[]> parameters = ImmutableList.builder();
    for (Solvers solver : Solvers.values()) {
      for (MachineModel machineModel :
          ImmutableList.of(MachineModel.LINUX32, MachineModel.LINUX64)) {
        parameters.add(new Object[] {solver, machineModel});
      }
    }
    return parameters.build();
  }

  @Parameter(0)
  public Solvers solverToUse;

  @Parameter(1)
  public MachineModel machineModel;

  private PathFormulaManager pfmgr;

  @Override
  protected Solvers solverToUse() {
    return solverToUse;
  }

  @Before
  public void createPathFormulaManager() throws Exception {
    pfmgr =
        new PathFormulaManagerImpl(
            mgrv,
            config,
            logger,
            ShutdownNotifier.createDummy(),
            machineModel,
            Optional.empty(),
            AnalysisDirection.FORWARD,
            Language.C);
  }

  /**
   * Asserts that converting the formula of the given C expression back to C preserves its meaning.
   */
  private void assertRoundTrip(String pExpression) throws Exception {
    BooleanFormula formula = toFormula(pExpression);
    String roundTripped = toCExpression(formula);
    assertThatFormula(toFormula(roundTripped)).isEquivalentTo(formula);
  }

  /** Returns the formula of the given C expression, with the variables not instantiated. */
  private BooleanFormula toFormula(String pExpression) throws Exception {
    PathFormula formula =
        TestCfaUtils.toFormula(
            DECLARATIONS,
            pExpression,
            pfmgr,
            Map.entry("analysis.machineModel", machineModel.name()));
    return mgrv.uninstantiate(formula.getFormula());
  }

  /** Returns the C expression that {@link FormulaToCVisitor} creates for the given formula. */
  private String toCExpression(BooleanFormula pFormula) {
    FormulaToCVisitor visitor = new FormulaToCVisitor(mgrv, Function.identity(), machineModel);
    assertThat(mgrv.visit(pFormula, visitor)).isTrue();
    return visitor.getString();
  }

  /**
   * A bitvector constant with its sign bit set must be written as a negative number. Written as the
   * unsigned interpretation of its bit pattern, the literal would get a wider type in C, and the
   * comparison would hold for every value of an {@code int} variable.
   */
  @Test
  public void roundTripNegativeConstantInComparison() throws Exception {
    assertRoundTrip("x < -268435455");
  }

  /** Equality does not tell us the signedness, but the C type of the operands is at least int. */
  @Test
  public void roundTripNegativeConstantInEquality() throws Exception {
    assertRoundTrip("x == -100");
  }

  /** The signedness of the comparison also applies to the constants below it. */
  @Test
  public void roundTripNegativeConstantBelowArithmetic() throws Exception {
    assertRoundTrip("x + -100 < y");
  }

  /**
   * INT_MIN has no representation as a negated literal in C, because the literal is typed before
   * the unary minus is applied.
   */
  @Test
  public void roundTripIntMin() throws Exception {
    assertRoundTrip("x < -2147483647 - 1");
  }

  /**
   * An unsigned comparison of operands with a signed C type needs a cast. Without it, the
   * comparison would hold for every value of an {@code int} variable instead of the non-negative
   * ones.
   */
  @Test
  public void roundTripUnsignedComparison() throws Exception {
    assertRoundTrip("(unsigned int) x < 2147483648u");
  }

  /** An unsigned variable is compared as unsigned without any cast being necessary. */
  @Test
  public void roundTripUnsignedVariable() throws Exception {
    assertRoundTrip("u > 42u");
  }

  /** The type of a constant depends on the bit-width of the operands. */
  @Test
  public void roundTripLongLongConstant() throws Exception {
    assertRoundTrip("ll < -4294967296LL");
  }

  /** A constant on the left-hand side is reinterpreted as well. */
  @Test
  public void roundTripNegativeConstantOnLeftHandSide() throws Exception {
    assertRoundTrip("-1073741824 < x");
  }

  /** The most frequent constant of the witnesses that were rejected: INT_MIN + 1. */
  @Test
  public void roundTripGreaterOrEqual() throws Exception {
    assertRoundTrip("x >= -2147483647");
  }

  /**
   * An unsigned comparison with the constant on the left-hand side needs the cast on both sides.
   */
  @Test
  public void roundTripUnsignedComparisonWithConstantOnLeftHandSide() throws Exception {
    assertRoundTrip("2147483646u < (unsigned int) x");
  }

  /** A shift is signedness-agnostic, so its operands keep the signedness of the comparison. */
  @Test
  public void roundTripShiftBelowComparison() throws Exception {
    requireBitvectorEncoding();
    assertRoundTrip("(x << 1) < -2147483647");
  }

  /** A negation of a comparison over a sum, as exported for the bitvector tasks. */
  @Test
  public void roundTripNegatedComparisonOverSum() throws Exception {
    assertRoundTrip("!(2147483519 < x + -1)");
  }

  /** Signed division reads both of its operands as signed. */
  @Test
  public void roundTripSignedDivision() throws Exception {
    requireBitvectorEncoding();
    assertRoundTrip("x / -2 == 3");
  }

  /** Expressions that need no reinterpretation must not be changed either. */
  @Test
  public void roundTripPositiveConstant() throws Exception {
    assertRoundTrip("x < 100");
  }
}
