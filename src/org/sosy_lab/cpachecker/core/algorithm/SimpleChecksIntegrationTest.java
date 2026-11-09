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

  /**
   * The property files that CPAchecker ships. The JUnit tests must not use the ones below {@code
   * test/programs/benchmarks/}, which is a submodule that is not always checked out.
   */
  private static final String PROPERTY_DIR = "config/properties/";

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
            .setOption("specification", PROPERTY_DIR + pProperty + ".prp")
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
  public void testExecutionDecidesWhenSimpleRulesAbstain() throws Exception {
    IntegrationTestResult result =
        run("unreach-call", "simple/interpreter/recursive-factorial-false.c");
    result.assertIsUnsafe();
    assertThat(result.log()).contains("interpreter.properties finished successfully");
  }

  @Test
  public void testExecutionProvesCountedLoopTerminates() throws Exception {
    IntegrationTestResult result = run("termination", "simplerules/counted-loop-unknown.c");
    result.assertIsSafe();
    assertThat(result.log()).contains("interpreter--termination.properties finished successfully");
  }

  @Test(timeout = 30000)
  public void testSimpleRulesCancelAnEndlessExecution() throws Exception {
    IntegrationTestResult result = run("termination", "simplerules/endless-loop-false.c");
    result.assertIsUnsafe();
    assertThat(result.log()).contains("simpleRules--termination.properties finished successfully");
    assertThat(result.cpaCheckerResult().getReached().hasWaitingState()).isFalse();
  }

  @Test
  public void testBothAnalysesCanAbstain() throws Exception {
    run("unreach-call", "simplerules/error-behind-input-unknown.c").assertIs(Result.UNKNOWN);
  }

  @Test
  public void testOverflowIsPreservedInTheSharedCfa() throws Exception {
    run("no-overflow", "simplerules/constant-overflow-false.c").assertIsUnsafe();
  }

  @Test
  public void testMemorySafetyUsesTheSmgExecution() throws Exception {
    // The execution reports the violation of a program that allocates. It cannot prove such a
    // program safe, because the execution in which an allocation fails is a different one.
    run("valid-memsafety", "simple/interpreter/heap-out-of-bounds-false.i").assertIsUnsafe();
  }

  @Test
  public void testMemoryCleanupCanBeProved() throws Exception {
    run("valid-memcleanup", "simplerules/no-memory-operation-true.c").assertIsSafe();
  }
}
