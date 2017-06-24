/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import java.io.PrintStream;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix = "cpa.singleSuccessorCompactor")
public class SingleSuccessorCompactorCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysisWithBAM {

  @Option(description = "max length of a chain of states, -1 for infinity")
  private int maxChainLength = -1;

  /** if BAM is used, break chains of edges at block entry and exit. */
  @Nullable private BlockPartitioning partitioning = null;

  private final StatHist chainSizes =
      new StatHist("Avg length of skipped chains") {
        @Override
        public String toString() {
          // overriding, because printing all chain-sizes is not that interesting
          return String.format("%.2f", getAvg());
        }
      };

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SingleSuccessorCompactorCPA.class);
  }

  private final LogManager logger;

  public SingleSuccessorCompactorCPA(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration config)
      throws InvalidConfigurationException {
    super(pCpa);
    config.inject(this, SingleSuccessorCompactorCPA.class);
    logger = pLogger;
  }

  @Override
  public SingleSuccessorCompactorTransferRelation getTransferRelation() {
    return new SingleSuccessorCompactorTransferRelation(
        getWrappedCpa().getTransferRelation(), partitioning, chainSizes, maxChainLength);
  }

  @Override
  public void setPartitioning(BlockPartitioning pPartitioning) {
    if (getWrappedCpa() instanceof ConfigurableProgramAnalysisWithBAM) {
      ((ConfigurableProgramAnalysisWithBAM)getWrappedCpa()).setPartitioning(pPartitioning);
    }
    partitioning = pPartitioning;
  }

  LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            StatisticsUtils.write(pOut, 0, 50, chainSizes);
          }

          @Override
          public @Nullable String getName() {
            return "SSC-CPA";
          }
        });
  }
}
