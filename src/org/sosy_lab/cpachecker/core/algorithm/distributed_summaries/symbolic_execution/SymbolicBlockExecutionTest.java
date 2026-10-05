// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class SymbolicBlockExecutionTest {

  private static final String PROGRAMS = "test/programs/simple/block_analysis/";

  private static final String CALLS =
      """
      extern void reach_error(void);
      extern int __VERIFIER_nondet_int(void);
      int inc(int a) { return a + 1; }
      int main(void) {
        int x = __VERIFIER_nondet_int();
        int y = inc(x);
        int z = inc(y);
        if (CONDITION) {
          reach_error();
        }
        return 0;
      }
      """;

  /** The forward exploration never ends, but no state can reach the target location. */
  private static final String BACKWARD_PROOF =
      """
      extern int __VERIFIER_nondet_int(void);
      extern void reach_error(void);
      int main(void) {
        int i = 0;
        while (__VERIFIER_nondet_int()) {
          i++;
        }
        if (i * 2 == 1) {
          reach_error();
        }
        return 0;
      }
      """;

  /**
   * The forward exploration ends quickly, but the backward exploration runs through a loop that is
   * not reachable and lowers the value of {@code x} in every iteration.
   */
  private static final String FORWARD_PROOF =
      """
      extern int __VERIFIER_nondet_int(void);
      extern void reach_error(void);
      int main(void) {
        int x = 0;
        int c = 0;
        if (c) {
          while (__VERIFIER_nondet_int()) {
            x++;
          }
        }
        if (x == -5) {
          reach_error();
        }
        return 0;
      }
      """;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private static Configuration configuration(int pThreads) throws Exception {
    return configuration(pThreads, true);
  }

  private static Configuration configuration(int pThreads, boolean pSubtractKnownStates)
      throws Exception {
    return TestUtils.configurationForTest()
        .loadFromFile("config/dss-symbolic-block-execution.properties")
        .setOption("analysis.machineModel", "Linux64")
        .setOption("specification", "config/specification/sv-comp-reachability.spc")
        .setOption("symbolicBlockExecution.threads", Integer.toString(pThreads))
        .setOption(
            "symbolicBlockExecution.subtractKnownStates", Boolean.toString(pSubtractKnownStates))
        .setOption("limits.time.wall", "60s")
        .build();
  }

  private IntegrationTestResult runProgram(String pProgram, int pThreads) throws Exception {
    Path program = tempFolder.getRoot().toPath().resolve("program" + pThreads + ".c");
    Files.writeString(program, pProgram);
    return IntegrationTestRunner.run(configuration(pThreads), program.toString());
  }

  @Test
  public void boundedLoopIsSafe() throws Exception {
    IntegrationTestRunner.run(configuration(0), PROGRAMS + "count_safe.c").assertIsSafe();
  }

  @Test
  public void boundedLoopIsUnsafe() throws Exception {
    IntegrationTestRunner.run(configuration(0), PROGRAMS + "count_unsafe.c").assertIsUnsafe();
  }

  @Test
  public void violationAfterGotoIntoLoopIsFound() throws Exception {
    IntegrationTestRunner.run(configuration(0), PROGRAMS + "complex_loop_unsafe.c")
        .assertIsUnsafe();
  }

  @Test
  public void withoutSubtractingKnownStates() throws Exception {
    IntegrationTestRunner.run(configuration(0, false), PROGRAMS + "count_safe.c").assertIsSafe();
    IntegrationTestRunner.run(configuration(0, false), PROGRAMS + "count_unsafe.c")
        .assertIsUnsafe();
  }

  @Test
  public void workStealingAgreesOnSafeProgram() throws Exception {
    IntegrationTestRunner.run(configuration(2), PROGRAMS + "count_safe.c").assertIsSafe();
  }

  @Test
  public void workStealingAgreesOnUnsafeProgram() throws Exception {
    IntegrationTestRunner.run(configuration(2), PROGRAMS + "count_unsafe.c").assertIsUnsafe();
  }

  @Test
  public void callsAreSafe() throws Exception {
    runProgram(CALLS.replace("CONDITION", "z != x + 2"), 0).assertIsSafe();
  }

  @Test
  public void callsAreUnsafe() throws Exception {
    runProgram(CALLS.replace("CONDITION", "z == x + 2 && x == 7"), 0).assertIsUnsafe();
  }

  @Test
  public void exhaustedBackwardExplorationProvesSafety() throws Exception {
    for (int threads : new int[] {0, 2}) {
      IntegrationTestResult result = runProgram(BACKWARD_PROOF, threads);
      result.assertIsSafe();
      assertThat(result.log()).contains("the backward exploration is exhausted");
    }
  }

  @Test
  public void exhaustedForwardExplorationProvesSafety() throws Exception {
    for (int threads : new int[] {0, 2}) {
      IntegrationTestResult result = runProgram(FORWARD_PROOF, threads);
      result.assertIsSafe();
      assertThat(result.log()).contains("the forward exploration is exhausted");
    }
  }

  /**
   * Every allocation in the loop gets its own object, also though the block of the loop body is
   * encoded only once.
   */
  private static final String ALLOCATIONS =
      """
      extern void reach_error(void);
      extern void *malloc(unsigned long);
      int main(void) {
        int *p[3];
        int i;
        for (i = 0; i < 3; i++) {
          p[i] = malloc(sizeof(int));
          if (p[i] == 0) {
            return 0;
          }
          *p[i] = i;
        }
        if (CONDITION) {
          reach_error();
        }
        return 0;
      }
      """;

  /** A write through a pointer in one block is visible to the next block. */
  private static final String POINTER_TO_LOCAL =
      """
      extern void reach_error(void);
      extern int __VERIFIER_nondet_int(void);
      int main(void) {
        int x = 0;
        int *p = &x;
        if (__VERIFIER_nondet_int()) {
          *p = 5;
        } else {
          *p = 6;
        }
        if (CONDITION) {
          reach_error();
        }
        return 0;
      }
      """;

  @Test
  public void allocationsInLoopAreDistinct() throws Exception {
    runProgram(ALLOCATIONS.replace("CONDITION", "p[0] == p[1] || *p[0] != 0 || *p[2] != 2"), 0)
        .assertIsSafe();
    runProgram(ALLOCATIONS.replace("CONDITION", "*p[1] == 1"), 2).assertIsUnsafe();
  }

  @Test
  public void writesThroughPointersReachLaterBlocks() throws Exception {
    runProgram(POINTER_TO_LOCAL.replace("CONDITION", "x < 5"), 0).assertIsSafe();
    runProgram(POINTER_TO_LOCAL.replace("CONDITION", "x == 6"), 2).assertIsUnsafe();
  }

  @Test
  public void arraysAreSupported() throws Exception {
    IntegrationTestRunner.run(configuration(0), PROGRAMS + "simple_array_safe.c").assertIsSafe();
    IntegrationTestRunner.run(configuration(0), PROGRAMS + "simple_array_unsafe.c")
        .assertIsUnsafe();
  }
}
