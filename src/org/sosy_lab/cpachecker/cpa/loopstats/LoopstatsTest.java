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

    TestResults results = runWithPredicateAnalysis(specFile, programFile);

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

    TestResults results = runWithPredicateAnalysis(specFile, programFile);

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

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsSafe();
    resultsBMC.assertIsSafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    statPA.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    statPA.assertThatNumber("Max. nesting of loops").isEqualTo(1);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    statBMC.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testWhileUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_while_false.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsUnsafe();
    resultsBMC.assertIsUnsafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    statPA.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the
    // analysis terminates before unrolling all loops)
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

//  @Test
//  public void testDoWhileUnrollingTrue() throws Exception {
//    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
//    final String programFile = "test/config/automata/encode/loop_unroll_do_while_true.c";
//
//    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
//    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);
//
//    resultsPA.assertIsSafe();
//    resultsBMC.assertIsSafe();
//
//    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
//    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
//    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
//    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());
//
//    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
//    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
//    statPA.assertThatNumber("Number of loops").isEqualTo(2);
//    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
//    statPA.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
//    statPA.assertThatNumber("Max. nesting of loops").isEqualTo(1);
//
//    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
//    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
//    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
//    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
//    statBMC.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
//    statBMC.assertThatNumber("Max. nesting of loops").isEqualTo(1);
//  }

  @Test
  public void testDoWhileUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_do_while_false.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsUnsafe();
    resultsBMC.assertIsUnsafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    statPA.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the
    // analysis terminates before unrolling all loops)
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  @Test
  public void testForUnrollingTrue() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_for_true.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsSafe();
    resultsBMC.assertIsSafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    statPA.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    statPA.assertThatNumber("Max. nesting of loops").isEqualTo(1);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    statBMC.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testForUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_for_false.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsUnsafe();
    resultsBMC.assertIsUnsafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 10");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    statPA.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 10");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the
    // analysis terminates before unrolling all loops)
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  @Test
  public void testGoToUnrollingTrue() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_goto_true.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsSafe();
    resultsBMC.assertIsSafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 13");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    statPA.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    //statPA.assertThatNumber("Max. nesting of loops").isEqualTo(1);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 13");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
    //statBMC.assertThatNumber("Max. nesting of loops").isEqualTo(1);
  }

  @Test
  public void testGoToUnrollingFalse() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_goto_false.c";

    TestResults resultsPA = runWithPredicateAnalysis(specFile, programFile);
    TestResults resultsBMC = runWithBMC(specFile, programFile, 100);

    resultsPA.assertIsUnsafe();
    resultsBMC.assertIsUnsafe();

    TestRunStatisticsParser statPA = new TestRunStatisticsParser();
    resultsPA.getCheckerResult().printStatistics(statPA.getPrintStream());
    TestRunStatisticsParser statBMC = new TestRunStatisticsParser();
    resultsBMC.getCheckerResult().printStatistics(statBMC.getPrintStream());

    statPA.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statPA.assertThatString("Loop with max. unrollings").contains("line 13");
    statPA.assertThatNumber("Number of loops").isEqualTo(2);
    statPA.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the analysis
    // terminates before unrolling all loops)
    statPA.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);

    statBMC.assertThatNumber("Max. unrollings of a loop").isAtMost(14);
    statBMC.assertThatString("Loop with max. unrollings").contains("line 13");
    statBMC.assertThatNumber("Number of loops").isEqualTo(2);
    statBMC.assertThatNumber("Number of loops entered").isAtLeast(2);
    // Actual results might be smaller than expected due to expected result FALSE (if the
    // analysis terminates before unrolling all loops)
    statBMC.assertThatNumber("Max. completed unrollings of a loop").isAtMost(13);
  }

  @Test
  public void testPartialUnrolling() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/loop_unroll_while_true.c";

    TestResults results1 = runWithBMC(specFile, programFile, 1);
    TestResults results2 = runWithBMC(specFile, programFile, 2);
    TestResults results10 = runWithBMC(specFile, programFile, 10);
    TestResults results15 = runWithBMC(specFile, programFile, 15);

    TestRunStatisticsParser stat1 = new TestRunStatisticsParser();
    TestRunStatisticsParser stat2 = new TestRunStatisticsParser();
    TestRunStatisticsParser stat10 = new TestRunStatisticsParser();
    TestRunStatisticsParser stat15 = new TestRunStatisticsParser();
    results1.getCheckerResult().printStatistics(stat1.getPrintStream());
    results2.getCheckerResult().printStatistics(stat2.getPrintStream());
    results10.getCheckerResult().printStatistics(stat10.getPrintStream());
    results15.getCheckerResult().printStatistics(stat15.getPrintStream());

    stat1.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(1);
    stat2.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(2);
    stat10.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(10);
    stat15.assertThatNumber("Max. completed unrollings of a loop").isEqualTo(13);
  }

  private TestResults runWithPredicateAnalysis(final String pSpecFile, final String pProgramFile)
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

  private TestResults runWithBMC(final String pSpecFile,
      final String pProgramFile,
      final int pLoopIterationBound)
      throws Exception{
    Map<String, String> prop = ImmutableMap.<String, String>builder()
        .put("specification", pSpecFile)
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("cfa.useMultiEdges", "false")
        .put("automata.properties.granularity", "BASENAME")
        .put("analysis.checkCounterexamples", "false")
        .put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, "
            + "cpa.loopstats.LoopstatsCPA, cpa.functionpointer.FunctionPointerCPA, "
            + "cpa.predicate.PredicateCPA, cpa.assumptions.storage.AssumptionStorageCPA, "
            + "cpa.bounds.BoundsCPA, cpa.value.ValueAnalysisCPA")
        .put("cpa.bounds.maxLoopIterations", String.valueOf(pLoopIterationBound))
        .build();

    Configuration cfg = TestDataTools.configurationForTest()
        .loadFromFile("config/bmc.properties")
        .setOptions(prop)
        .build();

    return CPATestRunner.run(cfg, pProgramFile, false);
  }

}
