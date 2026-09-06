// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Integration tests that transform a C program to SV-LIB and analyze the generated script.
 *
 * <p>These duplicate tests of the BuildBot, so they are disabled by default and can be run with
 * {@code ant tests -DenableExtendedTests=true}.
 */
public class CToSvLibAlgorithmIntegrationTest {

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private String examplesPathToSvLibTransformation() {
    return Path.of("test", "programs", "to_svlib_transformation").toAbsolutePath().toString();
  }

  // **************************************** with property ****************************************

  private void testAndVerifyError(Path pInputFilePath, Result pExpectedVerdict) throws Exception {
    testAndVerifyError(pInputFilePath, pExpectedVerdict, "config/transformToSvLib.properties");
  }

  /**
   * Verify a program whose correctness depends on the exact semantics of the machine integers, so
   * that it needs the encoding of the bitvectors as bitvectors.
   */
  private void testAndVerifyErrorWithBitvectors(Path pInputFilePath, Result pExpectedVerdict)
      throws Exception {
    testAndVerifyError(
        pInputFilePath, pExpectedVerdict, "config/transformToSvLib-bitVec.properties");
  }

  private void testAndVerifyError(
      Path pInputFilePath, Result pExpectedVerdict, String pConfigurationFile) throws Exception {
    Configuration config =
        TestUtils.configurationForTest().loadFromFile(Path.of(pConfigurationFile)).build();

    IntegrationTestResult results = IntegrationTestRunner.run(config, pInputFilePath.toString());
    results.assertIs(pExpectedVerdict);
    @SuppressWarnings("unused")
    String resultString = results.toString();
  }

  @Test(timeout = 90000)
  public void testAndVerifySimpleDivision() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyWhileInfinite() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "while-infinite.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyReservedWordVariables() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "reserved-word-variables.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyShadowedGlobal() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "shadowed-global.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyAllocationInLoopField() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "allocation-in-loop-field.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyAllocationInLoop() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "allocation-in-loop.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyNondeterministicValueInLoop() throws Exception {
    Path inputFilePath =
        Path.of(examplesPathToSvLibTransformation(), "nondeterministic-value-in-loop.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testAndVerifyPointerWhile() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "pointer-while.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** witness validation **********

  private String examplesWitnessValidation() {
    return Path.of("test", "programs", "witnessValidation").toAbsolutePath().toString();
  }

  @Test(timeout = 90000)
  public void testMax() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "max.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testMultiVar() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "multivar.i");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testValueInvariant() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "valueInvariant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testWeekdays() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "weekdays.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testWeekdaysNoTermination() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "weekdays_no_termination.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** block analysis **********

  private String examplesBlockAnalysis() {
    return Path.of("test", "programs", "simple", "block_analysis").toAbsolutePath().toString();
  }

  @Test(timeout = 90000)
  public void testAbstractionSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "abstraction_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testComplexLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "complex_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testDoubleLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "double_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testFaultUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "fault_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testForLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "for-loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testHardLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "hard_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testGotoLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "goto_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testInstantiateSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "instantiate_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testInstantiateUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "instantiate_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testMultiplicationSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "multiplication_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleArraySafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_array_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleArrayUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_array_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testSimpleCalculationsSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_calculations_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleCalculationsUnsafe() throws Exception {
    // This program only violates the property because negating the smallest int overflows, so it
    // needs the encoding of the bitvectors as bitvectors. With the default encoding as unbounded
    // integers, the property holds, which is also what the analysis of the untransformed program
    // reports with that encoding.
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_calculations_unsafe_1.c");
    testAndVerifyErrorWithBitvectors(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testSimpleForSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_for_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleFunctionCall() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_function_call.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testSimpleFunctionCalls() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_function_calls.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleLoopDoubleSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_double_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  @Ignore(
      "The program multiplies two variables, and the configurations for SV-LIB use exact"
          + " non-linear arithmetic (see config/includes/svlib.properties), which MathSAT5 cannot"
          + " interpolate.")
  public void testSimpleNondetSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_nondet_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testSimpleNondetUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_nondet_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  // ********** policy iteration **********

  private String examplesPolicyiteration() {
    return Path.of("test", "programs", "policyiteration").toAbsolutePath().toString();
  }

  @Test(timeout = 90000)
  public void testFormulaFail() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "formula_fail.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testInitial() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "initial.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testPolicyIterationLoop() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "loop.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testTimeout() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "timeout.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** program slicing **********

  private String examplesProgramSlicing() {
    return Path.of("test", "programs", "program_slicing").toAbsolutePath().toString();
  }

  @Test(timeout = 90000)
  public void testBranchBothRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_both_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testBranchNoneRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_none_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testBranchNoneRelevant2() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_none_relevant2.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testBranchOnlyElseRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_only_else_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testBranchOnlyIfRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_only_if_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** simple **********

  private String examplesSimple() {
    return Path.of("test", "programs", "simple").toAbsolutePath().toString();
  }

  @Test(timeout = 90000)
  public void testSimpleZeroModulo() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "zero-modulo-nondet.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testNondetAssign() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "nondetAssign.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void testEndlessLoop() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "endlessLoop.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 90000)
  public void testNondetDeclaration1() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "nondetDeclaration-1.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }
}
