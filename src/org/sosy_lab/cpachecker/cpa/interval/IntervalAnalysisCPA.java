// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options(prefix = "cpa.interval")
public class IntervalAnalysisCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider, ProofCheckerCPA {

  /**
   * This method returns a CPAfactory for the interval analysis CPA.
   *
   * @return the CPAfactory for the interval analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(IntervalAnalysisCPA.class);
  }

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which type of merge operator to use for IntervalAnalysisCPA")
  /** the merge type of the interval analysis */
  private String mergeType = "SEP";

  @Option(
      secure = true,
      description =
          "decides whether one (false) or two (true) successors should be created "
              + "when an inequality-check is encountered")
  private boolean splitIntervals = false;

  @Option(
      secure = true,
      description =
          "at most that many intervals will be tracked per variable, -1 if number not restricted")
  private int threshold = -1;

  private final StateToFormulaWriter writer;
  private final LogManager logger;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   */
  private IntervalAnalysisCPA(
      Configuration config, LogManager pLogger, ShutdownNotifier shutdownNotifier, CFA cfa)
      throws InvalidConfigurationException {
    super("irrelevant", "sep", DelegateAbstractDomain.<IntervalAnalysisState>getInstance(), null);
    config.inject(this);
    writer = new StateToFormulaWriter(config, pLogger, shutdownNotifier, cfa);
    logger = pLogger;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public Reducer getReducer() {
    return new IntervalAnalysisReducer();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new IntervalAnalysisState();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new IntervalAnalysisTransferRelation(splitIntervals, threshold, logger);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
