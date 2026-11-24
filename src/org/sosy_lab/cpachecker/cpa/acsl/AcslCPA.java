// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * This is a CPA whose goal is to find and inject ACSL invariants. For now this class is used for
 * testing and proof of concept.
 */
public class AcslCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  private final CFA cfa;
  private final LogManager logger;
  private final Configuration config;
  private final Solver solver;
  private FormulaToAcslConverter formulaConverter;
  private FormulaManagerView fmgr;
  private TransferRelation transfer;

  private AcslCPA(
      CFA pCFA, LogManager pLogManager, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super("join", "join", null);
    this.cfa = pCFA;
    this.config = pConfig;
    this.logger = pLogManager;
    solver = Solver.create(pConfig, this.logger, pShutdownNotifier);
    this.fmgr = solver.getFormulaManager();
    formulaConverter = new FormulaToAcslConverter(fmgr, cfa.getMachineModel());
    transfer = new AcslTransferRelation(cfa, logger, formulaConverter, fmgr, config);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AcslCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new AcslState(logger, ImmutableSetMultimap.of(), new CProgramScope(cfa, logger));
  }
}
