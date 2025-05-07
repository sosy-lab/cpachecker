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
    String fileName = "config/predicateAnalysis--informationFlow.properties";
    Configuration config = TestDataTools.configurationForTest().loadFromFile(fileName).build();

    String testDir = "test/programs/taint_analysis/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testExamplePublicSafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExamplePublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("publicVariableUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSecretSafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSecretUnsafe() throws Exception {
    TestResults results = runCPAchecker("secretVariableUnsafe.c");
    results.assertIsUnsafe();
  }
  
  @Test
  public void testExampleSetPublicVariableSecretSafe() throws Exception {
    TestResults results = runCPAchecker("setPublicVariableSecretSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSetPublicVariableSecretUnsafe() throws Exception {
    TestResults results = runCPAchecker("setPublicVariableSecretUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSanitizeSecretVariableByOverrideSafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeSecretVariableByOverrideSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSanitizeSecretVariableByOverrideUnsafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeSecretVariableByOverrideUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSetSecretVariablePublicSafe() throws Exception {
    TestResults results = runCPAchecker("setSecretVariablePublicSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSetSecretVariablePublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("setSecretVariablePublicUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleTaintPublicVariableSafe() throws Exception {
    TestResults results = runCPAchecker("taintPublicVariableSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleTaintPublicVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("taintPublicVariableUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExamplePublicArraySafe() throws Exception {
    TestResults results = runCPAchecker("publicArraySafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExamplePublicArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("publicArrayUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSecretArraySafe() throws Exception {
    TestResults results = runCPAchecker("secretArraySafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSecretArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("secretArrayUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleArraySanitizationBySettingPublicSafe() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationBySettingPublicSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleArraySanitizationBySettingPublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("arraySanitizationBySettingPublicUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleArrayTaintedBySetNotPublicSafe() throws Exception {
    TestResults results = runCPAchecker("arrayTaintedBySetNotPublicSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleArrayTaintedBySetNotPublicUnsafe() throws Exception {
    TestResults results = runCPAchecker("arrayTaintedBySetNotPublicUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArraySafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeContainedVariableButNotArraySafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testSanitizeContainedVariableButNotArrayUnsafe() throws Exception {
    TestResults results = runCPAchecker("sanitizeContainedVariableButNotArrayUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testMultipleSanitizeMethodsSafe() throws Exception {
    TestResults results = runCPAchecker("multipleSanitizeMethodsSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testMultipleSanitizeMethodsUnsafe() throws Exception {
    TestResults results = runCPAchecker("multipleSanitizeMethodsUnsafe.c");
    results.assertIsUnsafe();
  }
}
