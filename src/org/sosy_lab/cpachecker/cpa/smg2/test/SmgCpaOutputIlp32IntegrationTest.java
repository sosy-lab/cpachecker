// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import static com.google.common.truth.TruthJUnit.assume;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Tests YAML violation-witness output from SMG2 configurations on ILP32 programs. */
public class SmgCpaOutputIlp32IntegrationTest extends SMGCPAOutputIntegrationTest0 {

  @Test
  public void validDerefTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '*' in the for-loop update expression '*ptr = 1', line 22, column 26.
    assertThatILP32Program(
            "simple/memsafety/deref_tests/valid_deref-null_from_malloc_fail2-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 22, 26);
  }

  @Test
  public void validFreeTargetWaypoint() throws Exception {
    if (!TestUtils.shouldRunExtendedTests()) {
      assume().that(configToUse).isEqualTo(SVCOMP27);
      assume().that(specToUse).isEqualTo(VALID_MEMSAFETY_PROPERTY);
    }
    // Expected target: '(' starting the expression '(free(ptr), free(ptr))', line 25, column 3.
    assertThatILP32Program(
            "simple/memsafety/free_memory/valid_free-double_free_malloc_one_line-false.c")
        .returnsViolationWitnessWithTargetAt("valid-free", 25, 3);
  }

  @Test
  public void validFreeLocalVarTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '(' starting the comma expression, line 43, column 3.
    assertThatILP32Program("simple/memsafety/free_memory/valid_free_local_var-false.c")
        .returnsViolationWitnessWithTargetAt("valid-free", 43, 3);
  }

  @Test
  public void validFreeDoubleFreeTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: 'f' starting the expression 'free(ptr)', line 26, column 3.
    assertThatILP32Program("simple/memsafety/free_memory/valid_free-double_free_malloc1-false.c")
        .returnsViolationWitnessWithTargetAt("valid-free", 26, 3);
  }

  @Test
  public void validDerefWriteBeyondSizeTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '*' starting the assignment expression, line 25, column 5.
    assertThatILP32Program("simple/memsafety/deref_tests/valid_deref-write_beyond_size-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 25, 5);
  }

  @Test
  public void validDerefWriteBelowSizeTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '*' starting the for-loop initialization expression, line 26, column 8.
    assertThatILP32Program("simple/memsafety/deref_tests/valid_deref-write_below_size-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 26, 8);
  }

  @Test
  public void validDerefReadBeyondSizeTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '(' starting the parenthesized comma expression, line 29, column 3.
    assertThatILP32Program("simple/memsafety/deref_tests/valid_deref-read_beyond_size-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 29, 3);
  }

  @Test
  public void validDerefNullFromMallocFail3TargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: 'r' starting the assignment expression, line 28, column 5.
    assertThatILP32Program(
            "simple/memsafety/deref_tests/valid_deref-null_from_malloc_fail3-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 28, 5);
  }

  @Test
  public void validDerefNullFromMallocFail1TargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '*' starting the return expression, line 20, column 10.
    assertThatILP32Program(
            "simple/memsafety/deref_tests/valid_deref-null_from_malloc_fail1-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 20, 10);
  }

  @Test
  public void validDerefLiteralTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: '(' starting the for-loop condition expression, line 25, column 19.
    assertThatILP32Program("simple/memsafety/deref_tests/valid_deref-deref_literal-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 25, 19);
  }

  @Test
  public void validDerefAfterFreeTargetWaypoint() throws Exception {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
    // Expected target: 'f' starting the expression 'free((void *) *ptr)', line 26, column 3.
    assertThatILP32Program(
            "simple/memsafety/deref_tests/valid_deref-deref_after_free-malloc-false.c")
        .returnsViolationWitnessWithTargetAt("valid-deref", 26, 3);
  }
}
