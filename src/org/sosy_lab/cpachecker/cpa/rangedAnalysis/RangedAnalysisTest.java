// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysis;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class RangedAnalysisTest {

  private static final long TIMEOUT = 9000;

  private enum Testcases {
    BRANCH("branch.c"),
    BRANCH2("branch2.c");
    private final String name;

    Testcases(String pName) {
      name = pName;
    }
  }

  private enum RangedExecutionConfig {
    BoundedPredicateAnalysis("BoundedPredicateAnalysis");

    private final String fileName;

    RangedExecutionConfig(String pConfigName) {
      fileName = String.format("%s.properties", pConfigName);
    }
  }

  private static final String specificationFile = "config/specification/sv-comp-reachability.spc";
  private static final String TEST_DIR_PATH = "test/programs/rangedExecution/";
  private static final String SPECIFICATION_OPTION = "specification";
  private static LogManager logger;

  static {
    try {
      logger = BasicLogManager.create(Configuration.defaultConfiguration());
    } catch (InvalidConfigurationException pE) {
      logger.log(Level.INFO, Throwables.getStackTraceAsString(pE));
    }
  }

  @Test(timeout = TIMEOUT)
  public void leftAndRightBoundCorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.TRUE);
    tester.addOverrideOption("cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "leaf4.xml");
    tester.addOverrideOption(
        "cpa.rangedExecution.path2RightInputFile", TEST_DIR_PATH + "leaf5.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void leftAndRightBoundIncorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.FALSE);
    tester.addOverrideOption("cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "leaf3.xml");
    tester.addOverrideOption(
        "cpa.rangedExecution.path2RightInputFile", TEST_DIR_PATH + "leaf5.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void leftBoundCorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.TRUE);
    tester.addOverrideOption("cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "leaf4.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void leftBoundIncorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.FALSE);
    tester.addOverrideOption("cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "leaf3.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void leftBoundUnderspecIncorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.FALSE);
    tester.addOverrideOption(
        "cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "intermed1.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void rightBoundCorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.TRUE);
    tester.addOverrideOption(
        "cpa.rangedExecution.path2RightInputFile", TEST_DIR_PATH + "leaf2.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void rightBoundIncorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH, RangedExecutionConfig.BoundedPredicateAnalysis, Result.FALSE);
    tester.addOverrideOption(
        "cpa.rangedExecution.path2RightInputFile", TEST_DIR_PATH + "leaf4.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void rightBoundUnderspecIncorrect() throws Exception {
    RangeExecTester tester =
        new RangeExecTester(
            Testcases.BRANCH2, RangedExecutionConfig.BoundedPredicateAnalysis, Result.TRUE);
    tester.addOverrideOption("cpa.rangedExecution.path2LeftInputFile", TEST_DIR_PATH + "leaf4.xml");
    tester.addOverrideOption(
        "cpa.rangedExecution.path2RightInputFile", TEST_DIR_PATH + "intermed2.xml");
    tester.performTest();
  }

  private static void performTest(
      Testcases pFilename,
      RangedExecutionConfig pGenerationConfig,
      Map<String, String> pOverrideOptions,
      Result pExpectedResult)
      throws Exception {
    String fullPath = Path.of(TEST_DIR_PATH, pFilename.name).toString();

    Result result = startTransformation(pGenerationConfig, fullPath, pOverrideOptions);
    assertThat(result).isEqualTo(pExpectedResult);
  }

  /**
   * Execute a config
   *
   * @param pGenerationConfig the config to execute
   * @param pFilePath the path to the testcase, should be in test/programs/gia
   * @param pOverrideOptions options to override
   * @throws Exception happening during execution
   * @return the result of the analysis
   */
  private static Result startTransformation(
      RangedExecutionConfig pGenerationConfig,
      String pFilePath,
      Map<String, String> pOverrideOptions)
      throws Exception {
    Map<String, String> overrideOptions = new LinkedHashMap<>(pOverrideOptions);

    overrideOptions.put("counterexample.export.compressWitness", "false");
    overrideOptions.put("witness.checkProgramHash", "false");
    Configuration generationConfig =
        getProperties(pGenerationConfig.fileName, overrideOptions, specificationFile);
    TestResults res = CPATestRunner.run(generationConfig, pFilePath, Level.INFO);
    logger.log(Level.FINE, res.getLog());
    logger.log(Level.FINE, res.getCheckerResult().getResult());
    return res.getCheckerResult().getResult();
  }

  private static Configuration getProperties(
      String pConfigFile, Map<String, String> pOverrideOptions, String pSpecification)
      throws InvalidConfigurationException, IOException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(Path.of("config/", pConfigFile));
    if (!Strings.isNullOrEmpty(pSpecification)) {
      pOverrideOptions.put(SPECIFICATION_OPTION, pSpecification);
    }
    pOverrideOptions.keySet().forEach(k -> configBuilder.clearOption(k));
    return configBuilder.setOptions(pOverrideOptions).build();
  }

  private static class RangeExecTester {

    private final Testcases programFile;
    private final RangedExecutionConfig generationConfig;

    private final Map<String, String> overrideOptionsBuilder = new HashMap<>();
    private final Result expectedResult;

    public RangeExecTester(
        Testcases pProgramFile, RangedExecutionConfig pGenerationConfig, Result pResult) {
      programFile = Objects.requireNonNull(pProgramFile);
      generationConfig = Objects.requireNonNull(pGenerationConfig);
      this.expectedResult = pResult;
    }

    @CanIgnoreReturnValue
    public RangeExecTester addOverrideOption(String pOptionName, String pOptionValue) {
      overrideOptionsBuilder.put(pOptionName, pOptionValue);
      return this;
    }

    public void performTest() throws Exception {
      RangedAnalysisTest.performTest(
          programFile, generationConfig, overrideOptionsBuilder, expectedResult);
    }
  }
}
