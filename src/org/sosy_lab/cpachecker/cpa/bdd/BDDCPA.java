// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

@Options(prefix = "cpa.bdd")
public class BDDCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BDDCPA.class);
  }

  private final NamedRegionManager manager;
  private final BitvectorManager bvmgr;
  private final PredicateManager predmgr;
  private VariableTrackingPrecision precision;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final BDDStatistics stats;
  private final BitvectorComputer bvComputer;

  @Option(
      secure = true,
      description = "mergeType",
      values = {"sep", "join"})
  private String merge = "join";

  @Option(secure = true, description = "max bitsize for values and vars, initial value")
  private int bitsize = 64;

  @Option(
      secure = true,
      description = "use a smaller bitsize for all vars, that have only intEqual values")
  private boolean compressIntEqual = true;

  @Option(
      secure = true,
      description = "reduce and expand BDD states for BAM, otherwise use plain identity")
  private boolean useBlockAbstraction = false;

  private BDDCPA(
      CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;

    RegionManager rmgr = new BDDManagerFactory(config, logger).createRegionManager();

    precision =
        VariableTrackingPrecision.createStaticPrecision(
            config, cfa.getVarClassification(), getClass());

    manager = new NamedRegionManager(rmgr);
    bvmgr = new BitvectorManager(rmgr);
    predmgr = new PredicateManager(config, manager, cfa);
    bvComputer =
        new BitvectorComputer(
            compressIntEqual,
            cfa.getVarClassification().orElseThrow(),
            bvmgr,
            manager,
            predmgr,
            cfa.getMachineModel());
    stats = new BDDStatistics(config, cfa, logger, manager, predmgr);
  }

  public void injectRefinablePrecision() throws InvalidConfigurationException {
    precision = VariableTrackingPrecision.createRefineablePrecision(config, precision);
  }

  public NamedRegionManager getManager() {
    return manager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<BDDState>getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (merge) {
      case "sep":
        return MergeSepOperator.getInstance();
      case "join":
        return new MergeJoinOperator(getAbstractDomain());
      default:
        throw new AssertionError("unexpected operator: " + merge);
    }
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new BDDTransferRelation(manager, bvmgr, predmgr, cfa, bitsize, bvComputer);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new BDDState(manager, bvmgr, manager.makeTrue());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }

  @Override
  public Reducer getReducer() {
    return new BDDReducer(
        manager,
        bvmgr,
        predmgr,
        cfa.getMachineModel(),
        cfa.getVarClassification().orElseThrow(),
        shutdownNotifier,
        logger,
        useBlockAbstraction,
        bvComputer);
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public CFA getCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }
}
