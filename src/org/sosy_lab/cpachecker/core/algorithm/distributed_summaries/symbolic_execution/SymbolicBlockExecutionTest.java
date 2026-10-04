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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
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

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private static Configuration configuration(int pThreads) throws Exception {
    return TestUtils.configurationForTest()
        .loadFromFile("config/dss-symbolic-block-execution.properties")
        .setOption("analysis.machineModel", "Linux64")
        .setOption("specification", "config/specification/sv-comp-reachability.spc")
        .setOption("symbolicBlockExecution.threads", Integer.toString(pThreads))
        .setOption("limits.time.wall", "60s")
        .build();
  }

  private IntegrationTestResult runProgram(String pProgram, int pThreads) throws Exception {
    Path program = tempFolder.newFile("program.c").toPath();
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
  public void memoryAccessIsRejected() throws Exception {
    IntegrationTestResult result =
        runProgram(
            """
            extern void reach_error(void);
            int main(void) {
              int a[2] = {1, 2};
              if (a[0] == 1) {
                reach_error();
              }
              return 0;
            }
            """,
            0);
    result.assertIs(Result.UNKNOWN);
    assertThat(result.log()).contains("does not support programs that access memory");
  }
}
