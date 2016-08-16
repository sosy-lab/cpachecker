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
package org.sosy_lab.cpachecker.cpa.validvars;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;


public class ValidVarsTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {

    ValidVarsState state = (ValidVarsState)pState;
    ValidVars validVariables = state.getValidVariables();

    switch (pCfaEdge.getEdgeType()) {
    case BlankEdge:
      if (pCfaEdge.getDescription().equals("Function start dummy edge") && !(pCfaEdge.getPredecessor() instanceof FunctionEntryNode)) {
        validVariables = validVariables.extendLocalVarsFunctionCall(pCfaEdge.getSuccessor().getFunctionName(),
            ImmutableSet.<String> of());
      }
      if(pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
        validVariables = validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
      }
      break;
    case FunctionCallEdge:
      validVariables = validVariables.extendLocalVarsFunctionCall(pCfaEdge.getSuccessor().getFunctionName(),
          ((FunctionEntryNode) pCfaEdge.getSuccessor()).getFunctionParameterNames());
      break;
    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge) pCfaEdge).getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        if (declaration.isGlobal()) {
          validVariables = validVariables.extendGlobalVars(declaration.getName());
        } else {
          validVariables =
              validVariables.extendLocalVars(pCfaEdge.getPredecessor().getFunctionName(), declaration.getName());
        }
      }
      break;
    case ReturnStatementEdge:
      validVariables = validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
      break;
    default:
      break;
    }

    if (state.getValidVariables() == validVariables) {
      return Collections.singleton(state);
    }
    return Collections.singleton(new ValidVarsState(validVariables));
  }
}
