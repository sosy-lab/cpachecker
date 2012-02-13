/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link AutomatonState}
 * and strengthens an {@link AutomatonState.AutomatonUnknownState}.
 */
class AutomatonTransferRelation implements TransferRelation {

  private final ControlAutomatonCPA cpa;
  private final LogManager logger;

  Timer totalPostTime = new Timer();
  Timer matchTime = new Timer();
  Timer assertionsTime = new Timer();
  Timer actionTime = new Timer();
  Timer totalStrengthenTime = new Timer();

  public AutomatonTransferRelation(ControlAutomatonCPA pCpa, LogManager pLogger) {
    this.cpa = pCpa;
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
    totalPostTime.start();
    try {

      if (pElement instanceof AutomatonUnknownState) {
        // the last CFA edge could not be processed properly
        // (strengthen was not called on the AutomatonUnknownState or the strengthen operation had not enough information to determine a new following state.)
        AutomatonState top = cpa.getTopState();
        return Collections.singleton(top);
      }
      if (! (pElement instanceof AutomatonState)) {
        throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-AutomatonState AbstractElements.");
      }

      AutomatonState lCurrentAutomatonState = (AutomatonState)pElement;
      return getFollowStates(lCurrentAutomatonState, null, pCfaEdge, false);

    } finally {
      totalPostTime.stop();
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
   * @throws CPATransferException
   */
  private Collection<? extends AbstractElement> getFollowStates(AutomatonState state, List<AbstractElement> otherElements, CFAEdge edge, boolean strengthen) throws CPATransferException {
    Preconditions.checkArgument(!(state instanceof AutomatonUnknownState));
    if (state == cpa.getBottomState()) {
      return Collections.emptySet();
    }

    if (state.getInternalState().getTransitions().isEmpty()) {
      // shortcut
      return Collections.singleton(state);
    }

    Collection<AbstractElement> lSuccessors = new HashSet<AbstractElement>(2);
    AutomatonExpressionArguments exprArgs = new AutomatonExpressionArguments(state.getVars(), otherElements, edge, logger);
    boolean edgeMatched = false;
    boolean nonDetState = state.getInternalState().isNonDetState();

    // these transitions cannot be evaluated until last, because they might have sideeffects on other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<AutomatonTransition, Map<Integer, String>>> transitionsToBeTaken = new ArrayList<Pair<AutomatonTransition, Map<Integer, String>>>(2);

    for (AutomatonTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      matchTime.start();
      ResultValue<Boolean> match = t.match(exprArgs);
      matchTime.stop();
      if (match.canNotEvaluate()) {
        if (strengthen) {
          logger.log(Level.INFO, match.getFailureMessage() +" IN " + match.getFailureOrigin());
        }
        // if one transition cannot be evaluated the evaluation must be postponed until enough information is available
        return Collections.singleton(new AutomatonUnknownState(state));
      } else {
        if (match.getValue()) {
          edgeMatched = true;
          assertionsTime.start();
          ResultValue<Boolean> assertionsHold = t.assertionsHold(exprArgs);
          assertionsTime.stop();

          if (assertionsHold.canNotEvaluate()) {
            if (strengthen) {
              logger.log(Level.INFO, match.getFailureMessage() +" IN " + match.getFailureOrigin());
            }
            // cannot yet be evaluated
            return Collections.singleton(new AutomatonUnknownState(state));

          } else if (assertionsHold.getValue()) {
            if (!t.canExecuteActionsOn(exprArgs)) {
              // cannot yet execute, goto UnknownState
              return Collections.singleton(new AutomatonUnknownState(state));
            }

            // delay execution as described above
            Map<Integer, String> transitionVariables = ImmutableMap.copyOf(exprArgs.getTransitionVariables());
            transitionsToBeTaken.add(Pair.of(t, transitionVariables));

          } else {
            // matching transitions, but unfulfilled assertions: goto error state
            AutomatonState errorState = AutomatonState.automatonStateFactory(Collections.<String, AutomatonVariable>emptyMap(), AutomatonInternalState.ERROR, cpa);
            logger.log(Level.INFO, "Automaton going to ErrorState on edge \"" + edge.getDescription() + "\"");
            lSuccessors.add(errorState);
          }

          if (!nonDetState) {
            // not a nondet State, break on the first matching edge
            break;
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
        actionTime.start();
        Map<String, AutomatonVariable> newVars = deepCloneVars(state.getVars());
        exprArgs.setAutomatonVariables(newVars);
        exprArgs.putTransitionVariables(transitionVariables);
        t.executeActions(exprArgs);
        actionTime.stop();
        AutomatonState lSuccessor = AutomatonState.automatonStateFactory(newVars, t.getFollowState(), cpa);
        if (!(lSuccessor instanceof AutomatonState.BOTTOM)) {
          lSuccessors.add(lSuccessor);
        } // else add nothing
      }
      return lSuccessors;
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      return Collections.singleton(state);
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
      totalStrengthenTime.start();
      AutomatonUnknownState lUnknownState = (AutomatonUnknownState)pElement;
      Collection<? extends AbstractElement> lSuccessors = getFollowStates(lUnknownState.getPreviousState(), pOtherElements, pCfaEdge, true);
      totalStrengthenTime.stop();
      for (AbstractElement succ : lSuccessors) {
        if (succ instanceof AutomatonUnknownState) {
          // TODO this should give more details
          throw new CPATransferException("Automaton transition could not be matched against CFA edge");
        }
      }
      return lSuccessors;
    }
  }
}
