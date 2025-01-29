// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.CPATestRunner.ExpectedVerdict;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;

public class YAMLWitnessTest {

  private static final YAMLWitnessVersion WITNESS_VERSION = YAMLWitnessVersion.V2;

  private static final String TEST_DIR = "./test/programs/witnessValidation/";

  private static final String DEFAULT_CONFIG_PATH = "./config/default.properties";

  private static final String OUTPUT_DIR = "./output/";

  private static final String YML_WITNESS_FILE = "witness-" + WITNESS_VERSION.toString() + ".yml";

  private static final String CORRECTNESS_WITNESS_PATH = OUTPUT_DIR + YML_WITNESS_FILE;

  private static final String VIOLATION_WITNESS_PATH =
      OUTPUT_DIR + "Counterexample.1." + YML_WITNESS_FILE;

  private static final String WITNESS_OPTION = "--witness";

  @Test(timeout = 90000)
  public void max_true() throws Exception {
    String path = TEST_DIR + "max.c";
    ExpectedVerdict expectedVerdict = ExpectedVerdict.TRUE;
    // generate, parse and validate witness
    generateWitness(path, expectedVerdict);
    try (InputStream inputStream = createInputStream(expectedVerdict)) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(inputStream);
      assertThat(entries.isEmpty()).isFalse();
    }
    validateWitness(path, expectedVerdict);
  }

  @Test(timeout = 90000)
  public void minepump_spec1_product33_false() throws Exception {
    String path = TEST_DIR + "minepump_spec1_product33.cil.c";
    ExpectedVerdict expectedVerdict = ExpectedVerdict.FALSE;
    // generate, parse and validate yaml witness
    generateWitness(path, expectedVerdict);
    try (InputStream inputStream = createInputStream(expectedVerdict)) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(inputStream);
      assertThat(entries.isEmpty()).isFalse();
    }
    validateWitness(path, expectedVerdict);
  }

  private static void generateWitness(String pFilePath, ExpectedVerdict pExpectedVerdict)
      throws Exception {
    // just use default config
    Configuration generationConfig = createVerificationConfig();

    TestResults results = CPATestRunner.run(generationConfig, pFilePath);
    // trigger statistics so that the witness is written to the file
    results.getCheckerResult().writeOutputFiles();

    switch (pExpectedVerdict) {
      case TRUE:
        results.assertIsSafe();
        break;
      case FALSE:
        results.assertIsUnsafe();
        break;
      default:
        assertWithMessage("Cannot determine expected result.").fail();
        throw new AssertionError("Unreachable code.");
    }
  }

  private static InputStream createInputStream(ExpectedVerdict pExpectedVerdict)
      throws FileNotFoundException {
    switch (pExpectedVerdict) {
      case TRUE:
        return new FileInputStream(new File(CORRECTNESS_WITNESS_PATH));
      case FALSE:
        return new FileInputStream(new File(VIOLATION_WITNESS_PATH));
      default:
        throw new AssertionError("Unsupported verdict " + pExpectedVerdict);
    }
  }

  private static void validateWitness(String pFilePath, ExpectedVerdict pExpectedVerdict)
      throws Exception {
    Configuration config = createValidationConfig(pExpectedVerdict);

    TestResults results = CPATestRunner.run(config, pFilePath);
    // trigger statistics so that the witness is written to the file
    results.getCheckerResult().writeOutputFiles();

    switch (pExpectedVerdict) {
      case TRUE:
        results.assertIsSafe();
        break;
      case FALSE:
        results.assertIsUnsafe();
        break;
      default:
        assertWithMessage("Cannot determine expected result.").fail();
        throw new AssertionError("Unreachable code.");
    }
  }

  private static Configuration createVerificationConfig()
      throws InvalidConfigurationException, IOException {
    return TestDataTools.configurationForTest()
        .loadFromFile(DEFAULT_CONFIG_PATH)
        // configurationForTest() disables output, we override it to obtain witnesses
        .setOption("output.disable", "false")
        .build();
  }

  private static Configuration createValidationConfig(ExpectedVerdict pExpectedVerdict)
      throws InvalidConfigurationException, IOException {
    return TestDataTools.configurationForTest()
        .loadFromFile(DEFAULT_CONFIG_PATH)
        .setOption(WITNESS_OPTION, getWitnessFileName(pExpectedVerdict))
        .build();
  }

  private static String getWitnessFileName(ExpectedVerdict pExpectedVerdict) {
    switch (pExpectedVerdict) {
      case TRUE:
        return CORRECTNESS_WITNESS_PATH;
      case FALSE:
        return VIOLATION_WITNESS_PATH;
      default:
        throw new AssertionError("Unsupported verdict " + pExpectedVerdict);
    }
  }
}
