// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import java.nio.file.Path;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TaintAnalysisTest {

  private TestResults runCPAchecker(String pProgramName) throws Exception {
    String fileName = "config/predicateAnalysis--taintAnalysis.properties";
    Configuration config = TestDataTools.configurationForTest().loadFromFile(fileName).build();

    String testDir = "test/programs/taint_analysis/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testExamplePublicSafe() throws Exception {
    TestResults results = runCPAchecker("examplePublicSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExamplePublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("examplePublicUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSecretSafe() throws Exception {
    TestResults results = runCPAchecker("exampleSecretSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSecretUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSecretUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSetPublicVariableSecretSafe() throws Exception {
    TestResults results = runCPAchecker("exampleSetPublicVariableSecretSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSetPublicVariableSecretUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSetPublicVariableSecretUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSanitizeSecretVariableByOverrideSafe() throws Exception {
    TestResults results = runCPAchecker("exampleSanitizeSecretVariableByOverrideSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSanitizeSecretVariableByOverrideUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSanitizeSecretVariableByOverrideUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSetSecretVariablePublicSafe() throws Exception {
    TestResults results = runCPAchecker("exampleSetSecretVariablePublicSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSetSecretVariablePublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSetSecretVariablePublicUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleTaintPublicVariableSafe() throws Exception {
    TestResults results = runCPAchecker("exampleTaintPublicVariableSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleTaintPublicVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleTaintPublicVariableUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExamplePublicArraySafe() throws Exception {
    TestResults results = runCPAchecker("examplePublicArraySafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExamplePublicArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("examplePublicArrayUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSecretArraySafe() throws Exception {
    TestResults results = runCPAchecker("exampleSecretArraySafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSecretArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSecretArrayUnsafe.c");
    results.assertIsUnsafe();
  }
}
