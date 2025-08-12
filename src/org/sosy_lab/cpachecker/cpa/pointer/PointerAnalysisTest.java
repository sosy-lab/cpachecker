// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class PointerAnalysisTest {

  private static final String CPAS =
      "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.pointer.PointerAnalysisCPA";

  private static final String SPEC = "config/specification/default.spc";

  @Test
  public void pointerProgramShouldBeSafe() throws Exception {
    Map<String, String> props =
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS,
            "specification", SPEC,
            "parser.usePreprocessor", "true");

    TestResults results = CPATestRunner.run(props, "test/programs/pointer/pointer.c");
    results.assertIsSafe();
  }

  @Test
  public void nullPointerProgramShouldBeSafe() throws Exception {
    Map<String, String> props =
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS,
            "specification", SPEC,
            "parser.usePreprocessor", "true");

    TestResults results = CPATestRunner.run(props, "test/programs/pointer/null_pointer.c");
    results.assertIsSafe();
  }

  @Test
  public void aliasViaDoublePointerShouldBeUnsafe() throws Exception {
    Map<String, String> props =
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS,
            "specification", SPEC,
            "parser.usePreprocessor", "true");

    TestResults results =
        CPATestRunner.run(props, "test/programs/pointer/alias_via_double_pointer.c");
    results.assertIsUnsafe();
  }

  @Test
  public void simpleLoopShouldBeSafe() throws Exception {
    Map<String, String> props =
        ImmutableMap.of(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA, cpa.pointer.PointerAnalysisCPA",
            "specification",
            SPEC,
            "parser.usePreprocessor",
            "true");

    TestResults results = CPATestRunner.run(props, "test/programs/simple/loop1.c");
    results.assertIsSafe();
  }

  @Test
  public void simpleBitshiftShouldBeSafe() throws Exception {
    Map<String, String> props =
        ImmutableMap.of(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA, cpa.pointer.PointerAnalysisCPA",
            "specification",
            SPEC,
            "parser.usePreprocessor",
            "true");

    TestResults results = CPATestRunner.run(props, "test/programs/simple/simple_bitshift.c");
    results.assertIsSafe();
  }
}
