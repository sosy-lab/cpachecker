// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.io.Serial;
import java.util.List;
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
 * not needed, the methods can be implemented to throw a {@link
 * ReportingMethodNotImplementedException}. Be aware that if you do this the export of witnesses
 * will not be possible.
 *
 * <p><b>Extension for Memory Safety Witness Export:</b>
 * <ul>
 *   <li>Provides optional method to return ACSL-based memory safety invariants 
 *       for export (e.g., \valid, \allocated, buffer boundaries, temporal safety).
 *   <li>Default implementation throws {@link ReportingMethodNotImplementedException}.
 *   <li>Implement in states that support memory invariants (e.g., SMGState).
 * </ul>
 */
public interface ExpressionTreeReportingState extends AbstractState {

  class ReportingMethodNotImplementedException extends Exception {
    @Serial private static final long serialVersionUID = -1208757812;

    public ReportingMethodNotImplementedException(String pMessage) {
      super(pMessage);
    }
  }

  class TranslationToExpressionTreeFailedException extends Exception {
    @Serial private static final long serialVersionUID = -12936129745L;

    public TranslationToExpressionTreeFailedException(String pMessage) {
      super(pMessage);
    }
  }

  /**
   * Returns an ExpressionTree over-approximating the state.
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges
   * @throws InterruptedException if the computation is interrupted
   * @throws TranslationToExpressionTreeFailedException if the translation to an expression tree
   *     failed
   */
  ExpressionTree<Object> getFormulaApproximationAllVariablesInFunctionScope(
      FunctionEntryNode pFunctionScope, CFANode pLocation)
      throws InterruptedException, TranslationToExpressionTreeFailedException;

  /**
   * Returns an ExpressionTree over-approximating the state only considering the variables which are
   * in scope at the given location in the original program.
   *
   * @param pFunctionScope the function entry node
   * @param pLocation the formula should at least try to approximate variables referenced by
   *     entering edges at this location
   * @param pAstCfaRelation the relation between the AST and the CFA
   * @param useOldKeywordForVariables whether to use the old keyword for variables or not. For
   *     example if true the variable `x` should be denoted by `\old(x)` in the produced ACSL
   *     formula
   * @return the formula approximation
   * @throws InterruptedException if the computation is interrupted
   * @throws ReportingMethodNotImplementedException if the computation is not implemented
   * @throws TranslationToExpressionTreeFailedException if the translation to an expression tree
   *     failed
   */
  ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariables(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      AstCfaRelation pAstCfaRelation,
      boolean useOldKeywordForVariables)
      throws InterruptedException,
          ReportingMethodNotImplementedException,
          TranslationToExpressionTreeFailedException;

  /**
   * Only return the formula approximation for the return variable of the function. This means that,
   * in ACSL notation, every expression should have the keyword \return replaced with the given
   * variable i.e. \return > 0 becomes result > 0 if the given variable is result. This is
   * particularly useful in order to export function contracts.
   *
   * <p>The returned expression is only allowed to contain the given pFunctionReturnVariable and
   * constants. It is not allowed to contain any other variables.
   *
   * @param pFunctionScope the function entry node. It references the {@link
   *     org.sosy_lab.cpachecker.cfa.model.FunctionExitNode} if it exists
   * @param pFunctionReturnVariable the variable to replace function return expressions with
   * @return the formula approximation
   * @throws InterruptedException if the computation is interrupted
   * @throws ReportingMethodNotImplementedException if the computation is not implemented
   * @throws TranslationToExpressionTreeFailedException if the translation to an expression tree
   *     failed
   */
  ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
      FunctionEntryNode pFunctionScope, AIdExpression pFunctionReturnVariable)
      throws InterruptedException,
          ReportingMethodNotImplementedException,
          TranslationToExpressionTreeFailedException;

  /**
   * Returns a list of memory safety invariants (e.g., ACSL expressions such as \valid(p), 
   * \allocated(p), buffer properties, temporal safety) encoded for this abstract state.
   *
   * <p>States not supporting memory safety invariants may throw
   *     {@link ReportingMethodNotImplementedException}.
   *
   * @return a {@code List<String>} of ACSL memory safety invariants describing this state
   * @throws ReportingMethodNotImplementedException if not implemented
   */
  default List<String> getMemorySafetyInvariants()
      throws ReportingMethodNotImplementedException {
    throw new ReportingMethodNotImplementedException(
        "Memory safety invariant extraction not implemented");
  }
}
