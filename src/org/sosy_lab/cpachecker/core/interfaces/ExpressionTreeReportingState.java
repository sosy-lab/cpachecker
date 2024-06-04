// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import apron.NotImplementedException;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * Interface to implement in order for an abstract state to be able to be over-approximated by an
 * ExpressionTree representing the abstract state.
 *
 * <p>It is recommended to implement all methods in this class, in particular if the goal is to
 * export correct witnesses. Since this is a crucial part of the export. If this is not possible or
 * not needed, the methods can be implemented to throw a {@link NotImplementedException}. Be aware
 * that if you do this the export of witnesses will not be possible.
 */
public interface ExpressionTreeReportingState extends AbstractState {

  /**
   * Returns an ExpressionTree over-approximating the state.
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges
   * @throws InterruptedException if the computation is interrupted
   */
  ExpressionTree<Object> getFormulaApproximationAllVariables(
      FunctionEntryNode pFunctionScope, CFANode pLocation) throws InterruptedException;

  /**
   * Returns an ExpressionTree over-approximating the state only considering the variables which are
   * in scope at the given location in the original program.
   *
   * @param pFunctionScope the function entry node
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges at this location
   * @param pAstCfaRelation the relation between the AST and the CFA
   * @return the formula approximation
   * @throws InterruptedException if the computation is interrupted
   * @throws NotImplementedException if the computation is not implemented
   */
  ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariable(
      FunctionEntryNode pFunctionScope, CFANode pLocation, AstCfaRelation pAstCfaRelation)
      throws InterruptedException, UnsupportedOperationException;

  /**
   * Only return the formula approximation for the return variable of the function. This means that,
   * in ACSL notation, every expression should have the keyword \return replaced with the given
   * variable i.e. \return > 0 becomes result > 0 if the given variable is result. This is
   * particularly useful in order to export function contracts.
   *
   * @param pFunctionScope the function entry node. It references the {@link
   *     org.sosy_lab.cpachecker.cfa.model.FunctionExitNode} if it exists
   * @param pFunctionReturnVariable the variable to replace function return expressions with
   * @return the formula approximation
   * @throws InterruptedException if the computation is interrupted
   * @throws NotImplementedException if the computation is not implemented
   */
  ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
      FunctionEntryNode pFunctionScope, AIdExpression pFunctionReturnVariable)
      throws InterruptedException, UnsupportedOperationException;
}
