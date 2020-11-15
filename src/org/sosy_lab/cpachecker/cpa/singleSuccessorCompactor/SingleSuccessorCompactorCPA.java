// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import java.io.PrintStream;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
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

  private SingleSuccessorCompactorCPA(
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
    ((ConfigurableProgramAnalysisWithBAM) getWrappedCpa()).setPartitioning(pPartitioning);
    partitioning = pPartitioning;
  }

  @Override
  public Reducer getReducer() throws InvalidConfigurationException {
    return ((ConfigurableProgramAnalysisWithBAM) getWrappedCpa()).getReducer();
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
