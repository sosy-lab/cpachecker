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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class UsageAnalysisTest {
  // Specification Tests

  @Test
  public void usageInLoopedProgramTest1() throws Exception {
    // checks, if the analysis for usage of array elements detects, that all array elements are
    // used in the example program

    Map<String, String> props = getProps();

    TestResults results = CPATestRunner.run(props, "test/programs/simple/arrays/array_all-used.c");
    results.assertIsSafe();
  }

  @Test
  public void usageInLoopedProgramTest2() throws Exception {
    // checks, if the analysis for usage of array elements detects, that not all array elements are
    // used in the example program

    Map<String, String> props = getProps();
    TestResults results =
        CPATestRunner.run(props, "test/programs/simple/arrays/array_all-used-except-last.c");
    results.assertIsUnsafe();
  }

  @Test
  public void usageInLoopedProgramTest3() throws Exception {
    // checks, if the analysis for usage of array elements detects, that only all elements up to a
    // constant number (5) are used
    Map<String, String> props = getProps();

    TestResults results =
        CPATestRunner
            .run(props, "test/programs/simple/arrays/array_branch-in-loop-all-used-smaller-5.c");
    results.assertIsUnsafe();
  }

  private Map<String, String> getProps() {
    Map<String, String> prop =
        ImmutableMap.of(
            "cpa",
            "cpa.arg.ARGCPA",
            "ARGCPA.cpa",
            "cpa.composite.CompositeCPA",
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.arraySegmentation.usageAnalysis.ExtendedCLUAnalysisCPA, cpa.arraySegmentation.formula.FormulaCPA",
            "specification",
            "config/specification/usage.spc",
            "cpa.arrayContentCPA.merge",
            "JOIN");
    Map<String, String> prop2 =
        ImmutableMap.of(
            "cpa.arrayContentCPA.stop",
            "SEP",
            "cpa.arrayContentCPA.arrayName",
            "a",
            "solver.solver",
            "Z3",
            "analysis.stopAfterError",
            "false",
            "analysis.traversal.order",
            "BFS");
    Map<String, String> props = new HashMap<>();
    props.putAll(prop);
    props.putAll(prop2);
    return props;
  }

}
