// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowState;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowTransferRelation;

/** CPA for termination analysis of C programs.
 * Abstract states represent a memory, where we can store an already seen state.
 * Transition relation allows to non-deterministically store an already visiting state.*/
public class TerminationToReachCPA extends AbstractCPA {

  public TerminationToReachCPA(CFA pCfa, LogManager pLogger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TerminationToReachCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TerminationToReachTransferRelation();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new TerminationToReachState(ImmutableSet.of());
  }
}
