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

/**
 * Integration tests for the violation of witness validation for witness files in version 2.*
 * format.
 */
public final class AutomatonWitnessV2ValidationViolationIntegrationTest {

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
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    Path witnessFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /**
   * The program creates two threads, but the witness only has a function-enter waypoint for the
   * second one, so only that thread has an identifier in the witness.
   */
  @Test(timeout = 3000)
  public void validate_unreach_call_witness_not_mentioning_every_thread_creation()
      throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach-two-threads.c");
    Path witnessFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach-two-threads.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /**
   * The first segment of the witness must be passed without creating the first thread, which the
   * program does unconditionally, so the witness describes no execution and must be rejected.
   */
  @Test(timeout = 3000)
  public void reject_data_race_witness_avoiding_a_necessary_thread_creation() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    Path witnessFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.avoid-creation.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  /**
   * The avoid waypoint of the witness points to a thread creation that has already been passed when
   * its segment is entered, so it can never be matched and the witness still has to be confirmed.
   */
  @Test(timeout = 3000)
  public void validate_data_race_witness_avoiding_a_passed_thread_creation() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    Path witnessFilePath =
        Path.of(
            CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.avoid-past-creation.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreach_call_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach.c");
    WitnessV2ValidationTestUtils.verificationPlusValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreac_call_concurrency_goblint_regression_1() throws Exception {
    Path inputFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "36-apron_41-threadenter-no-locals_unknown_1_neg.c");
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path witnessFilePath =
        Path.of(
            CONCURRENCY_TEST_DIR_PATH,
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c.witness.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreac_call_concurrency_goblint_regression_2() throws Exception {
    Path inputFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "36-apron_41-threadenter-no-locals_unknown_1_neg.c");
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path witnessFilePath =
        Path.of(
            CONCURRENCY_TEST_DIR_PATH,
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c.second.witness.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 9000)
  public void validate_data_race_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    WitnessV2ValidationTestUtils.verificationPlusValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath);
  }
}
