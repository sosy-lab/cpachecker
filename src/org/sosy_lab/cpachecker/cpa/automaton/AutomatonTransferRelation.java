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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatIntHist;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link AutomatonState}
 * and strengthens an {@link AutomatonState.AutomatonUnknownState}.
 */
class AutomatonTransferRelation extends SingleEdgeTransferRelation {

  private final ControlAutomatonCPA cpa;
  private final LogManager logger;
  private final MachineModel machineModel;

  Timer totalPostTime = new Timer();
  Timer matchTime = new Timer();
  Timer assertionsTime = new Timer();
  Timer actionTime = new Timer();
  Timer totalStrengthenTime = new Timer();
  StatIntHist automatonSuccessors = new StatIntHist(StatKind.AVG, "Automaton transfer successors");

  public AutomatonTransferRelation(
      ControlAutomatonCPA pCpa, LogManager pLogger, MachineModel pMachineModel) {
    this.cpa = pCpa;
    this.logger = pLogger;
    this.machineModel = pMachineModel;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
                      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
                      throws CPATransferException {

    Preconditions.checkArgument(pElement instanceof AutomatonState);

    if (pElement instanceof AutomatonUnknownState) {
      // the last CFA edge could not be processed properly
      // (strengthen was not called on the AutomatonUnknownState or the strengthen operation had not enough information to determine a new following state.)
      AutomatonState top = cpa.getTopState();
      return Collections.singleton(top);
    }

    Collection<? extends AbstractState> result =
        getAbstractSuccessors0((AutomatonState) pElement, pCfaEdge);
    automatonSuccessors.setNextValue(result.size());
    return result;
  }

  private Collection<AutomatonState> getAbstractSuccessors0(
      AutomatonState pElement, CFAEdge pCfaEdge) throws CPATransferException {
    totalPostTime.start();
    try {
      if (pElement instanceof AutomatonUnknownState) {
        // happens only inside MultiEdges,
        // here we have no chance (because strengthen is called only at the end of the edge),
        // so we just stay in the previous state
        pElement = ((AutomatonUnknownState)pElement).getPreviousState();
      }

      return getFollowStates(pElement, null, pCfaEdge, false);

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
   */
  private Collection<AutomatonState> getFollowStates(AutomatonState state, List<AbstractState> otherElements, CFAEdge edge, boolean failOnUnknownMatch) throws CPATransferException {
    Preconditions.checkArgument(!(state instanceof AutomatonUnknownState));
    if (state == cpa.getBottomState()) {
      return Collections.emptySet();
    }

    if (state.getInternalState().getTransitions().isEmpty()) {
      // shortcut
      return Collections.singleton(state);
    }

    Collection<AutomatonState> lSuccessors = Sets.newLinkedHashSetWithExpectedSize(2);
    AutomatonExpressionArguments exprArgs = new AutomatonExpressionArguments(state, state.getVars(), otherElements, edge, logger);
    boolean edgeMatched = false;
    int failedMatches = 0;
    boolean nonDetState = state.getInternalState().isNonDetState();

    // these transitions cannot be evaluated until last, because they might have sideeffects on other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<AutomatonTransition, Map<Integer, String>>> transitionsToBeTaken = new ArrayList<>(2);

    for (AutomatonTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      matchTime.start();
      ResultValue<Boolean> match = t.match(exprArgs);
      matchTime.stop();

      if (match.canNotEvaluate()) {
        if (failOnUnknownMatch) {
          throw new CPATransferException("Automaton transition condition could not be evaluated: " + match.getFailureMessage());
        }
        // if one transition cannot be evaluated the evaluation must be postponed until enough information is available
        return Collections.<AutomatonState>singleton(new AutomatonUnknownState(state));
      } else {
        if (match.getValue()) {
          edgeMatched = true;
          assertionsTime.start();
          ResultValue<Boolean> assertionsHold = t.assertionsHold(exprArgs);
          assertionsTime.stop();

          if (assertionsHold.canNotEvaluate()) {
            if (failOnUnknownMatch) {
              throw new CPATransferException("Automaton transition assertions could not be evaluated: " + assertionsHold.getFailureMessage());
            }
            // cannot yet be evaluated
            return Collections.<AutomatonState>singleton(new AutomatonUnknownState(state));

          } else if (assertionsHold.getValue()) {
            if (!t.canExecuteActionsOn(exprArgs)) {
              if (failOnUnknownMatch) {
                throw new CPATransferException("Automaton transition action could not be executed");
              }
              // cannot yet execute, goto UnknownState
              return Collections.<AutomatonState>singleton(new AutomatonUnknownState(state));
            }

            // delay execution as described above
            Map<Integer, String> transitionVariables = ImmutableMap.copyOf(exprArgs.getTransitionVariables());
            transitionsToBeTaken.add(Pair.of(t, transitionVariables));

          } else {
            // matching transitions, but unfulfilled assertions: goto error state
            final String desc = Strings.nullToEmpty(t.getViolatedPropertyDescription(exprArgs));
            AutomatonSafetyProperty prop =
                new AutomatonSafetyProperty(state.getOwningAutomaton(), t, desc);

            AutomatonState errorState = AutomatonState.automatonStateFactory(
                Collections.<String, AutomatonVariable>emptyMap(), AutomatonInternalState.ERROR, cpa, 0, 0, prop);

            logger.log(Level.INFO, "Automaton going to ErrorState on edge \"" + edge.getDescription() + "\"");
            lSuccessors.add(errorState);
          }

          if (!nonDetState) {
            // not a nondet State, break on the first matching edge
            break;
          }
        } else {
          // do nothing if the edge did not match
          failedMatches++;
        }
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

        AutomatonSafetyProperty violatedProperty = null;
        if (t.getFollowState().isTarget()) {
          final String desc = Strings.nullToEmpty(t.getViolatedPropertyDescription(exprArgs));
          violatedProperty = new AutomatonSafetyProperty(state.getOwningAutomaton(), t, desc);
        }

        AutomatonState lSuccessor =
            AutomatonState.automatonStateFactory(
                newVars,
                t.getFollowState(),
                cpa,
                t.getAssumptions(edge, logger, machineModel),
                t.getCandidateInvariants(),
                state.getMatches() + 1,
                state.getFailedMatches(),
                violatedProperty);

        if (!(lSuccessor instanceof AutomatonState.BOTTOM)) {
          lSuccessors.add(lSuccessor);
        } else {
          // add nothing
        }
      }
      return lSuccessors;
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      AutomatonState stateNewCounters = AutomatonState.automatonStateFactory(state.getVars(), state.getInternalState(), cpa, state.getMatches(), state.getFailedMatches() + failedMatches, null);
      return Collections.singleton(stateNewCounters);
    }
  }

  private static Map<String, AutomatonVariable> deepCloneVars(Map<String, AutomatonVariable> pOld) {
    Map<String, AutomatonVariable> result = Maps.newHashMapWithExpectedSize(pOld.size());
    for (Entry<String, AutomatonVariable> e : pOld.entrySet()) {
      result.put(e.getKey(), e.getValue().clone());
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#strengthen(org.sosy_lab.cpachecker.core.interfaces.AbstractState, java.util.List, org.sosy_lab.cpachecker.cfa.model.CFAEdge, org.sosy_lab.cpachecker.core.interfaces.Precision)
   */
  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement,
                                    List<AbstractState> pOtherElements,
                                    CFAEdge pCfaEdge, Precision pPrecision)
                                    throws CPATransferException {
    if (! (pElement instanceof AutomatonUnknownState)) {
      return Collections.singleton(pElement);
    } else {
      totalStrengthenTime.start();
      AutomatonUnknownState lUnknownState = (AutomatonUnknownState)pElement;

      /*
       * Strengthening might depend on the strengthening of other automaton
       * states, so we do a fixed-point iteration.
       */
      Collection<List<AbstractState>> strengtheningCombinations = new HashSet<>();
      strengtheningCombinations.add(pOtherElements);
      boolean changed = from(pOtherElements).anyMatch(instanceOf(AutomatonUnknownState.class));
      while (changed) {
        changed = false;
        Collection<List<AbstractState>> newCombinations = new HashSet<>();
        for (List<AbstractState> otherStates : strengtheningCombinations) {
          Collection<List<AbstractState>> newPartialCombinations = new ArrayList<>();
          newPartialCombinations.add(new ArrayList<>());
          for (AbstractState otherState : otherStates) {
            AbstractState toAdd = otherState;
            if (otherState instanceof AutomatonUnknownState) {
              AutomatonUnknownState unknownState = (AutomatonUnknownState) otherState;

              // Compute the successors of the other unknown state
              List<AbstractState> statesOtherToCurrent = new ArrayList<>(otherStates);
              statesOtherToCurrent.remove(unknownState);
              statesOtherToCurrent.add(lUnknownState);
              Collection<? extends AbstractState> successors =
                  getFollowStates(unknownState.getPreviousState(), statesOtherToCurrent, pCfaEdge, true);

              // There might be zero or more than one successor,
              // so the list of states is multiplied with the list of successors
              Collection<List<AbstractState>> multipliedPartialCrossProduct = new ArrayList<>();
              for (List<AbstractState> newOtherStates : newPartialCombinations) {
                for (AbstractState successor : successors) {
                  List<AbstractState> multipliedNewOtherStates = new ArrayList<>(newOtherStates);
                  multipliedNewOtherStates.add(successor);
                  multipliedPartialCrossProduct.add(multipliedNewOtherStates);
                }
              }
              newPartialCombinations = multipliedPartialCrossProduct;
            } else {
              // Not an (unknown) automaton state, so just add it at the end of each list
              for (List<AbstractState> newOtherStates : newPartialCombinations) {
                newOtherStates.add(toAdd);
              }
            }
          }
          newCombinations.addAll(newPartialCombinations);
        }
        changed = !strengtheningCombinations.equals(newCombinations);
        strengtheningCombinations = newCombinations;
      }

      // For each list of other states, do the strengthening
      Collection<AbstractState> successors = new HashSet<>();
      for (List<AbstractState> otherStates : strengtheningCombinations) {
        successors.addAll(getFollowStates(lUnknownState.getPreviousState(), otherStates, pCfaEdge, true));
      }
      totalStrengthenTime.stop();
      assert !from(successors).anyMatch(instanceOf(AutomatonUnknownState.class));
      return successors;
    }
  }
}
