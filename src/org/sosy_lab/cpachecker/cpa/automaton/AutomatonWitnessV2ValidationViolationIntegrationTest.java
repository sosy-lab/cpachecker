// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
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

  private String CONCURRENCY_TEST_DIR_PATH = "test/programs/witness-v2-validation/concurrency";

  private String UNREACH_CALL_TEST_DIR_PATH = "test/programs/witness-v2-validation/unreach-call";

  private String TERMINATION_TEST_DIR_PATH = "test/programs/witness-v2-validation/termination";

  private String PROGRAMS_PATH = "test/programs/simple/witness_validation";

  private String SPECIFICATION_PATH = "config/properties/";

  /** The overflow happens in the condition of an if statement, before its branch is taken. */
  @Test(timeout = 9000)
  public void validate_overflow_in_condition_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "overflow-in-condition.c");
    WitnessV2ValidationTestUtils.verificationPlusValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath);
  }

  /** The name {@code data} in the assumption refers to the inner of two variables. */
  @Test(timeout = 3000)
  public void validate_assumption_with_shadowed_variable() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "shadowed-variable-violation.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "shadowed-variable-violation.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /** BAM does not handle the write through a pointer to a variable of the caller. */
  @Test(timeout = 9000)
  public void validate_recursion_with_pointer_write() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "recursion-with-pointer-write.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "recursion-with-pointer-write.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.UNKNOWN, specificationFilePath, witnessFilePath);
  }

  /** The condition of the branching waypoint calls a function in its second operand. */
  @Test(timeout = 3000)
  public void validate_branching_at_condition_with_calls() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "branching-at-condition-with-calls.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "branching-at-condition-with-calls.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void reject_wrong_branch_at_condition_with_calls() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "branching-at-condition-with-calls.c");
    Path witnessFilePath =
        Path.of(
            UNREACH_CALL_TEST_DIR_PATH,
            "branching-at-condition-with-calls.wrong-branch.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  /** Branching waypoints at a loop with an empty body and at a condition with constants. */
  @Test(timeout = 3000)
  public void validate_branching_at_empty_loop_body_and_constant_operand() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "empty-loop-body-and-constant-operand.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "empty-loop-body-and-constant-operand.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /** Function-enter and function-return waypoints exist since format version 2.0. */
  @Test(timeout = 3000)
  public void validate_function_waypoints() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "function-waypoints.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "function-waypoints.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /** The defined function never returns the value of the function-return waypoint. */
  @Test(timeout = 3000)
  public void reject_function_return_waypoint_with_wrong_result() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "function-waypoints.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "function-waypoints-wrong-result.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  /** Without a column, the waypoints refer to the first call in their line. */
  @Test(timeout = 3000)
  public void validate_function_waypoints_without_column() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "function-waypoints.c");
    Path witnessFilePath =
        Path.of(UNREACH_CALL_TEST_DIR_PATH, "function-waypoints-without-column.witness-2.0.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /** The exported assumption names the inner of two variables as in the input program. */
  @Test(timeout = 9000)
  public void validate_shadowed_variable_roundtrip() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "shadowed-variable-violation.c");
    Path witnessFilePath =
        WitnessV2ValidationTestUtils.verificationPlusValidationTest(
            inputFilePath, Result.FALSE, specificationFilePath);
    assertThat(Files.readString(witnessFilePath)).contains("\"(data == 5)\"");
  }

  /** The name {@code data} in the assumptions of the lasso refers to the inner of two variables. */
  @Test(timeout = 3000)
  public void validate_nontermination_assumption_with_shadowed_variable() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "termination.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "shadowed-variable-nontermination.c");
    Path witnessFilePath =
        Path.of(
            TERMINATION_TEST_DIR_PATH, "shadowed-variable-nontermination-inner.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  /** The assumptions of the lasso hold only for the outer variable, so the witness is rejected. */
  @Test(timeout = 3000)
  public void validate_nontermination_assumption_on_outer_shadowed_variable() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "termination.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "shadowed-variable-nontermination.c");
    Path witnessFilePath =
        Path.of(
            TERMINATION_TEST_DIR_PATH, "shadowed-variable-nontermination-outer.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  /** The waypoint at the loop refers to the loop and not to the if statement after it. */
  @Test(timeout = 3000)
  public void validate_nontermination_loop_and_branch_on_one_line() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "termination.prp");
    Path inputFilePath = Path.of(PROGRAMS_PATH, "loop-and-branch-on-one-line-nontermination.c");
    Path witnessFilePath =
        Path.of(
            TERMINATION_TEST_DIR_PATH,
            "loop-and-branch-on-one-line-nontermination.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

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
  @Test(timeout = 9000)
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

  /** The thread creation spans two lines, so its closing parenthesis is on the second one. */
  @Test(timeout = 3000)
  public void validate_function_enter_of_call_spanning_lines() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "unreach-call.prp");
    Path inputFilePath = Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach-multiline-call.c");
    Path witnessFilePath =
        Path.of(CONCURRENCY_TEST_DIR_PATH, "concurrent-unreach-multiline-call.witness-2.2.yml");
    WitnessV2ValidationTestUtils.performValidationTest(
        inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
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
