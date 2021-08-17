// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManagerOptions;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionStatistics;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.WeakeningOptions;

public class TraceAbstractionCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TraceAbstractionCPA.class);
  }

  private final FormulaManagerView formulaManagerView;
  private final PredicateAbstractionManager predicateAbstractionManager;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  @SuppressWarnings("resource")
  private TraceAbstractionCPA(
      Configuration pConfig, CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super("SEP", "SEP", null);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    itpSequenceStorage = new InterpolationSequenceStorage();

    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    formulaManagerView = solver.getFormulaManager();

    RegionManager regionManager = new BDDManagerFactory(pConfig, pLogger).createRegionManager();
    AbstractionManager abstractionManager =
        new AbstractionManager(regionManager, pConfig, pLogger, solver);

    PathFormulaManager pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    // TODO: check if this is beneficial for TraceAbstraction
    //    if (useCache) {
    //      pathFormulaManagerImpl = new CachingPathFormulaManager(pathFormulaManagerImpl);
    //    }

    PredicateAbstractionManagerOptions abstractionOptions =
        new PredicateAbstractionManagerOptions(pConfig);
    PredicateAbstractionsStorage abstractionStorage;
    try {
      abstractionStorage =
          new PredicateAbstractionsStorage(
              abstractionOptions.getReuseAbstractionsFrom(),
              pLogger,
              solver.getFormulaManager(),
              null);
    } catch (PredicateParsingFailedException e) {
      throw new InvalidConfigurationException(e.getMessage(), e);
    }

    predicateAbstractionManager =
        new PredicateAbstractionManager(
            abstractionManager,
            pathFormulaManager,
            solver,
            abstractionOptions,
            new WeakeningOptions(pConfig),
            abstractionStorage,
            pLogger,
            pShutdownNotifier,
            new PredicateAbstractionStatistics(),
            TrivialInvariantSupplier.INSTANCE);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return TraceAbstractionState.createInitState();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TraceAbstractionTransferRelation(
        formulaManagerView,
        predicateAbstractionManager,
        itpSequenceStorage,
        logger,
        shutdownNotifier);
  }

  InterpolationSequenceStorage getInterpolationSequenceStorage() {
    return itpSequenceStorage;
  }
}
