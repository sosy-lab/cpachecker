// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

  private final boolean acceptConstrained;

  public NondeterminismTransferRelation(CFA pCFA, boolean pAcceptConstrained) {
    cfa = pCFA;
    acceptConstrained = pAcceptConstrained;
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
            return pState.addNondetVariable(lhsVariable).addNondetVariable(functionName);
          }
          Optional<? extends AVariableDeclaration> retVar = functionEntryNode.getReturnVariable();
          if (!retVar.isPresent()) {
            return pState.addNondetVariable(lhsVariable);
          }
          // Propagate artificial function return variable
          String varName = retVar.get().getQualifiedName();
          return handleAssignment(pState, lhsVariable, Collections.singleton(varName));
        }
        // May miss nondeterministic assignments for function pointers
        return pState.removeNondetVariable(lhsVariable);
      }
      return pState.removeNondetVariable(lhsVariable);
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
    if (!pRhsVariables.isEmpty() && pState.getNondetVariables().containsAll(pRhsVariables)) {
      return pState.addNondetVariable(pLhsVariable);
    }
    return pState.removeNondetVariable(pLhsVariable);
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
        state = state.addNondetVariable(paramDecl.getQualifiedName());
      } else {
        state = handleAssignment(state, paramDecl.getQualifiedName(), argumentIt.next());
      }
    }
    return state;
  }

  private NondeterminismNonAbstractionState handleAssumption(
      NondeterminismNonAbstractionState pState, AssumeEdge pEdge) {
    if (acceptConstrained) {
      return pState;
    }
    return pState.removeNondetVariables(getVariables(pEdge.getExpression()));
  }

  private static Set<String> getVariables(AAstNode pAstNode) {
    return CFAUtils.traverseRecursively(pAstNode)
        .filter(CIdExpression.class)
        .transform(id -> id.getDeclaration().getQualifiedName())
        .toSet();
  }
}
