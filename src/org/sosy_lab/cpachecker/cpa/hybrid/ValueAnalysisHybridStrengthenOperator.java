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
package org.sosy_lab.cpachecker.cpa.hybrid;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class provides the strengthening for a HybridAnalysisState via a ValueAnalysisState
 */
public class ValueAnalysisHybridStrengthenOperator implements HybridStrengthenOperator {

  @Override
  public HybridAnalysisState strengthen(
      HybridAnalysisState pStateToStrengthen,
      AbstractState pStrengtheningState,
      CFAEdge pEdge) {

    // operator only excepts ValueAnalysisStates
    assert pStrengtheningState instanceof ValueAnalysisState;

    ValueAnalysisState strengtheningState = (ValueAnalysisState) pStrengtheningState;

    // check for assumptions containing a variable that is also tracked by the ValueAnalysis and remove them
    Set<CBinaryExpression> assumptions = Sets.newHashSet(
        pStateToStrengthen.getExplicitAssumptions());
    Set<MemoryLocation> trackedVariables = Sets.newHashSet(strengtheningState.getTrackedMemoryLocations());

    // used to collect all binary expressions, that are already tracked by the value analysis and thus can be removed
    Set<CBinaryExpression> removeableAssumptions = Sets.newHashSet();

    /*
     * TODO: if value in ValueAnalysis in Unknown, try to generate a value
     */

    for(CBinaryExpression binaryExpression : assumptions) {

      CExpression leftHandSide = binaryExpression.getOperand1();
      if(leftHandSide instanceof CIdExpression) {

        // simple variable definition
        final String name = ((CIdExpression) leftHandSide).getName();
        final boolean checkResult = checkMemoryLocations(trackedVariables, name, false);
        if(checkResult) {
          removeableAssumptions.add(binaryExpression);
        }

      } else if(leftHandSide instanceof CArraySubscriptExpression) {

        CArraySubscriptExpression subscriptExpression = (CArraySubscriptExpression) leftHandSide;
        CExpression arrayExpression = subscriptExpression.getArrayExpression();

        // now we need to check, if we can retrieve the name
        if(arrayExpression instanceof CIdExpression) {

          final String name = ((CIdExpression) arrayExpression).getName();
          // TODO: handle the offset !!!
          final boolean checkResult = checkMemoryLocations(trackedVariables, name, true);
          if(checkResult) {
            removeableAssumptions.add(binaryExpression);
          }
        }
      }
    }

    // remove unnecessary assumptions
    assumptions.removeAll(removeableAssumptions);

    // build collection with variable identifiers for the internal var cache
    Set<CExpression> variableIdentifiers = assumptions
      .stream()
      .map(assumption -> assumption.getOperand1())
      .collect(Collectors.toSet());
    
    return new HybridAnalysisState(assumptions, variableIdentifiers);
  }

  private boolean compareNames(
      String pVariableName,
      MemoryLocation pMemoryLocation,
      boolean keepOffset) {

    StringBuilder nameBuilder = new StringBuilder();

    if(pVariableName.contains("::")) {
      nameBuilder.append(pMemoryLocation.getFunctionName()).append("::");
    }

    nameBuilder.append(pMemoryLocation.getIdentifier());

    if(keepOffset) {
      nameBuilder.append("/").append(pMemoryLocation.getOffset());
    }

    return pVariableName.equals(nameBuilder.toString());
  }

  // checks whether the variable is tracked by the value analysis, or not
  private boolean checkMemoryLocations(
    Set<MemoryLocation> pLocations, 
    final String pVariableName, 
    final boolean keepOffset) {

    boolean match = false;
    Set<MemoryLocation> seenLocations = Sets.newHashSet();

    for(MemoryLocation location : pLocations) {
      
      if(compareNames(pVariableName, location, keepOffset)) {
        match = true;
        seenLocations.add(location);
      }
    }

    // remove seen locations from the set, hybrid value analysis tracks exactly one value per variable
    pLocations.removeAll(seenLocations);

    return match;
  }

}