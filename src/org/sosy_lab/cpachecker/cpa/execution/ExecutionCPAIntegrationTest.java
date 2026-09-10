// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class ExecutionCPAIntegrationTest {

  private static final String PROGRAM_DIR = "test/programs/execution/";

  private static final String REACHABILITY_SPECIFICATION =
      "config/specification/sv-comp-reachability.spc";

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private static IntegrationTestResult run(String pConfigFile, String pSpec, String pProgram)
      throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(pConfigFile)
            .setOption("specification", pSpec)
            .build();
    return IntegrationTestRunner.run(config, PROGRAM_DIR + pProgram);
  }

  private static IntegrationTestResult run(String pConfigFile, String pProgram) throws Exception {
    Configuration config = TestUtils.configurationForTest().loadFromFile(pConfigFile).build();
    return IntegrationTestRunner.run(config, PROGRAM_DIR + pProgram);
  }

  @Test
  public void recursionIsExecutedPrecisely() throws Exception {
    // The plain value analysis reports a spurious counterexample here, because it loses the
    // values of a caller during a recursive call.
    run("config/execution.properties", REACHABILITY_SPECIFICATION, "recursive-factorial-true.c")
        .assertIsSafe();
  }

  @Test
  public void recursionViolationIsFound() throws Exception {
    run("config/execution.properties", REACHABILITY_SPECIFICATION, "recursive-factorial-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void mutualRecursionIsExecutedPrecisely() throws Exception {
    run("config/execution.properties", REACHABILITY_SPECIFICATION, "mutual-recursion-true.c")
        .assertIsSafe();
  }

  @Test
  public void nondeterministicProgramIsRejected() throws Exception {
    // The program has more than one execution, so ExecutionCPA is not applicable to it
    // and must not claim any result.
    run("config/execution.properties", REACHABILITY_SPECIFICATION, "nondeterministic-input.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void everyStateInTheReachedSetGivesTheSameResult() throws Exception {
    // With stepsPerTransfer=1 the analysis adds every state of the execution to the reached set
    // instead of executing the whole program within a single call of the transfer relation.
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/execution.properties")
            .setOption("specification", REACHABILITY_SPECIFICATION)
            .setOption("cpa.execution.stepsPerTransfer", "1")
            .build();
    IntegrationTestRunner.run(config, PROGRAM_DIR + "recursive-factorial-true.c").assertIsSafe();
  }

  @Test
  public void terminationIsProvenByExecution() throws Exception {
    run("config/execution--termination.properties", "terminating-loop-true.c").assertIsSafe();
  }

  @Test
  public void pointerToLocalOfCallerNeedsTheSmgAnalysis() throws Exception {
    run(
            "config/execution-smg.properties",
            REACHABILITY_SPECIFICATION,
            "pointer-to-caller-local-true.c")
        .assertIsSafe();
  }

  @Test
  public void memorySafetyViolationIsFound() throws Exception {
    run("config/execution--memorysafety.properties", "heap-out-of-bounds-false.c").assertIsUnsafe();
  }

  @Test
  public void memorySafetyWithRecursionIsProven() throws Exception {
    run("config/execution--memorysafety.properties", "heap-recursion-true.c").assertIsSafe();
  }
}
