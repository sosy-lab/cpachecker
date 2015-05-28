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
package org.sosy_lab.cpachecker.cpa.wp;

import java.util.Collection;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;


@Options
public class WpCPA implements ConfigurableProgramAnalysis, StatisticsProvider, AutoCloseable {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(WpCPA.class);
  }

  /*
   * References to internal objects of the general CPAchecker framework.
   */
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  /*
   * Fields that describe components of formal CPA.
   */
  private final TransferRelation transfer;
  private final PrecisionAdjustment prec;
  private final WpAbstractDomain domain;
  private final MergeOperator merge;
  private final StopOperator stop;

  /*
   * Fields that are specific to this program analysis.
   */
  private final Solver solver;
  private final RegionManager regionManager;
  private final AbstractionManager abstractionManager;
  @SuppressWarnings("unused")
  private final PredicateAbstractionManager predicateManager;
  private final PathFormulaManager pathFormulaManager;


  protected WpCPA(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier
    ) throws InvalidConfigurationException, CPAException {

    //
    //
    // Inject the concrete configuration options
    //    for this verification run.
    pConfig.inject(this, WpCPA.class);

    //
    //
    // Store a reference to certain objects of the CPAchecker framework.
    config = pConfig;
    logger = pLogger;
    cfa = pCfa;

    //
    //
    // Create specific instances that are needed to run this analysis.
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    pathFormulaManager = new PathFormulaManagerImpl(formulaManager, config, logger, pShutdownNotifier, cfa, AnalysisDirection.BACKWARD);
    // TODO: We might use a caching path formula manager
    //    pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);


    regionManager = new BDDManagerFactory(config, logger).createRegionManager();
    // TODO: There are different implementations of the region manager.
    //    Evaluate the applicability of them.

    abstractionManager = new AbstractionManager(regionManager, formulaManager, config, logger, solver);
    predicateManager = new PredicateAbstractionManager(
        abstractionManager, formulaManager, pathFormulaManager,
        solver, config, logger, cfa.getLiveVariables());

    //
    //
    // Create and initialize the formal CPA components
    transfer = new WpTransferRelation(config, logger, pathFormulaManager, formulaManager, solver);
    domain = new WpAbstractDomain(pathFormulaManager, formulaManager);
    prec = StaticPrecisionAdjustment.getInstance();
    merge = new WpMergeOperator(domain);
    stop = new StopSepOperator(domain);
    // TODO: Rethink the choice of STOP and SEP
  }


  /**
   * @see ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  /**
   * @see ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  /**
   * @see ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  /**
   * @see ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  /**
   * @see ConfigurableProgramAnalysis#getPrecisionAdjustment()
   */
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  /**
   * @see ConfigurableProgramAnalysis#getInitialState()
   */
  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return domain.getTopInstance();
  }

  /**
   * @see ConfigurableProgramAnalysis#getInitialPrecision()
   */
  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    // FIXME: Exchange this by a WpPrecision
    return SingletonPrecision.getInstance();
  }


  @Override
  public void close() throws Exception {
    solver.close();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {

  }

}
