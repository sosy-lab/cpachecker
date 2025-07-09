// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.TruthJUnit.assume;

import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
    replacer = new ReplaceBitvectorWithNLAIntegerTheory(wrappingHandler, bmgr, imgr, config);
  }

  @Test
  public void testUnsignedWrapAround() throws Exception {
    final int[] values = new int[] {1, -1, 15, -15, 16, -16, 25, -25};
    for (Integer value : values) {
      IntegerFormula formula = imgr.makeNumber(value);
      IntegerFormula expected =
          imgr.makeNumber(BigInteger.valueOf(value).mod(BigInteger.valueOf(16)));
      BooleanFormula constraint = imgr.equal(replacer.wrapAround(formula, 4), expected);
      assertThatFormula(constraint).isSatisfiable();
    }
  }

  @Test
  public void testSignedWrapAround() throws Exception {
    final int[] values = new int[] {1, 7, 8, 15};
    for (int value : values) {
      int expectedNumber = value >= 8 ? value - 16 : value;
      IntegerFormula formula = imgr.makeNumber(value);
      IntegerFormula expected = imgr.makeNumber(expectedNumber);
      BooleanFormula constraint = imgr.equal(replacer.wrapAroundSigned(formula, 4), expected);
      assertThatFormula(constraint).isSatisfiable();
    }
  }

  @Test
  public void testRangeExtendExtractUnsigned() throws Exception {
    final BitvectorFormula base =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
    final BitvectorFormula larger =
        replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

    BooleanFormula range =
        replacer.addRangeConstraint(
            base, BigInteger.ZERO, BigInteger.TWO.pow(8).subtract(BigInteger.ONE));
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

    BooleanFormula range =
        replacer.addRangeConstraint(
            base, BigInteger.TWO.pow(7).negate(), BigInteger.TWO.pow(7).subtract(BigInteger.ONE));
    BooleanFormula extend = replacer.equal(replacer.extend(base, 8, true), larger);
    BooleanFormula extract = bmgr.not(replacer.equal(replacer.extract(larger, 7, 0), base));
    assertThatFormula(bmgr.and(range, extend, extract)).isUnsatisfiable();
  }

  @Test
  public void testModulo() throws Exception {
    checkModulo(0, 5, 0);
    checkModulo(-1, 5, -1);
    checkModulo(-5, 5, 0);
    checkModulo(-6, 5, -1);
    checkModulo(6, 5, 1);
    checkModulo(0, -5, 0);
    checkModulo(-1, -5, -1);
    checkModulo(-5, -5, 0);
    checkModulo(-6, -5, -1);
    checkModulo(6, -5, 1);
  }

  private void checkModulo(int aVal, int bVal, int expected) throws Exception {
    BitvectorFormula a = replacer.makeBitvector(4, aVal);
    BitvectorFormula b = replacer.makeBitvector(4, bVal);
    BitvectorFormula c = replacer.makeBitvector(4, expected);

    BitvectorFormula input = replacer.remainder(a, b, true);
    BooleanFormula constraint = replacer.equal(input, c);
    assertThatFormula(constraint).isSatisfiable();
  }

  @Test
  public void testDiv() throws Exception {
    checkDiv(0, 5, 0);
    checkDiv(-1, 5, 0);
    checkDiv(-5, 5, -1);
    checkDiv(-6, 5, -1);
    checkDiv(6, 5, 1);
    checkDiv(0, -5, 0);
    checkDiv(-1, -5, 0);
    checkDiv(-5, -5, 1);
    checkDiv(-6, -5, 1);
    checkDiv(6, -5, -1);
  }

  private void checkDiv(int aVal, int bVal, int expected) throws Exception {
    BitvectorFormula a = replacer.makeBitvector(4, aVal);
    BitvectorFormula b = replacer.makeBitvector(4, bVal);
    BitvectorFormula c = replacer.makeBitvector(4, expected);

    BitvectorFormula input = replacer.divide(a, b, true);
    BooleanFormula constraint = replacer.equal(input, c);
    assertThatFormula(constraint).isSatisfiable();
  }

  @Test
  public void testMultiply() throws Exception {
    final BitvectorFormula a = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(2), "a");
    final BitvectorFormula aWrapped =
        wrappingHandler.wrap(
            FormulaType.getBitvectorTypeWithSize(2),
            replacer.wrapAroundSigned((IntegerFormula) wrappingHandler.unwrap(a), 2));
    final BitvectorFormula b = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(2), "b");
    final BitvectorFormula bWrapped =
        wrappingHandler.wrap(
            FormulaType.getBitvectorTypeWithSize(2),
            replacer.wrapAroundSigned((IntegerFormula) wrappingHandler.unwrap(b), 2));

    BooleanFormula rangeA =
        replacer.addRangeConstraint(
            a, BigInteger.TWO.pow(1).negate(), BigInteger.TWO.pow(1).subtract(BigInteger.ONE));
    BooleanFormula rangeB =
        replacer.addRangeConstraint(
            b, BigInteger.TWO.pow(1).negate(), BigInteger.TWO.pow(1).subtract(BigInteger.ONE));
    BooleanFormula multiply =
        replacer.equal(replacer.multiply(a, b), replacer.multiply(aWrapped, bWrapped));
    assertThatFormula(bmgr.and(rangeA, rangeB, bmgr.not(multiply))).isUnsatisfiable();
  }
}
