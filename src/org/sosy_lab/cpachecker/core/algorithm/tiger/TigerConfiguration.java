/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;

import java.nio.file.Path;
import java.nio.file.Paths;

@Options(prefix = "tiger")
public class TigerConfiguration {

  public TigerConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Option(
      secure = true,
      name = "fqlQuery",
      description = "Coverage criterion given as an FQL query")
  String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(
      secure = true,
      name = "optimizeGoalAutomata",
      description = "Optimize the test goal automata")
  boolean optimizeGoalAutomata = true;

  @Option(secure = true, name = "printARGperGoal", description = "Print the ARG for each test goal")
  boolean dumpARGperPartition = false;

  @Option(
      secure = true,
      name = "useMarkingAutomata",
      description = "Compute the cross product of the goal automata?")
  boolean useMarkingAutomata = false;

  @Option(
      secure = true,
      name = "checkCoverage",
      description = "Checks whether a test case for one goal covers another test goal")
  boolean checkCoverage = true;

  @Option(
      secure = true,
      name = "usePowerset",
      description = "Construct the powerset of automata states.")
  boolean usePowerset = false;

  @Option(
      secure = true,
      name = "useComposite",
      description = "Handle all automata CPAs as one composite CPA.")
  boolean useComposite = false;

  @Option(
      secure = true,
      name = "testsuiteFile",
      description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(
      secure = true,
      name = "testcaseGeneartionTimesFile",
      description = "Filename for output of geneartion times of test cases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path testcaseGenerationTimesFile = Paths.get("generationTimes.csv");

  @Option(
      secure = true,
      description = "File for saving processed goal automata in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  PathTemplate dumpGoalAutomataTo = PathTemplate.ofFormatString("Automaton_%s.dot");

  @Option(
      secure = true,
      name = "useInfeasibilityPropagation",
      description = "Map information on infeasibility of one test goal to other test goals.")
  boolean useInfeasibilityPropagation = false;

  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT
  }

  @Option(
      secure = true,
      name = "timeoutStrategy",
      description = "How to proceed with timed-out goals if some time remains after processing all other goals.")
  TimeoutStrategy timeoutStrategy = TimeoutStrategy.SKIP_AFTER_TIMEOUT;

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu.increment",
      description = "Value for which timeout gets incremented if timed-out goals are re-processed.")
  int timeoutIncrement = 0;

  /*@Option(name = "globalCoverageCheckBeforeTimeout", description = "Perform a coverage check on all remaining coverage goals before the global time out happens.")
  boolean globalCoverageCheckBeforeTimeout = false;

  @Option(name = "timeForGlobalCoverageCheck", description = "Time budget for coverage check before global time out.")
  String timeForGlobalCoverageCheck = "0s";*/

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu",
      description = "Time limit per test goal in seconds (-1 for infinity).")
  long cpuTimelimitPerGoal = -1;

  @Option(
      secure = true,
      name = "useDynamicTimeouts",
      description = "Calculates the timeout for each partition depending on the time limit per goal * partition size.")
  boolean useDynamicTimeouts = false;

  @Option(
      secure = true,
      name = "inverseOrder",
      description = "Inverses the order of test goals each time a new round of re-processing of timed-out goals begins.")
  boolean inverseOrder = true;

  @Option(
      secure = true,
      name = "useOrder",
      description = "Enforce the original order each time a new round of re-processing of timed-out goals begins.")
  boolean useOrder = true;

  @Option(
      secure = true,
      name = "algorithmConfigurationFile",
      description = "Configuration file for internal cpa algorithm.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  Path algorithmConfigurationFile = Paths.get("config/includes/tiger-internal-algorithm-tgar.properties");

  @Option(
      secure = true,
      name = "tiger_with_presenceConditions",
      description = "Use Test Input Generator algorithm with an extension using the BDDCPA to model product line presence conditions.")
  boolean useTigerAlgorithm_with_pc = false;

  @Option(
      secure = true,
      name = "useBddForPresenceCondtion",
      description = "Use BDDCPA to model product line presence conditions.")
  boolean useBddForPresenceCondtion = false;

  @Option(
      secure = true,
      name = "useOmegaLabel",
      description = "Inserts the omega label at the end of each test goal automaton to enforce the tiger algorithm to generate only test cases from counterexamples that reach the end of the program.")
  boolean useOmegaLabel = true;

  @Option(
      secure = true,
      name = "numberOfTestGoalsPerRun",
      description = "The number of test goals processed per CPAchecker run (0: all test goals in one run).")
  int numberOfTestGoalsPerRun = 1;

  @Option(
      secure = true,
      name = "allCoveredGoalsPerTestCase",
      description = "Returns all test goals covered by a test case.")
  boolean allCoveredGoalsPerTestCase = false;

  @Option(
      secure = true,
      name = "inputInterface",
      description = "List of input variables: v1,v2,v3...")
  String inputInterface = "";

  @Option(
      secure = true,
      name = "outputInterface",
      description = "List of output variables: v1,v2,v3...")
  String outputInterface = "";

  @Option(
      secure = true,
      name = "printLabels",
      description = "Prints labels reached with the error path of a test case.")
  boolean printLabels = false;

  @Option(
      secure = true,
      name = "printPathFormulasPerGoal",
      description = "Writes all target state path formulas for a goal in a file.")
  boolean printPathFormulasPerGoal = false;

  @Option(
      secure = true,
      name = "pathFormulasFile",
      description = "Filename for output of path formulas")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path pathFormulaFile = Paths.get("pathFormulas.txt");
  
}
