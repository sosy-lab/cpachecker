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
package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CallstackCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithABM, ProofChecker {

  private final Reducer reducer = new CallstackReducer();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super("sep", "sep", new CallstackTransferRelation(config, pLogger));
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return new CallstackState(null, pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> computedSuccessors =
        getTransferRelation().getAbstractSuccessors(pElement, null, pCfaEdge);
    if (!(pSuccessors instanceof Set) || !(computedSuccessors instanceof Set)
        || pSuccessors.size() != computedSuccessors.size()) { return false; }
    boolean found;
    for (AbstractState e1 : pSuccessors) {
      found = false;
      for (AbstractState e2 : computedSuccessors) {
        if (((CallstackState) e1).sameStateInProofChecking((CallstackState) e2)) {
          found = true;
          break;
        }
      }
      if (!found) { return false; }
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException, InterruptedException {
    return (getAbstractDomain().isLessOrEqual(pElement, pOtherElement)) || ((CallstackState) pElement)
        .sameStateInProofChecking((CallstackState) pOtherElement);
  }
}