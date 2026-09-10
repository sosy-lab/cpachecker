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
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class ExecutionCPAIntegrationTest {

  private static final String PROGRAM_DIR = "test/programs/execution/";

  private static final String PROPERTY_DIR = "test/programs/benchmarks/properties/";

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

  /**
   * Create the configuration that CPAchecker uses after it has detected an SV-COMP property on the
   * command line and switched to the configuration named by the corresponding {@code *.config}
   * option: the given configuration file with the property file as specification. The entry
   * function is taken from the property file by CPAchecker itself, so we have to set it here.
   */
  private static ConfigurationBuilder configWithProperty(String pConfigFile, String pProperty)
      throws Exception {
    return TestUtils.configurationForTest()
        .loadFromFile(pConfigFile)
        .setOption("specification", PROPERTY_DIR + pProperty)
        .setOption("analysis.entryFunction", "main");
  }

  private static IntegrationTestResult runWithProperty(
      String pConfigFile, String pProperty, String pProgram) throws Exception {
    return IntegrationTestRunner.run(
        configWithProperty(pConfigFile, pProperty).build(), PROGRAM_DIR + pProgram);
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

  // The following tests use the SV-COMP property files, i.e., they check that the execution
  // configurations give the right answer for the specification that CPAchecker derives from
  // "--spec <property>.prp".

  @Test
  public void unreachCallProperty() throws Exception {
    runWithProperty("config/execution.properties", "unreach-call.prp", "recursive-factorial-true.c")
        .assertIsSafe();
    runWithProperty(
            "config/execution.properties", "unreach-call.prp", "recursive-factorial-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void terminationProperty() throws Exception {
    runWithProperty(
            "config/execution--termination.properties",
            "termination.prp",
            "terminating-loop-true.c")
        .assertIsSafe();
    // A non-terminating program cannot be shown to be non-terminating by executing it, so the
    // analysis must not report FALSE. (It runs into the time limit, hence the small limit here.)
    Configuration config =
        configWithProperty("config/execution--termination.properties", "termination.prp")
            .setOption("limits.time.cpu", "10s")
            .build();
    IntegrationTestRunner.run(config, PROGRAM_DIR + "nonterminating.c").assertIs(Result.UNKNOWN);
  }

  @Test
  public void noOverflowProperty() throws Exception {
    runWithProperty(
            "config/execution--overflow.properties", "no-overflow.prp", "terminating-loop-true.c")
        .assertIsSafe();
    runWithProperty(
            "config/execution--overflow.properties", "no-overflow.prp", "signed-overflow-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void validMemsafetyProperty() throws Exception {
    runWithProperty(
            "config/execution--memorysafety.properties",
            "valid-memsafety.prp",
            "heap-recursion-true.c")
        .assertIsSafe();
    runWithProperty(
            "config/execution--memorysafety.properties",
            "valid-memsafety.prp",
            "heap-out-of-bounds-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void samplingFindsAViolationThatDependsOnAnInput() throws Exception {
    // Without sampling these programs are rejected, because their execution depends on an input.
    runWithProperty(
            "config/execution-sampling.properties", "unreach-call.prp", "nondeterministic-input.c")
        .assertIsUnsafe();
    runWithProperty(
            "config/execution-sampling.properties",
            "unreach-call.prp",
            "input-error-in-second-branch-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void samplingNeverProvesSafety() throws Exception {
    // Only one of the executions of the program was explored, so TRUE must not be reported even
    // though this execution did not violate the specification.
    runWithProperty(
            "config/execution-sampling.properties", "unreach-call.prp", "input-safe-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void samplingDoesNotAffectDeterministicPrograms() throws Exception {
    runWithProperty(
            "config/execution-sampling.properties",
            "unreach-call.prp",
            "recursive-factorial-true.c")
        .assertIsSafe();
  }

  @Test
  public void validMemcleanupProperty() throws Exception {
    runWithProperty(
            "config/execution--memorycleanup.properties",
            "valid-memcleanup.prp",
            "heap-recursion-true.c")
        .assertIsSafe();
    runWithProperty(
            "config/execution--memorycleanup.properties",
            "valid-memcleanup.prp",
            "heap-leak-false.c")
        .assertIsUnsafe();
  }
}
