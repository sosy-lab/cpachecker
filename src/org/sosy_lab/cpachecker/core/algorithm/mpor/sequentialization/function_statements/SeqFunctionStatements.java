// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.SeqCallContext;

public record SeqFunctionStatements(
    ImmutableListMultimap<CFAEdgeForThread, SeqFunctionParameterAssignment> parameterAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionParameterAssignment> startRoutineArgAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment> returnValueAssignments,
    ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment> startRoutineExitAssignments) {

  public interface SeqFunctionStatement {
    CExpressionAssignmentStatement getExpressionAssignmentStatement();

    SeqCallContext getLeftHandSideCallContext();

    SeqCallContext getRightHandSideCallContext();
  }

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
   * @param leftHandSideCallContext The call context for the left-hand side of the parameter
   *     assignment. This is always present because it is the call to the function itself e.g.
   *     {@code function(0)} in the example above.
   * @param rightHandSideCallContext The call context for the right-hand side of the parameter
   *     assignment. This can be empty if the function call itself has no call context, e.g. if it
   *     is placed inside the {@code main()} function as shown in the example above.
   */
  public record SeqFunctionParameterAssignment(
      CExpressionAssignmentStatement expressionAssignmentStatement,
      SeqCallContext leftHandSideCallContext,
      SeqCallContext rightHandSideCallContext)
      implements SeqFunctionStatement {

    public SeqFunctionParameterAssignment {
      checkArgument(
          leftHandSideCallContext.cfaEdgeForThread().isPresent(),
          "leftHandSideCallContext must be present.");
    }

    @Override
    public CExpressionAssignmentStatement getExpressionAssignmentStatement() {
      return expressionAssignmentStatement;
    }

    @Override
    public SeqCallContext getLeftHandSideCallContext() {
      return leftHandSideCallContext;
    }

    @Override
    public SeqCallContext getRightHandSideCallContext() {
      return rightHandSideCallContext;
    }
  }

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
   * @param leftHandSideCallContext The call context for the left-hand side of the return value
   *     assignment. This can be empty e.g. for function return value assignments in the {@code
   *     main()} function as seen in the example above.
   * @param rightHandSideCallContext The call context for the right-hand side of the return value
   *     assignment. This is always present because the right-hand side is extracted from the {@code
   *     return} statements inside the function which always has a call context.
   */
  public record SeqFunctionReturnValueAssignment(
      CExpressionAssignmentStatement expressionAssignmentStatement,
      SeqCallContext leftHandSideCallContext,
      SeqCallContext rightHandSideCallContext)
      implements SeqFunctionStatement {

    public SeqFunctionReturnValueAssignment {
      if (rightHandSideCallContext.cfaEdgeForThread().isEmpty()) {
        System.out.println("");
      }
      checkArgument(
          rightHandSideCallContext.cfaEdgeForThread().isPresent(),
          "rightHandSideCallContext must be present.");
    }

    @Override
    public CExpressionAssignmentStatement getExpressionAssignmentStatement() {
      return expressionAssignmentStatement;
    }

    @Override
    public SeqCallContext getLeftHandSideCallContext() {
      return leftHandSideCallContext;
    }

    @Override
    public SeqCallContext getRightHandSideCallContext() {
      return rightHandSideCallContext;
    }
  }
}
