/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopstats;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;

@Options(prefix="cpa.loopstats")
public class LoopstatsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  protected final LoopStructure loopStructure;
  protected final LoopstatsStatistics stats;
  protected final LogManager logger;

  protected final PrecisionAdjustment precop;
  protected final TransferRelation transfer;
  protected final MergeOperator mergeop;
  protected final StopOperator stopop;
  protected final AbstractDomain domain;

  public static CPAFactory factory() {
    return new LoopstatsCPAFactory();
  }

  public LoopstatsCPA(Configuration pConfig, CFA pCfa, LogManager pLogger)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);
    logger = pLogger;

    if (!pCfa.getLoopStructure().isPresent()) {
      throw new CPAException("LoopstatsCPA cannot work without loop-structure information in CFA.");
    }

    loopStructure = pCfa.getLoopStructure().get();

    transfer = new LoopstatsTransferRelation(loopStructure);
    mergeop = MergeSepOperator.getInstance();
    domain = new FlatLatticeDomain();
    stopop = new StopAlwaysOperator();
    precop = StaticPrecisionAdjustment.getInstance();

    stats = new LoopstatsStatistics(pConfig);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new LoopstatsState(stats);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeop;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precop;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

}