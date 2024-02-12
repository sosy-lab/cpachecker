// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Optional;
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
   * Returns an ExpressionTree over-approximating the state. The expressions which represent the
   * return from a function are replaced by the given variable. This is particularly useful in order
   * to export function contracts.
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges
   * @param pFunctionReturnVariable the variable to replace function return expressions with
   */
  ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      Optional<AIdExpression> pFunctionReturnVariable)
      throws InterruptedException;
}
