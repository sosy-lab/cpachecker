/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.SpecificationProperty.PropertyType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class AutomatonGraphmlParserState {

  /** The name of the witness automaton. */
  private final String automatonName;

  /** The type of the witness automaton. */
  private final WitnessType witnessType;

  /** The specification types. */
  private final ImmutableSet<PropertyType> specificationTypes;

  /** The entry state. */
  private final GraphMLState entryState;

  /** The states. */
  private final ImmutableSet<GraphMLState> states;

  /** States and the transitions leaving them, represented in the GraphML model. */
  private final ImmutableMultimap<GraphMLState, GraphMLTransition> leavingTransitions;

  /** States and the transitions entering them, represented in the GraphML model. */
  private final ImmutableMultimap<GraphMLState, GraphMLTransition> enteringTransitions;

  /** Distances to violation states (in the GraphML model). */
  private final Map<GraphMLState, Integer> distances;

  /** States and, for each of them, the currently stored condition for stuttering at it. */
  private final Map<GraphMLState, AutomatonBoolExpr> stutterConditions = Maps.newHashMap();

  /** Automaton variables by their names. */
  private final Map<String, AutomatonVariable> automatonVariables = new HashMap<>();

  /**
   * States (represented in the GraphML model) and the call stack at each of them, for each thread.
   */
  private final Map<Integer, Map<GraphMLState, Deque<String>>> stacks = Maps.newHashMap();

  /**
   * States (represented in the GraphML model) and the transitions leaving them (in our automaton
   * model).
   */
  private final Map<GraphMLState, LinkedList<AutomatonTransition>> stateTransitions =
      Maps.newHashMap();

  /**
   * Initializes a new {@link AutomatonGraphmlParserState}.
   *
   * @param pAutomatonName the name of the witness automaton.
   * @param pWitnessType the type of the witness automaton.
   * @param pSpecificationTypes the specification types.
   * @param pStates the states.
   * @param pEnteringTransitions the transitions entering states.
   * @param pLeavingTransitions the transitions leaving states.
   * @throws WitnessParseException if there is not exactly one entry state.
   */
  private AutomatonGraphmlParserState(
      String pAutomatonName,
      WitnessType pWitnessType,
      ImmutableSet<PropertyType> pSpecificationTypes,
      ImmutableSet<GraphMLState> pStates,
      ImmutableMultimap<GraphMLState, GraphMLTransition> pEnteringTransitions,
      ImmutableMultimap<GraphMLState, GraphMLTransition> pLeavingTransitions)
      throws WitnessParseException {

    automatonName = Objects.requireNonNull(pAutomatonName);
    witnessType = Objects.requireNonNull(pWitnessType);
    specificationTypes = Objects.requireNonNull(pSpecificationTypes);

    FluentIterable<GraphMLState> filterableStates = FluentIterable.from(pStates);

    // Determine the entry state
    FluentIterable<GraphMLState> entryStates = filterableStates.filter(GraphMLState::isEntryState);
    if (entryStates.size() != 1) {
      throw new WitnessParseException(
          "There must be exactly one entry state. Found entry states: " + entryStates);
    }
    entryState = Objects.requireNonNull(entryStates.get(0));

    states = Objects.requireNonNull(pStates);
    enteringTransitions = Objects.requireNonNull(pEnteringTransitions);
    leavingTransitions = Objects.requireNonNull(pLeavingTransitions);

    // Determine distances to violation states
    distances =
        determineDistanceToViolation(
            pEnteringTransitions,
            filterableStates.filter(GraphMLState::isViolationState),
            filterableStates.filter(GraphMLState::isSinkState));
  }

  /**
   * Compute the distances from automaton states to violation states.
   *
   * <p>Violation states have a distance of {@code -1}, their predecessor states have distance
   * {@code 1}, and so on. The infinite distance of states with no path to a violation state is
   * represented by the value {@code -1}.
   *
   * @param pEnteringTransitions a map describing the witness automaton by providing a mapping from
   *     states to transitions entering those states.
   * @return a map from automaton-state identifiers to their distances to the next violation state.
   */
  private static Map<GraphMLState, Integer> determineDistanceToViolation(
      Multimap<GraphMLState, GraphMLTransition> pEnteringTransitions,
      Iterable<GraphMLState> pViolationStates,
      Iterable<GraphMLState> pSinkStates) {
    Queue<GraphMLState> waitlist = new ArrayDeque<>();
    Map<GraphMLState, Integer> distances = Maps.newHashMap();
    for (GraphMLState violationState : pViolationStates) {
      waitlist.add(violationState);
      distances.put(violationState, 0);
    }
    while (!waitlist.isEmpty()) {
      GraphMLState current = waitlist.poll();
      int newDistance = distances.get(current) + 1;
      for (GraphMLTransition enteringTransition : pEnteringTransitions.get(current)) {
        GraphMLState sourceState = enteringTransition.getSource();
        Integer oldDistance = distances.get(sourceState);
        if (oldDistance == null || oldDistance > newDistance) {
          distances.put(enteringTransition.getSource(), newDistance);
          waitlist.offer(enteringTransition.getSource());
        }
      }
    }
    // Sink nodes have infinite distance to the target location, encoded as -1
    for (GraphMLState sinkStateId : pSinkStates) {
      distances.put(sinkStateId, -1);
    }
    return distances;
  }

  /**
   * Initializes a new {@link AutomatonGraphmlParserState}.
   *
   * @param pAutomatonName the name of the witness automaton.
   * @param pWitnessType the type of the witness automaton.
   * @param pSpecificationTypes the specification types.
   * @param pStates the states.
   * @param pEnteringTransitions the transitions entering states.
   * @param pLeavingTransitions the transitions leaving states.
   * @throws WitnessParseException if there is not exactly one entry state.
   * @return the initialized {@link AutomatonGraphmlParserState}.
   */
  public static AutomatonGraphmlParserState initialize(
      String pAutomatonName,
      WitnessType pWitnessType,
      Set<PropertyType> pSpecificationTypes,
      Iterable<GraphMLState> pStates,
      Multimap<GraphMLState, GraphMLTransition> pEnteringTransitions,
      Multimap<GraphMLState, GraphMLTransition> pLeavingTransitions)
      throws WitnessParseException {
    return new AutomatonGraphmlParserState(
        pAutomatonName,
        pWitnessType,
        ImmutableSet.copyOf(pSpecificationTypes),
        ImmutableSet.copyOf(pStates),
        ImmutableMultimap.copyOf(pEnteringTransitions),
        ImmutableMultimap.copyOf(pLeavingTransitions));
  }

  /**
   * Gets the name of the witness automaton.
   *
   * @return the name of the witness automaton.
   */
  public String getAutomatonName() {
    return automatonName;
  }

  /**
   * Gets the type of the witness automaton.
   *
   * @return the type of the witness automaton.
   */
  public WitnessType getWitnessType() {
    return witnessType;
  }

  /**
   * Gets the specification types.
   *
   * @return the specification types.
   */
  public ImmutableSet<PropertyType> getSpecificationTypes() {
    return specificationTypes;
  }

  /**
   * Gets the entry state.
   *
   * @return the entry state.
   */
  public GraphMLState getEntryState() {
    return entryState;
  }

  /**
   * Gets the states.
   *
   * @return the states.
   */
  public ImmutableSet<GraphMLState> getStates() {
    return states;
  }

  /**
   * Gets the transitions entering states, represented in the GraphML model.
   *
   * @return the transitions entering states, represented in the GraphML model.
   */
  public ImmutableMultimap<GraphMLState, GraphMLTransition> getEnteringTransitions() {
    return enteringTransitions;
  }

  /**
   * Gets the transitions leaving states, represented in the GraphML model.
   *
   * @return the transitions leaving states, represented in the GraphML model.
   */
  public ImmutableMultimap<GraphMLState, GraphMLTransition> getLeavingTransitions() {
    return leavingTransitions;
  }

  /**
   * Gets for a given state (in the GraphML model) the distance to the next violation state.
   *
   * @return the distance of the given state to the next violation state.
   */
  public int getDistance(GraphMLState pState) {
    Integer distance = distances.get(pState);
    if (distance == null) {
      return Integer.MAX_VALUE;
    }
    return distance;
  }

  /**
   * Gets the currently stored mapping from states to the conditions for stuttering at the
   * corresponding states.
   *
   * @return the currently stored mapping from states to the conditions for stuttering at the
   *     corresponding states.
   */
  public Map<GraphMLState, AutomatonBoolExpr> getStutterConditions() {
    return stutterConditions;
  }

  /**
   * Gets the automaton variables by their names.
   *
   * @return the automaton variables by their names.
   */
  public Map<String, AutomatonVariable> getAutomatonVariables() {
    return automatonVariables;
  }

  /**
   * Gets the call stacks currently stored for the states (represented in the GraphML model) for the
   * given thread.
   *
   * @param pThreadId the thread identifier.
   * @return the call stacks currently stored for the states (represented in the GraphML model) for
   *     the given thread.
   */
  private Map<GraphMLState, Deque<String>> getOrCreateThreadStacks(int pThreadId) {
    Map<GraphMLState, Deque<String>> threadStacks = stacks.get(pThreadId);
    if (threadStacks == null) {
      threadStacks = Maps.newHashMap();
      stacks.put(pThreadId, threadStacks);
    }
    return threadStacks;
  }

  /**
   * Gets the call stack currently stored for the given thread and state (represented in the GraphML
   * model).
   *
   * @param pThreadId the thread identifier.
   * @param pState the state, represented in the GraphML model.
   * @return the call stack currently stored for the given thread and state (represented in the
   *     GraphML model).
   */
  public Deque<String> getOrCreateStack(int pThreadId, GraphMLState pState) {
    Objects.requireNonNull(pState);
    Map<GraphMLState, Deque<String>> threadStacks = getOrCreateThreadStacks(pThreadId);
    Deque<String> stack = threadStacks.get(pState);
    if (stack == null) {
      stack = Queues.newArrayDeque();
      threadStacks.put(pState, stack);
    }
    return stack;
  }

  /**
   * Stores the given call stack for the given thread and GraphML state (represented in the GraphML
   * model).
   *
   * @param pThreadId the thread identifier.
   * @param pState the state, represented in the GraphML model.
   * @param pStack the call stack.
   */
  public void putStack(int pThreadId, GraphMLState pState, Deque<String> pStack) {
    Objects.requireNonNull(pStack);
    getOrCreateThreadStacks(pThreadId).put(pState, pStack);
  }

  /**
   * Gets the currently collected list of state transitions (in our automaton model) leaving each
   * states (represented in the GraphML model).
   *
   * @return the currently collected list of state transitions (in our automaton model) leaving each
   *     states (represented in the GraphML model).
   */
  public Map<GraphMLState, LinkedList<AutomatonTransition>> getStateTransitions() {
    return stateTransitions;
  }

  /**
   * Checks if the entry state is connected to a violation state.
   *
   * @return {@code true} if the entry state is connected to a violation state,
   * {@code false} otherwise.
   */
  public boolean isEntryConnectedToViolation() {
    return distances.get(getEntryState()) != null;
  }
}
