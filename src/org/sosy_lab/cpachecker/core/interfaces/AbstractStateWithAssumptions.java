// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;

/**
 * Sub-interface for {@link AbstractState}s that marks states with an assumption. This is intended
 * for other CPAs to use in their strengthen operator, such that all the other CPAs can add these
 * assumptions to their abstract state.
 */
public interface AbstractStateWithAssumptions extends AbstractState {

  // TODO: add assertion to ensure all assumptions are logical. Some CPAs seem to expect binary
  //  logical expressions and can't handle it if we just return expressions that return integers
  //  as C style booleans.
  // TODO: work on the CPAs exporting non-logical expressions or transform them to logical
  //  expressions here. Or make sure all CPAs can handle integers as booleans.
  // TODO: suggestion: add a isLogicalExpression() check to all expressions?
  /**
   * Get the list of assumptions represented as AssumeEdges.
   *
   * <p>Implementors should make sure that only expressions are returned which would also occur in
   * the CFA, i.e., the expressions should be simplified and normalized. For example, this means
   * that the expression "x" is not valid and should "x != 0" instead.
   *
   * <p>Assumptions about function return value are transformed from "return N;" to "retVar == N",
   * where "retVar" is the name of a pseudo variable (just as {@link
   * AReturnStatement#asAssignment()} does.
   *
   * @return A (possibly empty list) of expressions.
   */
  List<? extends AExpression> getAssumptions();
}
