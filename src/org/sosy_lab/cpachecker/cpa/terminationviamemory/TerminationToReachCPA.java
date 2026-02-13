// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * CPA for termination analysis of C programs. Abstract states represent a memory, where we can
 * store an already seen state. Transition relation allows to non-deterministically store an already
 * visiting state.
 */
public class TerminationToReachCPA extends AbstractCPA implements StatisticsProvider {
  private Solver solver;
  private InterpolationManager itpMgr;
  private PathFormulaManager pfmgr;
  private Configuration configuration;
  private ShutdownNotifier shutdownNotifier;
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private PrecisionAdjustment precisionAdjustment;
  private final CFA cfa;
  private final TerminationToReachStatistics statistics;
  private final LogManager logger;

  public TerminationToReachCPA(
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    statistics = new TerminationToReachStatistics(pConfiguration, pLogger, pCFA);
    cfa = pCFA;
    configuration = pConfiguration;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TerminationToReachCPA.class);
  }

  public void setSolver(PredicateCPA pCPA) throws InvalidConfigurationException {
    solver = pCPA.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = pCPA.getPathFormulaManager();
    itpMgr =
        new InterpolationManager(
            pCPA.getPathFormulaManager(),
            solver,
            Optional.empty(),
            Optional.empty(),
            configuration,
            shutdownNotifier,
            logger);
    precisionAdjustment =
        new TerminationToReachPrecisionAdjustment(
            solver, statistics, logger, cfa, bfmgr, fmgr, itpMgr);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TerminationToReachTransferRelation(fmgr, pfmgr);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new TerminationToReachState(
        ImmutableMap.of(),
        ImmutableMap.of(),
        ImmutableMap.of(),
        Optional.empty(),
        Optional.empty());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
