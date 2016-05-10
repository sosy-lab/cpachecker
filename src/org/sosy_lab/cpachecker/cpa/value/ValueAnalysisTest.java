/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.util.Map;

public class ValueAnalysisTest {
  // Specification Tests
  @Test
  public void ignoreVariablesTest1() throws Exception {
    // check whether a variable can be ignored (this will lead to a spurious counterexample be found)

    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
            "specification", "config/specification/default.spc",
            "ValueAnalysisCPA.precision.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
            "cpa.composite.precAdjust", "COMPONENT");

      TestResults results = CPATestRunner.run(
          prop,
          "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
      results.assertIsUnsafe();
  }
  @Test
  public void ignoreVariablesTest2() throws Exception {
    // check whether the counterexample is indeed not found if the variable is not ignored

    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
        "specification",     "config/specification/default.spc",
        "ValueAnalysisCPA.precision.variableBlacklist", "somethingElse"
      );

      TestResults results = CPATestRunner.run(
          prop,
          "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
      results.assertIsSafe();
  }
}
