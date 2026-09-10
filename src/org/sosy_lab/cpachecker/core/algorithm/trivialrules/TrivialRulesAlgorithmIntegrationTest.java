// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests for the trivial rules. A rule has to abstain whenever its argument does not hold, so a
 * large part of these tests expects the result UNKNOWN.
 */
public class TrivialRulesAlgorithmIntegrationTest {

  private static final String PROGRAM_DIR = "test/programs/trivialrules/";

  private static final String PROPERTY_DIR = "test/programs/benchmarks/properties/";

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private static ConfigurationBuilder config(String pConfigFile) throws Exception {
    return TestUtils.configurationForTest().loadFromFile(pConfigFile);
  }

  /**
   * Create the configuration that CPAchecker uses after it has detected an SV-COMP property on the
   * command line and switched to the configuration named by the corresponding {@code *.config}
   * option: the given configuration file with the property file as specification. The entry
   * function is taken from the property file by CPAchecker itself, so we have to set it here.
   */
  private static ConfigurationBuilder configWithProperty(String pConfigFile, String pProperty)
      throws Exception {
    return config(pConfigFile)
        .setOption("specification", PROPERTY_DIR + pProperty)
        .setOption("analysis.entryFunction", "main");
  }

  private static IntegrationTestResult runWithProperty(
      String pConfigFile, String pProperty, String pProgram) throws Exception {
    return IntegrationTestRunner.run(
        configWithProperty(pConfigFile, pProperty).build(), PROGRAM_DIR + pProgram);
  }

  /** Run with only the given rules, in order to test that the other rules abstain. */
  private static IntegrationTestResult runWithRules(
      String pConfigFile, String pProperty, String pRules, String pProgram) throws Exception {
    Configuration configuration =
        configWithProperty(pConfigFile, pProperty).setOption("trivialrules.rules", pRules).build();
    return IntegrationTestRunner.run(configuration, PROGRAM_DIR + pProgram);
  }

  // ------------------------------------------------------------------------------------------
  // unreach-call
  // ------------------------------------------------------------------------------------------

  private static final String REACHABILITY_CONFIG = "config/trivialRules.properties";

  @Test
  public void programWithoutErrorCallIsProven() throws Exception {
    runWithProperty(REACHABILITY_CONFIG, "unreach-call.prp", "no-error-call-true.c").assertIsSafe();
  }

  @Test
  public void errorCallInDeadCodeIsProven() throws Exception {
    runWithProperty(REACHABILITY_CONFIG, "unreach-call.prp", "error-in-dead-function-true.c")
        .assertIsSafe();
  }

  @Test
  public void errorCallBehindConstantConditionIsProven() throws Exception {
    // Needs the values of the variables that are constant in every execution.
    runWithProperty(
            REACHABILITY_CONFIG, "unreach-call.prp", "error-behind-constant-condition-true.c")
        .assertIsSafe();
  }

  @Test
  public void errorCallOnEveryExecutionIsRefuted() throws Exception {
    runWithProperty(REACHABILITY_CONFIG, "unreach-call.prp", "error-called-unconditionally-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void errorCallOnEveryExecutionIsNotProven() throws Exception {
    // The rule that proves unreach-call has to abstain here: a target location is reachable.
    runWithRules(
            REACHABILITY_CONFIG,
            "unreach-call.prp",
            "no-reachable-target-location",
            "error-called-unconditionally-false.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void errorCallBehindInputIsUndecided() throws Exception {
    // Whether the error is reached depends on the input, so every rule has to abstain.
    runWithProperty(REACHABILITY_CONFIG, "unreach-call.prp", "error-behind-input-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  // ------------------------------------------------------------------------------------------
  // termination
  // ------------------------------------------------------------------------------------------

  private static final String TERMINATION_CONFIG = "config/trivialRules--termination.properties";

  @Test
  public void loopFreeProgramTerminates() throws Exception {
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "loop-free-true.c").assertIsSafe();
  }

  @Test
  public void loopInDeadCodeDoesNotPreventTermination() throws Exception {
    // The loop is behind a condition that is never true, so no execution reaches it.
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "loop-in-dead-code-true.c")
        .assertIsSafe();
  }

  @Test
  public void endlessLoopOnEveryExecutionIsRefuted() throws Exception {
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "endless-loop-false.c").assertIsUnsafe();
  }

  @Test
  public void loopThatDependsOnInputIsUndecided() throws Exception {
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "input-loop-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void countedLoopIsUndecided() throws Exception {
    // No rule argues about how often a loop runs.
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "counted-loop-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void recursionIsUndecided() throws Exception {
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "recursion-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  // ------------------------------------------------------------------------------------------
  // no-overflow
  // ------------------------------------------------------------------------------------------

  private static final String OVERFLOW_CONFIG = "config/trivialRules--overflow.properties";

  @Test
  public void programWithoutArithmeticHasNoOverflow() throws Exception {
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "no-arithmetic-true.c").assertIsSafe();
  }

  @Test
  public void boundedArithmeticHasNoOverflow() throws Exception {
    // The operands are narrow enough for every result to fit into an int, and the unsigned
    // arithmetic may wrap around.
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "bounded-arithmetic-true.c").assertIsSafe();
  }

  @Test
  public void constantOverflowIsRefuted() throws Exception {
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "constant-overflow-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void divisionOverflowIsRefuted() throws Exception {
    // INT_MIN / -1 leaves the range of int, just like an addition that is too large.
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "division-overflow-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void shiftOverflowIsRefuted() throws Exception {
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "shift-overflow-false.c").assertIsUnsafe();
  }

  @Test
  public void overflowThatDependsOnInputIsUndecided() throws Exception {
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "input-overflow-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void constantOverflowIsNotFoundIfConstantsAreFolded() throws Exception {
    // The parser evaluates the constant expression and puts the value it wrapped around to into
    // the CFA, so the rule that proves the absence of an overflow has to abstain.
    Configuration configuration =
        configWithProperty(OVERFLOW_CONFIG, "no-overflow.prp")
            .setOption("cfa.simplifyConstExpressions", "true")
            .build();
    IntegrationTestRunner.run(configuration, PROGRAM_DIR + "constant-overflow-false.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void specificationWithoutPropertyFileIsNotDecided() throws Exception {
    // The rules need to know which propositions to settle, which the property file states.
    Configuration configuration =
        config(REACHABILITY_CONFIG)
            .setOption("specification", "config/specification/sv-comp-reachability.spc")
            .build();
    IntegrationTestRunner.run(configuration, PROGRAM_DIR + "error-called-unconditionally-false.c")
        .assertIs(Result.DONE);
  }
}
