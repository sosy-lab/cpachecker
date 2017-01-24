/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.nondeterminism;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismAbstractionState;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismNonAbstractionState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class NondeterminismTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;

  public NondeterminismTransferRelation(CFA pCFA) {
    this.cfa = pCFA;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    final NondeterminismNonAbstractionState state;
    if (pState instanceof NondeterminismAbstractionState) {
      state = new NondeterminismNonAbstractionState();
    } else {
      state = (NondeterminismNonAbstractionState) pState;
    }
    return Collections.singleton(handleEdge(state, pEdge));
  }

  private NondeterminismNonAbstractionState handleEdge(
      NondeterminismNonAbstractionState pState, CFAEdge pEdge) {
    Objects.requireNonNull(pState);
    switch (pEdge.getEdgeType()) {
      case DeclarationEdge:
        return handleDeclaration(pState, (ADeclarationEdge) pEdge);
      case StatementEdge:
        return handleStatement(pState, (AStatementEdge) pEdge);
      case ReturnStatementEdge:
        return handleReturnStatement(pState, (AReturnStatementEdge) pEdge);
      case FunctionCallEdge:
        return handleFunctionCall(pState, (FunctionCallEdge) pEdge);
      case AssumeEdge:
        return handleAssumption(pState, (AssumeEdge) pEdge);
      default:
        break;
    }
    return pState;
  }

  private static NondeterminismNonAbstractionState handleDeclaration(
      NondeterminismNonAbstractionState pState, ADeclarationEdge pEdge) {
    ADeclaration declaration = pEdge.getDeclaration();
    if (declaration instanceof AVariableDeclaration) {
      AVariableDeclaration variableDeclaration = (AVariableDeclaration) declaration;
      if (variableDeclaration.getInitializer() != null) {
        AInitializer initializer = variableDeclaration.getInitializer();
        return handleAssignment(pState, declaration.getQualifiedName(), getVariables(initializer));
      }
    }
    return pState;
  }

  private NondeterminismNonAbstractionState handleStatement(
      NondeterminismNonAbstractionState pState, AStatementEdge pEdge) {
    AStatement statement = pEdge.getStatement();
    if (statement instanceof AExpressionStatement) {
      return pState;
    }
    if (statement instanceof AAssignment) {
      AAssignment assignment = (AAssignment) statement;
      ALeftHandSide lhs = assignment.getLeftHandSide();
      if (!(lhs instanceof AIdExpression)) {
        // Unhandled left-hand side
        return new NondeterminismNonAbstractionState();
      }
      String lhsVariable = ((AIdExpression) lhs).getDeclaration().getQualifiedName();
      if (assignment instanceof AExpressionAssignmentStatement) {
        AExpressionAssignmentStatement exprAssignment = (AExpressionAssignmentStatement) assignment;
        return handleAssignment(pState, lhsVariable, exprAssignment.getRightHandSide());
      }
      if (assignment instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement callAssignment =
            (AFunctionCallAssignmentStatement) assignment;
        AFunctionCallExpression callExpression = callAssignment.getRightHandSide();
        AExpression functionNameExpression = callExpression.getFunctionNameExpression();
        if (functionNameExpression instanceof AIdExpression) {
          AIdExpression functionIdExpression = (AIdExpression) functionNameExpression;
          String functionName = functionIdExpression.getName();
          FunctionEntryNode functionEntryNode = cfa.getAllFunctions().get(functionName);
          if (functionEntryNode == null) {
            // External function
            return pState
                .addUnconstrainedNondetVariable(lhsVariable)
                .addUnconstrainedNondetVariable(functionName);
          }
          Optional<? extends AVariableDeclaration> retVar = functionEntryNode.getReturnVariable();
          if (!retVar.isPresent()) {
            return pState.addUnconstrainedNondetVariable(lhsVariable);
          }
          // Propagate artificial function return variable
          String varName = retVar.get().getQualifiedName();
          return handleAssignment(pState, lhsVariable, Collections.singleton(varName));
        }
        // May miss nondeterministic assignments for function pointers
        return pState.removeUnconstrainedNondetVariable(lhsVariable);
      }
      return pState.removeUnconstrainedNondetVariable(lhsVariable);
    }
    return pState;
  }

  private static NondeterminismNonAbstractionState handleReturnStatement(
      NondeterminismNonAbstractionState pState, AReturnStatementEdge pEdge) {
    if (!pEdge.getExpression().isPresent()) {
      return pState;
    }
    Optional<? extends AVariableDeclaration> retVar =
        pEdge.getSuccessor().getEntryNode().getReturnVariable();
    if (!retVar.isPresent()) {
      return pState;
    }
    return handleAssignment(pState, retVar.get().getQualifiedName(), pEdge.getExpression().get());
  }

  private static NondeterminismNonAbstractionState handleAssignment(
      NondeterminismNonAbstractionState pState, String pLhsVariable, ARightHandSide pRhs) {
    Set<String> rhsVariables = getVariables(pRhs);
    return handleAssignment(pState, pLhsVariable, rhsVariables);
  }

  private static NondeterminismNonAbstractionState handleAssignment(
      NondeterminismNonAbstractionState pState, String pLhsVariable, Set<String> pRhsVariables) {
    if (!pRhsVariables.isEmpty()
        && pState.getUnconstrainedNondetVariables().containsAll(pRhsVariables)) {
      return pState.addUnconstrainedNondetVariable(pLhsVariable);
    }
    return pState.removeUnconstrainedNondetVariable(pLhsVariable);
  }

  private static NondeterminismNonAbstractionState handleFunctionCall(
      NondeterminismNonAbstractionState pState, FunctionCallEdge pEdge) {
    FunctionSummaryEdge summaryEdge = pEdge.getSummaryEdge();
    if (summaryEdge == null) {
      return pState;
    }
    return handleFunctionCall(
        pState, summaryEdge.getFunctionEntry().getFunctionParameters(), pEdge.getArguments());
  }

  private static NondeterminismNonAbstractionState handleFunctionCall(
      NondeterminismNonAbstractionState pState,
      List<? extends AParameterDeclaration> pFunctionParameters,
      List<? extends AExpression> pCallArguments) {
    Iterator<? extends AParameterDeclaration> parameterIt = pFunctionParameters.iterator();
    Iterator<? extends AExpression> argumentIt = pCallArguments.iterator();
    NondeterminismNonAbstractionState state = pState;
    while (parameterIt.hasNext()) {
      AParameterDeclaration paramDecl = parameterIt.next();
      if (!argumentIt.hasNext()) {
        state = state.addUnconstrainedNondetVariable(paramDecl.getQualifiedName());
      } else {
        state = handleAssignment(state, paramDecl.getQualifiedName(), argumentIt.next());
      }
    }
    return state;
  }

  private static NondeterminismNonAbstractionState handleAssumption(
      NondeterminismNonAbstractionState pState, AssumeEdge pEdge) {
    return pState.removeUnconstrainedNondetVariables(getVariables(pEdge.getExpression()));
  }

  private static Set<String> getVariables(AAstNode pAstNode) {
    return CFAUtils.traverseRecursively(pAstNode)
        .filter(CIdExpression.class)
        .transform(id -> id.getDeclaration().getQualifiedName())
        .toSet();
  }
}
