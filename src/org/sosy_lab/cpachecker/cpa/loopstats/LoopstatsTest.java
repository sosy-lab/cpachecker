/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopstats;

import java.util.Map;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.sosy_lab.cpachecker.util.test.TestRunStatisticsParser;

import com.google.common.collect.ImmutableMap;

public class LoopstatsTest {

  @Test
  public void testEncodingOfLdvRule118_Safe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/ldv_118_test.c";

    TestResults results = runWithSetup(specFile, programFile);

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Number of times merged").isAtLeast(2);
    stat.assertThatNumber("Number of refinements").isAtMost(3);
    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(2);
  }

  @Test
  public void testEncodingOfLdvRule118_Unsafe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/ldv_118_test_false.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Number of times merged").isAtLeast(0);
    stat.assertThatNumber("Number of successful refinements").isAtMost(3);
    stat.assertThatNumber("Max states per location").isAtMost(2);
  }

  @Test
  public void testWhileUnrollingTrue() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_while_true.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 10");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    stat.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    stat.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testWhileUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_while_false.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 10");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller due to expected result FALSE (if the analysis terminates
    // before unrolling all loops)
    stat.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

//  @Test
//  public void testDoWhileUnrollingTrue() throws Exception {
//    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
//    final String programFile = "test/config/automata/encode/loop_unroll_do_while_true.c";
//
//    TestResults results = runWithSetup(specFile, programFile);
//
//    results.assertIsSafe();
//
//    TestRunStatisticsParser stat = new TestRunStatisticsParser();
//    results.getCheckerResult().printStatistics(stat.getPrintStream());
//
//    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
//    stat.assertThatString("Loop with max. unrollings").contains("line 10");
//    stat.assertThatNumber("Number of loops").isEqualTo(2);
//    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
//    stat.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(0);
//    stat.assertThatNumber("Max. nesting of loops").isEqualTo(1);
//  }

  @Test
  public void testDoWhileUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_do_while_false.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 10");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    stat.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  @Test
  public void testForUnrollingTrue() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_for_true.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 10");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    stat.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    stat.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testForUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_for_false.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 10");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    stat.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  @Test
  public void testGoToUnrollingTrue() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_goto_true.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 13");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    stat.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    //stat.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testGoToUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_goto_false.c";

    TestResults results = runWithSetup(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    stat.assertThatString("Loop with max. unrollings").contains("line 13");
    stat.assertThatNumber("Number of loops").isEqualTo(2);
    stat.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the
    // analysis terminates before unrolling all loops)
    stat.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  private TestResults runWithSetup(final String pSpecFile, final String pProgramFile)
      throws Exception {
    Map<String, String> prop = ImmutableMap.<String, String>builder()
        .put("specification", pSpecFile)
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("cfa.useMultiEdges", "false")
        .put("automata.properties.granularity", "BASENAME")
        .put("analysis.checkCounterexamples", "false")
        .put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, "
            + "cpa.loopstats.LoopstatsCPA, cpa.functionpointer.FunctionPointerCPA, "
            + "cpa.predicate.PredicateCPA, cpa.coverage.CoverageCPA")
        .build();

    Configuration cfg = TestDataTools.configurationForTest()
        .loadFromFile("config/predicateAnalysis-PredAbsRefiner-ABEl.properties")
        .setOptions(prop)
        .build();

    return CPATestRunner.run(cfg, pProgramFile, false);
  }

}
