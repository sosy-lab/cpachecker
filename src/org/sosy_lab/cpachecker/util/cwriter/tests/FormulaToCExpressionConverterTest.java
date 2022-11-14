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
    public void convertVar() throws InterruptedException {
      BooleanFormula var = bmgrv.makeVariable("x");
      checkThat(converter.formulaToCExpression(var)).isEquivalentTo("x");
    }

    @Test
    public void convertConstant() throws InterruptedException {
      BooleanFormula trueFormula = bmgrv.makeTrue();
      checkThat(converter.formulaToCExpression(trueFormula)).isEquivalentTo("true");
      BooleanFormula falseFormula = bmgrv.makeFalse();
      checkThat(converter.formulaToCExpression(falseFormula)).isEquivalentTo("false");
    }

    @Test
    public void convertNot() throws InterruptedException {
      BooleanFormula formula = bmgrv.not(bmgrv.makeVariable("x"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(!x)");
    }

    @Test
    public void convertAnd() throws InterruptedException {
      BooleanFormula formula = bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x && y)");
    }

    @Test
    public void convertNegatedAnd() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((!x)\n|| (!y))");
    }

    @Test
    public void convertOr() throws InterruptedException {
      BooleanFormula formula = bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x\n|| y)");
    }

    @Test
    public void convertNegatedOr() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((!x) && (!y))");
    }

    @Test
    public void convertImplication() throws InterruptedException {
      BooleanFormula formula = bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected = "((!x)\n|| y)";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(y || (!x))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedImplication() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x && (!y))");
    }

    @Test
    public void convertEquivalence() throws InterruptedException {
      BooleanFormula formula = bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected = "((x && y)\n|| ((!x) && (!y)))";
      if (solverToUse() == Solvers.Z3) {
        expected = "((y || (!x)) && (x || (!y)))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedEquivalence() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      String expected = "(((!x)\n|| (!y)) && (x\n|| y))";
      if (solverToUse() == Solvers.Z3) {
        expected = "((x || y) && ((!x) || (!y)))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertXor() throws InterruptedException {
      BooleanFormula formula = bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      String expected = "((x && (!y))\n|| ((!x) && y))";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.PRINCESS) {
        expected = "(((!x) || (!y)) && (x || y))";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((x || y) && ((!x) || (!y)))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedXor() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      String expected = "(((!x)\n|| y) && (x\n|| (!y)))";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.PRINCESS) {
        expected = "((x && y) || ((!x) && (!y)))";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((y || (!x)) && (x || (!y)))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertEqual() throws InterruptedException {
      BooleanFormula formula = imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x == y)");
    }

    @Test
    public void convertNegatedEqual() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(!(x == y))");
    }

    @Test
    public void convertLessThan() throws InterruptedException {
      BooleanFormula formula = imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected = "(x < y)";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(!(y <= x))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((y + (-1 * x))+ -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedLessThan() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected = "(!(x < y))";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(y <= x)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(!(((y + (-1 * x)) + -1) >= 0))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertLessOrEquals() throws InterruptedException {
      BooleanFormula formula = imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected = "(x <= y)";
      if (solverToUse() == Solvers.PRINCESS) {
        expected = "((y + (-1 * x)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedLessOrEquals() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected = "(!(x<=y))";
      if (solverToUse() == Solvers.PRINCESS) {
        expected = "(!((y + (-1 * x)) >= 0))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertGreaterThan() throws InterruptedException {
      BooleanFormula formula = imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected = "(x > y)";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(!(x<=y))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((x + (-1 * y)) + -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedGreaterThan() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected = "(!(x > y))";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(x <= y)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(!(((x + (-1 * y)) + -1) >= 0))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertGreaterOrEquals() throws InterruptedException {
      BooleanFormula formula =
          imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      String expected = "(x >= y)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(y <= x)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertNegatedGreaterOrEquals() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      String expected = "(!(x >= y))";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(!(y <= x))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(!((x + (-1 * y)) >= 0))";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertAddition() throws InterruptedException {
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
    public void convertSubtraction() throws InterruptedException {
      BooleanFormula formula =
          imgrv.equal(
              imgrv.subtract(imgrv.makeVariable("x"), imgrv.makeVariable("y")),
              imgrv.makeVariable("z"));
      String expected = "((x - y) == z)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "((x + ((-1 * y) + (-1 * z))) == 0)";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((x + (-1 * y)) == z)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) == z)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertMultiplication() throws InterruptedException {
      BooleanFormula formula =
          imgrv.equal(
              imgrv.multiply(imgr.makeNumber(3), imgrv.makeVariable("x")), imgrv.makeVariable("y"));
      String expected = "((3 * x) == y)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(((3 * x) + (-1 * y)) == 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertDivision() throws InterruptedException {
      skipTestForSolvers(Solvers.MATHSAT5, Solvers.PRINCESS);
      BooleanFormula formula =
          imgrv.equal(
              imgrv.divide(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x / 2) == y)");
    }

    @Test
    public void convertModulo() throws InterruptedException {
      skipTestForSolvers(Solvers.MATHSAT5, Solvers.PRINCESS);
      BooleanFormula formula =
          imgrv.equal(
              imgrv.modulo(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x % 2) == y)");
    }

    @Test
    public void convertBVLessThan() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected = "(x < y)";
      if (solverToUse() == Solvers.Z3) {
        expected = "(!(y <= x))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((y + (-1 * x)) + -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVLessOrEquals() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected = "(x <= y)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(!(y<x))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((y + (-1 * x)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVGreaterThan() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected = "(x > y)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(y < x)";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "(!(x <= y))";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((x + (-1 * y)) + -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVGreaterOrEquals() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      BooleanFormula unsigned =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      String expected = "(x >= y)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "(!(x < y))";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "(y <= x)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVNot() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.not(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      String expected = "((~x) == y)";
      if (solverToUse() == Solvers.SMTINTERPOL || solverToUse() == Solvers.PRINCESS) {
        expected = "((x) == y)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVAnd() throws InterruptedException {
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
    public void convertBVOr() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.or(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x | y) == z)");
    }

    @Test
    public void convertBVXor() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.xor(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x ^ y) == z)");
    }

    @Test
    public void convertBVAddition() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.add(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x + y) == z)");
    }

    @Test
    public void convertBVSubtraction() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.subtract(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      String expected = "((x - y) == z)";
      if (solverToUse() == Solvers.MATHSAT5) {
        expected = "((x + (-y)) == z)";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((x + (31 * y)) == z)"; // 31 is equal -1 with bitsize=5
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) == z)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVNeg() throws InterruptedException {
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.negate(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      String expected = "((-x) == y)";
      if (solverToUse() == Solvers.SMTINTERPOL || solverToUse() == Solvers.PRINCESS) {
        expected = "((-1 * x) == y)";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((31 * x) == y)"; // 31 is equal -1 with bitsize=5
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVMultiplication() throws InterruptedException {
      skipTestForSolvers(Solvers.SMTINTERPOL);
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.multiply(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x * y) == z)");
    }

    @Test
    public void convertBVDivision() throws InterruptedException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.Z3);
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

    @Test
    public void convertBVModulo() throws InterruptedException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.Z3);
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.equal(
              bvmgrv.modulo(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true),
              bvmgrv.makeVariable(5, "z"));
      BooleanFormula unsigned =
          bvmgrv.equal(
              bvmgrv.modulo(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false),
              bvmgrv.makeVariable(5, "z"));
      String expected = "((x % y) == z)";
      if (solverToUse() == Solvers.CVC4) {
        expected = "(((0 == y) ? x : (x % y)) == z)";
      }
      checkThat(converter.formulaToCExpression(signed)).isEquivalentTo(expected);
      checkThat(converter.formulaToCExpression(unsigned)).isEquivalentTo(expected);
    }

    @Test
    public void convertBVSHL() throws InterruptedException {
      skipTestForSolvers(
          Solvers.Z3); // How to transform EXTRACT to C expression (without too much overhead?
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.shiftLeft(bvmgrv.makeVariable(8, "x"), bvmgrv.makeBitvector(8, 4)),
              bvmgrv.makeVariable(8, "y"));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x << 4) == y)");
    }

    @Test
    public void convertFPEqual() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("(x == y)");
    }

    @Test
    public void convertFPLessThan() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessThan(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "(x < y)";
      if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((y + (-1 * x)) + -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPLessOrEquals() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessOrEquals(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "(x <= y)";
      if (solverToUse() == Solvers.PRINCESS) {
        expected = "((y + (-1 * x)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPGreaterThan() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterThan(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "(x > y)";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(y < x)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "(((x + (-1 * y)) + -1) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPGreaterOrEquals() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterOrEquals(
              fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "(x >= y)";
      if (solverToUse() == Solvers.MATHSAT5 || solverToUse() == Solvers.Z3) {
        expected = "(y <= x)";
      } else if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) >= 0)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPAddition() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.add(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x + y) == z)");
    }

    @Test
    public void convertFPSubtraction() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.subtract(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "((x - y) == z)";
      if (solverToUse() == Solvers.PRINCESS) {
        expected = "((x + (-1 * y)) == z)";
      } else if (solverToUse() == Solvers.Z3) {
        expected = "((x + (-y)) == z)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPNeg() throws InterruptedException {
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.negate(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType()));
      String expected = "((-x) == y)";
      if (solverToUse() == Solvers.SMTINTERPOL || solverToUse() == Solvers.PRINCESS) {
        expected = "((-1 * x) == y)";
      }
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo(expected);
    }

    @Test
    public void convertFPMultiplication() throws InterruptedException {
      skipTestForSolvers(Solvers.SMTINTERPOL);
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.multiply(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x * y) == z)");
    }

    @Test
    public void convertFPDivision() throws InterruptedException {
      skipTestForSolvers(Solvers.SMTINTERPOL, Solvers.PRINCESS);
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.divide(
                  fmgrv.makeVariable("x", FormulaType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FormulaType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FormulaType.getSinglePrecisionFloatingPointType()));
      checkThat(converter.formulaToCExpression(formula)).isEquivalentTo("((x / y) == z)");
    }
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
