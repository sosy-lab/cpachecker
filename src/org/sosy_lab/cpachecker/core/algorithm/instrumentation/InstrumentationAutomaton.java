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
  // Subset of liveVariables but those that are not yet declared
  private ImmutableMap<String, String> undeclaredVariables;
  private ImmutableList<InstrumentationTransition> instrumentationTransitions;
  private InstrumentationState initialState;

  /** Currently supported properties with encoded automata. */
  enum InstrumentationProperty {
    TERMINATION,
    TERMINATIONWITHCOUNTERS,
    ONESTEPREACHABILITY,
    NOOVERFLOW,
    MEMCLEANUP
  }

  /** The annotation is used to match a property of a CFA node. */
  enum StateAnnotation {
    TRUE,
    LOOPHEAD,
    INIT,
    FUNCTIONHEAD,
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
   * @param pInstrumentationProperty temporary indication of which property is used in the
   *     transformation
   * @param pLiveVariablesAndTypes the mapping from variable names used, but not declared, in a loop
   *     to their types
   */
  public InstrumentationAutomaton(
      InstrumentationProperty pInstrumentationProperty,
      ImmutableMap<String, String> pLiveVariablesAndTypes,
      ImmutableMap<String, String> pUndeclaredVariables,
      int pIndex) {
    this.liveVariablesAndTypes = pLiveVariablesAndTypes;
    this.undeclaredVariables = pUndeclaredVariables;

    switch (pInstrumentationProperty) {
      case TERMINATION -> constructTerminationAutomaton(pIndex);
      case TERMINATIONWITHCOUNTERS -> constructTerminationWithCountersAutomaton(pIndex);
      case ONESTEPREACHABILITY -> constructOneStepReachabilityAutomaton(pIndex);
      case NOOVERFLOW -> constructOverflowAutomaton();
      case MEMCLEANUP -> constructMemCleanupAutomaton();
      default -> throw new IllegalArgumentException();
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
    StringBuilder originalType = new StringBuilder();
    for (int i = 0; i < pType.length(); i++) {
      if (pType.charAt(i) == '*') {
        return originalType.toString();
      }
      originalType.append(pType.charAt(i));
    }
    return originalType.toString();
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
                "int TRANS_INT_MAX = 2147483647; int TRANS_INT_MIN = -2147483648;"
                    + " long long TRANS_LONG_LONG_MIN = -9223372036854775807;"
                    + " long long TRANS_LONG_LONG_MAX = 9223372036854775807;"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ADD"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!((((x_instr_2) > 0) && ((x_instr_1) >"
                    + " (x_instr_4 - (x_instr_2)))) || (((x_instr_2) < 0) && ((x_instr_1) <"
                    + " (x_instr_5 - (x_instr_2)))))); }"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("SUB"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!((((x_instr_2) > 0 && (x_instr_1) < x_instr_5"
                    + " + (x_instr_2)) || ((x_instr_2) < 0 && (x_instr_1) > x_instr_4 +"
                    + " (x_instr_2)))));}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t4 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("MUL"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!((((x_instr_1) > 0) && ((x_instr_2) > 0) &&"
                    + " ((x_instr_1) > (x_instr_4 / (x_instr_2))))|| (((x_instr_1) > 0) &&"
                    + " ((x_instr_2) <= 0) && ((x_instr_2) < (x_instr_5 / (x_instr_1))))||"
                    + " (((x_instr_1) <= 0) && ((x_instr_2) > 0) && ((x_instr_1) < (x_instr_5 /"
                    + " (x_instr_2))))|| (((x_instr_1) <= 0) && ((x_instr_2) <= 0) && ((x_instr_1)"
                    + " != 0 && ((x_instr_2) < (x_instr_4 / (x_instr_1))))))); }"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t5 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("DIV"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!(((x_instr_2) == 0) || (((x_instr_1) =="
                    + " x_instr_5) && ((x_instr_2) == -1))));}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t6 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("MOD"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!(((x_instr_2) == 0) || (((x_instr_1) =="
                    + " x_instr_5) && ((x_instr_2) == -1))));}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t7 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("SHIFT"),
            new InstrumentationOperation(
                "if (x_instr_3) { __VERIFIER_assert(!(((x_instr_1) < 0) || ((x_instr_2) < 0) ||"
                    + "((x_instr_2) >= x_instr_4) ||"
                    + "((x_instr_1) > (x_instr_4 >> (x_instr_2))))); }"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t8 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("EQ"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t9 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("GEQ"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t10 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("GR"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t11 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("LEQ"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t12 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("LS"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t13 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("NEQ"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t14 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("RSHIFT"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t15 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("AND"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t16 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("OR"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t17 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("XOR"),
            new InstrumentationOperation(""),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t18 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("NEG"),
            new InstrumentationOperation(
                "if((x_instr_2)) { __VERIFIER_assert(!((x_instr_1) == x_instr_4));}"),
            InstrumentationOrder.BEFORE,
            q2);

    this.instrumentationTransitions =
        ImmutableList.of(
            t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18);
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
                "; int saved_"
                    + pIndex
                    + " = 0; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                entry.getValue()
                                    + " "
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + (entry.getKey().charAt(0) == '*'
                                        ? " = alloca(sizeof("
                                            + getAllocationForPointer(entry.getValue())
                                            + "))"
                                        : ""))
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? ";" : "")),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("[cond]"),
            new InstrumentationOperation(
                "if(__VERIFIER_nondet_int() && saved_"
                    + pIndex
                    + " == 0) { saved_"
                    + pIndex
                    + " =1; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + " = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? "; " : "")
                    + "} else { __VERIFIER_assert((saved_"
                    + pIndex
                    + " == 0)"
                    + (!liveVariablesAndTypes.isEmpty() ? " || " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "("
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + " != "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + ")")
                        .collect(Collectors.joining("||"))
                    + ");}"),
            InstrumentationOrder.AFTER,
            q3);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q3,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(""),
            InstrumentationOrder.AFTER,
            q3);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3);
  }

  private void constructTerminationWithCountersAutomaton(int pIndex) {
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.FUNCTIONHEAD, this);
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q4 = new InstrumentationState("q4", StateAnnotation.FALSE, this);
    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                (pIndex == 0 ? "; int saved = 0; int pc = 0; int pc_INSTR = 0; " : "")
                    + undeclaredVariables.entrySet().stream()
                        .map(
                            (entry) ->
                                entry.getValue()
                                    + " "
                                    + entry.getKey()
                                    + "_INSTR"
                                    + (entry.getKey().charAt(0) == '*'
                                        ? " = alloca(sizeof("
                                            + getAllocationForPointer(entry.getValue())
                                            + "))"
                                        : ""))
                        .collect(Collectors.joining("; "))
                    + (!undeclaredVariables.isEmpty() ? ";" : "")),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("[cond]"),
            new InstrumentationOperation(
                "pc = "
                    + pIndex
                    + "; "
                    + "if(__VERIFIER_nondet_int() && saved"
                    + " == 0) { saved"
                    + " =1; pc_INSTR = pc; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR"
                                    + " = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? "; " : "")
                    + "} else { __VERIFIER_assert((saved"
                    + " == 0) || (pc_INSTR != pc)"
                    + (!liveVariablesAndTypes.isEmpty() ? " || " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "("
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + " != "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR"
                                    + ")")
                        .collect(Collectors.joining("||"))
                    + ");}"),
            InstrumentationOrder.AFTER,
            q3);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("[cond]"),
            new InstrumentationOperation(
                "if (saved == 0 && !((pc_INSTR == pc)"
                    + (!liveVariablesAndTypes.isEmpty() ? " && " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "("
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + " == "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR"
                                    + ")")
                        .collect(Collectors.joining("&&"))
                    + ")){abort();}"),
            InstrumentationOrder.BEFORE,
            q4);
    InstrumentationTransition t4 =
        new InstrumentationTransition(
            q4,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(""),
            InstrumentationOrder.AFTER,
            q4);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3, t4);
  }

  private void constructOneStepReachabilityAutomaton(int pIndex) {
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.FALSE, this);
    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation("int first_INSTR_" + pIndex + " = 0;"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("[cond]"),
            new InstrumentationOperation("first_INSTR_" + pIndex + " = 1;"),
            InstrumentationOrder.AFTER,
            q3);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q3,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(""),
            InstrumentationOrder.AFTER,
            q3);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3);
  }

  private void constructMemCleanupAutomaton() {
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.TRUE, this);
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.INIT, this);

    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                "extern void *realloc(void *ptr, long unsigned int new_size );\\n"
                    + " extern void *malloc (long unsigned int __size);\\n"
                    + " extern void *calloc(long unsigned int nitems, long unsigned int size);\\n"
                    + " extern void free(void *ptr);\\n"
                    + " void *__instrumentation_track_allocated_pointer = 0;\\n"
                    + " unsigned long long __instrumentation_track_allocated_length = 0;\\n"
                    + " void __instrumentation_memory_free(void *ptr)\\n"
                    + " {\\n"
                    + "   free(ptr);\\n"
                    + "   if (ptr == __instrumentation_track_allocated_pointer)\\n"
                    + "   {\\n"
                    + "     __instrumentation_track_allocated_pointer = 0;\\n"
                    + "     __instrumentation_track_allocated_length = 0;\\n"
                    + "   }\\n"
                    + " }\\n"
                    + " void *__instrumentation_memory_malloc(long unsigned int size)\\n"
                    + " {\\n"
                    + "   void *p = malloc(size);\\n"
                    + "   if (__VERIFIER_nondet_int())\\n"
                    + "   {\\n"
                    + "     __instrumentation_track_allocated_pointer = p;\\n"
                    + "     __instrumentation_track_allocated_length = size;\\n"
                    + "   }\\n"
                    + "   return p;\\n"
                    + " }\\n"
                    + " \\n"
                    + " void *__instrumentation_memory_calloc(long unsigned int nitems, long"
                    + " unsigned int size)\\n"
                    + " {\\n"
                    + "   void *p = calloc(nitems, size);\\n"
                    + "   if (__VERIFIER_nondet_int())\\n"
                    + "   {\\n"
                    + "     __instrumentation_track_allocated_pointer = p;\\n"
                    + "     __instrumentation_track_allocated_length = size;\\n"
                    + "   }\\n"
                    + "   return p;\\n"
                    + " }\\n"
                    + " \\n"
                    + " void *__instrumentation_memory_realloc(void *ptr, long unsigned int"
                    + " size)\\n"
                    + " {\\n"
                    + "   void *p = realloc(ptr, size);\\n"
                    + "   if (__VERIFIER_nondet_int() || (__instrumentation_track_allocated_pointer"
                    + " == ptr))\\n"
                    + "   {\\n"
                    + "     __instrumentation_track_allocated_pointer = p;\\n"
                    + "     __instrumentation_track_allocated_length = size;\\n"
                    + "   }\\n"
                    + "   return p;\\n"
                    + " }\\n"
                    + " "),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(free)"),
            new InstrumentationOperation("__instrumentation_memory_free"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(malloc)"),
            new InstrumentationOperation("__instrumentation_memory_malloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t4 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(calloc)"),
            new InstrumentationOperation("__instrumentation_memory_calloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t5 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(realloc)"),
            new InstrumentationOperation("__instrumentation_memory_realloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t6 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(abort)"),
            new InstrumentationOperation(
                "if (__instrumentation_track_allocated_pointer != 0)" + "{reach_error();}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t7 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(exit)"),
            new InstrumentationOperation(
                "if (__instrumentation_track_allocated_pointer != 0)" + "{reach_error();}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t8 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(return 0;)"),
            new InstrumentationOperation(
                "if (__instrumentation_track_allocated_pointer != 0)" + "{reach_error();}"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t9 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(reach_error)"),
            new InstrumentationOperation(
                "if (__instrumentation_track_allocated_pointer != 0)" + "{reach_error();}"),
            InstrumentationOrder.BEFORE,
            q2);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3, t4, t5, t6, t7, t8, t9);
  }
}
