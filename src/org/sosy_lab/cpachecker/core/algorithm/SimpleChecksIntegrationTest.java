// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.truth.Truth.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class SimpleChecksIntegrationTest {

  @BeforeClass
  public static void requireExtendedTests() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private IntegrationTestResult run(String pProperty, String pProgram) throws Exception {
    String suffix =
        switch (pProperty) {
          case "termination" -> "--termination";
          case "no-overflow" -> "--overflow";
          case "valid-memsafety" -> "--memorysafety";
          case "valid-memcleanup" -> "--memorycleanup";
          default -> "";
        };
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/simpleChecks" + suffix + ".properties")
            .setOption("specification", "test/programs/benchmarks/properties/" + pProperty + ".prp")
            .setOption("analysis.entryFunction", "main")
            .build();
    IntegrationTestResult result = IntegrationTestRunner.run(config, "test/programs/" + pProgram);
    ReachedSet reached = result.cpaCheckerResult().getReached();
    for (AbstractState state : reached) {
      assertThat(reached.getReached(state)).contains(state);
    }
    return result;
  }

  @Test
  public void executionDecidesWhensimpleRulesAbstain() throws Exception {
    IntegrationTestResult result =
        run("unreach-call", "simple/interpreter/recursive-factorial-false.c");
    result.assertIsUnsafe();
    assertThat(result.log()).contains("interpreter.properties finished successfully");
  }

  @Test
  public void executionProvesCountedLoopTerminates() throws Exception {
    IntegrationTestResult result = run("termination", "simplerules/counted-loop-unknown.c");
    result.assertIsSafe();
    assertThat(result.log()).contains("interpreter--termination.properties finished successfully");
  }

  @Test(timeout = 30000)
  public void simpleRulesCancelAnEndlessExecution() throws Exception {
    IntegrationTestResult result = run("termination", "simplerules/endless-loop-false.c");
    result.assertIsUnsafe();
    assertThat(result.log()).contains("simpleRules--termination.properties finished successfully");
    assertThat(result.cpaCheckerResult().getReached().hasWaitingState()).isFalse();
  }

  @Test
  public void bothAnalysesCanAbstain() throws Exception {
    run("unreach-call", "simplerules/error-behind-input-unknown.c").assertIs(Result.UNKNOWN);
  }

  @Test
  public void overflowIsPreservedInTheSharedCfa() throws Exception {
    run("no-overflow", "simplerules/constant-overflow-false.c").assertIsUnsafe();
  }

  @Test
  public void memorySafetyUsesTheSmgExecution() throws Exception {
    run("valid-memsafety", "simple/interpreter/heap-recursion-true.i").assertIsSafe();
  }

  @Test
  public void memoryCleanupCanBeProved() throws Exception {
    run("valid-memcleanup", "simplerules/no-memory-operation-true.c").assertIsSafe();
  }
}
