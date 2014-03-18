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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;

public class BDDCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BDDCPA.class);
  }

  private final NamedRegionManager manager;
  private final BitvectorManager bvmgr;
  private final PredicateManager predmgr;
  private final BDDDomain abstractDomain;
  private final BDDPrecision precision;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final BDDTransferRelation transferRelation;
  private final BDDReducer reducer;

  private BDDCPA(CFA cfa, Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    BDDRegionManager rmgr = BDDRegionManager.getInstance(config, logger);
    manager = new NamedRegionManager(rmgr);
    bvmgr = new BitvectorManager(config, rmgr);
    abstractDomain = new BDDDomain();
    precision = new BDDPrecision(config, cfa.getVarClassification());
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopSepOperator(abstractDomain);
    predmgr = new PredicateManager(config, manager, precision, cfa);
    transferRelation = new BDDTransferRelation(manager, bvmgr, predmgr, logger, config, cfa);
    reducer = new BDDReducer(manager, predmgr);
  }

  public NamedRegionManager getManager() {
    return manager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return new BDDState(manager, bvmgr, manager.makeTrue());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        transferRelation.printStatistics(out);
      }

      @Override
      public String getName() {
        return "BDDCPA";
      }
    });
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }
}