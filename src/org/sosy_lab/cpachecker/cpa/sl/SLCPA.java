// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.sl;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 *
 */
public class SLCPA extends AbstractCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SLCPA.class);
  }

  private final CFA cfa;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pfm;
  private final Solver solver;
  private final SLStatistics stats;

  private SLCPA(
      CFA pCfa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    super("sep", "sep", new FlatLatticeDomain(), null);

    stats = new SLStatistics();
    cfa = pCfa;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    solver = Solver.create(config, logger, shutdownNotifier);
    pfm =
        new PathFormulaManagerImpl(
            solver,
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            cfa.getMachineModel(),
            cfa.getVarClassification(),
            AnalysisDirection.FORWARD,
            stats);
  }

  @Override
  public SLState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    PathFormula store = pfm.makeEmptyPathFormula();
    return new SLState.Builder(store).build();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SLTransferRelation(logger, solver, pfm, stats);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
