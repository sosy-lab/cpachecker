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
  private String SV_BENCHMARKS_TEST_DIR_PATH = "test/programs/benchmarks";
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

  @Test(timeout = 30000000)
  public void validate_unreach_call_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach.c");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreac_call_concurrency_goblint_regression_1() throws Exception {
    Path inputFilePath =
        Path.of(
            SV_BENCHMARKS_TEST_DIR_PATH,
            "goblint-regression",
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c");
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path witnessFilePath =
        Path.of(
            CONCURRENCY_TEST_DIR_PATH,
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c.witness.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_unreac_call_concurrency_goblint_regression_2() throws Exception {
    Path inputFilePath =
        Path.of(
            SV_BENCHMARKS_TEST_DIR_PATH,
            "goblint-regression",
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c");
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path witnessFilePath =
        Path.of(
            CONCURRENCY_TEST_DIR_PATH,
            "36-apron_41-threadenter-no-locals_unknown_1_neg.c.second.witness.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-data-race.c");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency_roundtrip_gcd() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(SV_BENCHMARKS_TEST_DIR_PATH, "pthread-atomic/gcd-2.i");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 3000)
  public void validate_data_race_concurrency_roundtrip_qw2004() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath = Path.of(SV_BENCHMARKS_TEST_DIR_PATH, "pthread-lit/qw2004-1.i");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }

  @Test(timeout = 9000)
  public void validate_data_race_concurrency_roundtrip_read_write_lock() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-data-race.prp");
    Path inputFilePath =
        Path.of(SV_BENCHMARKS_TEST_DIR_PATH, "pthread-atomic/read_write_lock-2b.i");
    verificationPlusValidationTest(inputFilePath, Result.FALSE, specificationFilePath);
  }
}
