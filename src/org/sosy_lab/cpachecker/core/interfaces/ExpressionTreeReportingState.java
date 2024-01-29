// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * Interface to implement in order for an abstract state to be able to be over-approximated by an
 * ExpressionTree representing the abstract state.
 */
public interface ExpressionTreeReportingState extends AbstractState {

  /**
   * Returns an ExpressionTree over-approximating the state.
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges.
   * @return an ExpressionTree over-approximating the state.
   */
  ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) throws InterruptedException;

  /**
   * Returns an ExpressionTree over-approximating the state. With the extensions that expressions
   * which represent the return from a function are replaced by the given variable. This is
   * particularly useful in order to export function contracts. By default, or when
   * pFunctionReturnVariable is null this method calls {@link
   * #getFormulaApproximation(FunctionEntryNode, CFANode)}
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges
   * @param pFunctionReturnVariable the variable to replace function return expressions with
   */
  default ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      @Nullable AIdExpression pFunctionReturnVariable)
      throws InterruptedException {
    return getFormulaApproximation(pFunctionScope, pLocation);
  }
}
