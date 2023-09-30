// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.aggressiveloopbound;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class AggressiveLoopBoundCPA extends AbstractCPA implements ProofCheckerCPA {

  private final LoopStructure loopStructure;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AggressiveLoopBoundCPA.class);
  }

  AggressiveLoopBoundCPA(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException, CPAException {
    super("sep", "sep", new AgressiveLoopBoundTransferRelation(pConfig, pCFA, pLogger));
    //        pConfig.inject(this);
    loopStructure = pCFA.getLoopStructure().orElseThrow();
  }

  @Override
  public StopOperator getStopOperator() {

    return super.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    AgressiveLoopBoundState initialState = new AgressiveLoopBoundState();
    for (Loop loop : loopStructure.getLoopsForLoopHead(pNode)) {
      initialState = initialState.visitLoopHeadInitially(loop);
    }
    return initialState;
  }

  @Override
  public AgressiveLoopBoundTransferRelation getTransferRelation() {
    assert super.getTransferRelation() instanceof AgressiveLoopBoundTransferRelation;
    return (AgressiveLoopBoundTransferRelation) super.getTransferRelation();
  }

  public void setAutomatonTramsferRelation(AutomatonTransferRelation pTransferRelation) {
    this.getTransferRelation().setAutomatonTransferRelation(pTransferRelation);
  }
}
