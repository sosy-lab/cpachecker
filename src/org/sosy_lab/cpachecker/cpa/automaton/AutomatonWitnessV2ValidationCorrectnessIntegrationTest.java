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
  private static final Path PROGRAMS_PATH = Path.of("test/programs/simple/witness_validation");

  private void validate(String pProgramFileName, String pWitnessFileName, Result pExpectedResult)
      throws Exception {
    WitnessV2ValidationTestUtils.performValidationTest(
        Path.of(TEST_DIR_PATH, pProgramFileName),
        pExpectedResult,
        Path.of(SPECIFICATION_PATH, "no-overflow.prp"),
        Path.of(TEST_DIR_PATH, pWitnessFileName));
  }

  private void validate(Path pProgram, String pWitnessFileName, Result pExpectedResult)
      throws Exception {
    WitnessV2ValidationTestUtils.performValidationTest(
        pProgram,
        pExpectedResult,
        Path.of(SPECIFICATION_PATH, "no-overflow.prp"),
        Path.of(TEST_DIR_PATH, pWitnessFileName));
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_1() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2--1.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_2() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2--2.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_3() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2--3.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_4() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2--4.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_1() throws Exception {
    validate("simple.c", "simple-valid-witness-v2--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_2() throws Exception {
    validate("simple.c", "simple-valid-witness-v2--2.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d1_valid_loop_invariant() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d1--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_valid_loop_invariant() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d2--1.yml", Result.TRUE);
  }

  @Test(timeout = 9000)
  public void validate_witness_v2d2_invalid_loop_invariant() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2d2--1.yml", Result.FALSE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_valid_location_invariant() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d2-location--1.yml", Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_witness_v2d2_invalid_location_invariant() throws Exception {
    validate("simple.c", "simple-invalid-witness-v2d2-location--1.yml", Result.FALSE);
  }

  /**
   * Location invariants hold before their statement, also inside a function body, whose declaration
   * in the global part of the program covers the lines of the invariants.
   */
  @Test(timeout = 3000)
  public void validate_valid_location_invariants_in_functions() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("function-calls.c"),
        "function-calls-valid-location-invariants.yml",
        Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_invalid_location_invariant_in_called_function() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("function-calls.c"),
        "function-calls-invalid-location-invariant-in-callee.yml",
        Result.FALSE);
  }

  /** The invariant holds after the statement `i++;`, but not before it. */
  @Test(timeout = 3000)
  public void validate_invalid_location_invariant_before_statement() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("function-calls.c"),
        "function-calls-invalid-location-invariant-before-statement.yml",
        Result.FALSE);
  }

  /** Transition invariants used to be rejected in version 2.2, they were only allowed in 2.1. */
  @Test(timeout = 3000)
  public void validate_witness_v2d2_transition_invariant() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d2-transition--1.yml", Result.TRUE);
  }

  /**
   * A transition invariant on a location is matched to its statement, like a location invariant.
   */
  @Test(timeout = 3000)
  public void validate_witness_v2d2_location_transition_invariant() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d2-location-transition--1.yml", Result.TRUE);
  }

  /** Function contracts cannot be validated yet, but they must not make the witness invalid. */
  @Test(timeout = 3000)
  public void validate_witness_v2d2_function_contract() throws Exception {
    validate("simple.c", "simple-valid-witness-v2d2-contract--1.yml", Result.TRUE);
  }

  /** The name {@code data} in the invariant refers to the inner of two variables with this name. */
  @Test(timeout = 3000)
  public void validate_valid_invariant_with_shadowed_variable() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("shadowed-variable.c"),
        "shadowed-variable-valid-loop-invariant.yml",
        Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_invalid_invariant_with_shadowed_variable() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("shadowed-variable.c"),
        "shadowed-variable-invalid-loop-invariant.yml",
        Result.FALSE);
  }

  /** The names in a loop invariant refer to the function of the loop, not to the called one. */
  @Test(timeout = 3000)
  public void validate_valid_loop_invariant_with_call_in_body() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("loop-with-call-in-body.c"),
        "loop-with-call-in-body-valid-loop-invariant.yml",
        Result.TRUE);
  }

  @Test(timeout = 3000)
  public void validate_invalid_loop_invariant_with_call_in_body() throws Exception {
    validate(
        PROGRAMS_PATH.resolve("loop-with-call-in-body.c"),
        "loop-with-call-in-body-invalid-loop-invariant.yml",
        Result.FALSE);
  }
}
