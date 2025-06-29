// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.cpachecker.util.predicates.smt.ReplaceIntegerWithBitvectorTheory.ReplaceIntegerEncodingOptions;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class ReplaceBitvectorWithNLAIntegerTheoryTest {

  private static SolverContext context;
  private static IntegerFormulaManager ifm;
  private static BooleanFormulaManager bfm;
  private static FormulaWrappingHandler wrappingHandler;
  private static ReplaceBitvectorWithNLAIntegerTheory replacer;

  @BeforeClass
  public static void initSolver() throws Exception {
    context =
        SolverContextFactory.createSolverContext(
            Configuration.defaultConfiguration(),
            LogManager.createNullLogManager(),
            ShutdownManager.create().getNotifier(),
            Solvers.Z3);
    wrappingHandler =
        new FormulaWrappingHandler(
            context.getFormulaManager(),
            Theory.INTEGER,
            Theory.FLOAT,
            Theory.INTEGER,
            new ReplaceIntegerEncodingOptions(Configuration.defaultConfiguration()));
    ifm = context.getFormulaManager().getIntegerFormulaManager();
    bfm = context.getFormulaManager().getBooleanFormulaManager();
    replacer =
        new ReplaceBitvectorWithNLAIntegerTheory(
            wrappingHandler, bfm, ifm, Configuration.defaultConfiguration());
  }

  @AfterClass
  public static void shutdownSolver() {
    context.close();
  }

  @Test
  public void testUnsignedWrapAround() throws Exception {
    final Integer[] values = new Integer[] {1, -1, 15, -15, 16, -16, 25, -25};
    for (Integer value : values) {
      IntegerFormula formula = ifm.makeNumber(BigInteger.valueOf(value));
      IntegerFormula expected =
          ifm.makeNumber(BigInteger.valueOf(value).mod(BigInteger.valueOf(16)));
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = ifm.equal(replacer.wrapAround(formula, 4), expected);
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
      IntegerFormula formula = ifm.makeNumber(BigInteger.valueOf(value));
      IntegerFormula expected = ifm.makeNumber(expectedNumber);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = ifm.equal(replacer.wrapAroundSigned(formula, 4), expected);
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
      BooleanFormula extract = bfm.not(replacer.equal(replacer.extract(larger, 7, 0), base));
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
      BooleanFormula extract = bfm.not(replacer.equal(replacer.extract(larger, 7, 0), base));
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
    final ImmutableList<Pair<Pair<Integer, Integer>, Integer>> testData =
        ImmutableList.of(
            Pair.of(Pair.of(0, 5), 0),
            Pair.of(Pair.of(-1, 5), -1),
            Pair.of(Pair.of(-5, 5), 0),
            Pair.of(Pair.of(-6, 5), -1),
            Pair.of(Pair.of(6, 5), 1),
            Pair.of(Pair.of(0, -5), 0),
            Pair.of(Pair.of(-1, -5), -1),
            Pair.of(Pair.of(-5, -5), 0),
            Pair.of(Pair.of(-6, -5), -1),
            Pair.of(Pair.of(6, -5), 1));
    for (Pair<Pair<Integer, Integer>, Integer> testDatum : testData) {
      BitvectorFormula a = replacer.makeBitvector(4, testDatum.getFirst().getFirst());
      BitvectorFormula b = replacer.makeBitvector(4, testDatum.getFirst().getSecond());
      BitvectorFormula c = replacer.makeBitvector(4, testDatum.getSecond());

      BitvectorFormula input = replacer.remainder(a, b, true);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = replacer.equal(input, c);
        prover.addConstraint(constraint);
        assertWithMessage(
                "Formula %n%s for inputs%n%s%nshould be trivially satisfiable"
                    .formatted(constraint, testDatum))
            .that(prover.isUnsat())
            .isFalse();
      }
    }
  }

  @Test
  public void testDiv() throws Exception {
    final ImmutableList<Pair<Pair<Integer, Integer>, Integer>> testData =
        ImmutableList.of(
            Pair.of(Pair.of(0, 5), 0),
            Pair.of(Pair.of(-1, 5), 0),
            Pair.of(Pair.of(-5, 5), -1),
            Pair.of(Pair.of(-6, 5), -1),
            Pair.of(Pair.of(6, 5), 1),
            Pair.of(Pair.of(0, -5), 0),
            Pair.of(Pair.of(-1, -5), 0),
            Pair.of(Pair.of(-5, -5), 1),
            Pair.of(Pair.of(-6, -5), 1),
            Pair.of(Pair.of(6, -5), -1));
    for (Pair<Pair<Integer, Integer>, Integer> testDatum : testData) {
      BitvectorFormula a = replacer.makeBitvector(4, testDatum.getFirst().getFirst());
      BitvectorFormula b = replacer.makeBitvector(4, testDatum.getFirst().getSecond());
      BitvectorFormula c = replacer.makeBitvector(4, testDatum.getSecond());

      BitvectorFormula input = replacer.divide(a, b, true);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        BooleanFormula constraint = replacer.equal(input, c);
        prover.addConstraint(constraint);
        assertWithMessage(
                "Formula %n%s for inputs%n%s%nshould be trivially satisfiable"
                    .formatted(constraint, testDatum))
            .that(prover.isUnsat())
            .isFalse();
      }
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
      prover.addConstraint(bfm.not(multiply));
      boolean unsat = prover.isUnsat();
      assertWithMessage(
              "Formulas {%n%s, %n%s, %n%s%n} should be trivially unsatisfiable, but found model: %n%s"
                  .formatted(rangeA, rangeB, multiply, unsat ? "" : prover.getModel()))
          .that(unsat)
          .isTrue();
    }
  }
}
