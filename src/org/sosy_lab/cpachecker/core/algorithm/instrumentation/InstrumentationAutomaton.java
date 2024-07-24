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
      ImmutableMap<String, String> pLiveVariablesAndTypes) {
    this.liveVariablesAndTypes = pLiveVariablesAndTypes;

    if (pInstrumentationProperty == InstrumentationProperty.TERMINATION) {
      InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.INIT);
      InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD);
      this.instrumentationStates = ImmutableList.of(q1, q2);

      InstrumentationTransition t1 =
          new InstrumentationTransition(
              q1,
              "true",
              "int saved = 0;\n" +
              liveVariablesAndTypes.entrySet().stream()
                  .map((entry) -> entry.getValue() + " " + entry.getKey() + "_instr")
                  .collect(Collectors.joining(";\n")),
              InstrumentationOrder.BEFORE,
              q2);
      InstrumentationTransition t2 =
          new InstrumentationTransition(
              q2,
              "[cond]",
              "__VERIFIER_nondet_int() && saved == 0 ? " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) -> entry.getKey() + " = " + entry.getKey() + "_instr")
                      .collect(Collectors.joining(";")) +
                  " : " + "__VERIFIER_assert((saved == 0) | " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) -> "(" + entry.getKey() + " != " + entry.getKey() + "_instr" + ")")
                      .collect(Collectors.joining("|")) +
                  ");",
              InstrumentationOrder.BEFORE,
              q2);
      this.instrumentationTransitions =
          ImmutableList.of(t1, t2);
    }
  }

  private class InstrumentationState {
    private String name;
    private StateAnnotation stateAnnotation;

    public InstrumentationState(String pName, StateAnnotation pStateAnnotation) {
      this.name = pName;
      this.stateAnnotation = pStateAnnotation;
    }

    public StateAnnotation getStateAnnotation() {
      return stateAnnotation;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private class InstrumentationTransition {
    private InstrumentationState source;
    private InstrumentationState destination;
    /**
     * TODO: Implement pattern class and matching (look for possible regexes?)
     */
    private String pattern;
    private String operation;
    private InstrumentationOrder order;

    public InstrumentationTransition(InstrumentationState pSource,
                              String pPattern,
                              String pOperation,
                              InstrumentationOrder pOrder,
                              InstrumentationState pDestination) {
      this.source = pSource;
      this.operation = pOperation;
      this.pattern = pPattern;
      this.order = pOrder;
      this.destination = pDestination;
    }

    @Override
    public String toString() {
      return source.toString() +
          " | " + pattern +
          " | " + operation +
          " | " + order.name() +
          " | " + destination.toString();
    }
  }
}
