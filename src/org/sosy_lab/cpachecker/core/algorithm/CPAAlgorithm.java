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
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CPAAlgorithm implements Algorithm, StatisticsProvider {

  private final CPAStatistics               stats = new CPAStatistics();
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager                  logger;

  public CPAAlgorithm(ConfigurableProgramAnalysis cpa, LogManager logger) {
    this.cpa = cpa;
    this.logger = logger;
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    stats.totalTimer.start();

    Worker w = WorkerFactory.createNewInstance(reachedSet, cpa, logger, stats);
    w.work();

    stats.totalTimer.stop();
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
