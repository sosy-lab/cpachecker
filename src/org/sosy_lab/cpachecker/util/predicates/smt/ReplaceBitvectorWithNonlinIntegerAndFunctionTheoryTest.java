// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.List;
import org.junit.*;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.cpachecker.util.predicates.smt.ReplaceIntegerWithBitvectorTheory.ReplaceIntegerEncodingOptions;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class ReplaceBitvectorWithNonlinIntegerAndFunctionTheoryTest {

  private static SolverContext context;
  private static IntegerFormulaManager ifm;
  private static BooleanFormulaManager bfm;
  private static FormulaWrappingHandler wrappingHandler;
  private static ReplaceBitvectorWithNonlinIntegerAndFunctionTheory replacer;

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
            Theory.INTEGER_NLA,
            Theory.FLOAT,
            Theory.INTEGER,
            new ReplaceIntegerEncodingOptions(Configuration.defaultConfiguration()));
    ifm = context.getFormulaManager().getIntegerFormulaManager();
    bfm = context.getFormulaManager().getBooleanFormulaManager();
    replacer =
        new ReplaceBitvectorWithNonlinIntegerAndFunctionTheory(
            wrappingHandler, bfm, ifm, Configuration.defaultConfiguration());
  }

  @AfterClass
  public static void shutdownSolver() {
    context.close();
  }

  @Test
  public void testUnsignedWrapAround() throws Exception {
    final var values = new Integer[] {1, -1, 15, -15, 16, -16, 25, -25};
    for (Integer value : values) {
      var formula = ifm.makeNumber(BigInteger.valueOf(value));
      var expected = ifm.makeNumber(BigInteger.valueOf(value).mod(BigInteger.valueOf(16)));
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        var constraint = ifm.equal(replacer.wrapAround(formula, 4), expected);
        prover.addConstraint(constraint);
        assertFalse(
            "Formula %s should be trivially satisfiable".formatted(constraint), prover.isUnsat());
      }
    }
  }

  @Test
  public void testSignedWrapAround() throws Exception {
    final var values = new Integer[] {1, 7, 8, 15};
    for (Integer value : values) {
      var expectedNumber = value >= 8 ? BigInteger.valueOf(value - 16) : BigInteger.valueOf(value);
      var formula = ifm.makeNumber(BigInteger.valueOf(value));
      var expected = ifm.makeNumber(expectedNumber);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        var constraint = ifm.equal(replacer.wrapAroundSigned(formula, 4), expected);
        prover.addConstraint(constraint);
        assertFalse(
            "Formula %s should be trivially satisfiable".formatted(constraint), prover.isUnsat());
      }
    }
  }

  @Test
  public void testRangeExtendExtractUnsigned() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final var base = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
      final var larger = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

      BooleanFormula range =
          replacer.addRangeConstraint(
              base, BigInteger.ZERO, BigInteger.TWO.pow(8).subtract(BigInteger.ONE));
      prover.addConstraint(range);
      BooleanFormula extend = replacer.equal(replacer.extend(base, 8, false), larger);
      prover.addConstraint(extend);
      BooleanFormula extract = bfm.not(replacer.equal(replacer.extract(larger, 7, 0), base));
      prover.addConstraint(extract);
      var unsat = prover.isUnsat();
      assertTrue(
          "Formulas {\n%s, \n%s, \n%s\n} should be trivially unsatisfiable, but found model: \n%s"
              .formatted(range, extend, extract, unsat ? "" : prover.getModel()),
          unsat);
    }
  }

  @Test
  public void testRangeExtendExtractSigned() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final var base = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(8), "base");
      final var larger = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(16), "larger");

      BooleanFormula range =
          replacer.addRangeConstraint(
              base, BigInteger.TWO.pow(7).negate(), BigInteger.TWO.pow(7).subtract(BigInteger.ONE));
      prover.addConstraint(range);
      BooleanFormula extend = replacer.equal(replacer.extend(base, 8, true), larger);
      prover.addConstraint(extend);
      BooleanFormula extract = bfm.not(replacer.equal(replacer.extract(larger, 7, 0), base));
      prover.addConstraint(extract);
      var unsat = prover.isUnsat();
      assertTrue(
          "Formulas {\n%s, \n%s, \n%s\n} should be trivially unsatisfiable, but found model: \n%s"
              .formatted(range, extend, extract, unsat ? "" : prover.getModel()),
          unsat);
    }
  }

  @Test
  public void testModulo() throws Exception {
    final var testData =
        List.of(
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
      var a = replacer.makeBitvector(4, testDatum.getFirst().getFirst());
      var b = replacer.makeBitvector(4, testDatum.getFirst().getSecond());
      var c = replacer.makeBitvector(4, testDatum.getSecond());

      var input = replacer.remainder(a, b, true);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        var constraint = replacer.equal(input, c);
        prover.addConstraint(constraint);
        assertFalse(
            "Formula \n%s for inputs\n%s\nshould be trivially satisfiable"
                .formatted(constraint, testDatum),
            prover.isUnsat());
      }
    }
  }

  @Test
  public void testDiv() throws Exception {
    final var testData =
        List.of(
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
      var a = replacer.makeBitvector(4, testDatum.getFirst().getFirst());
      var b = replacer.makeBitvector(4, testDatum.getFirst().getSecond());
      var c = replacer.makeBitvector(4, testDatum.getSecond());

      var input = replacer.divide(a, b, true);
      try (ProverEnvironment prover = context.newProverEnvironment()) {
        var constraint = replacer.equal(input, c);
        prover.addConstraint(constraint);
        assertFalse(
            "Formula \n%s for inputs\n%s\nshould be trivially satisfiable"
                .formatted(constraint, testDatum),
            prover.isUnsat());
      }
    }
  }

  @Test
  public void testMultiply() throws Exception {
    try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final var a = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(4), "a");
      final var aWrapped =
          wrappingHandler.wrap(
              FormulaType.getBitvectorTypeWithSize(4),
              replacer.wrapAroundSigned((IntegerFormula) wrappingHandler.unwrap(a), 4));
      final var b = replacer.makeVariable(FormulaType.getBitvectorTypeWithSize(4), "b");
      final var bWrapped =
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
      var unsat = prover.isUnsat();
      assertTrue(
          "Formulas {\n%s, \n%s, \n%s\n} should be trivially unsatisfiable, but found model: \n%s"
              .formatted(rangeA, rangeB, multiply, unsat ? "" : prover.getModel()),
          unsat);
    }
  }
}
