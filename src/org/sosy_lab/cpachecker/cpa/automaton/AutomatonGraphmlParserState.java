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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.GraphMLTransition.GraphMLThread;
import org.sosy_lab.cpachecker.util.SpecificationProperty.PropertyType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class AutomatonGraphmlParserState {

  private static final String CLONED_FUNCTION_INFIX = "__cloned_function__";

  protected static final Pattern CLONED_FUNCTION_NAME_PATTERN =
      Pattern.compile("(.+)(" + CLONED_FUNCTION_INFIX + ")(\\d+)");

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
  private final Map<GraphMLTransition.GraphMLThread, Map<GraphMLState, Deque<String>>> stacks =
      Maps.newHashMap();

  /** The names of all functions available in the CFA. */
  private final Set<String> functionNames;

  /**
   * A mapping from function names to names of their copies (in the context of concurrency).
   * Populated only on demand.
   */
  private final Multimap<String, FunctionInstance> functionCopies = TreeMultimap.create();

  /** The functions currently occupied by each thread. */
  private final Multimap<GraphMLTransition.GraphMLThread, FunctionInstance> occupiedFunctions =
      TreeMultimap.create();

  /**
   * States (represented in the GraphML model) and the transitions leaving them (in our automaton
   * model).
   */
  private final Map<GraphMLState, List<AutomatonTransition>> stateTransitions = Maps.newHashMap();

  /**
   * Initializes a new {@link AutomatonGraphmlParserState}.
   *
   * @param pAutomatonName the name of the witness automaton.
   * @param pWitnessType the type of the witness automaton.
   * @param pSpecificationTypes the specification types.
   * @param pStates the states.
   * @param pEnteringTransitions the transitions entering states.
   * @param pLeavingTransitions the transitions leaving states.
   * @param pFunctionNames the names of all functions available in the CFA.
   * @throws WitnessParseException if there is not exactly one entry state.
   */
  private AutomatonGraphmlParserState(
      String pAutomatonName,
      WitnessType pWitnessType,
      ImmutableSet<PropertyType> pSpecificationTypes,
      ImmutableSet<GraphMLState> pStates,
      ImmutableMultimap<GraphMLState, GraphMLTransition> pEnteringTransitions,
      ImmutableMultimap<GraphMLState, GraphMLTransition> pLeavingTransitions,
      ImmutableSet<String> pFunctionNames)
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

    functionNames = Objects.requireNonNull(pFunctionNames);
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
   * @param pFunctionNames the names of all functions available in the CFA.
   * @throws WitnessParseException if there is not exactly one entry state.
   * @return the initialized {@link AutomatonGraphmlParserState}.
   */
  public static AutomatonGraphmlParserState initialize(
      String pAutomatonName,
      WitnessType pWitnessType,
      Set<PropertyType> pSpecificationTypes,
      Iterable<GraphMLState> pStates,
      Multimap<GraphMLState, GraphMLTransition> pEnteringTransitions,
      Multimap<GraphMLState, GraphMLTransition> pLeavingTransitions,
      Set<String> pFunctionNames)
      throws WitnessParseException {
    return new AutomatonGraphmlParserState(
        pAutomatonName,
        pWitnessType,
        ImmutableSet.copyOf(pSpecificationTypes),
        ImmutableSet.copyOf(pStates),
        ImmutableMultimap.copyOf(pEnteringTransitions),
        ImmutableMultimap.copyOf(pLeavingTransitions),
        ImmutableSet.copyOf(pFunctionNames));
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
   * @param pThread the thread identifier andname.
   * @return the call stacks currently stored for the states (represented in the GraphML model) for
   *     the given thread.
   */
  private Map<GraphMLState, Deque<String>> getOrCreateThreadStacks(
      GraphMLTransition.GraphMLThread pThread) {
    Map<GraphMLState, Deque<String>> threadStacks = stacks.get(pThread);
    if (threadStacks == null) {
      threadStacks = Maps.newHashMap();
      stacks.put(pThread, threadStacks);
    }
    return threadStacks;
  }

  /**
   * Gets the call stack currently stored for the given thread and state (represented in the GraphML
   * model).
   *
   * @param pThread the thread identifier and name.
   * @param pState the state, represented in the GraphML model.
   * @return the call stack currently stored for the given thread and state (represented in the
   *     GraphML model).
   */
  public Deque<String> getOrCreateStack(GraphMLTransition.GraphMLThread pThread, GraphMLState pState) {
    Objects.requireNonNull(pState);
    Map<GraphMLState, Deque<String>> threadStacks = getOrCreateThreadStacks(pThread);
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
   * @param pThread the thread identifier and name.
   * @param pState the state, represented in the GraphML model.
   * @param pStack the call stack.
   */
  public void putStack(
      GraphMLTransition.GraphMLThread pThread, GraphMLState pState, Deque<String> pStack) {
    Objects.requireNonNull(pStack);
    getOrCreateThreadStacks(pThread).put(pState, pStack);
  }

  /**
   * Tries to obtain (a copy of) the given function for the given thread.
   *
   * @param pThread the thread identifier and name.
   * @param pDesiredFunctionName the original name of the desired function.
   * @return (a copy of) the given function, or {@code Optional.absent()} if all existing copies of
   *     this function are already occupied by other threads.
   */
  public Optional<String> getFunctionForThread(
      GraphMLTransition.GraphMLThread pThread, String pDesiredFunctionName) {
    // If the function does not exist, we cannot provide it to the caller anyway
    if (!functionNames.contains(pDesiredFunctionName)) {
      return Optional.empty();
    }

    // If we have not yet computed the equivalence classes, we can try if we can do without them
    if (functionCopies.isEmpty()
        && (occupiedFunctions.isEmpty() || occupiedFunctions.keySet().contains(pThread))) {
      FunctionInstance originalFunction = new FunctionInstance(pDesiredFunctionName);

      // If the thread already owns the function, we can trivially hand it out
      if (occupiedFunctions.get(pThread).contains(originalFunction)) {
        return Optional.of(pDesiredFunctionName);
      }
      // If the function is available, we mark it as occupied and hand it out
      if (!occupiedFunctions.values().contains(originalFunction)) {
        if (occupy(pThread, originalFunction)) {
          return Optional.of(pDesiredFunctionName);
        }
      }

    }

    // If the previous checks were unsuccessful, we need the equivalence classes, so we can not
    // delay computing them any longer
    computeFunctionCloneEquivalenceClasses();

    // Check the equivalence class of this function
    for (FunctionInstance functionInstance : functionCopies.get(pDesiredFunctionName)) {
      // If the thread already owns the instance, we can trivially hand it out
      if (occupiedFunctions.get(pThread).contains(functionInstance)) {
        return Optional.of(functionInstance.getCloneName());
      }
      // If the function instance is available, we mark it as occupied and hand it out
      if (!occupiedFunctions.values().contains(functionInstance)) {
        if (!occupy(pThread, functionInstance)) {
          return Optional.empty();
        }
        return Optional.of(functionInstance.getCloneName());
      }
    }

    // If the function is not available, we cannot hand it out
    return Optional.empty();
  }

  private void computeFunctionCloneEquivalenceClasses() {
    if (functionCopies.isEmpty()) {
      for (String functionName : functionNames) {
        FunctionInstance functionInstance = parseFunctionInstance(functionName);
        functionCopies.put(functionInstance.originalName, functionInstance);
      }
      // If any function is already reserved for a thread, it must be the main
      // thread; reserve all other "original" functions for it.
      if (!occupiedFunctions.isEmpty()) {
        assert occupiedFunctions.keySet().size() == 1;
        GraphMLThread originalFunctionThread = occupiedFunctions.keySet().iterator().next();
        for (String originalName : functionCopies.keySet()) {
          FunctionInstance originalFunction = new FunctionInstance(originalName);
          occupiedFunctions.put(originalFunctionThread, originalFunction);
        }
      }
    }
  }

  private boolean occupy(GraphMLThread pThread, FunctionInstance pFunctionInstance) {
    if (functionCopies.isEmpty()) {
      occupiedFunctions.put(pThread, pFunctionInstance);
      return true;
    }
    Collection<FunctionInstance> copies = Lists.newArrayListWithCapacity(5);
    boolean desiredInstanceAvailable = false;
    for (String originalName : functionCopies.keySet()) {
      FunctionInstance copy = new FunctionInstance(originalName, pFunctionInstance.cloneNumber);
      if (functionCopies.containsEntry(originalName, copy)) {
        if (!occupiedFunctions.get(pThread).contains(copy)
            && occupiedFunctions.values().contains(copy)) {
          return false;
        }
        if (copies.add(copy) && copy.equals(pFunctionInstance)) {
          desiredInstanceAvailable = true;
        }
      }
    }
    if (!desiredInstanceAvailable) {
      return false;
    }
    for (FunctionInstance copy : copies) {
      occupiedFunctions.put(pThread, copy);
    }
    return true;
  }

  /**
   * Releases all functions occupied by the given thread.
   *
   * @param pThread the thread identifier and name.
   */
  public void releaseFunctions(GraphMLTransition.GraphMLThread pThread) {
    occupiedFunctions.removeAll(pThread);
  }

  /**
   * Gets the currently collected list of state transitions (in our automaton model) leaving the
   * given state.
   *
   * @param pGraphMLState the state to get the leaving transitions for.
   * @return the currently collected list of state transitions (in our automaton model) leaving the
   *     given state (represented in the GraphML model).
   */
  public List<AutomatonTransition> getStateTransitions(GraphMLState pGraphMLState) {
    List<AutomatonTransition> result = stateTransitions.get(pGraphMLState);
    if (result == null) {
      result = new ArrayList<>(4);
      stateTransitions.put(pGraphMLState, result);
    }
    return result;
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

  private static class FunctionInstance implements Comparable<FunctionInstance> {

    private final String originalName;

    private final int cloneNumber;

    public FunctionInstance(String pOriginalName) {
      this(pOriginalName, Integer.MIN_VALUE);
    }

    public FunctionInstance(String pOriginalName, int pCloneNumber) {
      originalName = Objects.requireNonNull(pOriginalName);
      cloneNumber = pCloneNumber;
    }

    public boolean isOriginal() {
      return cloneNumber == Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof FunctionInstance) {
        FunctionInstance other = (FunctionInstance) pOther;
        return originalName.equals(other.originalName)
            && cloneNumber == other.cloneNumber;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(originalName, cloneNumber);
    }

    public String getCloneName() {
      if (isOriginal()) {
        return originalName;
      }
      return originalName + CLONED_FUNCTION_INFIX + cloneNumber;
    }

    @Override
    public String toString() {
      return getCloneName();
    }

    @Override
    public int compareTo(FunctionInstance pOther) {
      return ComparisonChain.start()
          .compare(cloneNumber, pOther.cloneNumber)
          .compare(originalName, pOther.originalName)
          .result();
    }
  }

  private static FunctionInstance parseFunctionInstance(String pFunctionName) {
    Matcher matcher = CLONED_FUNCTION_NAME_PATTERN.matcher(pFunctionName);
    if (!matcher.matches()) {
      return new FunctionInstance(pFunctionName);
    }
    return new FunctionInstance(matcher.group(1), Integer.parseInt(matcher.group(3)));
  }
}
