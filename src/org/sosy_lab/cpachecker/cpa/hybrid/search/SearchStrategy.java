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
package org.sosy_lab.cpachecker.cpa.hybrid.search;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.AssumptionSearchStrategy;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;

public class SearchStrategy implements AssumptionSearchStrategy {

  private final LogManager logger;

  public SearchStrategy(LogManager pLogManager) {
    logger = pLogManager;
  }

  @Nullable
  @Override
  public AssumptionContext runStrategy(
      ARGState pState,
      List<CAssumeEdge> pRemainingAssumptions) throws InvalidAssumptionException {

    // will never be empty
    // we choose the assumption that is furthest up in the cfa (allAssumptions are ordered)
    CBinaryExpression nextAssumptionToFlip = (CBinaryExpression) pRemainingAssumptions.get(0).getExpression();

    ARGState priorAssumptionState = searchARGStatePriorAssumption(pState, nextAssumptionToFlip);

    AssumptionContext newContext =  new AssumptionContext(priorAssumptionState, nextAssumptionToFlip);

    List<ARGState> pathStates = findARGPathWithVariables(
        newContext.getPriorAssumptionState(),
        newContext.getVariables());

    if(pathStates.isEmpty()) {
      logger.log(Level.WARNING, "Search strategy was unable to find ");
    }

    // we need to reverse the list, because right now the child-states come before the parent-states
    pathStates = Lists.reverse(pathStates);

    newContext.setParentToAssumptionPath(pathStates);
    newContext.setParentState(pathStates.get(0));

    return newContext;
  }

  /*
   * finds the ARGState prior to the assumption (to it's opposite part)
   */
  private  ARGState searchARGStatePriorAssumption(
      ARGState pState, CBinaryExpression pAssumption) throws InvalidAssumptionException {

    for(ARGState parentState : pState.getParents()) {

      for(CFAEdge edge : parentState.getEdgesToChild(pState)) {

        if(edge instanceof CAssumeEdge
          && ((CAssumeEdge)edge).getExpression().equals(pAssumption)) {
          return parentState;
        }
      }
      return searchARGStatePriorAssumption(parentState, pAssumption);
    }

    throw new AssertionError("Could not find ARGState with definitely existing assumption on edge to child."); // this should never happen
  }


  /**
    * Finds the shortest path in the ARG that contains the variables (changes)
    * @param pState The state to begin with
    * @param pVariables The variables contained in the assumption
    * @return A list of ARGState denoting the path from the assumption to the parent state of the last change
    * (empty list possible)
    */
  private List<ARGState> findARGPathWithVariables(
      ARGState pState,
      Set<CIdExpression> pVariables) {

    List<List<ARGState>> allPaths = new ArrayList<>();

    BiPredicate<ARGState, ARGState> checkState = (child, parent) -> {

      for(CFAEdge edge : parent.getEdgesToChild(child)) {

        if(!(edge instanceof CStatementEdge)) {
          continue;
        }

        // now check for assignment and function call assignment
        CStatementEdge statementEdge = (CStatementEdge) edge;
        if(ExpressionUtils.assignmentContainsVariable(statementEdge, pVariables)) {
          return true;
        }
      }

      return false;

    };

    findAllARGPathsWithVariables(
        pState,
        checkState,
        pVariables.size(),
        allPaths,
        new ArrayList<>());

    if(allPaths.isEmpty()) {
      return Collections.emptyList();
    }

    allPaths.sort((l1, l2) -> Integer.compare(l1.size(), l2.size()));

    return allPaths.get(0);
  }

  private void findAllARGPathsWithVariables(
      ARGState pState,
      BiPredicate<ARGState, ARGState> pCheckStateForVariable,
      int pVariableCounter,
      List<List<ARGState>> pAllAccumulator,
      List<ARGState> pAccumulator) {

    pAccumulator.add(pState);

    for(ARGState parentState : pState.getParents()) {

      if(pCheckStateForVariable.test(pState, parentState)) {

        if(--pVariableCounter == 0) {
          // no more variables to search for
          pAccumulator.add(parentState);
          pAllAccumulator.add(pAccumulator);
          return;
        }
      }

      // call for parent with list copy
      findAllARGPathsWithVariables(
          parentState,
          pCheckStateForVariable,
          pVariableCounter,
          pAllAccumulator,
          new ArrayList<>(pAccumulator)
      );
    }
  }
}
