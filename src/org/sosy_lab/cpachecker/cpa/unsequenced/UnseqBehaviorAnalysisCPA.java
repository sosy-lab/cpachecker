// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options(prefix = "cpa.unseqbehavior")
public class UnseqBehaviorAnalysisCPA extends AbstractCPA implements StatisticsProvider {
  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for UnseqBehaviorAnalysisCPA ")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER"},
      description = "which stop operator to use for UnseqBehaviorAnalysisCPA")
  private String stopType = "SEP";

  private final StateToFormulaWriter writer;
  private final LogManager logger;

  private UnseqBehaviorAnalysisCPA(
      Configuration config, LogManager pLogger, ShutdownNotifier shutdownNotifier, CFA cfa)
      throws InvalidConfigurationException {
    super("sep", "sep", DelegateAbstractDomain.<IntervalAnalysisState>getInstance(), null);
    config.inject(this);
    writer = new StateToFormulaWriter(config, pLogger, shutdownNotifier, cfa);
    logger = pLogger;
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UnseqBehaviorAnalysisCPA.class);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new UnseqBehaviorAnalysisTransferRelation(logger);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new UnseqBehaviorAnalysisState();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
