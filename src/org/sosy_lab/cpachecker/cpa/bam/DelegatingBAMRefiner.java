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
package org.sosy_lab.cpachecker.cpa.bam;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.base.Preconditions;

public class DelegatingBAMRefiner extends AbstractBAMBasedRefiner {

  private final List<AbstractBAMBasedRefiner> refiners;

  private final List<StatCounter> totalRefinementsSelected;
  private final List<StatCounter> totalRefinementsFinished;

  public DelegatingBAMRefiner(
      final ConfigurableProgramAnalysis pCpa, final AbstractBAMBasedRefiner... pRefiners)
      throws InvalidConfigurationException {
    super(pCpa);

    refiners = Arrays.asList(pRefiners);

    totalRefinementsSelected = new ArrayList<>();
    totalRefinementsFinished = new ArrayList<>();

    for (int i = 0; i < refiners.size(); i++) {
      totalRefinementsSelected.add(new StatCounter("Number of selected refinement"));
      totalRefinementsFinished.add(new StatCounter("Number of finished refinement"));
    }

    assert refiners.size() == totalRefinementsSelected.size();
    assert refiners.size() == totalRefinementsFinished.size();
  }

  @Override
  protected CounterexampleInfo performRefinement0(final ARGReachedSet reached, ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    CounterexampleInfo cex = null;

    // TODO here, we could sort the refiners to get a better result,
    //      like the score-based sorting from ValueAnalysisDelegatingRefiner

    for (int i = 0; i < refiners.size(); i++) {
      totalRefinementsSelected.get(i).inc();
      cex = refiners.get(i).performRefinement(reached, pErrorPath);
      if (cex.isSpurious()) {
        totalRefinementsFinished.get(i).inc();
        break;
      }
    }

    return Preconditions.checkNotNull(cex);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return DelegatingBAMRefiner.class.getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);

        for (int i = 0; i < refiners.size(); i++) {
          pOut.println(String.format("Analysis %d (%s):", i + 1, refiners.get(i).getClass().getSimpleName()));
          writer.beginLevel().put(totalRefinementsSelected.get(i));
          writer.beginLevel().put(totalRefinementsFinished.get(i));
          writer.spacer();
        }
      }
    });

    for (AbstractBAMBasedRefiner refiner : refiners) {
      refiner.collectStatistics(pStatsCollection);
    }
  }
}
