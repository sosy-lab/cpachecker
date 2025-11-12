// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.java_smt.SolverContextFactory.Solvers.PRINCESS;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.function.BinaryOperator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.cpachecker.util.predicates.smt.ReplaceIntegerWithBitvectorTheory.ReplaceIntegerEncodingOptions;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@RunWith(Parameterized.class)
public class ReplaceBitvectorWithNLAIntegerTheoryTest extends SolverViewBasedTest0 {
  @Parameters(name = "{0}")
  public static Solvers[] getAllSolvers() {
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
    ConfigurationBuilder testConfigBuilder = super.createTestConfigBuilder();
    testConfigBuilder.setOption("solver.nonLinearArithmetic", "USE");
    return testConfigBuilder;
  }

  private FormulaWrappingHandler wrappingHandler;
  private ReplaceBitvectorWithNLAIntegerTheory replacer;

  private void requireModuloDivMultiply() {
    assume()
        .withMessage("Solver %s does not support modulo/div/multiply", solverToUse())
        .that(solverToUse())
        .isNoneOf(Solvers.MATHSAT5, Solvers.CVC4, Solvers.SMTINTERPOL, Solvers.OPENSMT);
  }

  @Before
  public void setupParams() throws InvalidConfigurationException {
    requireIntegers();
    requireModuloDivMultiply();
    wrappingHandler =
        new FormulaWrappingHandler(
            context.getFormulaManager(),
            Theory.INTEGER,
            Theory.FLOAT,
            Theory.INTEGER,
            new ReplaceIntegerEncodingOptions(config));
    replacer = new ReplaceBitvectorWithNLAIntegerTheory(wrappingHandler, bmgr, imgr, fmgr, config);
  }

  @Test
  public void testUnsignedWrapAround() throws Exception {
    final int[] values = new int[] {1, -1, 15, -15, 16, -16, 25, -25};
    for (int value : values) {
      IntegerFormula formula = imgr.makeNumber(value);
      IntegerFormula expected =
          imgr.makeNumber(BigInteger.valueOf(value).mod(BigInteger.valueOf(16)));
      BooleanFormula constraint = imgr.equal(replacer.wrapAround(formula, 4), expected);
      assertThatFormula(constraint).isTautological();
    }
  }

  @Test
  public void testSignedWrapAround() throws Exception {
    final int[] values = new int[] {1, 7, 8, 15};
    for (int value : values) {
      int expectedNumber = value >= 8 ? value - 16 : value;
      IntegerFormula formula = imgr.makeNumber(value);
      IntegerFormula expected = imgr.makeNumber(expectedNumber);
      BooleanFormula constraint = imgr.equal(replacer.mapToSignedRange(formula, 4), expected);
      assertThatFormula(constraint).isTautological();
    }
  }

  @Test
  public void testRangeExtendExtractUnsigned() throws Exception {
    final BitvectorFormula base =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
    final BitvectorFormula larger =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

    BooleanFormula range = replacer.makeDomainRangeConstraint(base, false);
    BooleanFormula extend = replacer.equal(replacer.extend(base, 8, false), larger);
    BooleanFormula extract = bmgr.not(replacer.equal(replacer.extract(larger, 7, 0), base));
    assertThatFormula(bmgr.and(range, extend, extract)).isUnsatisfiable();
  }

  @Test
  public void testRangeExtendExtractSigned() throws Exception {
    final BitvectorFormula base =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
    final BitvectorFormula larger =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

    BooleanFormula range = replacer.makeDomainRangeConstraint(base, true);
    BooleanFormula extend = replacer.equal(replacer.extend(base, 8, true), larger);
    BooleanFormula extract = bmgr.not(replacer.equal(replacer.extract(larger, 7, 0), base));
    assertThatFormula(bmgr.and(range, extend, extract)).isUnsatisfiable();
  }

  // methods to test which are tested against a known baseline -- modulo, remainder, div, etc
  // the first of each pair is the method to test, the second is the baseline.
  private List<Pair<BinaryOperator<BitvectorFormula>, BinaryOperator<BitvectorFormula>>>
      methodsToTest =
          ImmutableList.of(
              Pair.of(
                  (a, b) -> replacer.remainder(a, b, true),
                  (a, b) -> mgrv.getBitvectorFormulaManager().remainder(a, b, true)),
              Pair.of(
                  (a, b) -> replacer.remainder(a, b, false),
                  (a, b) -> mgrv.getBitvectorFormulaManager().remainder(a, b, false)),
              Pair.of(
                  (a, b) -> replacer.divide(a, b, true),
                  (a, b) -> mgrv.getBitvectorFormulaManager().divide(a, b, true)),
              Pair.of(
                  (a, b) -> replacer.divide(a, b, false),
                  (a, b) -> mgrv.getBitvectorFormulaManager().divide(a, b, false)));

  @Test
  public void testModuloRemainderDiv() throws Exception {
    checkModuloRemainderDiv(0, 5);
    checkModuloRemainderDiv(-1, 5);
    checkModuloRemainderDiv(-5, 5);
    checkModuloRemainderDiv(-6, 5);
    checkModuloRemainderDiv(6, 5);
    checkModuloRemainderDiv(0, -5);
    checkModuloRemainderDiv(-1, -5);
    checkModuloRemainderDiv(-5, -5);
    checkModuloRemainderDiv(-6, -5);
    checkModuloRemainderDiv(6, -5);
  }

  private void checkModuloRemainderDiv(int aVal, int bVal) throws Exception {
    assume().that(solverToUse).isNotEqualTo(PRINCESS);
    for (Pair<BinaryOperator<BitvectorFormula>, BinaryOperator<BitvectorFormula>> testMethods :
        methodsToTest) {
      BinaryOperator<BitvectorFormula> toTest = testMethods.getFirst();
      BinaryOperator<BitvectorFormula> baseline = testMethods.getSecond();
      BitvectorFormula a = replacer.makeBitvector(4, aVal);
      BitvectorFormula b = replacer.makeBitvector(4, bVal);
      // Use bitvector semantics as the baseline for expected result
      BitvectorFormula expectedBv =
          baseline.apply(bvmgr.makeBitvector(4, aVal), bvmgr.makeBitvector(4, bVal));

      BitvectorFormula input = toTest.apply(a, b);
      BooleanFormula constraint =
          bvmgr.equal(bvmgr.makeBitvector(4, replacer.toIntegerFormula(input, false)), expectedBv);
      assertThatFormula(constraint).isTautological();
    }
  }

  @Test
  public void testMultiply() throws Exception {
    final BitvectorFormula a = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(2), "a");
    final BitvectorFormula aWrapped =
        wrappingHandler.wrap(
            FormulaType.getBitvectorTypeWithSize(2),
            replacer.mapToSignedRange((IntegerFormula) wrappingHandler.unwrap(a), 2));
    final BitvectorFormula b = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(2), "b");
    final BitvectorFormula bWrapped =
        wrappingHandler.wrap(
            FormulaType.getBitvectorTypeWithSize(2),
            replacer.mapToSignedRange((IntegerFormula) wrappingHandler.unwrap(b), 2));

    BooleanFormula rangeA = replacer.makeDomainRangeConstraint(a, true);
    BooleanFormula rangeB = replacer.makeDomainRangeConstraint(b, true);
    BooleanFormula multiply =
        replacer.equal(replacer.multiply(a, b), replacer.multiply(aWrapped, bWrapped));
    assertThatFormula(bmgr.and(rangeA, rangeB, bmgr.not(multiply))).isUnsatisfiable();
  }
}
