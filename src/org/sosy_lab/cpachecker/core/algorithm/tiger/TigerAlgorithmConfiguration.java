/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerBaseAlgorithm.TimeoutStrategy;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;


@Options(prefix = "tiger")
public class TigerAlgorithmConfiguration {

  public enum CoverageCheck {
    NONE,
    SINGLE,
    ALL
  }

  public enum GoalReduction {
    COMPLEX,
    SIMPLE,
    NONE
  }

  public TigerAlgorithmConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  // TODO
  // @Option(secure = true, name = "reusePredicates", description = "Reuse predicates across
  // modifications of an ARG.")
  // private boolean reusePredicates = true;

  // @Option(
  // secure = true,
  // name = "useInfeasibilityPropagation",
  // description = "Map information on infeasibility of one test goal to other test goals.")
  // private boolean useInfeasibilityPropagation = false;

  @Option(
      secure = true,
      name = "fqlQuery",
      description = "Coverage criterion given as an FQL query")
  private String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(
      secure = true,
      name = "optimizeGoalAutomata",
      description = "Optimize the test goal automata")
  private boolean optimizeGoalAutomata = true;

  @Option(
    secure = true,
    name = "limitsPerGoal.time.cpu",
    description = "Time limit per test goal in seconds (-1 for infinity).")
  private long cpuTimelimitPerGoal = -1;

  @Option(
      secure = true,
      name = "algorithmConfigurationFile",
      description = "Configuration file for internal cpa algorithm.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path algorithmConfigurationFile = Paths.get("config/tiger-internal-algorithm.properties");

//  @Option(secure = true, name = "reuseARG", description = "Reuse ARG across test goals")
//  private boolean reuseARG = true;

  @Option(
      secure = true,
      name = "testsuiteFile",
      description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(
      secure = true,
      name = "timeoutStrategy",
      description = "How to proceed with timed-out goals if some time remains after processing all other goals.")
  private TimeoutStrategy timeoutStrategy = TimeoutStrategy.SKIP_AFTER_TIMEOUT;

  @Option(
      secure = true,
      name = "limitsPerGoal.time.cpu.increment",
      description = "Value for which timeout gets incremented if timed-out goals are re-processed.")
  private int timeoutIncrement = 0;

  @Option(
      secure = true,
      name = "inverseOrder",
      description = "Inverses the order of test goals each time a new round of re-processing of timed-out goals begins.")
  private boolean inverseOrder = true;

  @Option(
      secure = true,
      name = "useOrder",
      description = "Enforce the original order each time a new round of re-processing of timed-out goals begins.")
  private boolean useOrder = true;

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
    name = "useSingleFeatureGoalCoverage",
    description = "Only need one Testcase with a valid feature configuration for each goal")
  private boolean useSingleFeatureGoalCoverage = false;

  @Option(secure = true, name = "coverageCheck", description = "")
  private CoverageCheck coverageCheck = CoverageCheck.SINGLE;

  @Option(
    secure = true,
    name = "validProductMethodName",
    description = "Name of the method to validate the feature model")
  private String validProductMethodName = "validProduct";

  @Option(
    secure = true,
    name = "featureVariablePrefix",
    description = "Prefix of the feature variables")
  private String featureVariablePrefix = "__SELECTED_FEATURE_";

  @Option(
    secure = true,
    name = "removeFeaturePrefix",
    description = "removes the feature prefix from presence conditions in the testsuite")
  private boolean removeFeaturePrefix = true;

  @Option(
    secure = true,
    name = "useOmegaLabel",
    description = "determines, whether the ARG will be explored until the return statement of the function")
  private boolean useOmegaLabel = true;

  @Option(
    secure = true,
    name = "useTestCompOutput",
    description = "uses output specification of testcomp")
  private boolean useTestCompOutput = false;

  @Option(secure = true, name = "timeout", description = "walltime for the algorithm")
  private int timeout = 900;

  @Option(
    secure = true,
    name = "goalReduction",
    description = "")
  private GoalReduction goalReduction = GoalReduction.COMPLEX;

  public boolean shouldUseTestCompOutput() {
    return useTestCompOutput;
  }

  public String getFeatureVariablePrefix() {
    return featureVariablePrefix;
  }

  public String getValidProductMethodName() {
    return validProductMethodName;
  }

  public CoverageCheck getCoverageCheck() {
    return coverageCheck;
  }

  public boolean shouldUseSingleFeatureGoalCoverage() {
    return useSingleFeatureGoalCoverage;
  }

  public String getOutputInterface() {
    return outputInterface;
  }

  public String getInputInterface() {
    return inputInterface;
  }

  public boolean useOrder() {
    return useOrder;
  }

  public boolean useInverseOrder() {
    return inverseOrder;
  }

  public int getTimeoutIncrement() {
    return timeoutIncrement;
  }

  public TimeoutStrategy getTimeoutStrategy() {
    return timeoutStrategy;
  }

   public Path getTestsuiteFile(){
    return testsuiteFile;
   }

   public Path getAlgorithmConfigurationFile(){
    return algorithmConfigurationFile;
   }

  public long getCpuTimelimitPerGoal() {
    return cpuTimelimitPerGoal;
  }
   public boolean shouldOptimizeGoalAutomata(){
    return optimizeGoalAutomata;
   }
   public String getFqlQuery(){
    return fqlQuery;
   }

  public void increaseCpuTimelimitPerGoal(int pTimeoutIncrement) {
    cpuTimelimitPerGoal += pTimeoutIncrement;

  }

  public boolean shouldRemoveFeatureVariablePrefix() {
    return removeFeaturePrefix;
  }

  public boolean shouldUseOmegaLabel() {
    return useOmegaLabel;
  }

  public void setFQLQuery(String pString) {
    this.fqlQuery = pString;
  }

  public GoalReduction getGoalReduction() {
    return goalReduction;
  }

  public int getTimeout() {
    return timeout;
  }

}
