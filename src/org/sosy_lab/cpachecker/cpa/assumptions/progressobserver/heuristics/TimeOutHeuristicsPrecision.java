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

@Options(prefix="cpa.assumptions.progressobserver.heuristics.timeOutHeuristics")
public class TimeOutHeuristicsPrecision implements HeuristicPrecision {

  @Option(description = "threshold for timeout heuristics")
  private int threshold = -1;

  @Option(description = "how many times to adjust timeout threshold?")
  private int adjustmentLimit = -1;

  // how many times did we adjusted?
  private int adjustmentIdx = 0;

  private final TimeOutHeuristics timeOutHeuristics;

  public TimeOutHeuristicsPrecision(TimeOutHeuristics pTimeOutHeuristics, Configuration pConfig, LogManager pLogger)
  throws InvalidConfigurationException{
    pConfig.inject(this);
    timeOutHeuristics = pTimeOutHeuristics;
  }

  public int getThreshold() {
    return threshold;
  }

  @Override
  public boolean adjustPrecision() {
    if(adjustmentLimit != -1 && adjustmentIdx > adjustmentLimit){
      return false;
    }
    timeOutHeuristics.resetStartTime();
    adjustmentLimit ++;
    return true;
  }

}
