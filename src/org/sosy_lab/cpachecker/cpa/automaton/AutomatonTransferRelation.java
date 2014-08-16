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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.AutomatonUnknownState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.SourceLocationMapper;
import org.sosy_lab.cpachecker.util.statistics.StatIntHist;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link AutomatonState}
 * and strengthens an {@link AutomatonState.AutomatonUnknownState}.
 */
@Options(prefix = "cpa.automaton")
class AutomatonTransferRelation implements TransferRelation {

  @Option(description = "Collect information about matched (and traversed) tokens.")
  private boolean collectTokenInformation = false;

  private final ControlAutomatonCPA cpa;
  private final LogManager logger;

  Timer totalPostTime = new Timer();
  Timer matchTime = new Timer();
  Timer assertionsTime = new Timer();
  Timer actionTime = new Timer();
  Timer totalStrengthenTime = new Timer();
  StatIntHist automatonSuccessors = new StatIntHist(StatKind.AVG, "Automaton transfer successors");

  public AutomatonTransferRelation(ControlAutomatonCPA pCpa, Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = pCpa;
    this.logger = pLogger;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#getAbstractSuccessors(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.cfa.model.CFAEdge)
   */
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
                      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
                      throws CPATransferException {

    Preconditions.checkArgument(pElement instanceof AutomatonState);

    if (!(pCfaEdge instanceof MultiEdge)) {
      Collection<? extends AbstractState> result = getAbstractSuccessors0(pElement, pPrecision, pCfaEdge);
      automatonSuccessors.setNextValue(result.size());
      return result;
    }

    final List<CFAEdge> edges = ((MultiEdge)pCfaEdge).getEdges();
    checkArgument(!edges.isEmpty());

    // As long as each transition produces only 0 or 1 successors,
    // we can just iterate through the edges.
    AutomatonState currentState = (AutomatonState)pElement;
    Collection<AutomatonState> currentSuccessors = null;
    int edgeIndex;
    for (edgeIndex=0; edgeIndex<edges.size(); edgeIndex++) {
      CFAEdge edge = edges.get(edgeIndex);
      currentSuccessors = getAbstractSuccessors0(currentState, pPrecision, edge);
      if (currentSuccessors.isEmpty()) {
        automatonSuccessors.setNextValue(0);
        return currentSuccessors; // bottom
      } else if (currentSuccessors.size() == 1) {
        automatonSuccessors.setNextValue(1);
        currentState = Iterables.getOnlyElement(currentSuccessors);
      } else { // currentSuccessors.size() > 1
        break;
      }
    }

    if (edgeIndex == edges.size()) {
      automatonSuccessors.setNextValue(currentSuccessors.size());
      return currentSuccessors;
    }

    // If there are two or more successors once, we use a waitlist algorithm.
    Deque<Pair<AutomatonState, Integer>> queue = new ArrayDeque<>(1);
    for (AutomatonState successor : currentSuccessors) {
      queue.addLast(Pair.of(successor, edgeIndex));
    }
    currentSuccessors.clear();

    List<AutomatonState> results = new ArrayList<>();
    while (!queue.isEmpty()) {
      Pair<AutomatonState, Integer> entry = queue.pollFirst();
      AutomatonState state = entry.getFirst();
      edgeIndex = entry.getSecond();
      CFAEdge edge = edges.get(edgeIndex);
      Integer successorIndex = edgeIndex+1;

      if (successorIndex == edges.size()) {
        // last iteration
        results.addAll(getAbstractSuccessors0(state, pPrecision, edge));

      } else {
        for (AutomatonState successor : getAbstractSuccessors0(state, pPrecision, edge)) {
          queue.addLast(Pair.of(successor, successorIndex));
        }
      }

    }

    automatonSuccessors.setNextValue(results.size());
    return results;
  }

  private Collection<AutomatonState> getAbstractSuccessors0(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    totalPostTime.start();
    try {

      if (pElement instanceof AutomatonUnknownState) {
        // the last CFA edge could not be processed properly
        // (strengthen was not called on the AutomatonUnknownState or the strengthen operation had not enough information to determine a new following state.)
        AutomatonState top = cpa.getTopState();
        return Collections.singleton(top);
      }
      if (! (pElement instanceof AutomatonState)) {
        throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-AutomatonState AbstractStates.");
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
  private Collection<AutomatonState> getFollowStates(AutomatonState state, List<AbstractState> otherElements, CFAEdge edge, boolean failOnUnknownMatch) throws CPATransferException {
    Preconditions.checkArgument(!(state instanceof AutomatonUnknownState));
    if (state == cpa.getBottomState()) {
      return Collections.emptySet();
    }

    if (collectTokenInformation) {
      SourceLocationMapper.getKnownToEdge(edge);
    }

    if (state.getInternalState().getTransitions().isEmpty()) {
      // shortcut
      return Collections.singleton(state);
    }

    Collection<AutomatonState> lSuccessors = new HashSet<>(2);
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

//      System.out.println("----------------------");
//      System.out.println(t.getTrigger());
//      System.out.println(t.getFollowState().getName());
//      System.out.println(edge.getPredecessor().getNodeNumber());
//      System.out.println(edge.getCode());
//      System.out.println(match.getValue());


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
            AutomatonState errorState = AutomatonState.automatonStateFactory(Collections.<String, AutomatonVariable>emptyMap(), AutomatonInternalState.ERROR, cpa, 0, 0, "");
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
        String violatedPropertyDescription = null;
        if (t.getFollowState().isTarget()) {
          violatedPropertyDescription = t.getViolatedPropertyDescription(exprArgs);
        }
        AutomatonState lSuccessor = AutomatonState.automatonStateFactory(newVars, t.getFollowState(), cpa, t.getAssumptions(), state.getMatches() + 1, state.getFailedMatches(), violatedPropertyDescription);
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
      if (collectTokenInformation) {
        stateNewCounters.addNoMatchTokens(state.getTokensSinceLastMatch());
        if (edge.getEdgeType() != CFAEdgeType.DeclarationEdge) {
          stateNewCounters.addNoMatchTokens(SourceLocationMapper.getAbsoluteTokensFromCFAEdge(edge, true));
        }
      }
      return Collections.singleton(stateNewCounters);
    }
  }

  private static Map<String, AutomatonVariable> deepCloneVars(Map<String, AutomatonVariable> pOld) {
    Map<String, AutomatonVariable> result = new HashMap<>(pOld.size());
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
      return null;
    } else {
      totalStrengthenTime.start();
      AutomatonUnknownState lUnknownState = (AutomatonUnknownState)pElement;
      Collection<? extends AbstractState> lSuccessors = getFollowStates(lUnknownState.getPreviousState(), pOtherElements, pCfaEdge, true);
      totalStrengthenTime.stop();
      assert !from(lSuccessors).anyMatch(instanceOf(AutomatonUnknownState.class));
      return lSuccessors;
    }
  }
}
