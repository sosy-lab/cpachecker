// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.argReplay;

import com.google.common.base.Preconditions;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/** This CPA explores the state space of a powerset domain, backed by an old reached-set.
 * Each abstract state contains the corresponding states of the old reached-set.
 * The program location of new abstract state (in the current analysis)
 * is equal to the abstract state from the reached-set. */
public class ARGReplayCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARGReplayCPA.class);
  }

  private ReachedSet reached = null;
  private ConfigurableProgramAnalysis cpa = null;

  public ARGReplayCPA() {
    super(
        "JOIN",
        "JOIN",
        DelegateAbstractDomain.<ARGReplayState>getInstance(),
        new ARGReplayTransferRelation());
  }

  /** This method should be run directly after the constructor. */
  public void setARGAndCPA(ReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(this.reached == null, "ReachedSet should only be set once.");
    this.reached  = pReached;

    Preconditions.checkNotNull(pCpa);
    Preconditions.checkState(this.cpa == null, "CPA should only be set once.");
    this.cpa   = pCpa;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Preconditions.checkNotNull(reached);
    return new ARGReplayState(Collections.singleton((ARGState)reached.getFirstState()), cpa);
  }
}
