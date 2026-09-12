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
 * Integration tests for the correctness of witness validation for witness files in version 2.*
 * format.
 */
public final class AutomatonWitnessV2ValidationCorrectnessIntegrationTest {

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private String TEST_DIR_PATH = "test/programs/witness-v2-validation/no-overflow";
  private String SPECIFICATION_PATH = "config/properties/";

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--1.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_3() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--3.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_4() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--4.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--1.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  private void validate(String pWitnessFileName, Result pExpectedResult) throws Exception {
    WitnessV2ValidationTestUtils.performValidationTest(
        Path.of(TEST_DIR_PATH, "simple.c"),
        pExpectedResult,
        Path.of(SPECIFICATION_PATH, "no-overflow.prp"),
        Path.of(TEST_DIR_PATH, pWitnessFileName));
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d1_valid_loop_invariant() throws Exception {
    validate("simple-valid-witness-v2d1--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_valid_loop_invariant() throws Exception {
    validate("simple-valid-witness-v2d2--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_invalid_loop_invariant() throws Exception {
    validate("simple-invalid-witness-v2d2--1.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_valid_location_invariant() throws Exception {
    validate("simple-valid-witness-v2d2-location--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_invalid_location_invariant() throws Exception {
    validate("simple-invalid-witness-v2d2-location--1.yml", Result.FALSE);
  }

  /** Transition invariants used to be rejected in version 2.2, they were only allowed in 2.1. */
  @Test(timeout = 3000)
  public void validate_witness_v2d2_transition_invariant() throws Exception {
    validate("simple-valid-witness-v2d2-transition--1.yml", Result.TRUE);
  }

  /** Function contracts cannot be validated yet, but they must not make the witness invalid. */
  @Test(timeout = 3000)
  public void validate_witness_v2d2_function_contract() throws Exception {
    validate("simple-valid-witness-v2d2-contract--1.yml", Result.TRUE);
  }

  /** Contracts written by CPAchecker up to version 2.1 were wrapped in an invariant. */
  @Test(timeout = 3000)
  public void validate_witness_v2d1_legacy_function_contract() throws Exception {
    validate("simple-valid-witness-v2d1-contract-legacy--1.yml", Result.TRUE);
  }
}
