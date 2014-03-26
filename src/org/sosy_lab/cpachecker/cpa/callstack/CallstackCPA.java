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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;

public class CallstackCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  private final Reducer reducer = new CallstackReducer();

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA(Configuration config, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super("sep", "sep", new CallstackTransferRelation(config, pLogger));
    this.cfa = pCFA;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    if (cfa.getLoopStructure().isPresent()) {
      ImmutableMultimap<String, Loop> loopStructure = cfa.getLoopStructure().get();
      if (loopStructure.size() == 1) {
        String functionName = Iterables.getOnlyElement(loopStructure.keys());
        if (functionName.equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME)) {
          ImmutableCollection<Loop> loops = loopStructure.get(functionName);
          if (loops.size() == 1) {
            Loop loop = Iterables.getOnlyElement(loops);
            if (loop.getLoopNodes().contains(pNode)) {
              return new CallstackState(null, functionName, pNode);
            }
          }
        }
      }
    }
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