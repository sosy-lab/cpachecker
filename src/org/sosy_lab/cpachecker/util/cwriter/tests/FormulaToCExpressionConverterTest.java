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
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

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

    private void skipTestForSolvers(Iterable<Solvers> solversToSkip) {
      assume()
          .withMessage(
              "Solver %s does not support tested features or uses different representation",
              solverToUse())
          .that(solverToUse())
          .isNotIn(solversToSkip);
    }

    @Test
    public void convertVar() throws InterruptedException {
      BooleanFormula var = bmgrv.makeVariable("x");
      assertThat(converter.formulaToCExpression(var)).isEqualTo("x");
    }

    @Test
    public void convertConstant() throws InterruptedException {
      BooleanFormula trueFormula = bmgrv.makeTrue();
      assertThat(converter.formulaToCExpression(trueFormula)).isEqualTo("true");
      BooleanFormula falseFormula = bmgrv.makeFalse();
      assertThat(converter.formulaToCExpression(falseFormula)).isEqualTo("false");
    }

    @Test
    public void convertNot() throws InterruptedException {
      BooleanFormula formula = bmgrv.not(bmgrv.makeVariable("x"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!(x)");
    }

    @Test
    public void convertAnd() throws InterruptedException {
      BooleanFormula formula = bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x && y)");
    }

    @Test
    public void convertNegatedAnd() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.and(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(!(x)\n|| !(y))");
    }

    @Test
    public void convertOr() throws InterruptedException {
      BooleanFormula formula = bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x\n|| y)");
    }

    @Test
    public void convertNegatedOr() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.or(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(!(x) && !(y))");
    }

    @Test
    public void convertImplication() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3));
      BooleanFormula formula = bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(!(x)\n|| y)");
    }

    @Test
    public void convertNegatedImplication() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(bmgrv.implication(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x && !(y))");
    }

    @Test
    public void convertEquivalence() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.Z3));
      BooleanFormula formula = bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula))
          .isEqualTo("((x && y)\n|| (!(x) && !(y)))");
    }

    @Test
    public void convertNegatedEquivalence() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.Z3));
      BooleanFormula formula =
          bmgrv.not(bmgrv.equivalence(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula))
          .isEqualTo("((!(x)\n|| !(y)) && (x\n|| y))");
    }

    @Test
    public void convertXor() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula = bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula))
          .isEqualTo("((x && !(y))\n|| (!(x) && y))");
    }

    @Test
    public void convertNegatedXor() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula =
          bmgrv.not(bmgrv.xor(bmgrv.makeVariable("x"), bmgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula))
          .isEqualTo("((!(x)\n|| y) && (x\n|| !(y)))");
    }

    @Test
    public void convertEqual() throws InterruptedException {
      BooleanFormula formula = imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x == y)");
    }

    @Test
    public void convertNegatedEqual() throws InterruptedException {
      BooleanFormula formula =
          bmgrv.not(imgrv.equal(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!((x == y))");
    }

    @Test
    public void convertEqualsZero() throws InterruptedException {
      // TODO: How to create formula containing FunctionDeclarationKind.EQ_ZERO
    }

    @Test
    public void convertGreaterOrEqualsZero() throws InterruptedException {
      // TODO: How to create formula containing FunctionDeclarationKind.GTE_ZERO
    }

    @Test
    public void convertLessThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula = imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x < y)");
    }

    @Test
    public void convertNegatedLessThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula =
          bmgrv.not(imgrv.lessThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!((x < y))");
    }

    @Test
    public void convertLessOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.PRINCESS));
      BooleanFormula formula = imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x <= y)");
    }

    @Test
    public void convertNegatedLessOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.PRINCESS));
      BooleanFormula formula =
          bmgrv.not(imgrv.lessOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!((x <= y))");
    }

    @Test
    public void convertGreaterThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula = imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x > y)");
    }

    @Test
    public void convertNegatedGreaterThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterThan(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!((x > y))");
    }

    @Test
    public void convertGreaterOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.PRINCESS));
      BooleanFormula formula =
          imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x >= y)");
    }

    @Test
    public void convertNegatedGreaterOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.PRINCESS));
      BooleanFormula formula =
          bmgrv.not(imgrv.greaterOrEquals(imgrv.makeVariable("x"), imgrv.makeVariable("y")));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("!((x >= y))");
    }

    @Test
    public void convertAddition() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5));
      BooleanFormula formula =
          imgrv.equal(
              imgrv.add(imgrv.makeVariable("x"), imgrv.makeVariable("y")), imgrv.makeVariable("z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x + y) == z)");
    }

    @Test
    public void convertSubtraction() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.Z3, Solvers.PRINCESS));
      BooleanFormula formula =
          imgrv.equal(
              imgrv.subtract(imgrv.makeVariable("x"), imgrv.makeVariable("y")),
              imgrv.makeVariable("z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x - y) == z)");
    }

    @Test
    public void convertUnaryMinus() throws InterruptedException {
      // TODO: How to create formula containing FunctionDeclarationKind.UMINUS
    }

    @Test
    public void convertMultiplication() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5));
      BooleanFormula formula =
          imgrv.equal(
              imgrv.multiply(imgr.makeNumber(3), imgrv.makeVariable("x")), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((3 * x) == y)");
    }

    @Test
    public void convertDivision() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.CVC4));
      BooleanFormula formula =
          imgrv.equal(
              imgrv.divide(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x / 2) == y)");
    }

    @Test
    public void convertModulo() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.CVC4));
      BooleanFormula formula =
          imgrv.equal(
              imgrv.modulo(imgrv.makeVariable("x"), imgrv.makeNumber(2)), imgrv.makeVariable("y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x % 2) == y)");
    }

    @Test
    public void convertBVEqual() throws InterruptedException {
      // TODO: How to create formula containing FunctionDeclarationKind.BV_EQ
    }

    @Test
    public void convertBVLessThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("(x < y)");
      BooleanFormula unsigned =
          bvmgrv.lessThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("(x < y)");
    }

    @Test
    public void convertBVLessOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("(x <= y)");
      BooleanFormula unsigned =
          bvmgrv.lessOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("(x <= y)");
    }

    @Test
    public void convertBVGreaterThan() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("(x > y)");
      BooleanFormula unsigned =
          bvmgrv.greaterThan(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("(x > y)");
    }

    @Test
    public void convertBVGreaterOrEquals() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true);
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("(x >= y)");
      BooleanFormula unsigned =
          bvmgrv.greaterOrEquals(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false);
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("(x >= y)");
    }

    @Test
    public void convertBVNot() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.not(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(~(x) == y)");
    }

    @Test
    public void convertBVAnd() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.and(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x && y) == z)");
    }

    @Test
    public void convertBVOr() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.or(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x\n|| y) == z)");
    }

    @Test
    public void convertBVAddition() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.add(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x + y) == z)");
    }

    @Test
    public void convertBVSubtraction() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.subtract(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x - y) == z)");
    }

    @Test
    public void convertBVNeg() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(bvmgrv.negate(bvmgrv.makeVariable(5, "x")), bvmgrv.makeVariable(5, "y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(-(x) == y)");
    }

    @Test
    public void convertBVMultiplication() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.multiply(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y")),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x * y) == z)");
    }

    @Test
    public void convertBVDivision() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.equal(
              bvmgrv.divide(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("((x / y) == z)");
      BooleanFormula unsigned =
          bvmgrv.equal(
              bvmgrv.divide(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("((x / y) == z)");
    }

    @Test
    public void convertBVModulo() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula signed =
          bvmgrv.equal(
              bvmgrv.modulo(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), true),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(signed)).isEqualTo("((x % y) == z)");
      BooleanFormula unsigned =
          bvmgrv.equal(
              bvmgrv.modulo(bvmgrv.makeVariable(5, "x"), bvmgrv.makeVariable(5, "y"), false),
              bvmgrv.makeVariable(5, "z"));
      assertThat(converter.formulaToCExpression(unsigned)).isEqualTo("((x % y) == z)");
    }

    @Test
    public void convertBVSHL() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS, Solvers.CVC4));
      BitvectorFormulaManagerView bvmgrv = mgrv.getBitvectorFormulaManager();
      BooleanFormula formula =
          bvmgrv.equal(
              bvmgrv.shiftLeft(bvmgrv.makeVariable(8, "x"), bvmgrv.makeBitvector(8, 4)),
              bvmgrv.makeVariable(8, "y"));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x << 4) == y)");
    }

    @Test
    public void convertFPEqual() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x == y)");
    }

    @Test
    public void convertFPLessThan() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessThan(
              fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x < y)");
    }

    @Test
    public void convertFPLessOrEquals() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.lessOrEquals(
              fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x <= y)");
    }

    @Test
    public void convertFPGreaterThan() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterThan(
              fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x > y)");
    }

    @Test
    public void convertFPGreaterOrEquals() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.MATHSAT5, Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.greaterOrEquals(
              fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(x >= y)");
    }

    @Test
    public void convertFPAddition() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS, Solvers.CVC4));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.add(
                  fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x + y) == z)");
    }

    @Test
    public void convertFPSubtraction() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS, Solvers.CVC4));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.subtract(
                  fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x - y) == z)");
    }

    @Test
    public void convertFPNeg() throws InterruptedException {
      skipTestForSolvers(ImmutableList.of(Solvers.SMTINTERPOL, Solvers.PRINCESS, Solvers.CVC4));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.negate(
                  fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("(-(x) == y)");
    }

    @Test
    public void convertFPMultiplication() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS, Solvers.CVC4));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.multiply(
                  fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x * y) == z)");
    }

    @Test
    public void convertFPDivision() throws InterruptedException {
      skipTestForSolvers(
          ImmutableList.of(Solvers.SMTINTERPOL, Solvers.Z3, Solvers.PRINCESS, Solvers.CVC4));
      FloatingPointFormulaManagerView fmgrv = mgrv.getFloatingPointFormulaManager();
      BooleanFormula formula =
          fmgrv.equalWithFPSemantics(
              fmgrv.divide(
                  fmgrv.makeVariable("x", FloatingPointType.getSinglePrecisionFloatingPointType()),
                  fmgrv.makeVariable("y", FloatingPointType.getSinglePrecisionFloatingPointType())),
              fmgrv.makeVariable("z", FloatingPointType.getSinglePrecisionFloatingPointType()));
      assertThat(converter.formulaToCExpression(formula)).isEqualTo("((x / y) == z)");
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
