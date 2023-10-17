// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import java.util.HashMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.termination.TerminationPrecisionAdjustment;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/** CPA for termination analysis of C programs.
 * Abstract states represent a memory, where we can store an already seen state.
 * Transition relation allows to non-deterministically store an already visiting state.*/
public class TerminationToReachCPA extends AbstractCPA {
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private final PrecisionAdjustment precisionAdjustment;

  public TerminationToReachCPA(LogManager pLogger,
                               Configuration pConfiguration,
                               ShutdownNotifier pShutdownNotifier)
  throws InvalidConfigurationException {
    super("sep", "sep", null);
    Solver solver = Solver.create(pConfiguration, pLogger, pShutdownNotifier);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    precisionAdjustment = new TerminationToReachPrecisionAdjustment(solver, bfmgr, fmgr);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TerminationToReachCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TerminationToReachTransferRelation(bfmgr, fmgr);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new TerminationToReachState(new HashMap<>(), new HashMap<>());
  }
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
}
