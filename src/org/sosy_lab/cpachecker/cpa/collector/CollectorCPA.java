/*
 * CPAchecker is a tool for configurable software verification.
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.collector;


import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoin;
import org.sosy_lab.cpachecker.cpa.arg.ARGPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGStopSep;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options
public class CollectorCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CollectorCPA.class);
  }


  private final MergeOperator merge;
  private final LogManager logger;
  private final ARGStatistics stats;
  private final CollectorStatistics statistics;
  private final StateToFormulaWriter writer;
  private final ShutdownNotifier shutdownNotifier;
  private StopOperator stopOperator;

  private CollectorCPA(
      ConfigurableProgramAnalysis cpa,
      LogManager clogger,
      Configuration config,
      Specification pSpecification,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException {
    super(cpa);
    this.logger = clogger;
    this.shutdownNotifier = pShutdownNotifier;


      statistics = new CollectorStatistics(this, config, logger);
      writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);


    if ( cpa instanceof ARGCPA) {

      ARGMergeJoin wrappedMergeOperator = (ARGMergeJoin) cpa.getMergeOperator();
      merge = new CollectorMergeJoin(wrappedMergeOperator, cpa.getAbstractDomain(), config, logger);
      stats = new ARGStatistics(config, logger, this, pSpecification, cfa);
    } else {
      throw new InvalidConfigurationException("This is not a valid CPA");
    }
  }
  @Override
  public MergeOperator getMergeOperator() { return merge; }


  @Override
  public TransferRelation getTransferRelation() {

    TransferRelation supertr = super.getWrappedCpa().getTransferRelation();
    if (!(supertr instanceof ARGTransferRelation)){
      throw new AssertionError("Transfer relation not ARG!");
    }
    return new CollectorTransferRelation(supertr, logger);
  }


  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {

    AbstractState initialState = super.getInitialState(pNode, pPartition);
    CollectorState is = new CollectorState(initialState, null, logger);
    return is;
  }


@Override
public void collectStatistics(Collection<Statistics> pStatsCollection) {
  pStatsCollection.add(stats);
  pStatsCollection.add(statistics);
  writer.collectStatistics(pStatsCollection);

}


  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    PrecisionAdjustment wrappedPrecSUPER = super.getPrecisionAdjustment();
    if (!(wrappedPrecSUPER instanceof ARGPrecisionAdjustment)){
      throw new AssertionError("PrecisionAdjustment not ARG!");
    }
     return new CollectorPrecisionAdjustment(wrappedPrecSUPER,logger);
  }

  @Override
  public StopOperator getStopOperator() {
    stopOperator = super.getStopOperator();
    if (!(stopOperator instanceof ARGStopSep)){
      throw new AssertionError("StopOperator not ARG!");
    }
    return new CollectorStop(stopOperator, logger);
  }


  protected LogManager getLogger() {
    return logger;
  }
}
