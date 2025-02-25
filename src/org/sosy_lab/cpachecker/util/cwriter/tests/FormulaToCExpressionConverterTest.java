// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.tests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionVisitor.C99_NAN;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.cwriter.CExpressionInvariantExporter;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.ToCTranslationTest;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

/** Tests for {@link FormulaToCExpressionConverter}. */
@RunWith(Enclosed.class)
public class FormulaToCExpressionConverterTest {

  /** Unit tests. */
  @RunWith(Parameterized.class)
  public static class ConversionTest extends SolverViewBasedTest0 {

    private FormulaToCExpressionConverter converter;

    @Parameters(name = "{0}")
    public static Object[] getAllSolvers() {
      return Solvers.values();
    }

    @Parameter public Solvers solverToUse;

    @Override
    protected Solvers solverToUse() {
      return solverToUse;
    }

    @Before
    public void setup() {
      converter = new FormulaToCExpressionConverter(mgrv);
    }

    private void skipTestForSolvers(Solvers... solversToSkip) {
      assume()
          .withMessage(
              "Solver %s does not support tested features or uses different representation",
              solverToUse())
          .that(solverToUse())
          .isNotIn(ImmutableList.copyOf(solversToSkip));
    }

    private static class Observation {
      private String observable;

      private Observation(String pObservable) {
        observable = pObservable;
      }

      private void isEquivalentTo(String reference) {
        assertThat(observable).isNotNull();
        assertThat(reference).isNotNull();
        assertThat(prune(observable)).isEqualTo(prune(reference));
      }

      private static String prune(String input) {
        return input.replaceAll("\\s+", "");
      }
    }

    private static Observation checkThat(String formula) {
      return new Observation(formula);
    }

    // TODO: Add tests for FunctionDeclarationKind.EQ_ZERO, FunctionDeclarationKind.GTE_ZERO,
    //                     FunctionDeclarationKind.UMINUS, FunctionDeclarationKind.BV_EQ
    //  Problem: How to create Formulas containing these?
    //
    // Answer:
    // Depending on solver-internal functions and formula-structure is fragile and should be
    // avoided. Some function declarations can not be enforced from user-side.

    @Test
    public void convertVar() throws InterruptedException, SolverException {
      BooleanFormula var = bmgrv.makeVariable("x");
      checkThat(converter.formulaToCExpression(var)).isEquivalentTo("x");
    }

    @Test
    public void convertConstant() throws InterruptedException, SolverException {
      BooleanFormula trueFormula = bmgrv.makeTrue();
      checkThat(converter.formulaToCExpression(trueFormula)).isEquivalentTo("true");
      BooleanFormula falseFormula = bmgrv.makeFalse();
      checkThat(converter.formulaToCExpression(falseFormula)).isEquivalentTo("false");
    }

    @Test
    public void convertNot() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.not(bmgrv.makeVariable("x"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(!x)");
    }

    @Test
    public void convertAnd() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x && y)");
    }

    @Test
    public void convertNegatedAnd() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> "(!(x && y))";
            default -> "((!x) || (!y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertOr() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> "(!((!x) && (!y)))";
            default -> "(x || y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedOr() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((!x) && (!y))");
    }

    @Test
    public void convertImplication() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3 -> "(y || (!x))";
            case BITWUZLA -> "(!(x && (!y)))";
            default -> "((!x) || y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedImplication() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x && (!y))");
    }

    @Test
    public void convertEquivalence() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case Z3 -> "((y || (!x)) && (x || (!y)))";
            case BITWUZLA -> "(x == y)";
            default -> "((x && y) || ((!x) && (!y)))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedEquivalence() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case Z3 -> "((x || y) && ((!x) || (!y)))";
            case BITWUZLA -> "((!((!x) && (!y))) && (!(x && y)))";
            default -> "(((!x) || (!y)) && (x || y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertXor() throws InterruptedException, SolverException {
      BooleanFormula formula = bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, PRINCESS -> "(((!x) || (!y)) && (x || y))";
            case Z3 -> "((x || y) && ((!x) || (!y)))";
            case BITWUZLA -> "((!x) == y)";
            default -> "((x && (!y)) || ((!x) && y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedXor() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, PRINCESS -> "((x && y) || ((!x) && (!y)))";
            case Z3 -> "((y || (!x)) && (x || (!y)))";
            case BITWUZLA -> "((!(y && (!x))) && (!(x && (!y))))";
            default -> "(((!x) || y) && (x || (!y)))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertEqual() throws InterruptedException, SolverException {
      BooleanFormula formula = imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x == y)");
    }

    @Test
    public void convertNegatedEqual() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(!(x == y))");
    }

    @Test
    public void convertLessThan() throws InterruptedException, SolverException {
      BooleanFormula formula = imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3 -> "(!(y <= x))";
            case PRINCESS -> "(((y + (-1 * x)) + -1) >= 0)";
            case OPENSMT -> "(!(0 <= (x + (-1 * y))))";
            default -> "(x < y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedLessThan() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3 -> "(y <= x)";
            case PRINCESS -> "(!(((y + (-1 * x)) + -1) >= 0))";
            case OPENSMT -> "(0 <= (x + (-1 * y)))";
            default -> "(!(x < y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertLessOrEquals() throws InterruptedException, SolverException {
      BooleanFormula formula = imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case PRINCESS -> "((y + (-1 * x)) >= 0)";
            case OPENSMT -> "(0 <= ((-1 * x) + y))";
            case BITWUZLA -> "(!(y < x))";
            default -> "(x <= y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedLessOrEquals() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case PRINCESS -> "(!((y + (-1 * x)) >= 0))";
            case OPENSMT -> "(!(0 <= ((-1 * x) + y)))";
            case BITWUZLA -> "(y < x)";
            default -> "(!(x <= y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertGreaterThan() throws InterruptedException, SolverException {
      BooleanFormula formula = imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3 -> "(!(x <= y))";
            case PRINCESS -> "(((x + (-1 * y)) + -1) >= 0)";
            case OPENSMT -> "(!(0 <= ((-1 * x) + y)))";
            case BITWUZLA -> "(y < x)";
            default -> "(x > y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedGreaterThan() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3 -> "(x <= y)";
            case PRINCESS -> "(!(((x + (-1 * y)) + -1) >= 0))";
            case OPENSMT -> "(0 <= ((-1 * x) + y))";
            case BITWUZLA -> "(!(y < x))";
            default -> "(!(x > y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertGreaterOrEquals() throws InterruptedException, SolverException {
      BooleanFormula formula =
          imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5 -> "(y <= x)";
            case PRINCESS -> "((x + (-1 * y)) >= 0)";
            case OPENSMT -> "(0 <= (x + (-1 * y)))";
            case BITWUZLA -> "(!(x < y))";
            default -> "(x >= y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedGreaterOrEquals() throws InterruptedException, SolverException {
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5 -> "(!(y <= x))";
            case PRINCESS -> "(!((x + (-1 * y)) >= 0))";
            case OPENSMT -> "(!(0 <= (x + (-1 * y))))";
            case BITWUZLA -> "(x < y)";
            default -> "(!(x >= y))";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertAddition() throws InterruptedException, SolverException {
      BooleanFormula formula =
          imgrv.equal(
              imgrv.add(imgrv.makeVariable("x"), imgrv.makeVariable("y")), imgrv.makeVariable("z"));
      String expected = "((x + y) == z)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "((x + (y + (-1 * z))) == 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertSubtraction() throws InterruptedException, SolverException {
      BooleanFormula formula =
          imgrv.equal(
              imgrv.subtract(imgrv.makeVariable("x"), imgrv.makeVariable("y")),
              imgrv.makeVariable("z"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5 -> "((x + ((-1 * y) + (-1 * z))) == 0)";
            case Z3, PRINCESS, OPENSMT -> "((x + (-1 * y)) == z)";
            case BITWUZLA -> "((y + z) == x)";
            default -> "((x - y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertMultiplication() throws InterruptedException, SolverException {
      requireIntegers();

      BooleanFormula formula =
          imgrv.equal(
              imgrv.multiply(imgr.makeNumber(3), imgrv.makeVariable("x")), imgrv.makeVariable("y"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5 -> "(((3 * x) + (-1 * y)) == 0)";
            default -> "((3 * x) == y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertDivision() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.MATHSAT5, Solvers.PRINCESS, Solvers.BITWUZLA);
      BooleanFormula formula =
          imgrv.equal(
              imgrv.divide(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x / 2) == y)");
    }

    @Test
    public void convertModulo() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.MATHSAT5, Solvers.PRINCESS, Solvers.BITWUZLA);
      BooleanFormula formula =
          imgrv.equal(
              imgrv.modulo(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x % 2) == y)");
    }

    @Test
    public void convertBVLessThan() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected =
          switch (solverToUse()) {
            case Z3 -> "(!(y <= x))";
            case PRINCESS -> "(((y + (-1 * x)) + -1) >= 0)";
            case OPENSMT -> "(!(0 <= (x + (-1 * y))))";
            default -> "(x < y)";
          };
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVLessOrEquals() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, BITWUZLA -> "(!(y<x))";
            case PRINCESS -> "((y + (-1 * x)) >= 0)";
            case OPENSMT -> "(0 <= ((-1 * x) + y))";
            default -> "(x <= y)";
          };
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVGreaterThan() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, BITWUZLA -> "(y < x)";
            case Z3 -> "(!(x <= y))";
            case PRINCESS -> "(((x + (-1 * y)) + -1) >= 0)";
            case OPENSMT -> "(!(0 <= ((-1 * x) + y)))";
            default -> "(x > y)";
          };
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVGreaterOrEquals() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, BITWUZLA -> "(!(x < y))";
            case Z3 -> "(y <= x)";
            case PRINCESS -> "((x + (-1 * y)) >= 0)";
            case OPENSMT -> "(0 <= (x + (-1 * y)))";
            default -> "(x >= y)";
          };
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVNot() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.not(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      String expected =
          switch (solverToUse()) {
            case SMTINTERPOL, PRINCESS, OPENSMT -> "((x) == y)";
            default -> "((~x) == y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVAnd() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.and(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      String expected = "((x & y) == z)";
      if (solverToUse() == Solvers.Z3) {
        expected = "((~((~x) | (~y))) == z)"; // DeMorgan on bit-level
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVOr() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.or(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> "((~((~x) & (~y))) == z)";
            default -> "((x | y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVXor() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.xor(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x ^ y) == z)");
    }

    @Test
    public void convertBVAddition() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.add(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x + y) == z)");
    }

    @Test
    public void convertBVSubtraction() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.subtract(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5 -> "((x + (-y)) == z)";
            case Z3 -> "((x + (31 * y)) == z)"; // 31 is equal -1 with bitsize=5
            case PRINCESS, OPENSMT -> "((x + (-1 * y)) == z)";
            case BITWUZLA -> "((y + z) == x)";
            default -> "((x - y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVNeg() throws InterruptedException, SolverException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.negate(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      String expected =
          switch (solverToUse()) {
            case SMTINTERPOL, PRINCESS, OPENSMT -> "((-1 * x) == y)";
            case Z3 -> "((31 * x) == y)";
            case BITWUZLA -> "(((~x) + 1) == y)";
            default -> "((-x) == y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVMultiplication() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.OPENSMT);
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.multiply(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x * y) == z)");
    }

    @Test
    public void convertBVDivision() throws InterruptedException, SolverException {
      skipTestForSolvers(
          Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.Z3, Solvers.OPENSMT, Solvers.BITWUZLA);
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.equal(
              bvmgrv.divide(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true),
              bvmgrv.makeVariable(5, "z"));
      BooleanFormula unsigned =
          bvmgrv.equal(
              bvmgrv.divide(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false),
              bvmgrv.makeVariable(5, "z"));
      String expected = "((x / y) == z)";
      if (solverToUse() == Solvers.CVC4) {
        expected = "(((0 == y) ? ((x < 0) ? 1 : 31) : (x / y)) == z)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    /*
     * The correct equivalent for the % operator in C is signed remainder in SMTLIB2.
     */
    @Test
    public void convertBVModulo() throws InterruptedException, SolverException {
      skipTestForSolvers(
          Solvers.SMTINTERPOL,
          Solvers.OPENSMT,
          Solvers.MATHSAT5,
          Solvers.PRINCESS,
          Solvers.BITWUZLA);
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.equal(
              bvmgrv.remainder(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true),
              bvmgrv.makeVariable(5, "z"));
      BooleanFormula unsigned =
          bvmgrv.equal(
              bvmgrv.remainder(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false),
              bvmgrv.makeVariable(5, "z"));
      String expected = "((x % y) == z)";
      if (solverToUse() == Solvers.CVC4) {
        expected = "(((0 == y) ? x : (x % y)) == z)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVSHL() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.Z3, Solvers.BITWUZLA);
      // TODO How to transform EXTRACT to C expression (without too much overhead?
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.shiftLeft(bvmgrv.makeVariable(8, "x"), bvmgrv.makeBitvector(8, 4)),
              bvmgrv.makeVariable(8, "y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x << 4) == y)");
    }

    @Test
    public void convertFPEqual() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> getFpEqualityForBitwuzla("x", "y", "x == y");
            default -> "(x == y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPLessThan() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessThan(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case PRINCESS -> "(((y + (-1 * x)) + -1) >= 0)";
            case OPENSMT -> "(!(0<=(x+(-1*y))))";
            default -> "(x < y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPLessOrEquals() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessOrEquals(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case PRINCESS -> "((y + (-1 * x)) >= 0)";
            case OPENSMT -> "(0 <= ((-1 * x) + y))";
            default -> "(x <= y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPGreaterThan() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterThan(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3, BITWUZLA -> "(y < x)";
            case PRINCESS -> "(((x + (-1 * y)) + -1) >= 0)";
            case OPENSMT -> "(!(0 <= ((-1 * x) + y)))";
            default -> "(x > y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPGreaterOrEquals() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterOrEquals(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case MATHSAT5, Z3, BITWUZLA -> "(y <= x)";
            case PRINCESS -> "((x + (-1 * y)) >= 0)";
            case OPENSMT -> "(0 <= (x + (-1 * y)))";
            default -> "(x >= y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPAddition() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.add(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> getFpEqualityForBitwuzla("(x + y)", "z", "(x + y) == z");
            default -> "((x + y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPSubtraction() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.subtract(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case PRINCESS, OPENSMT -> "((x + (-1 * y)) == z)";
            case Z3 -> "((x + (-y)) == z)";
            case BITWUZLA -> getFpEqualityForBitwuzla("(x + (-y))", "z", "(x + (-y)) == z");
            default -> "((x - y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPNeg() throws InterruptedException, SolverException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.negate(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case SMTINTERPOL, PRINCESS, OPENSMT -> "((-1 * x) == y)";
            case BITWUZLA -> getFpEqualityForBitwuzla("x", "y", "(-x) == y");
            default -> "((-x) == y)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPMultiplication() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.OPENSMT);
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.multiply(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> getFpEqualityForBitwuzla("(x * y)", "z", "(x * y) == z");
            default -> "((x * y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPDivision() throws InterruptedException, SolverException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.OPENSMT);
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.divide(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected =
          switch (solverToUse()) {
            case BITWUZLA -> getFpEqualityForBitwuzla("(x / y)", "z", "(x / y) == z");
            default -> "((x / y) == z)";
          };
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }
  }

  private static String getFpEqualityForBitwuzla(String a, String b, String aEqB) {
    return "(((!("
        + a
        + "=="
        + C99_NAN
        + ")) && (!("
        + b
        + "=="
        + C99_NAN
        + "))) && (!((!("
        + aEqB
        + ")) && (!(("
        + a
        + "== 0.0) && ("
        + b
        + "== 0.0))))))";
  }

  @RunWith(Parameterized.class)
  public static class TranslationTest extends ToCTranslationTest {

    private static final String TEST_DIR = "test/programs/induction/";

    private final Path program;
    private final Configuration config;

    public TranslationTest(Path pProgram, boolean pVerdict)
        throws InvalidConfigurationException, IOException {
      super(
          TempFile.builder().create().toAbsolutePath(),
          pVerdict,
          TestDataTools.configurationForTest()
              .loadFromResource(TranslationTest.class, "kInduction-kipdrdfInvariants.properties")
              .build());
      program = pProgram;
      config =
          TestDataTools.configurationForTest()
              .loadFromResource(
                  FormulaToCExpressionConverterTest.class,
                  "kInduction-kipdrdfInvariants.properties")
              .build();
    }

    @Override
    protected void createProgram(Path pTargetPath) throws Exception {
      CPAcheckerResult result = CPATestRunner.run(config, program.toString()).getCheckerResult();
      CFA cfa = result.getCfa();
      UnmodifiableReachedSet reached = result.getReached();
      assertThat(cfa).isNotNull();
      assertThat(reached).isNotNull();

      CExpressionInvariantExporter exporter =
          new CExpressionInvariantExporter(
              config,
              LogManager.createTestLogManager(),
              ShutdownNotifier.createDummy(),
              PathTemplate.ofFormatString(pTargetPath.toString()));
      exporter.exportInvariant(cfa, reached);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      ImmutableList.Builder<Object[]> b = ImmutableList.builder();
      b.add(exportInvariantsTest("induction1.c", true));
      b.add(exportInvariantsTest("induction2.c", true));
      b.add(exportInvariantsTest("induction2_BUG.c", false));
      b.add(exportInvariantsTest("induction-next-state.c", true));
      return b.build();
    }

    private static Object[] exportInvariantsTest(String program, boolean verdict) {
      return new Object[] {Path.of(TEST_DIR, program), verdict};
    }
  }
}
