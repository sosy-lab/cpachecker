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
  private ImmutableMap<String, String> liveVariablesAndTypes;
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
    INIT,
    FALSE
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

    if (pInstrumentationProperty == InstrumentationProperty.NOOVERFLOW) {
      constructOverflowAutomaton();
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

  private String getAllocationForPointer(String pType) {
    String originalType = "";
    for (int i = 0; i < pType.length(); i++) {
      if (pType.charAt(i) == '*') {
        return originalType;
      }
      originalType = originalType + pType.charAt(i);
    }
    return originalType;
  }

  private void constructOverflowAutomaton() {
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.TRUE, this);
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.INIT, this);

    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                "int INT_MAX = 2147483647; int INT_MIN = -2147483648;"
            ),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ADD"),
            new InstrumentationOperation(
                "__VERIFIER_assert(((x2 > 0) && (x1 > (INT_MAX - x2))) "
                    + "|| ((x2 < 0) && (x1 < (INT_MIN - x2))));"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("SUB"),
            new InstrumentationOperation(
                "__VERIFIER_assert(((x2 > 0 && x1 < INT_MIN + x2) ||"
                    + " (x2 < 0 && x1 > INT_MAX + x2)));"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t4 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("MUL"),
            new InstrumentationOperation(
                "__VERIFIER_assert((!((x1 > 0) && (x2 > 0)) || !(x1 > (INT_MAX/x2)))"
                          + " && (!((x1 > 0) && (x2 <= 0)) || !(x2 < (INT_MAX/x1)))"
                          + " && (!((x1 <= 0) && (x2 > 0)) || !(x1 < (INT_MAX/x2)))"
                          + " && (!((x1 <= 0) && (x2 <= 0)) || !((x1 != 0) && x2 > (INT_MAX/x1))));"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t5 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("DIV"),
            new InstrumentationOperation(
                "__VERIFIER_assert(!((x2 == 0) || ((x1 == INT_MIN) && (x2 == -1))));"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t6 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("MOD"),
            new InstrumentationOperation(
                "__VERIFIER_assert(!((x2 == 0) || ((x1 == INT_MIN) && (x2 == -1))));"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t7 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("SHIFT"),
            new InstrumentationOperation(
                "__VERIFIER_assert(!((x1 < 0) || (x2 < 0) ||"
                        + "(x2 >= INT_MAX) ||"
                        + "(x1 > (INT_MAX >> x2)))));"),
            InstrumentationOrder.BEFORE,
            q2);

    this.instrumentationTransitions =
        ImmutableList.of(t1, t2, t3, t4, t5, t6, t7);
  }

  private void constructTerminationAutomaton(int pIndex) {
      InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.LOOPHEAD, this);
      InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
      InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.FALSE, this);
      this.initialState = q1;

      InstrumentationTransition t1 =
          new InstrumentationTransition(
              q1,
              new InstrumentationPattern("true"),
              new InstrumentationOperation(
              "int saved_" + pIndex + " = 0; " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) ->
                          entry.getValue() + " " + entry.getKey() + "_instr_" + pIndex +
                              (entry.getValue().charAt(entry.getValue().length() - 1) == '*'
                               ? " = alloca(sizeof(" + getAllocationForPointer(entry.getValue()) + "))"
                               : ""))
                      .collect(Collectors.joining("; ")) +
                      (!liveVariablesAndTypes.isEmpty() ? ";" : "")),
              InstrumentationOrder.BEFORE,
              q2);
      InstrumentationTransition t2 =
          new InstrumentationTransition(
              q2,
              new InstrumentationPattern("[cond]"),
              new InstrumentationOperation("if(__VERIFIER_nondet_int() && saved_" + pIndex + " == 0) { saved_" + pIndex + " =1; " +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) ->
                          getDereferencesForPointer(entry.getValue()) + entry.getKey()
                              + "_instr_" + pIndex + " = " +
                          getDereferencesForPointer(entry.getValue()) + entry.getKey())
                      .collect(Collectors.joining("; ")) +
                  (!liveVariablesAndTypes.isEmpty() ? "; " : "") +
                  "} else { __VERIFIER_assert((saved_" + pIndex + " == 0)" +
                  (!liveVariablesAndTypes.isEmpty() ? " || " : "") +
                  liveVariablesAndTypes.entrySet().stream()
                      .map((entry) ->
                          "(" + getDereferencesForPointer(entry.getValue()) + entry.getKey()
                              + " != " + getDereferencesForPointer(entry.getValue()) + entry.getKey()
                              + "_instr_" + pIndex + ")")
                      .collect(Collectors.joining("||")) +
                  ");}"),
              InstrumentationOrder.AFTER,
              q3);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q3,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(""),
            InstrumentationOrder.AFTER,
            q3);
      this.instrumentationTransitions =
          ImmutableList.of(t1, t2, t3);
  }
}
