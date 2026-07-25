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

public final class AutomatonWitnessV2d0ValidationIntegrationTest {

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private String TEST_DIR_PATH = "test/programs/witness-v2-validation";
  private String SPECIFICATION_PATH = "config/properties/";

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--1.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--2.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_3() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--3.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_4() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--4.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--1.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--2.yml");
    WitnessV2ValidationUtilsTest.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }
}
