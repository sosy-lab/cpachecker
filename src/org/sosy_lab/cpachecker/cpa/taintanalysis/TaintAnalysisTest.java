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
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption(
                "CompositeCPA.cpas",
                "cpa.location.LocationCPA, " +
                    "cpa.callstack.CallstackCPA, " +
                    "cpa.functionpointer.FunctionPointerCPA, " +
                    "cpa.predicate.PredicateCPA, " +
                    "cpa.taintanalysis.TaintAnalysisCPA")
            .build();

    String testDir = "test/programs/taint_analysis/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testExampleNotTaintedSafe() throws Exception {
    TestResults results = runCPAchecker("exampleNotTaintedSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleNotTaintedUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleNotTaintedUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleTaintedSafe() throws Exception {
    TestResults results = runCPAchecker("exampleTaintedSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleTaintedUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleTaintedUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleSanitizeTaintedVariableSafe() throws Exception {
    TestResults results = runCPAchecker("exampleSanitizeTaintedVariableSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleSanitizeTaintedVariableUnsafe() throws Exception {
    TestResults results = runCPAchecker("exampleSanitizeTaintedVariableUnsafe.c");
    results.assertIsUnsafe();
  }
}
