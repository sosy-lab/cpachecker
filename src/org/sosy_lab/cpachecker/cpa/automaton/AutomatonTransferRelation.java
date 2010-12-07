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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link AutomatonState}
 * and strengthens an {@link AutomatonState.AutomatonUnknownState}.
 * @author rhein
 */
class AutomatonTransferRelation implements TransferRelation {
  private final LogManager logger;

  long totalPostTime = 0;
  long matchTime = 0;
  long assertionsTime = 0;
  long actionTime = 0;
  long totalStrengthenTime = 0;

  public AutomatonTransferRelation(Automaton pAutomaton, LogManager pLogger) {
    this.logger = pLogger;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#getAbstractSuccessors(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge)
   */
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
                      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
                      throws CPATransferException {
                          Preconditions.checkArgument(pElement instanceof AutomatonState);
    long start = System.currentTimeMillis();
    try {

      if (pElement instanceof AutomatonUnknownState) {
        // the last CFA edge could not be processed properly
        // (strengthen was not called on the AutomatonUnknownState or the strengthen operation had not enough information to determine a new following state.)
        AutomatonState top = ((AutomatonUnknownState)pElement).getAutomatonCPA().getTopState();
        return Collections.singleton((AbstractElement)top);
      }
      if (! (pElement instanceof AutomatonState)) {
        throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-AutomatonState AbstractElements.");
      }
      
      AutomatonState lCurrentAutomatonState = (AutomatonState)pElement;
      return getFollowStates(lCurrentAutomatonState, null, pCfaEdge, false);
    
    } finally {
      totalPostTime += System.currentTimeMillis() - start;
    }
  }
  
  /**
   * Returns the <code>AutomatonStates</code> that follow this State in the ControlAutomatonCPA.
   * If the passed <code>AutomatonExpressionArguments</code> are not sufficient to determine the following state
   * this method returns a <code>AutomatonUnknownState</code> that contains this as previous State.
   * The strengthen method of the <code>AutomatonUnknownState</code> should be used once enough Information is available to determine the correct following State.
   * 
   * If the state is a NonDet-State multiple following states may be returned.
   * If the only following state is BOTTOM an empty set is returned.
   */
  private Collection<? extends AbstractElement> getFollowStates(AutomatonState state, List<AbstractElement> otherElements, CFAEdge edge, boolean strengthen) {
    if (state == state.getAutomatonCPA().getTopState()) {
      return Collections.singleton(state);
    }
    if (state == state.getAutomatonCPA().getBottomState()) {
      return Collections.emptySet();
    }
    
    Collection<AbstractElement> lSuccessors = new HashSet<AbstractElement>();    
    AutomatonExpressionArguments exprArgs = new AutomatonExpressionArguments(state.getVars(), otherElements, edge, logger);
    boolean edgeMatched = false;
    boolean nonDetState = state.getInternalState().isNonDetState();
  
    // these transitions cannot be evaluated until last, because they might have sideeffects on other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<AutomatonTransition, Map<Integer, String>>> transitionsToBeTaken = new ArrayList<Pair<AutomatonTransition, Map<Integer, String>>>();
    for (AutomatonTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      long startMatch = System.currentTimeMillis();
      ResultValue<Boolean> match = t.match(exprArgs);      
      matchTime += System.currentTimeMillis() - startMatch;
      if (match.canNotEvaluate()) {
        if (strengthen) {
          logger.log(Level.INFO, match.getFailureMessage() +" IN " + match.getFailureOrigin());
        }
        // if one transition cannot be evaluated the evaluation must be postponed until enough information is available
        return Collections.singleton((AbstractElement)new AutomatonUnknownState(state));
      } else {
        if (match.getValue().equals(Boolean.TRUE)) {
          edgeMatched = true;
          long startAssertions = System.currentTimeMillis();
          ResultValue<Boolean> assertionsHold = t.assertionsHold(exprArgs);
          assertionsTime += System.currentTimeMillis() - startAssertions;
          if (assertionsHold.canNotEvaluate()) {
            if (strengthen) {
              logger.log(Level.INFO, match.getFailureMessage() +" IN " + match.getFailureOrigin());
            }
            // cannot yet be evaluated
            return Collections.singleton((AbstractElement)new AutomatonUnknownState(state));
          } else if (assertionsHold.getValue().equals(Boolean.TRUE)) {
            if (t.canExecuteActionsOn(exprArgs)) {
              Map<Integer, String> transitionVariables = new HashMap<Integer, String>(exprArgs.getTransitionVariables()); 
              if (nonDetState) {
                transitionsToBeTaken.add(Pair.of(t, transitionVariables)); 
              } else { // not a nondet State, return the first state, that was found
                long startAction = System.currentTimeMillis();
                Map<String, AutomatonVariable> newVars = deepCloneVars(state.getVars());
                exprArgs.setAutomatonVariables(newVars);
                exprArgs.putTransitionVariables(transitionVariables);
                t.executeActions(exprArgs);
                actionTime += System.currentTimeMillis() - startAction;
                AutomatonState lSuccessor = AutomatonState.automatonStateFactory(newVars, t.getFollowState(), state.getAutomatonCPA());
                if (lSuccessor instanceof AutomatonState.BOTTOM) {
                  return Collections.emptySet();
                } else {
                  return Collections.singleton((AbstractElement)lSuccessor);
                }
              }
            } else {
              // cannot yet execute, goto UnknownState
              return Collections.singleton((AbstractElement)new AutomatonUnknownState(state));
            }
          } else {
            // matching transitions, but unfulfilled assertions: goto error state
            AutomatonState errorState = AutomatonState.automatonStateFactory(Collections.<String, AutomatonVariable>emptyMap(), AutomatonInternalState.ERROR, state.getAutomatonCPA());
            logger.log(Level.INFO, "Automaton going to ErrorState on edge \"" + edge.getRawStatement() + "\"");
            if (nonDetState) {
              lSuccessors.add(errorState);
            } else {
              return Collections.singleton((AbstractElement)errorState); 
            }
          }
        }
        // do nothing if the edge did not match
      }
    }
    if (edgeMatched) {
      // execute Transitions
      for (Pair<AutomatonTransition, Map<Integer, String>> pair : transitionsToBeTaken) {
        // this transition will be taken. copy the variables
        AutomatonTransition t = pair.getFirst();
        Map<Integer, String> transitionVariables = pair.getSecond();
        long startAction = System.currentTimeMillis();
        Map<String, AutomatonVariable> newVars = deepCloneVars(state.getVars());
        exprArgs.setAutomatonVariables(newVars);
        exprArgs.putTransitionVariables(transitionVariables);
        t.executeActions(exprArgs);
        actionTime += System.currentTimeMillis() - startAction;
        AutomatonState lSuccessor = AutomatonState.automatonStateFactory(newVars, t.getFollowState(), state.getAutomatonCPA());
        // non-det state
        if (!(lSuccessor instanceof AutomatonState.BOTTOM)) {
          lSuccessors.add(lSuccessor);
        } // else add nothing
      }
      return lSuccessors;
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      return Collections.singleton((AbstractElement)state);
    }
  }

  private static Map<String, AutomatonVariable> deepCloneVars(Map<String, AutomatonVariable> pOld) {
    Map<String, AutomatonVariable> result = new HashMap<String, AutomatonVariable>(pOld.size());
    for (Entry<String, AutomatonVariable> e : pOld.entrySet()) {
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
    if (! (pElement instanceof AutomatonUnknownState)) {
      return null;
    } else {
      long start = System.currentTimeMillis();
      AutomatonUnknownState lUnknownState = (AutomatonUnknownState)pElement;
      Collection<? extends AbstractElement> lSuccessors = getFollowStates(lUnknownState.getPreviousState(), pOtherElements, pCfaEdge, true);
      totalStrengthenTime += System.currentTimeMillis() - start;
      assert (!(lSuccessors instanceof AutomatonUnknownState)): "automaton.strengthen returned an unknownState!";
      return lSuccessors;
    }
  }
}
