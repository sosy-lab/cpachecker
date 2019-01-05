/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.hybrid.abstraction;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;

@FunctionalInterface
public interface AssumptionSearchStrategy {

  /**
   * Creates an assumption context starting the search from the given state
   * @param pState The bottom state to start with
   * @param pAssumptions All remaining assumptions to choose from (
   *                     if an assumption is not contained in the set, it has been visited already)
   * @return The new assumption context
   */
  @Nullable
  AssumptionContext runStrategy(
      ARGState pState, List<CAssumeEdge> pRemainingAssumptions)
      throws InvalidAssumptionException;


  /**
   * This class defines the context for an not yet visited assumption
   *  1) The assumption itself
   *  2) The depending variables
   *  3) The ARGState under which to insert the new state with changed variable values in compliance to the assumption
   *  4) The ARGState prior to the assumption's opposite
   */
  class AssumptionContext {

    private final CBinaryExpression assumption;
    private final ARGState priorAssumptionState;
    private final Set<CIdExpression> variables;
    private ARGState parentState;
    private ARGPath parentToAssumptionPath;

    /**
     * Constructs a new instance of this class.
     * Extracts the variables contained in the assumption.
     * @param pPriorAssumptionState The state appearing in the ARG prior to the chosen assumption
     * @param pAssumeExpression The assumption
     * @throws InvalidAssumptionException The Hybrid Analysis can only work on CBinaryExpressions
     */
    public AssumptionContext(
        ARGState pPriorAssumptionState,
        CExpression pAssumeExpression)
        throws InvalidAssumptionException {

      priorAssumptionState = Preconditions.checkNotNull(pPriorAssumptionState);
      Preconditions.checkNotNull(pAssumeExpression);

      if(!(pAssumeExpression instanceof CBinaryExpression)) {
        throw new InvalidAssumptionException(
            String.format("Assumption contained in assume edge %s is not applicable for hybrid execution.", pAssumeExpression));
      }

      variables = ExpressionUtils.extractAllVariableIdentifiers(pAssumeExpression);

      assumption = (CBinaryExpression) pAssumeExpression;
    }

    @Nullable
    public ARGState getParentState() {
      return parentState;
    }

    public ARGState getPriorAssumptionState() {
      return priorAssumptionState;
    }

    public Set<CIdExpression> getVariables() {
      return variables;
    }

    @Nullable
    public ARGPath getParentToAssumptionPath() {
      return parentToAssumptionPath;
    }

    public void setParentToAssumptionPath(List<ARGState> pPathList) {
      parentToAssumptionPath = new ARGPath(pPathList);
    }

    public void setParentState(ARGState pState) {
      parentState = pState;
    }
  }
}
