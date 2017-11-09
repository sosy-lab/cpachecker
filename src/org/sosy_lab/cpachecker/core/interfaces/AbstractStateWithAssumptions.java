/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Sub-interface for {@link AbstractState}s that marks states
 * with an assumption.
 * This is intended for other CPAs to use in their strengthen operator,
 * such that all the other CPAs can add these assumptions to their abstract state.
 */
public interface AbstractStateWithAssumptions extends AbstractState {

  /**
   * Get the list of assumptions represented as AssumeEdges.
   *
   * Implementors should make sure that only expressions are returned
   * which would also occur in the CFA, i.e., the expressions should be simplified and normalized.
   * For example, this means that the expression "x" is not valid
   * and should "x != 0" instead.
   *
   * Assumptions about function return value are transformed from
   * "return N;" to "retVar == N", where "retVar" is the name of a pseudo variable
   * (just as {@link AReturnStatement#asAssignment()} does.
   *
   * @return A (possibly empty list) of expressions.
   */
  List<? extends AExpression> getAssumptions();

  /**
   * Get a list of assumptions that should hold in the previous (=parent) state,
   * that means before the edge to this state is evaluated.
   * For implementors, the same requirements hold as for
   * {@link AbstractStateWithAssumptions#getAssumptions()}
   * @return A (possibly empty list) of expressions.
   */
  default List<? extends AExpression> getPreconditionAssumptions() {
    return ImmutableList.of();
  }

  /**
   * Get the path formula with which the assumptions from
   * {@link AbstractStateWithAssumptions#getPreconditionAssumptions()}
   * should be instantiated.
   * @param pPathFormula path formula for which the previous path formula
            shall be returned
   * @return a path formula (may be null)
   */
  default @Nullable PathFormula getPreviousPathFormula(PathFormula pPathFormula) {
    return null;
  }
}
