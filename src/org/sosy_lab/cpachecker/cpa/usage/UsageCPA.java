/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
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
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.cpa.lock.LockCPA;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.CPAs;

@Options
public class UsageCPA extends AbstractSingleWrapperCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  private final UsageDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;
  private final UsageCPAStatistics statistics;
  //Do not remove container from CPA - we clean all states while refinement
  private UsageContainer container;
  private UsagePrecision precision;
  private final CFA cfa;
  private final LogManager logger;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UsageCPA.class);
  }

  @Option(description="A path to precision", name="precision.path")
  @FileOption(Type.OUTPUT_FILE)
  private Path outputFileName = Paths.get("localsave");

  private UsageCPA(ConfigurableProgramAnalysis pCpa, CFA pCfa, LogManager pLogger,
      Configuration pConfig) throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    this.cfa = pCfa;
    this.abstractDomain = new UsageDomain(pCpa.getAbstractDomain());
    this.mergeOperator = MergeSepOperator.getInstance();
    this.stopOperator = new StopSepOperator(abstractDomain);

    LockCPA LockCPA = (CPAs.retrieveCPA(this, LockCPA.class));
    this.statistics = new UsageCPAStatistics(pConfig, pLogger,
        LockCPA != null ? (LockTransferRelation) LockCPA.getTransferRelation() : null);
    this.container = new UsageContainer(pConfig, pLogger);
    this.precisionAdjustment = new UsagePrecisionAdjustment(pCpa.getPrecisionAdjustment());
    if (pCpa instanceof ConfigurableProgramAnalysisWithBAM) {
      Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithBAM)pCpa).getReducer();
      if (wrappedReducer != null) {
        reducer = new UsageReducer(wrappedReducer, LockCPA.getReducer());
      } else {
        reducer = null;
      }
    } else {
      reducer = null;
    }
    logger = pLogger;
    this.transferRelation = new UsageTransferRelation(pCpa.getTransferRelation(), pConfig, pLogger, statistics
        , (CallstackTransferRelation) (CPAs.retrieveCPA(this, CallstackCPA.class)).getTransferRelation());

  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition p) throws InterruptedException {
    precision = new UsagePrecision(this.getWrappedCpa().getInitialPrecision(pNode, p));
    PresisionParser parser = new PresisionParser(outputFileName.toString(), cfa, logger);
    parser.parse(precision);
    return precision;
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    return new UsageState(getWrappedCpa().getInitialState(pNode, pPartition), container);
  }

  @Override
  public void setPartitioning(BlockPartitioning pPartitioning) {
    ConfigurableProgramAnalysis cpa = getWrappedCpa();
    assert cpa instanceof ConfigurableProgramAnalysisWithBAM;
    ((ConfigurableProgramAnalysisWithBAM) cpa).setPartitioning(pPartitioning);
  }
}
