/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.observeranalysis.ObserverBoolExpr.MaybeBoolean;
import org.sosy_lab.cpachecker.cpa.observeranalysis.ObserverState.ObserverUnknownState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link ObserverState}
 * and strengthens an {@link ObserverState.ObserverUnknownState}.
 * @author rhein
 */
class ObserverTransferRelation implements TransferRelation {
  private final LogManager logger;

  long totalPostTime = 0;
  long matchTime = 0;
  long assertionsTime = 0;
  long actionTime = 0;
  long totalStrengthenTime = 0;

  public ObserverTransferRelation(ObserverAutomaton pAutomaton, LogManager pLogger) {
    this.logger = pLogger;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#getAbstractSuccessors(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge)
   */
  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
                      throws CPATransferException {
                          Preconditions.checkArgument(pElement instanceof ObserverState);
    long start = System.currentTimeMillis();
    try {

      if (pElement instanceof ObserverUnknownState) {
        // the last CFA edge could not be processed properly
        // (strengthen was not called on the ObserverUnknownState or the strengthen operation had not enough information to determine a new following state.)
        ObserverState top = ((ObserverUnknownState)pElement).getAutomatonCPA().getTopState();
        return Collections.singleton((AbstractElement)top);
      }
      if (! (pElement instanceof ObserverState)) {
        throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-ObserverState AbstractElements.");
      }
      
      ObserverState lCurrentObserverState = (ObserverState)pElement;
      return getFollowStates(lCurrentObserverState, null, pCfaEdge);
    
    } finally {
      totalPostTime += System.currentTimeMillis() - start;
    }
  }
  
  /**
   * Returns the <code>ObserverStates</code> that follow this State in the ObserverAutomatonCPA.
   * If the passed <code>ObserverExpressionArguments</code> are not sufficient to determine the following state
   * this method returns a <code>ObserverUnknownState</code> that contains this as previous State.
   * The strengthen method of the <code>ObserverUnknownState</code> should be used once enough Information is available to determine the correct following State.
   * 
   * If the state is a NonDet-State multiple following states may be returned.
   * If the only following state is BOTTOM an empty set is returned.
   */
  private Collection<AbstractElement> getFollowStates(ObserverState state, List<AbstractElement> otherElements, CFAEdge edge) {
    if (state == state.getAutomatonCPA().getTopState()) {
      return Collections.singleton((AbstractElement)state);
    }
    if (state == state.getAutomatonCPA().getBottomState()) {
      return Collections.emptySet();
    }
    if (state.isError()) {
      return Collections.singleton((AbstractElement)state);
    }
    
    Collection<AbstractElement> lSuccessors = new HashSet<AbstractElement>();    
    ObserverExpressionArguments exprArgs = new ObserverExpressionArguments(state.getVars(), otherElements, edge, logger);
    boolean edgeMatched = false;
    boolean nonDetState = state.getInternalState().isNonDetState();
  
    // these transitions cannot be evaluated until last, because they might have sideeffects on other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<ObserverTransition, Map<Integer, String>>> transitionsToBeTaken = new ArrayList<Pair<ObserverTransition, Map<Integer, String>>>();
    boolean stopEdgeMatching = false;
    for (ObserverTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      long startMatch = System.currentTimeMillis();
      MaybeBoolean match = t.match(exprArgs);      
      matchTime += System.currentTimeMillis() - startMatch;

      switch (match) {
      case TRUE :
        edgeMatched = true;
        long startAssertions = System.currentTimeMillis();
        MaybeBoolean assertionsHold = t.assertionsHold(exprArgs);
        assertionsTime += System.currentTimeMillis() - startAssertions;
        if (assertionsHold == MaybeBoolean.TRUE) {
          if (t.canExecuteActionsOn(exprArgs)) {
            Map<Integer, String> transitionVariables = new HashMap<Integer, String>(exprArgs.getTransitionVariables()); 
            if (nonDetState) {
              transitionsToBeTaken.add(new Pair<ObserverTransition, Map<Integer, String>>(t, transitionVariables)); 
            } else { // not a nondet State, return the first state, that was found
              long startAction = System.currentTimeMillis();
              Map<String, ObserverVariable> newVars = deepCloneVars(state.getVars());
              exprArgs.setObserverVariables(newVars);
              exprArgs.putTransitionVariables(transitionVariables);
              t.executeActions(exprArgs);
              actionTime += System.currentTimeMillis() - startAction;
              ObserverState lSuccessor = ObserverState.observerStateFactory(newVars, t.getFollowState(), state.getAutomatonCPA());
              if (lSuccessor instanceof ObserverState.BOTTOM) {
                return Collections.emptySet();
              } else {
                return Collections.singleton((AbstractElement)lSuccessor);
              }
            }
          } else {
            // cannot yet execute, goto UnknownState
            return Collections.singleton((AbstractElement)new ObserverUnknownState(state));
          }
        } else if (assertionsHold == MaybeBoolean.MAYBE) {
          // cannot yet be evaluated
          return Collections.singleton((AbstractElement)new ObserverUnknownState(state));
        } else if (assertionsHold == MaybeBoolean.FALSE) {
          // matching transitions, but unfulfilled assertions: goto error state
          ObserverState errorState = ObserverState.observerStateFactory(Collections.<String, ObserverVariable>emptyMap(), ObserverInternalState.ERROR, state.getAutomatonCPA());
          logger.log(Level.INFO, "ObserverAutomaton going to ErrorState on edge \"" + edge.getRawStatement() + "\"");
          if (nonDetState) {
            lSuccessors.add(errorState);
          } else {
            return Collections.singleton((AbstractElement)errorState); 
          }
        }
        break;

      case MAYBE :
        // if one transition cannot be evaluated the evaluation must be postponed until enough information is available
        
        return Collections.singleton((AbstractElement)new ObserverUnknownState(state));
      case FALSE :
      default :
        // consider next transition
      }
    }
    if (edgeMatched) {
      // execute Transitions
      for (Pair<ObserverTransition, Map<Integer, String>> pair : transitionsToBeTaken) {
        // this transition will be taken. copy the variables
        ObserverTransition t = pair.getFirst();
        Map<Integer, String> transitionVariables = pair.getSecond();
        long startAction = System.currentTimeMillis();
        Map<String, ObserverVariable> newVars = deepCloneVars(state.getVars());
        exprArgs.setObserverVariables(newVars);
        exprArgs.putTransitionVariables(transitionVariables);
        t.executeActions(exprArgs);
        actionTime += System.currentTimeMillis() - startAction;
        ObserverState lSuccessor = ObserverState.observerStateFactory(newVars, t.getFollowState(), state.getAutomatonCPA());
        // non-det state
        if (!(lSuccessor instanceof ObserverState.BOTTOM)) {
          lSuccessors.add(lSuccessor);
        } // else add nothing
      }
      return lSuccessors;
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      return Collections.singleton((AbstractElement)state);
    }
  }

  private static Map<String, ObserverVariable> deepCloneVars(Map<String, ObserverVariable> pOld) {
    Map<String, ObserverVariable> result = new HashMap<String, ObserverVariable>(pOld.size());
    for (Entry<String, ObserverVariable> e : pOld.entrySet()) {
      result.put(e.getKey(), e.getValue().clone());
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#strengthen(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, java.util.List, org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge, org.sosy_lab.cpachecker.core.interfaces.Precision)
   */
  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
                                    List<AbstractElement> pOtherElements,
                                    CFAEdge pCfaEdge, Precision pPrecision)
                                    throws CPATransferException {
    if (! (pElement instanceof ObserverUnknownState)) {
      return null;
    } else {
      long start = System.currentTimeMillis();
      ObserverUnknownState lUnknownState = (ObserverUnknownState)pElement;
      Collection<AbstractElement> lSuccessors = getFollowStates(lUnknownState.getPreviousState(), pOtherElements, pCfaEdge);
      totalStrengthenTime += System.currentTimeMillis() - start;
      return lSuccessors;
    }
  }
}
