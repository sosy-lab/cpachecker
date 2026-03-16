// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;

/**
 * Utility class for detecting and extracting information from pthread-related C function calls
 * ({@code pthread_create}, {@code pthread_join}).
 */
public final class PthreadFunctions {

  private static final ImmutableSet<String> CREATE_FUNCTIONS = ImmutableSet.of("pthread_create");
  private static final ImmutableSet<String> JOIN_FUNCTIONS = ImmutableSet.of("pthread_join");

  private PthreadFunctions() {}

  /** Returns {@code true} if the given function name is a thread creation function. */
  public static boolean isCreateFunction(String functionName) {
    return CREATE_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a thread join function. */
  public static boolean isJoinFunction(String functionName) {
    return JOIN_FUNCTIONS.contains(functionName);
  }

  /**
   * Extracts the thread handle's qualified name from a {@code pthread_create} call's parameter
   * list.
   *
   * @param params the parameter expressions of the {@code pthread_create} call (must have 4
   *     elements)
   * @return the qualified name of the thread handle variable
   */
  public static String extractCreateHandle(List<? extends AExpression> params) {
    checkState(params.size() == 4, "Malformed pthread_create (not 4 params): %s", params);
    checkState(
        params.get(0) instanceof CUnaryExpression cUnaryExpression
            && cUnaryExpression.getOperator() == UnaryOperator.AMPER
            && cUnaryExpression.getOperand() instanceof CIdExpression,
        "Malformed/unsupported pthread_create"
            + " (Thread handle not unary expression with variable reference): %s",
        params.get(0));
    return ((CIdExpression) ((CUnaryExpression) params.get(0)).getOperand())
        .getDeclaration()
        .getQualifiedName();
  }

  /**
   * Extracts the started function's name from a {@code pthread_create} call's parameter list.
   *
   * @param params the parameter expressions of the {@code pthread_create} call (must have 4
   *     elements)
   * @return the simple name of the function to be started in the new thread
   */
  public static String extractCreateFunctionName(List<? extends AExpression> params) {
    checkState(params.size() == 4, "Malformed pthread_create (not 4 params): %s", params);
    checkState(
        params.get(2) instanceof CUnaryExpression cUnaryExpression
            && cUnaryExpression.getOperator() == UnaryOperator.AMPER,
        "Malformed pthread_create (Thread not unary expression with reference): %s",
        params.get(2));
    checkState(
        ((CUnaryExpression) params.get(2)).getOperand() instanceof CIdExpression,
        "Malformed pthread_create (Thread not CIdExpression): %s",
        ((CUnaryExpression) params.get(2)).getOperand());
    return ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand()).getName();
  }

  /**
   * Extracts the thread handle's qualified name from a {@code pthread_join} call's parameter list.
   *
   * @param params the parameter expressions of the {@code pthread_join} call (must have 2
   *     elements)
   * @return the qualified name of the thread handle variable
   */
  public static String extractJoinHandle(List<? extends AExpression> params) {
    checkState(params.size() == 2, "Malformed pthread_join (not 2 params): %s", params);
    final var handleParam = params.get(0);
    checkState(
        handleParam instanceof CUnaryExpression cUnaryExpression
            && cUnaryExpression.getOperator() == UnaryOperator.AMPER
            && cUnaryExpression.getOperand() instanceof CIdExpression,
        "Malformed/unsupported pthread_join"
            + " (Thread handle not unary expression with variable reference): %s",
        handleParam);
    return ((CIdExpression) ((CUnaryExpression) handleParam).getOperand())
        .getDeclaration()
        .getQualifiedName();
  }
}
