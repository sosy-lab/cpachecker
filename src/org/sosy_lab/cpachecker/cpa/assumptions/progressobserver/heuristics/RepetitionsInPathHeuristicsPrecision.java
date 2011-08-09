/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import com.google.common.base.Function;
import com.google.common.base.Functions;

@Options(prefix="cpa.assumptions.progressobserver.heuristics.repetitionsInPathHeuristics")
public class RepetitionsInPathHeuristicsPrecision implements HeuristicPrecision {

  @Option(description = "threshold for heuristics of progressobserver")
  private int threshold = -1;
  private Function<? super CFAEdge, Integer> thresholdFunction;
  private int increaseThresholdBy;

  public RepetitionsInPathHeuristicsPrecision(Configuration config, LogManager logger)
              throws InvalidConfigurationException {
    config.inject(this);
    // there is no dead code in the next line,
    // because config.inject(this) can change the value of threshold
    thresholdFunction = Functions.constant((threshold <= 0) ? null : threshold);
  }

  public Function<? super CFAEdge, Integer> getThresholdFunction() {
    return thresholdFunction;
  }

  @Override
  public boolean adjustPrecision() {
    // set the initial threshold value
    if(threshold == -1){
      threshold = RepetitionsInPathHeuristicsData.getMaxNumberOfRepetitions() / 5;
      increaseThresholdBy = threshold;
    }
    else {
      threshold = threshold + increaseThresholdBy;
    }
    thresholdFunction = Functions.constant((threshold <= 0) ? null : threshold);
    return true;
  }

}
