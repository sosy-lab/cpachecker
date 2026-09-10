// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

  // ------------------------------------------------------------------------------------------
  // memory safety
  // ------------------------------------------------------------------------------------------

  private static final String MEMORY_SAFETY_CONFIG = "config/trivialRules--memorysafety.properties";

  private static final String MEMORY_CLEANUP_CONFIG =
      "config/trivialRules--memorycleanup.properties";

  @Test
  public void programWithoutMemoryOperationIsSafe() throws Exception {
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "no-memory-operation-true.c")
        .assertIsSafe();
    runWithProperty(MEMORY_CLEANUP_CONFIG, "valid-memcleanup.prp", "no-memory-operation-true.c")
        .assertIsSafe();
  }

  @Test
  public void programWithoutAllocationLeaksNothing() throws Exception {
    // The program dereferences a pointer, so valid-deref is not proven, but without an allocation
    // there is no block that could be leaked.
    runWithProperty(MEMORY_CLEANUP_CONFIG, "valid-memcleanup.prp", "pointer-to-local-true.c")
        .assertIsSafe();
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "pointer-to-local-true.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void arrayAccessInsideBoundsIsSafe() throws Exception {
    // The type of the index allows only values that are inside the array.
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "array-in-bounds-true.c")
        .assertIsSafe();
  }

  @Test
  public void arrayAccessOutsideBoundsIsRefuted() throws Exception {
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "array-out-of-bounds-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void arrayAccessThatDependsOnInputIsUndecided() throws Exception {
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "array-index-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void freeOfNonHeapObjectIsRefuted() throws Exception {
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "free-of-local-false.c")
        .assertIsUnsafe();
  }

  @Test
  public void allocationIsUndecided() throws Exception {
    // A program that calls free() is not decided by any rule, not even for memcleanup.
    runWithProperty(MEMORY_CLEANUP_CONFIG, "valid-memcleanup.prp", "free-of-local-false.c")
        .assertIs(Result.UNKNOWN);
  }

  // ------------------------------------------------------------------------------------------
  // no-data-race and programs that do nothing
  // ------------------------------------------------------------------------------------------

  private static final String DATA_RACE_CONFIG = "config/trivialRules--datarace.properties";

  @Test
  public void singleThreadedProgramHasNoDataRace() throws Exception {
    runWithProperty(DATA_RACE_CONFIG, "no-data-race.prp", "single-threaded-true.c").assertIsSafe();
  }

  @Test
  public void programThatCreatesThreadIsUndecided() throws Exception {
    runWithProperty(DATA_RACE_CONFIG, "no-data-race.prp", "thread-creation-unknown.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void programThatDoesNothingSatisfiesEverySpecification() throws Exception {
    runWithProperty(REACHABILITY_CONFIG, "unreach-call.prp", "empty-main-true.c").assertIsSafe();
    runWithProperty(TERMINATION_CONFIG, "termination.prp", "empty-main-true.c").assertIsSafe();
    runWithProperty(OVERFLOW_CONFIG, "no-overflow.prp", "empty-main-true.c").assertIsSafe();
    runWithProperty(MEMORY_SAFETY_CONFIG, "valid-memsafety.prp", "empty-main-true.c")
        .assertIsSafe();
    runWithProperty(DATA_RACE_CONFIG, "no-data-race.prp", "empty-main-true.c").assertIsSafe();
  }

  // ------------------------------------------------------------------------------------------
  // witnesses
  // ------------------------------------------------------------------------------------------

  @Rule public TemporaryFolder outputDirectory = new TemporaryFolder();

  private String runAndReadWitness(String pConfigFile, String pProperty, String pProgram)
      throws Exception {
    Configuration configuration =
        TestUtils.configurationForTestWithOutput(outputDirectory)
            .loadFromFile(pConfigFile)
            .setOption("specification", PROPERTY_DIR + pProperty)
            .setOption("analysis.entryFunction", "main")
            .setOption("trivialrules.witness", "witness.yml")
            .build();
    IntegrationTestResult result = IntegrationTestRunner.run(configuration, PROGRAM_DIR + pProgram);
    // The output files are written by the caller of CPAchecker, not by CPAchecker itself.
    result.cpaCheckerResult().writeOutputFiles();
    Path witness = outputDirectory.getRoot().toPath().resolve("output").resolve("witness.yml");
    assertThat(Files.exists(witness)).isTrue();
    return Files.readString(witness);
  }

  @Test
  public void correctnessWitnessIsAnEmptyInvariantSet() throws Exception {
    // A trivial rule argues about the program as a whole, so it has no invariant to offer.
    String witness =
        runAndReadWitness(REACHABILITY_CONFIG, "unreach-call.prp", "no-error-call-true.c");
    assertThat(witness).contains("entry_type: \"invariant_set\"");
    assertThat(witness).contains("content: []");
  }

  @Test
  public void violationWitnessPointsToTheViolatedLocation() throws Exception {
    String witness =
        runAndReadWitness(
            REACHABILITY_CONFIG, "unreach-call.prp", "error-called-unconditionally-false.c");
    assertThat(witness).contains("entry_type: \"violation_sequence\"");
    assertThat(witness).contains("type: \"target\"");
    assertThat(witness).contains("error-called-unconditionally-false.c");
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
