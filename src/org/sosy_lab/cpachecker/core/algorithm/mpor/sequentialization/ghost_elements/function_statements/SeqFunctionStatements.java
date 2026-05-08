// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public record SeqFunctionStatements(
    ImmutableListMultimap<CFAEdgeForThread, SeqFunctionParameterAssignment> parameterAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionParameterAssignment> startRoutineArgAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment> returnValueAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment> startRoutineExitAssignments) {

  /**
   * A wrapper class to keep track of function parameter assignments such as {@code arg = 0;} from
   * the following example:
   *
   * <pre>{@code
   * void function(int arg) {
   *    // ..
   * }
   * int main() {
   *    function(0);
   * }
   * }</pre>
   *
   * @param expressionAssignmentStatement The assignment statement. This is not a {@link
   *     CFunctionCallAssignmentStatement} to exclude parameters that call a function such as {@code
   *     function(another_function());}.
   */
  public record SeqFunctionParameterAssignment(
      CExpressionAssignmentStatement expressionAssignmentStatement) {}

  /**
   * A wrapper class to keep track of function return value assignments to the respective calling
   * context such as {@code result = arg + arg;} from the following example:
   *
   * <pre>{@code
   * int result;
   * void function(int arg) {
   *    return arg + arg;
   * }
   * int main() {
   *    result = function(16);
   * }
   * }</pre>
   *
   * @param expressionAssignmentStatement The assignment statement. This is not a {@link
   *     CFunctionCallAssignmentStatement} to exclude return statements that call a function such as
   *     {@code return another_function();}.
   */
  public record SeqFunctionReturnValueAssignment(
      CExpressionAssignmentStatement expressionAssignmentStatement) {}
}
