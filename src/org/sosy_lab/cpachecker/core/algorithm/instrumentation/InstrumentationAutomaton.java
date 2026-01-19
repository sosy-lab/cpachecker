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
    TERMINATIONWITHABORTS,
    ONESTEPREACHABILITY,
    NOOVERFLOW,
    DATA_RACE,
    MEMCLEANUP,
    VALID_DEREF,
    VALID_FREE,
    MEMTRACK,
    MEMSAFETY
  }

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
      case TERMINATIONWITHABORTS -> constructTerminationWithAbortsAutomaton(pIndex);
      case ONESTEPREACHABILITY -> constructOneStepReachabilityAutomaton(pIndex);
      case NOOVERFLOW -> constructOverflowAutomaton();
      case DATA_RACE -> constructDataRaceAutomaton();
      case MEMCLEANUP -> constructMemCleanupAutomaton();
      case VALID_DEREF -> constructValidDeref();
      case VALID_FREE -> constructValidFree();
      case MEMTRACK -> constructMemTrack();
      case MEMSAFETY -> constructMemSafety();
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

  private void constructDataRaceAutomaton() {
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.INIT, this);
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.TRUE, this);
    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "int read_INSTR_"
                                    + entry.getKey()
                                    + " = 0; "
                                    + "int write_INSTR_"
                                    + entry.getKey()
                                    + " = 0; ")
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? ";" : "")),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ADD"),
            new InstrumentationOperation(
                "__VERIFIER_atomic_begin();\\n"
                    + " if (x_instr_3) { __VERIFIER_assert((write_INSTR_x_instr_1 <= 0 &&"
                    + " write_INSTR_x_instr_2 <= 0)); }\\n"
                    + "write_INSTR_x_instr_1++; write_INSTR_x_instr_2++;\\n"
                    + "__VERIFIER_atomic_end();\\n"
                    + "__VERIFIER_atomic_begin();"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ADD"),
            new InstrumentationOperation(
                "write_INSTR_x_instr_1--; write_INSTR_x_instr_2--;\\n"
                    + "__VERIFIER_atomic_end();\\n"),
            InstrumentationOrder.AFTER,
            q2);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3);
  }

  private InstrumentationState initializeMemorySafety(
      ImmutableList.Builder<InstrumentationTransition> builder) {
    InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.TRUE, this);
    InstrumentationState q2 =
        new InstrumentationState("q2", StateAnnotation.MAINFUNCTIONHEAD, this);
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.INIT, this);

    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                "extern void *realloc(void *ptr, long unsigned int size );\\n"
                    + "extern void *malloc (long unsigned int size);\\n"
                    + "extern void *calloc(long unsigned int nitems, long unsigned int size);\\n"
                    + "extern void *alloca(long unsigned int size);\\n"
                    + "extern void *memcpy(void *dest, const void *src, long unsigned int n);\\n"
                    + "extern void *memmove(void *dest, const void *src, long unsigned int n);\\n"
                    + "extern void *memset(void *s, int c, long unsigned int n);\\n"
                    + "extern void free(void *ptr);\\n"
                    + "typedef struct \\n"
                    + "{\\n"
                    + "  void *address;\\n"
                    + "  long unsigned int size;\\n"
                    + "  int freed;\\n"
                    + "  int allocation_type;\\n"
                    + "} MemAllocation;\\n"
                    + "MemAllocation *__allocations;\\n"
                    + "int allocation_count = 0;\\n"
                    + "int allocation_capacity = 100;\\n"
                    + "void __add_allocation(void *ptr, long unsigned int size, int"
                    + " allocation_type)\\n"
                    + "{\\n"
                    + "  if (allocation_count == allocation_capacity)\\n"
                    + "  {\\n"
                    + "    allocation_capacity *= 2;\\n"
                    + "    __allocations = malloc(4294967295);\\n"
                    + "  }\\n"
                    + "  __allocations[allocation_count].address = ptr;\\n"
                    + "  __allocations[allocation_count].size = size;\\n"
                    + "  __allocations[allocation_count].freed = 0;\\n"
                    + "  __allocations[allocation_count].allocation_type = allocation_type;\\n"
                    + "  allocation_count++;\\n"
                    + "}\\n"
                    + "int __init_memory_tracker()\\n"
                    + "{\\n"
                    + "  __allocations = malloc(allocation_capacity * (sizeof(MemAllocation)));\\n"
                    + "  return 0;\\n"
                    + "}\\n"
                    + "void __register_stack(void *ptr, long unsigned int size)\\n"
                    + "{\\n"
                    + "  __add_allocation(ptr, size, 1);\\n"
                    + "}\\n"
                    + "void __deregister_stack(void *ptr)\\n"
                    + "{\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if ((__allocations[i].address == ptr) &&"
                    + " (__allocations[i].allocation_type == 1))\\n"
                    + "    {\\n"
                    + "      __allocations[i].freed = 1;\\n"
                    + "      break;\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "}\\n"
                    + "void __register_global(void *ptr, long unsigned int size)\\n"
                    + "{\\n"
                    + "  __add_allocation(ptr, size, 2);\\n"
                    + "}\\n"
                    + "void __track_free(void *ptr)\\n"
                    + "{\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    return;\\n"
                    + "  }\\n"
                    + "  int found = 0;\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if ((__allocations[i].address == ptr) &&"
                    + " (__allocations[i].allocation_type == 0))\\n"
                    + "    {\\n"
                    + "      found = 1;\\n"
                    + "      if (! __allocations[i].freed)\\n"
                    + "      {\\n"
                    + "        __allocations[i].freed = 1;\\n"
                    + "        __allocations[i].allocation_type = - 1;\\n"
                    + "        free(ptr);\\n"
                    + "        return;\\n"
                    + "      }\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  if (! found)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "}\\n"
                    + "void *__track_malloc(long unsigned int size)\\n"
                    + "{\\n"
                    + "  void *ptr = malloc(size);\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  __add_allocation(ptr, size, 0);\\n"
                    + "  return ptr;\\n"
                    + "}\\n"
                    + "void *__track_calloc(long unsigned int nitems, long unsigned int size)\\n"
                    + "{\\n"
                    + "  void *ptr = calloc(nitems, size);\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  __add_allocation(ptr, nitems * size, 0);\\n"
                    + "  return ptr;\\n"
                    + "}\\n"
                    + "void *__track_realloc(void *ptr, long unsigned int size)\\n"
                    + "{\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  int found = 0;\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if (__allocations[i].address == ptr)\\n"
                    + "    {\\n"
                    + "      if (__allocations[i].freed)\\n"
                    + "      {\\n"
                    + "        reach_error();\\n"
                    + "      }\\n"
                    + "      found = 1;\\n"
                    + "      break;\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  if (! found)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  void *new_ptr = realloc(ptr, size);\\n"
                    + "  if (! new_ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if (__allocations[i].address == ptr)\\n"
                    + "    {\\n"
                    + "      __allocations[i].freed = 1;\\n"
                    + "      break;\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  __add_allocation(new_ptr, size, 0);\\n"
                    + "  return new_ptr;\\n"
                    + "}\\n"
                    + "void *__track_alloca(long unsigned int size)\\n"
                    + "{\\n"
                    + "  void *ptr = alloca(size);\\n"
                    + "  __add_allocation(ptr, size, 1);\\n"
                    + "  return ptr;\\n"
                    + "}\\n"
                    + "void *__safety_mem_func(void *ptr, long unsigned int size)\\n"
                    + "{\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  int valid = 0;\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if (((ptr >= __allocations[i].address) && ((ptr + size) <="
                    + " (__allocations[i].address + __allocations[i].size))) && (!"
                    + " __allocations[i].freed))\\n"
                    + "    {\\n"
                    + "      valid = 1;\\n"
                    + "      break;\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  if (! valid)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  return ptr;\\n"
                    + "}\\n"
                    + "void *__track_memset(void *s, int c, long unsigned int n)\\n"
                    + "{\\n"
                    + "  __safety_mem_func(s, n);\\n"
                    + "  return memset(s, c, n);\\n"
                    + "}\\n"
                    + "void *__track_memmove(void *dest, const void *src, long unsigned int n)\\n"
                    + "{\\n"
                    + "  __safety_mem_func(dest, n);\\n"
                    + "  __safety_mem_func(src, n);\\n"
                    + "  return memmove(dest, src, n);\\n"
                    + "}\\n"
                    + "void *__track_memcpy(void *dest, const void *src, long unsigned int n)\\n"
                    + "{\\n"
                    + "  __safety_mem_func(dest, n);\\n"
                    + "  __safety_mem_func(src, n);\\n"
                    + "  return memcpy(dest, src, n);\\n"
                    + "}\\n"
                    + "void *__valid_ptr(void *ptr)\\n"
                    + "{\\n"
                    + "  if (! ptr)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  int valid = 0;\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if (((ptr >= __allocations[i].address) && (ptr <"
                    + " (__allocations[i].address + __allocations[i].size))) && (!"
                    + " __allocations[i].freed))\\n"
                    + "    {\\n"
                    + "      valid = 1;\\n"
                    + "      break;\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  if (! valid)\\n"
                    + "  {\\n"
                    + "    reach_error();\\n"
                    + "  }\\n"
                    + "  return ptr;\\n"
                    + "}\\n"
                    + "void __check_memory_leaks()\\n"
                    + "{\\n"
                    + "  for (int i = 0; i < allocation_count; i++)\\n"
                    + "  {\\n"
                    + "    if ((! __allocations[i].freed) && (__allocations[i].allocation_type =="
                    + " 0))\\n"
                    + "    {\\n"
                    + "      reach_error();\\n"
                    + "    }\\n"
                    + "  }\\n"
                    + "  free(__allocations);\\n"
                    + "  __allocations = 0;\\n"
                    + "}\\n"
                    + "void __safe_exit(int x)\\n"
                    + "{\\n"
                    + "  __check_memory_leaks();\\n"
                    + "  exit(x);\\n"
                    + "}\\n"
                    + "void __safe_abort()\\n"
                    + "{\\n"
                    + "  __check_memory_leaks();\\n"
                    + "  abort();\\n"
                    + "}\\n"
                    + "void __safe_return(int x)\\n"
                    + "{\\n"
                    + "  __check_memory_leaks();\\n"
                    + "  exit(x);\\n"
                    + "}\\n"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("true"),
            new InstrumentationOperation("__init_memory_tracker();"),
            InstrumentationOrder.BEFORE,
            q3);
    builder.add(t1, t2);
    return q3;
  }

  private InstrumentationState trackMemory(
      ImmutableList.Builder<InstrumentationTransition> builder) {
    InstrumentationState q2 = initializeMemorySafety(builder);

    InstrumentationTransition t2 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(free)"),
            new InstrumentationOperation("__track_free"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t3 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(malloc)"),
            new InstrumentationOperation("__track_malloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t4 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(calloc)"),
            new InstrumentationOperation("__track_calloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t5 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(realloc)"),
            new InstrumentationOperation("__track_realloc"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t6 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(memset)"),
            new InstrumentationOperation("__track_memset"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t7 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(memmove)"),
            new InstrumentationOperation("__track_memmove"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t8 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(memcpy)"),
            new InstrumentationOperation("__track_memcpy"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t9 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(alloca)"),
            new InstrumentationOperation("__track_alloca"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t10 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(__builtin_alloca)"),
            new InstrumentationOperation("__track_alloca"),
            InstrumentationOrder.SAME_LINE,
            q2);
    InstrumentationTransition t11 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ptr_declar"),
            new InstrumentationOperation("__register_stack(x_instr_1, sizeof(x_instr_1));"),
            InstrumentationOrder.AFTER,
            q2);
    InstrumentationTransition t12 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("declar"),
            new InstrumentationOperation("__register_stack(& x_instr_1, sizeof(x_instr_1));"),
            InstrumentationOrder.AFTER,
            q2);

    builder.add(t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
    return q2;
  }

  private InstrumentationState trackMemoryWhenExiting(
      ImmutableList.Builder<InstrumentationTransition> builder) {
    InstrumentationState q2 = trackMemory(builder);

    InstrumentationTransition t6 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(abort)"),
            new InstrumentationOperation("__check_memory_leaks();"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t7 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(exit)"),
            new InstrumentationOperation("__check_memory_leaks();"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t8 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(return 0;)"),
            new InstrumentationOperation("__check_memory_leaks();"),
            InstrumentationOrder.BEFORE,
            q2);
    InstrumentationTransition t9 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("FUNC(reach_error)"),
            new InstrumentationOperation("__check_memory_leaks();"),
            InstrumentationOrder.BEFORE,
            q2);

    builder.add(t6, t7, t8, t9);
    return q2;
  }

  private void constructValidFree() {
    ImmutableList.Builder<InstrumentationTransition> builder = ImmutableList.builder();
    trackMemory(builder);
    this.instrumentationTransitions = builder.build();
  }

  private void constructValidDeref() {
    ImmutableList.Builder<InstrumentationTransition> builder = ImmutableList.builder();
    InstrumentationState q2 = trackMemory(builder);

    InstrumentationTransition t10 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ptr_deref"),
            new InstrumentationOperation("* ((typeof(x_instr_1)) __valid_ptr(x_instr_1))"),
            InstrumentationOrder.SAME_LINE,
            q2);
    builder.add(t10);
    this.instrumentationTransitions = builder.build();
  }

  private void constructMemTrack() {
    ImmutableList.Builder<InstrumentationTransition> builder = ImmutableList.builder();
    trackMemoryWhenExiting(builder);
    this.instrumentationTransitions = builder.build();
  }

  private void constructMemSafety() {
    ImmutableList.Builder<InstrumentationTransition> builder = ImmutableList.builder();
    InstrumentationState q2 = trackMemoryWhenExiting(builder);

    InstrumentationTransition t10 =
        new InstrumentationTransition(
            q2,
            new InstrumentationPattern("ptr_deref"),
            new InstrumentationOperation("* ((typeof(x_instr_1)) __valid_ptr(x_instr_1))"),
            InstrumentationOrder.SAME_LINE,
            q2);
    builder.add(t10);
    this.instrumentationTransitions = builder.build();
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
                "; int __saved_INSTR_"
                    + pIndex
                    + " = 0; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                entry.getValue()
                                    + " __"
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
                "if(__VERIFIER_nondet_int() && __saved_INSTR_"
                    + pIndex
                    + " == 0) { __saved_INSTR_"
                    + pIndex
                    + " =1; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                getDereferencesForPointer(entry.getValue())
                                    + " __"
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + " = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? "; " : "")
                    + "} else { __VERIFIER_assert((__saved_INSTR_"
                    + pIndex
                    + " == 0)"
                    + (!liveVariablesAndTypes.isEmpty() ? " || " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "("
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + " != __"
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

  private void constructTerminationWithAbortsAutomaton(int pIndex) {
    InstrumentationState q1 = new InstrumentationState("q1", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.FALSE, this);
    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                "; int __saved_INSTR_"
                    + pIndex
                    + " = 0; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                entry.getValue()
                                    + " __"
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
                "if(__VERIFIER_nondet_int() && __saved_INSTR_"
                    + pIndex
                    + " == 0) { __saved_INSTR_"
                    + pIndex
                    + " =1; "
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                getDereferencesForPointer(entry.getValue())
                                    + " __"
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + " = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                        .collect(Collectors.joining("; "))
                    + (!liveVariablesAndTypes.isEmpty() ? "; " : "")
                    + "} else { if((__saved_INSTR_"
                    + pIndex
                    + " == 1)"
                    + (!liveVariablesAndTypes.isEmpty() ? " && " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                "("
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + " == "
                                    + getDereferencesForPointer(entry.getValue())
                                    + " __"
                                    + entry.getKey()
                                    + "_INSTR_"
                                    + pIndex
                                    + ")")
                        .collect(Collectors.joining("&&"))
                    + "){abort();}}"),
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
    InstrumentationState q1 =
        new InstrumentationState("q1", StateAnnotation.FUNCTIONHEADFORLOOP, this);
    InstrumentationState q2 = new InstrumentationState("q2", StateAnnotation.LOOPHEAD, this);
    InstrumentationState q3 = new InstrumentationState("q3", StateAnnotation.FALSE, this);
    this.initialState = q1;

    InstrumentationTransition t1 =
        new InstrumentationTransition(
            q1,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(
                (pIndex == 0 ? "; int saved = 0; int pc_INSTR = 0; " : "")
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
                "if (saved == 0) {pc_INSTR = "
                    + pIndex
                    + (!liveVariablesAndTypes.isEmpty() ? " ; " : "")
                    + liveVariablesAndTypes.entrySet().stream()
                        .map(
                            (entry) ->
                                getDereferencesForPointer(entry.getValue())
                                    + entry.getKey()
                                    + "_INSTR"
                                    + " = "
                                    + getDereferencesForPointer(entry.getValue())
                                    + entry.getKey())
                        .collect(Collectors.joining(";"))
                    + ";}\\n"
                    + "if(__VERIFIER_nondet_int() && saved"
                    + " == 0) { saved"
                    + " =1; pc_INSTR = "
                    + pIndex
                    + "; "
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
                    + " == 0) || (pc_INSTR != "
                    + pIndex
                    + ")"
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
            q3,
            new InstrumentationPattern("true"),
            new InstrumentationOperation(""),
            InstrumentationOrder.AFTER,
            q3);
    this.instrumentationTransitions = ImmutableList.of(t1, t2, t3);
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
                    + " }\\n"),
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
