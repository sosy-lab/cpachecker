// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Data structure that defines the required transformation of CFA. It is used in {@link
 * SequentializationOperatorAlgorithm}, which inserts the operations of its transitions into the
 * program.
 *
 * <p>The automaton is parsed from the string that TransVer creates from an automaton in a YAML
 * file. The string consists of the states and the transitions separated by " |||||". The states
 * have the form "q0|||LOOPHEAD||||, q1|||LOOPHEAD||||", i.e., the name and the annotation of every
 * state, and the transitions have the form "q0|||cond|||op|||AFTER|||q1||||, ...", i.e., the
 * source, the pattern, the operation, the order and the destination of every transition.
 */
public class InstrumentationAutomaton {
  private final ImmutableMap<String, String> liveVariablesAndTypes;
  // The live variables of all loops in the function, which share the ghost variables without index
  private final ImmutableMap<String, String> liveVariablesOfFunction;
  // Whether the operations before the loop are inserted at the beginning of its function
  private final boolean isInitializedAtFunctionHead;
  private final InstrumentationState initialState;
  private final ImmutableList<InstrumentationTransition> instrumentationTransitions;

  /** The annotation is used to match a property of a CFA node. */
  enum StateAnnotation {
    TRUE,
    LOOPHEAD,
    INIT,
    FUNCTIONHEADFORLOOP,
    MAINFUNCTIONHEAD,
    FALSE
  }

  /**
   * The order is used in each instrumentation transition to denote whether the operation should be
   * included after or before the original CFA transition.
   */
  enum InstrumentationOrder {
    AFTER,
    BEFORE,
    SAME_LINE // Careful to not change the original behavior of the program
  }

  /**
   * @param pParsedInstrumentationAutomaton the automaton parsed from a YAML file by TransVer
   * @param pLiveVariablesAndTypes the mapping from variable names used, but not declared, in a loop
   *     to their types
   * @param pLiveVariablesOfFunction the same mapping for all loops in the function of the loop, the
   *     ghost variables without index (__INSTR_init_in_scope()) are declared for these variables,
   *     because they are shared by all these loops
   * @param pIndex the index of the loop for which the automaton is constructed, it replaces
   *     INSTR_INDEX in the operations
   * @param pIsInitializedAtFunctionHead whether the operations before the loop are inserted at the
   *     beginning of its function instead of before the loop. The variables can be undeclared
   *     there, so the ghost variables (__INSTR_init_in_scope(INSTR_INDEX_init)) are not initialized
   *     with their values.
   * @throws CPAException if the automaton is malformed
   */
  public InstrumentationAutomaton(
      String pParsedInstrumentationAutomaton,
      ImmutableMap<String, String> pLiveVariablesAndTypes,
      ImmutableMap<String, String> pLiveVariablesOfFunction,
      int pIndex,
      boolean pIsInitializedAtFunctionHead)
      throws CPAException {
    liveVariablesAndTypes = pLiveVariablesAndTypes;
    liveVariablesOfFunction = pLiveVariablesOfFunction;
    isInitializedAtFunctionHead = pIsInitializedAtFunctionHead;
    List<String> statesAndTransitions =
        Splitter.on(" |||||").splitToList(pParsedInstrumentationAutomaton);
    if (statesAndTransitions.size() != 2) {
      throw new CPAException(
          "The instrumentation automaton must consist of states and transitions separated by"
              + " ' |||||'.");
    }
    try {
      ImmutableList<InstrumentationState> states = parseStates(statesAndTransitions.getFirst());
      initialState = states.getFirst();
      instrumentationTransitions = parseTransitions(statesAndTransitions.get(1), states, pIndex);
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      throw new CPAException("The instrumentation automaton is malformed: " + e.getMessage(), e);
    }
  }

  /**
   * Parses the list of states from a string with the following form: [q0,loop_head|||,
   * q1,loop_head|||, q2,ff|||] Every state is divided by the splitter |||, and it contains first
   * the name and then the annotation.
   */
  private ImmutableList<InstrumentationState> parseStates(String parsedStatesFromYAML) {
    ImmutableList.Builder<InstrumentationState> stateBuilder = ImmutableList.builder();

    for (String state :
        Splitter.on("||||, ").split(parsedStatesFromYAML.replace("[", "").replace("||||]", ""))) {
      stateBuilder.add(
          new InstrumentationState(
              Iterables.get(Splitter.on("|||").split(state), 0),
              StateAnnotation.valueOf(Iterables.get(Splitter.on("|||").split(state), 1)),
              this));
    }
    return stateBuilder.build();
  }

  /**
   * Parses the list of transitions from a string with the following form:
   * [q0|||cond|||op|||A|||q1|||...||||] Every transition is divided by the splitter |||, and it
   * contains the source state, pattern, operation, order and the destination state.
   */
  private ImmutableList<InstrumentationTransition> parseTransitions(
      String parsedTransitionsFromYAML, ImmutableList<InstrumentationState> states, int pIndex)
      throws CPAException {
    ImmutableList.Builder<InstrumentationTransition> transitionBuilder = ImmutableList.builder();
    for (String transition : Splitter.on("||||, ").split(parsedTransitionsFromYAML)) {
      Iterable<String> transitionComponents = Splitter.on("|||").split(transition);
      InstrumentationState source =
          findStateWithNameOrThrow(states, Iterables.get(transitionComponents, 0));
      InstrumentationState destination =
          findStateWithNameOrThrow(states, Iterables.get(transitionComponents, 4));
      transitionBuilder.add(
          new InstrumentationTransition(
              source,
              new InstrumentationPattern(Iterables.get(transitionComponents, 1)),
              parseOperation(Iterables.get(transitionComponents, 2), pIndex),
              InstrumentationOrder.valueOf(Iterables.get(transitionComponents, 3)),
              destination));
    }

    return transitionBuilder.build();
  }

  private InstrumentationOperation parseOperation(String operation, int pIndex) {
    operation = initializeReadWriteVariables(operation);
    operation = initializeGhostVariables(operation, pIndex);
    operation = assignGhostVariables(operation, pIndex);
    operation = assumeGhostVariables(operation, pIndex);
    operation = operation.replace("INSTR_INDEX", Integer.toString(pIndex));
    return new InstrumentationOperation(operation);
  }

  private String initializeReadWriteVariables(String operation) {
    return operation.replace(
        "__INSTR_init_read_write_vars();",
        liveVariablesAndTypes.entrySet().stream()
            .map(entry ->
                "int read_INSTR_"
                    + entry.getKey()
                    + " = 0; "
                    + "int write_INSTR_"
                    + entry.getKey()
                    + " = 0; ")
            .collect(Collectors.joining("; ")));
  }

  private String initializeGhostVariables(String operation, int pIndex) {
    String index;
    boolean initializeGhostVars;
    ImmutableMap<String, String> variables;
    if (operation.contains("__INSTR_init_in_scope(INSTR_INDEX_init)")) {
      operation =
          operation.replace("__INSTR_init_in_scope(INSTR_INDEX_init)", "__INSTR_init_in_scope()");
      index = Integer.toString(pIndex);
      initializeGhostVars = !isInitializedAtFunctionHead;
      variables = liveVariablesAndTypes;
    } else {
      index = "";
      initializeGhostVars = false;
      variables = liveVariablesOfFunction;
    }
    operation =
        operation.replace(
            "__INSTR_init_in_scope();",
            variables.entrySet().stream()
                    .map(entry ->
                        entry.getValue()
                            + " __INSTR_"
                            + entry.getKey()
                            + "_"
                            + index
                            + (initializeGhostVars
                                ? (" = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                                : ""))
                    .collect(Collectors.joining("; "))
                + (!variables.isEmpty() ? "; " : ""));
    return operation;
  }

  private String assumeGhostVariables(String operation, int pIndex) {
    String index;
    if (operation.contains("__INSTR_assume_in_scope(INSTR_INDEX_assume)")) {
      operation =
          operation.replace(
              "__INSTR_assume_in_scope(INSTR_INDEX_assume)", "__INSTR_assume_in_scope()");
      index = Integer.toString(pIndex);
    } else {
      index = "";
    }
    operation =
        operation.replace(
            "__INSTR_assume_in_scope()",
            liveVariablesAndTypes.entrySet().stream()
                .map(entry ->
                    "("
                        + getDereferencesForPointer(entry.getValue())
                        + entry.getKey()
                        + " != __INSTR_"
                        + getDereferencesForPointer(entry.getValue())
                        + entry.getKey()
                        + "_"
                        + index
                        + ")")
                .collect(Collectors.joining("||")));
    return operation;
  }

  private String assignGhostVariables(String operation, int pIndex) {
    String index;
    if (operation.contains("__INSTR_assign_in_scope(INSTR_INDEX_assign)")) {
      operation =
          operation.replace(
              "__INSTR_assign_in_scope(INSTR_INDEX_assign)", "__INSTR_assign_in_scope()");
      index = Integer.toString(pIndex);
    } else {
      index = "";
    }
    operation =
        operation.replace(
            "__INSTR_assign_in_scope();",
            liveVariablesAndTypes.entrySet().stream()
                    .map(entry ->
                        " __INSTR_"
                            + entry.getKey()
                            + "_"
                            + index
                            + " = "
                            + getDereferencesForPointer(entry.getValue())
                            + entry.getKey())
                    .collect(Collectors.joining("; "))
                + (!liveVariablesAndTypes.isEmpty() ? ";" : ""));
    return operation;
  }

  private InstrumentationState findStateWithNameOrThrow(
      ImmutableList<InstrumentationState> states, String name) throws CPAException {
    for (InstrumentationState state : states) {
      if (state.toString().equals(name)) {
        return state;
      }
    }
    throw new CPAException("Non-existing state " + name + " was used in transition!");
  }

  public InstrumentationState getInitialState() {
    return initialState;
  }

  public boolean isInitializedAtFunctionHead() {
    return isInitializedAtFunctionHead;
  }

  /** Decides whether some transition of the automaton has the given pattern. */
  public boolean hasTransitionWithPattern(String pPattern) {
    for (InstrumentationTransition transition : instrumentationTransitions) {
      if (transition.getPattern().toString().equals(pPattern)) {
        return true;
      }
    }
    return false;
  }

  public Set<InstrumentationTransition> getTransitions(InstrumentationState pState) {
    Set<InstrumentationTransition> transitions = new HashSet<>();
    for (InstrumentationTransition transition : instrumentationTransitions) {
      if (transition.getSource() == pState) {
        transitions.add(transition);
      }
    }
    return transitions;
  }

  /** Returns as many dereference operators as the type has trailing pointer stars. */
  private String getDereferencesForPointer(String pType) {
    String dereferences = "";
    for (int i = pType.length() - 1; i >= 0; i--) {
      if (pType.charAt(i) != '*') {
        return dereferences;
      }
      dereferences += "*";
    }
    return dereferences;
  }
}
