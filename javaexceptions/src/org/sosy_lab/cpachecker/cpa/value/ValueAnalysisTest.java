// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class ValueAnalysisTest {
  // Specification Tests
  @Test
  public void ignoreVariablesTest1() throws Exception {
    // check whether a variable can be ignored (this will lead to a spurious counterexample be
    // found)

    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
            "specification", "config/specification/default.spc",
            "ValueAnalysisCPA.precision.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
            "cpa.composite.precAdjust", "COMPONENT");

    TestResults results =
        CPATestRunner.run(prop, "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
    results.assertIsUnsafe();
  }

  @Test
  public void ignoreVariablesTest2() throws Exception {
    // check whether the counterexample is indeed not found if the variable is not ignored

    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
            "specification", "config/specification/default.spc",
            "ValueAnalysisCPA.precision.variableBlacklist", "somethingElse");

    TestResults results =
        CPATestRunner.run(prop, "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
    results.assertIsSafe();
  }
}
