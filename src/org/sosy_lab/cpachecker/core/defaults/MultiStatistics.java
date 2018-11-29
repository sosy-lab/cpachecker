/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.defaults;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

public abstract class MultiStatistics implements Statistics {

  private final Collection<Statistics> subStats = new ArrayList<>();
  protected final LogManager logger;

  public MultiStatistics(LogManager pLogger) {
    logger = pLogger;
  }

  /** returns a modifiable collection of statistics. */
  public Collection<Statistics> getSubStatistics() {
    return subStats;
  }

  /** removes all available statistics. */
  public void resetSubStatistics() {
    subStats.clear();
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    for (Statistics s : subStats) {
      StatisticsUtils.printStatistics(s, out, logger, result, reached);
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    for (Statistics s : subStats) {
      StatisticsUtils.writeOutputFiles(s, logger, pResult, pReached);
    }
  }
}
