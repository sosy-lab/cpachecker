// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data structure that defines the required transformation of CFA. It is used in
 * InstrumentationOperatorAlgorithm and injects new transitions into an original CFA.
 */
public class InstrumentationAutomaton {
  private InstrumentationProperty instrumentationProperty;
  private ImmutableMap<String, String> liveVariablesAndTypes;
  private ImmutableList<InstrumentationState> instrumentationStates;
  private ImmutableList<InstrumentationTransition> instrumentationTransitions;
  private InstrumentationState initialState;

  /**
   * Currently supported properties with encoded automata.
   */
  enum InstrumentationProperty {
    TERMINATION,
    NOOVERFLOW
  }

  /**
   * The annotation is used to match a property of a CFA node.
   */
  enum StateAnnotation {
    TRUE,
    LOOPHEAD,
    INIT
  }

  /**
   * The order is used in each instrumentation transition to denote whether the operation should
   * be included after or before the original CFA transition.
   */
  enum InstrumentationOrder {
    AFTER,
    BEFORE
  }

  /**
   * @param pInstrumentationProperty temporary indication of which property is used in the
   *     transformation
   * @param pLiveVariablesAndTypes the mapping from variable names used, but not declared, in a loop
   *     to their types
   */
  public InstrumentationAutomaton(
      InstrumentationProperty pInstrumentationProperty,
      ImmutableMap<String, String> pLiveVariablesAndTypes,
      int pIndex) {
    this.liveVariablesAndTypes = pLiveVariablesAndTypes;

    if (pInstrumentationProperty == InstrumentationProperty.TERMINATION) {
      constructTerminationAutomaton(pIndex);
    }
  }

  public InstrumentationState getInitialState() {
    return initialState;
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

  private void constructTerminationAutomaton(int pIndex) {
      InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.LOOPHEAD, this);
      InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
      this.instrumentationStates = ImmutableList.of(q1, q2);
      this.initialState = q1;

      InstrumentationTransition t1 =
          new InstrumentationTransition(
              q1,
              "true",
              "int saved = 0; " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) -> entry.getValue() + " " + entry.getKey() + "_instr_" + pIndex)
                      .collect(Collectors.joining("; ")) + ";",
              InstrumentationOrder.BEFORE,
              q2);
      InstrumentationTransition t2 =
          new InstrumentationTransition(
              q2,
              "[cond]",
              "__VERIFIER_nondet_int() && saved == 0 ? " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) -> entry.getKey() + " = " + entry.getKey() + "_instr_" + pIndex)
                      .collect(Collectors.joining(";")) +
                  " : " + "__VERIFIER_assert((saved == 0) | " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) -> "(" + entry.getKey() + " != " + entry.getKey() + "_instr_" + pIndex + ")")
                      .collect(Collectors.joining("|")) +
                  ");",
              InstrumentationOrder.AFTER,
              q2);
      this.instrumentationTransitions =
          ImmutableList.of(t1, t2);
  }
}
