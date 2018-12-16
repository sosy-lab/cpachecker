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

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class provides the strengthening for a HybridAnalysisState via a ValueAnalysisState
 */
public class ValueAnalysisHybridStrengthenOperator
    implements HybridStrengthenOperator {

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
    Set<MemoryLocation> trackedVariables = strengtheningState.getTrackedMemoryLocations();

    for(CBinaryExpression binaryExpression : assumptions) {

      CExpression leftHandSide = binaryExpression.getOperand1();
      if(leftHandSide instanceof CIdExpression) {

        // simple variable definition

      } else if(leftHandSide instanceof CArraySubscriptExpression) {

        CArraySubscriptExpression subscriptExpression = (CArraySubscriptExpression) leftHandSide;
        CExpression arrayExpression = subscriptExpression.getArrayExpression();

        // now we need to check, if we can
        if(arrayExpression instanceof CIdExpression) {


        }
      }
    }


    return pStateToStrengthen;
  }

  private boolean compareNames(
      String pAssumptionVarName,
      MemoryLocation pMemoryLocation,
      boolean keepOffset) {

    StringBuilder nameBuilder = new StringBuilder();

    if(pAssumptionVarName.contains("::")) {
      nameBuilder.append(pMemoryLocation.getFunctionName()).append("::");
    }

    nameBuilder.append(pMemoryLocation.getIdentifier());

    if(keepOffset) {
      nameBuilder.append("/").append(pMemoryLocation.getOffset());
    }

    return pAssumptionVarName.equals(nameBuilder.toString());
  }

}