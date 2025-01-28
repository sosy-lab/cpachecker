// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.CPATestRunner.ExpectedVerdict;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class YAMLWitnessTest {

  private static final String TEST_DIR_PATH = "test/programs/witnessValidation/";

  @Test(timeout = 90000)
  public void max_true() throws Exception {
    verifyProgram(TEST_DIR_PATH + "max.c", ExpectedVerdict.TRUE);
  }

  @Test(timeout = 90000)
  public void minepump_spec1_product33_false() throws Exception {
    verifyProgram(TEST_DIR_PATH + "minepump_spec1_product33.cil.c", ExpectedVerdict.FALSE);
  }

  private static void verifyProgram(String pFilePath, ExpectedVerdict pExpected) throws Exception {
    // just use default config
    Configuration generationConfig =
      TestDataTools.configurationForTest().loadFromFile("./config/default.properties").build();

    TestResults results = CPATestRunner.run(generationConfig, pFilePath);
    // trigger statistics so that the witness is written to the file
    results.getCheckerResult().writeOutputFiles();

    switch (pExpected) {
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
}
