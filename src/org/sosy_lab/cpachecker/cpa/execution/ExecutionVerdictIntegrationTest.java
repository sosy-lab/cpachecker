// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

@RunWith(Parameterized.class)
public class ExecutionVerdictIntegrationTest {

  @Parameters(name = "stepsPerTransfer={0}")
  public static Object[] stepsPerTransfer() {
    return new Object[] {-1, 1, 2};
  }

  @Parameter public int stepsPerTransfer;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private IntegrationTestResult run(String pProgram, Map<String, String> pOptions)
      throws Exception {
    Path program = tempFolder.newFile().toPath();
    Files.writeString(program, pProgram);
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/execution.properties")
            .setOption("specification", "config/specification/sv-comp-reachability.spc")
            // Keep addressed variables out of the process-wide blacklist for other tests.
            .setOption("analysis.entryFunction", "execution_verdict_test")
            .setOption("cpa.execution.stepsPerTransfer", Integer.toString(stepsPerTransfer))
            .setOptions(pOptions)
            .build();
    return IntegrationTestRunner.run(config, program.toString());
  }

  private Map<String, String> specificationForExternalCall(String pAction) throws Exception {
    Path specification = tempFolder.newFile().toPath();
    Files.writeString(
        specification,
        "CONTROL AUTOMATON External\nINITIAL STATE Init;\nSTATE USEFIRST Init:\n"
            + "MATCH {external($?)} -> "
            + pAction
            + ";\nEND AUTOMATON\n");
    return ImmutableMap.of("specification", specification.toString());
  }

  private void checkVerdicts(
      String pDeclarations, String pStatements, Result pWithoutTarget, Result pWithTarget)
      throws Exception {
    for (boolean target : new boolean[] {false, true}) {
      IntegrationTestResult result =
          run(
              "extern void reach_error(void);\n"
                  + pDeclarations
                  + "\nint execution_verdict_test(void) { int x = 0; "
                  + pStatements
                  + (target ? " reach_error(); " : "")
                  + " return 0; }",
              ImmutableMap.of());
      Result expected = target ? pWithTarget : pWithoutTarget;
      result.assertIs(expected);
      if (expected == Result.UNKNOWN) {
        // Check that execution continued to the verdict instead of aborting at the external call.
        assertThat(result.log()).contains(target ? "no longer precise" : "no longer sound");
      }
    }
  }

  @Test
  public void voidCallWithoutPointerArgumentsPreservesBothVerdicts() throws Exception {
    checkVerdicts("extern void external(int);", "external(x);", Result.TRUE, Result.FALSE);
  }

  @Test
  public void overapproximatedReturnValueOnlyPermitsTrue() throws Exception {
    checkVerdicts("extern int external(void);", "external();", Result.TRUE, Result.UNKNOWN);
  }

  @Test
  public void ignoredSideEffectsOnlyPermitFalse() throws Exception {
    checkVerdicts("extern void external(int *);", "external(&x);", Result.UNKNOWN, Result.FALSE);
  }

  @Test
  public void ignoredSideEffectsAndReturnValuePermitNeitherVerdict() throws Exception {
    checkVerdicts("extern int external(int *);", "external(&x);", Result.UNKNOWN, Result.UNKNOWN);
  }

  @Test
  public void assignedReturnValueOnlyPermitsTrue() throws Exception {
    checkVerdicts("extern int external(void);", "x = external();", Result.TRUE, Result.UNKNOWN);
  }

  @Test
  public void assignmentAlsoTracksIgnoredSideEffects() throws Exception {
    checkVerdicts(
        "extern int external(int *);", "x = external(&x);", Result.UNKNOWN, Result.UNKNOWN);
  }

  @Test
  public void restrictionsAccumulateAcrossCallsAndFunctionReturns() throws Exception {
    String declarations =
        """
        extern int external(void);
        extern void modify(int *);
        void helper(void) { external(); }
        """;
    checkVerdicts(declarations, "modify(&x); helper();", Result.UNKNOWN, Result.UNKNOWN);
    checkVerdicts(declarations, "helper(); modify(&x);", Result.UNKNOWN, Result.UNKNOWN);
  }

  @Test
  public void unexecutedCallsDoNotAffectVerdicts() throws Exception {
    checkVerdicts(
        "extern int external(int *);",
        "int condition = 0; if (condition) { external(&x); }",
        Result.TRUE,
        Result.FALSE);
  }

  @Test
  public void modeledBuiltinPreservesPrecision() throws Exception {
    checkVerdicts(
        "extern double __builtin_fabs(double);",
        "double y = __builtin_fabs(-1.0);",
        Result.TRUE,
        Result.FALSE);
  }

  @Test
  public void explicitlyAllowedCallsRespectTheValueAnalysisConfiguration() throws Exception {
    run(
            "extern int external(int *); int execution_verdict_test(void) { int x = 0;"
                + " external(&x); return 0; }",
            ImmutableMap.of("cpa.value.allowedUnsupportedFunctions", "external"))
        .assertIsSafe();
  }

  @Test
  public void strictUnknownCallOptionStillThrowsImmediately() throws Exception {
    IntegrationTestResult result =
        run(
            "extern int external(void); int execution_verdict_test(void) { int x = external();"
                + " return 0; }",
            ImmutableMap.of("cpa.value.ignoreCallsToUnknownFunctions", "false"));
    result.assertIs(Result.UNKNOWN);
    assertThat(result.log()).contains("Unhandled call to function");
  }

  @Test
  public void callWithoutSuccessorsStillAffectsSoundness() throws Exception {
    IntegrationTestResult result =
        run(
            "extern void external(int *); int execution_verdict_test(void) { int x = 0;"
                + " external(&x); return 0; }",
            specificationForExternalCall("STOP"));
    result.assertIs(Result.UNKNOWN);
    assertThat(result.log()).contains("no longer sound");
  }

  @Test
  public void targetOnOverapproximatedCallIsRejected() throws Exception {
    IntegrationTestResult result =
        run(
            "extern int external(void); int execution_verdict_test(void) { external(); return 0; }",
            specificationForExternalCall("ERROR(\"external call\")"));
    result.assertIs(Result.UNKNOWN);
    assertThat(result.log()).contains("no longer precise");
  }
}
