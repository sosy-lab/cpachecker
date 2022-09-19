// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.validvars;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
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

public class ValidVarsTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ValidVarsState state = (ValidVarsState) pState;
    ValidVars validVariables = state.getValidVariables();

    switch (pCfaEdge.getEdgeType()) {
      case BlankEdge:
        if (pCfaEdge.getDescription().equals("Function start dummy edge")
            && !(pCfaEdge.getPredecessor() instanceof FunctionEntryNode)) {
          validVariables =
              validVariables.extendLocalVarsFunctionCall(
                  pCfaEdge.getSuccessor().getFunctionName(), ImmutableSet.of());
        }
        if (pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
          validVariables =
              validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
        }
        break;
      case FunctionCallEdge:
        validVariables =
            validVariables.extendLocalVarsFunctionCall(
                pCfaEdge.getSuccessor().getFunctionName(),
                ((FunctionEntryNode) pCfaEdge.getSuccessor()).getFunctionParameterNames());
        break;
      case DeclarationEdge:
        CDeclaration declaration = ((CDeclarationEdge) pCfaEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          if (declaration.isGlobal()) {
            validVariables = validVariables.extendGlobalVars(declaration.getName());
          } else {
            validVariables =
                validVariables.extendLocalVars(
                    pCfaEdge.getPredecessor().getFunctionName(), declaration.getName());
          }
        }
        break;
      case ReturnStatementEdge:
        validVariables =
            validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
        break;
      default:
        break;
    }

    if (state.getValidVariables() == validVariables) {
      return Collections.singleton(state);
    }
    return ImmutableSet.of(new ValidVarsState(validVariables));
  }
}
