// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

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
        .isNotIn(
            ImmutableSet.of(Solvers.MATHSAT5, Solvers.CVC4, Solvers.SMTINTERPOL, Solvers.OPENSMT));
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
      IntegerFormula formula = imgr.makeNumber(BigInteger.valueOf(value));
      IntegerFormula expected =
          imgr.makeNumber(BigInteger.valueOf(value).mod(BigInteger.valueOf(16)));
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = imgr.equal(replacer.wrapAround(formula, 4), expected);
        prover.addConstraint(constraint);
        assertWithMessage("Formula %s should be trivially satisfiable".formatted(constraint))
            .that(prover.isUnsat())
            .isFalse();
      }
    }
  }

  @Test
  public void testSignedWrapAround() throws Exception {
    final Integer[] values = new Integer[] {1, 7, 8, 15};
    for (Integer value : values) {
      BigInteger expectedNumber =
          value >= 8 ? BigInteger.valueOf(value - 16) : BigInteger.valueOf(value);
      IntegerFormula formula = imgr.makeNumber(BigInteger.valueOf(value));
      IntegerFormula expected = imgr.makeNumber(expectedNumber);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = imgr.equal(replacer.wrapAroundSigned(formula, 4), expected);
        prover.addConstraint(constraint);
        assertWithMessage("Formula %s should be trivially satisfiable".formatted(constraint))
            .that(prover.isUnsat())
            .isFalse();
      }
    }
  }

  @Test
  public void testRangeExtendExtractUnsigned() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final BitvectorFormula base =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
      final BitvectorFormula larger =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

      BooleanFormula range =
          replacer.addRangeConstraint(
              base, BigInteger.ZERO, BigInteger.TWO.pow(8).subtract(BigInteger.ONE));
      prover.addConstraint(range);
      BooleanFormula extend = replacer.equal(replacer.extend(base, 8, false), larger);
      prover.addConstraint(extend);
      BooleanFormula extract = bmgr.not(replacer.equal(replacer.extract(larger, 7, 0), base));
      prover.addConstraint(extract);
      boolean unsat = prover.isUnsat();

      assertWithMessage(
              "Formulas {%n%s, %n%s, %n%s%n} should be trivially unsatisfiable, but found model: %n%s"
                  .formatted(range, extend, extract, unsat ? "" : prover.getModel()))
          .that(unsat)
          .isTrue();
    }
  }

  @Test
  public void testRangeExtendExtractSigned() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final BitvectorFormula base =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
      final BitvectorFormula larger =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

      BooleanFormula range =
          replacer.addRangeConstraint(
              base, BigInteger.TWO.pow(7).negate(), BigInteger.TWO.pow(7).subtract(BigInteger.ONE));
      prover.addConstraint(range);
      BooleanFormula extend = replacer.equal(replacer.extend(base, 8, true), larger);
      prover.addConstraint(extend);
      BooleanFormula extract = bmgr.not(replacer.equal(replacer.extract(larger, 7, 0), base));
      prover.addConstraint(extract);
      boolean unsat = prover.isUnsat();
      assertWithMessage(
              "Formulas {%n%s, %n%s, %n%s%n} should be trivially unsatisfiable, but found model: %n%s"
                  .formatted(range, extend, extract, unsat ? "" : prover.getModel()))
          .that(unsat)
          .isTrue();
    }
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
    try (ProverEnvironment prover = context.newProverEnvironment()) {
      BooleanFormula constraint = replacer.equal(input, c);
      prover.addConstraint(constraint);
      assertWithMessage(
              "Formula %n%s for inputs%n(%d %% %d == %d)%nshould be trivially satisfiable"
                  .formatted(constraint, aVal, bVal, expected))
          .that(prover.isUnsat())
          .isFalse();
    }
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
    try (ProverEnvironment prover = context.newProverEnvironment()) {
      BooleanFormula constraint = replacer.equal(input, c);
      prover.addConstraint(constraint);
      assertWithMessage(
              "Formula %n%s for inputs%n(%d / %d == %d)%nshould be trivially satisfiable"
                  .formatted(constraint, aVal, bVal, expected))
          .that(prover.isUnsat())
          .isFalse();
    }
  }

  @Test
  public void testMultiply() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final BitvectorFormula a =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(4), "a");
      final BitvectorFormula aWrapped =
          wrappingHandler.wrap(
              FormulaType.getBitvectorTypeWithSize(4),
              replacer.wrapAroundSigned((IntegerFormula) wrappingHandler.unwrap(a), 4));
      final BitvectorFormula b =
          replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(4), "b");
      final BitvectorFormula bWrapped =
          wrappingHandler.wrap(
              FormulaType.getBitvectorTypeWithSize(4),
              replacer.wrapAroundSigned((IntegerFormula) wrappingHandler.unwrap(b), 4));

      BooleanFormula rangeA =
          replacer.addRangeConstraint(
              a, BigInteger.TWO.pow(3).negate(), BigInteger.TWO.pow(3).subtract(BigInteger.ONE));
      BooleanFormula rangeB =
          replacer.addRangeConstraint(
              b, BigInteger.TWO.pow(3).negate(), BigInteger.TWO.pow(3).subtract(BigInteger.ONE));
      prover.addConstraint(rangeA);
      prover.addConstraint(rangeB);
      BooleanFormula multiply =
          replacer.equal(replacer.multiply(a, b), replacer.multiply(aWrapped, bWrapped));
      prover.addConstraint(bmgr.not(multiply));
      boolean unsat = prover.isUnsat();
      assertWithMessage(
              "Formulas {%n%s, %n%s, %n%s%n} should be trivially unsatisfiable, but found model: %n%s"
                  .formatted(rangeA, rangeB, multiply, unsat ? "" : prover.getModel()))
          .that(unsat)
          .isTrue();
    }
  }
}
