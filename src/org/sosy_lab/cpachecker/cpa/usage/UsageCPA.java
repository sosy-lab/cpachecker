// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import java.nio.file.Path;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.lock.LockCPA;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.util.CPAs;

@Options
public class UsageCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  private final StopOperator stopOperator;
  private final MergeOperator mergeOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;
  private final UsageCPAStatistics statistics;
  private final CFA cfa;
  private final LogManager logger;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UsageCPA.class);
  }

  @Option(description = "A path to precision", name = "precision.path", secure = true)
  @FileOption(Type.OUTPUT_FILE)
  private Path outputFileName = Path.of("localsave");

  private UsageCPA(
      ConfigurableProgramAnalysis pCpa, CFA pCfa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    cfa = pCfa;
    stopOperator = new UsageStopOperator(pCpa.getStopOperator());
    mergeOperator = new UsageMergeOperator(pCpa.getMergeOperator());

    LockCPA lockCPA = CPAs.retrieveCPA(this, LockCPA.class);
    statistics =
        new UsageCPAStatistics(
            pConfig,
            pLogger,
            pCfa,
            lockCPA != null ? (LockTransferRelation) lockCPA.getTransferRelation() : null);
    precisionAdjustment = new UsagePrecisionAdjustment(pCpa.getPrecisionAdjustment());
    if (pCpa instanceof ConfigurableProgramAnalysisWithBAM) {
      Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithBAM) pCpa).getReducer();
      reducer = new UsageReducer(wrappedReducer);
    } else {
      reducer = null;
    }
    logger = pLogger;
    transferRelation =
        new UsageTransferRelation(pCpa.getTransferRelation(), pConfig, pLogger, statistics);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<UsageState>getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition p)
      throws InterruptedException {
    PresisionParser parser = new PresisionParser(cfa, logger);
    return UsagePrecision.create(
        getWrappedCpa().getInitialPrecision(pNode, p), parser.parse(outputFileName));
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
    super.collectStatistics(pStatsCollection);
  }

  public UsageCPAStatistics getStats() {
    return statistics;
  }

  public LogManager getLogger() {
    return logger;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return UsageState.createInitialState(getWrappedCpa().getInitialState(pNode, pPartition));
  }

  @Override
  public void setPartitioning(BlockPartitioning pPartitioning) {
    ConfigurableProgramAnalysis cpa = getWrappedCpa();
    assert cpa instanceof ConfigurableProgramAnalysisWithBAM;
    ((ConfigurableProgramAnalysisWithBAM) cpa).setPartitioning(pPartitioning);
  }
}
