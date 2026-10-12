// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

public enum SeqPointerAssignmentType {

  /**
   * An explicit pointer assignment such as:
   *
   * <pre>{@code
   * int main() {
   *    int var;
   *    int *ptr = & var;
   * }
   * }</pre>
   */
  EXPLICIT(false),

  /**
   * A pointer assignment through a parameter such as:
   *
   * <pre>{@code
   * void function(int* ptr_param) {
   *    // ...
   * }
   * int main() {
   *    int var;
   *    function(& var);
   * }
   * }</pre>
   */
  PARAMETER(false),

  /**
   * A pointer assignment through the return value of a function such as:
   *
   * <pre>{@code
   * int* function() {
   *    int* return_value = malloc(sizeof(int));
   *    return return_value;
   * }
   * int main() {
   *    int* ptr = function();
   * }
   * }</pre>
   */
  RETURN_VALUE(true),

  /**
   * A pointer assignment through a call to {@code pthread_create} of a thread such as:
   *
   * <pre>{@code
   * void start_routine(void* arg) {
   *    // ...
   * }
   * int main() {
   *    pthread_t thread;
   *    int i = 0;
   *    pthread_create(& thread, (void *) 0, start_routine, (void *) & i);
   * }
   * }</pre>
   */
  START_ROUTINE_ARG(false),

  /**
   * A pointer assignment through a call to {@code pthread_exit} in the {@code start_routine} of a
   * thread and a call to {@code pthread_join} in the waiting thread such as:
   *
   * <pre>{@code
   * void start_routine(void* arg) {
   *    int* result = malloc(sizeof(int));
   *    pthread_exit((void*)result);
   * }
   * int main() {
   *    pthread_t thread;
   *    pthread_create(& thread, (void *) 0, start_routine, (void *) 0);
   *    void* return_value;
   *    pthread_join(& thread, & return_value);
   * }
   * }</pre>
   */
  START_ROUTINE_EXIT(true);

  private final boolean isPerformedOnFunctionReturn;

  SeqPointerAssignmentType(boolean pIsPerformedOnFunctionReturn) {
    isPerformedOnFunctionReturn = pIsPerformedOnFunctionReturn;
  }

  /**
   * Whether the pointer assignment is performed on function return, i.e., not inside the scope of
   * the right-hand side of the assignment.
   */
  public boolean isPerformedOnFunctionReturn() {
    return isPerformedOnFunctionReturn;
  }
}
