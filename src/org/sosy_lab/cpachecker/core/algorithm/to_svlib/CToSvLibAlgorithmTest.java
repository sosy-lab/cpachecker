// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class CToSvLibAlgorithmTest {

  private final String encodeBitvectorsAsIntegersOption = "INTEGER";
  private final String encodeBitvectorsAsBitvectorsOption = "BITVECTOR";

  private void testTransformationToSvLib(Path pInputFilePath, String bitVectorEncoding)
      throws InvalidConfigurationException,
          ParserException,
          IOException,
          InterruptedException,
          SvLibAstParseException,
          CPATransferException {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration config =
        TestUtils.configurationForTest()
            .setOptions(
                ImmutableMap.of(
                    "cpa.predicate.encodeBitvectorAs",
                    bitVectorEncoding,
                    "cpa.predicate.ignoreIrrelevantVariables",
                    "false",
                    "solver.solver",
                    "z3"))
            .build();
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    SvLibScript script;
    try (CToSvLibAlgorithm algorithm =
        new CToSvLibAlgorithm(
            config, Specification.alwaysSatisfied(), logger, shutdownNotifier, inputCfa)) {
      script = algorithm.transformCfaToSvLibScript();
    }

    String scriptAsString = script.toASTString();
    SvLibToAstParser.parseScript(scriptAsString);
  }

  // *********************************** Test for config file ***********************************
  private void transformationConfigFileTest(Path pInputFilePath) throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(Path.of("config/transformToSvLib.properties"))
            .build();

    IntegrationTestRunner.run(config, pInputFilePath.toString());
  }

  @Test
  public void testSimple_File() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    transformationConfigFileTest(inputFilePath);
  }

  // *********************************** ToSvLibTransformation ***********************************

  private String examplesPathToSvLibTransformation() {
    return Path.of("test", "programs", "to_svlib_transformation").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testSimpleDivision() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  // *********************************** CfaToCExport ***********************************

  // This test actually only takes about 600 ms, but when running it in isolation within an IDE,
  // one needs to factor in a startup time of around 1 second.
  @Test(timeout = 3000)
  public void testAllCfaToC() throws Exception {
    Path directoryPath = Path.of("test", "programs", "cfa_to_c_export").toAbsolutePath();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.c")) {
      for (Path path : stream) {
        testTransformationToSvLib(path, encodeBitvectorsAsIntegersOption);
      }
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input files", e);
    }
  }

  // *********************************** Programtranslation  ***********************************

  private String examplesPathProgramTranslation() {
    return Path.of("test", "programs", "programtranslation").toAbsolutePath().toString();
  }

  // TODO encodeBitvectorsAsBitvectorsOption requires not yet supported extend operation
  @Test(timeout = 1800)
  public void testGotos() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "gotos.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  @Test(timeout = 1800)
  public void testFunctionReturn() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "functionreturn.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  @Test(timeout = 1800)
  public void testFunctionReturnBitvectorEncoding() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "functionreturn.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsBitvectorsOption);
  }

  // *********************************** Real C ***********************************

  private String examplesPathRealC() {
    return Path.of("test", "programs", "realc").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testTestOr() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "test-or.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  @Test(timeout = 1800)
  public void testTestOrBitvectorEncoding() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "test-or.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsBitvectorsOption);
  }

  @Test(timeout = 1800)
  public void testRandom() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "random.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  @Test(timeout = 1800)
  public void testRandomBitvectorEncoding() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "random.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsBitvectorsOption);
  }

  // **************************************** with property ****************************************

  private void testAndVerifyError(Path pInputFilePath, Result pExpectedVerdict) throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(Path.of("config/transformToSvLib.properties"))
            .build();

    IntegrationTestResult results = IntegrationTestRunner.run(config, pInputFilePath.toString());
    results.assertIs(pExpectedVerdict);
    @SuppressWarnings("unused")
    String resultString = results.toString();
  }

  @Test(timeout = 1800)
  public void testAndVerifySimpleDivision() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testAndVerifyWhileInfinite() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "while-infinite.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testAndVerifyPointerWhile() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "pointer-while.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** witness validation **********

  private String examplesWitnessValidation() {
    return Path.of("test", "programs", "witnessValidation").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testMax() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "max.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testMultiVar() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "multivar.i");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testValueInvariant() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "valueInvariant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testWeekdays() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "weekdays.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testWeekdaysNoTermination() throws Exception {
    Path inputFilePath = Path.of(examplesWitnessValidation(), "weekdays_no_termination.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** block analysis **********

  private String examplesBlockAnalysis() {
    return Path.of("test", "programs", "block_analysis").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testAbstractionSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "abstraction_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testComplexLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "complex_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testDoubleLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "double_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testFaultUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "fault_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testForLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "for-loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testHardLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "hard_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testGotoLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "goto_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testInstantiateSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "instantiate_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testInstantiateUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "instantiate_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testMultiplicationSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "multiplication_safe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testSimpleArraySafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_array_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleArrayUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_array_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testSimpleCalculationsSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_calculations_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleCalculationsUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_calculations_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testSimpleForSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_for_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleFunctionCall() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_function_call.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testSimpleFunctionCalls() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_function_calls.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleLoopDoubleSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_double_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 6000)
  public void testSimpleLoopSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleLoopUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_loop_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testSimpleNondetSafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_nondet_safe.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testSimpleNondetUnsafe() throws Exception {
    Path inputFilePath = Path.of(examplesBlockAnalysis(), "simple_nondet_unsafe.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  // ********** policy iteration **********

  private String examplesPolicyiteration() {
    return Path.of("test", "programs", "policyiteration").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testFormulaFail() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "formula_fail.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testInitial() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "initial.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testPolicyIterationLoop() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "loop.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testTimeout() throws Exception {
    Path inputFilePath = Path.of(examplesPolicyiteration(), "timeout.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** program slicing **********

  private String examplesProgramSlicing() {
    return Path.of("test", "programs", "program_slicing").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testBranchBothRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_both_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testBranchNoneRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_none_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testBranchNoneRelevant2() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_none_relevant2.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testBranchOnlyElseRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_only_else_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testBranchOnlyIfRelevant() throws Exception {
    Path inputFilePath = Path.of(examplesProgramSlicing(), "branch_only_if_relevant.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  // ********** simple **********

  private String examplesSimple() {
    return Path.of("test", "programs", "simple").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testSimpleZeroModulo() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "zero-modulo-nondet.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testNondetAssign() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "nondetAssign.c");
    testAndVerifyError(inputFilePath, Result.FALSE);
  }

  @Test(timeout = 1800)
  public void testEndlessLoop() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "endlessLoop.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }

  @Test(timeout = 1800)
  public void testNondetDeclaration1() throws Exception {
    Path inputFilePath = Path.of(examplesSimple(), "explicit", "symbolic", "nondetDeclaration-1.c");
    testAndVerifyError(inputFilePath, Result.TRUE);
  }
}
