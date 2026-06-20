// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;

public class AutomatonWitnessV2d2ValidationIntegrationTest
    extends AutomatonWitnessV2d0ValidationIntegrationTest {

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private String CONCURRENCY_TEST_DIR_PATH = "test/programs/concurrency";
  private String SPECIFICATION_PATH = "config/properties/";

  @Test(timeout = 3000)
  public void validate_unreach_call_concurrency() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach.c");
    Path witnessFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach.witness-2.2.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    Path witnessFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.witness-2.2.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreach_call_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach.c");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }
}
