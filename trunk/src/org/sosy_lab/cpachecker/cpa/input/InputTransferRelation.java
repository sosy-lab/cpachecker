// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.input;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class InputTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;

  public InputTransferRelation(CFA pCFA) {
    cfa = Objects.requireNonNull(pCFA);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge) {
    return Collections.singleton(getAbstractSuccessorForEdge(pEdge));
  }

  private InputState getAbstractSuccessorForEdge(CFAEdge pEdge) {
    switch (pEdge.getEdgeType()) {
      case DeclarationEdge:
        return handleDeclarationEdge((ADeclarationEdge) pEdge);
      case StatementEdge:
        return handleStatementEdge((AStatementEdge) pEdge);
      case FunctionCallEdge:
        return handleFunctionCallEdge((FunctionCallEdge) pEdge);
      default:
        return InputState.empty();
    }
  }

  private static InputState handleDeclarationEdge(ADeclarationEdge pEdge) {
    ADeclaration declaration = pEdge.getDeclaration();
    if (declaration instanceof AVariableDeclaration) {
      AVariableDeclaration variableDeclaration = (AVariableDeclaration) declaration;
      if (variableDeclaration.getInitializer() == null) {
        return InputState.forInputs(Collections.singleton(variableDeclaration.getQualifiedName()));
      }
    }
    return InputState.empty();
  }

  private InputState handleStatementEdge(AStatementEdge pEdge) {
    AStatement statement = pEdge.getStatement();
    if (statement instanceof AAssignment) {
      AAssignment assignment = (AAssignment) statement;
      ALeftHandSide lhs = assignment.getLeftHandSide();
      if (!(lhs instanceof AIdExpression)) {
        // Unhandled left-hand side
        return InputState.empty();
      }
      String lhsVariable = ((AIdExpression) lhs).getDeclaration().getQualifiedName();
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
            return InputState.forInputs(ImmutableSet.of(lhsVariable, functionName));
          }
          Optional<? extends AVariableDeclaration> retVar = functionEntryNode.getReturnVariable();
          if (!retVar.isPresent()) {
            return InputState.forInputs(Collections.singleton(lhsVariable));
          }
        }
      }
    }
    return InputState.empty();
  }

  private static InputState handleFunctionCallEdge(FunctionCallEdge pEdge) {
    FunctionSummaryEdge summaryEdge = pEdge.getSummaryEdge();
    if (summaryEdge == null) {
      return InputState.empty();
    }
    return handleFunctionCall(
        summaryEdge.getFunctionEntry().getFunctionParameters(), pEdge.getArguments());
  }

  private static InputState handleFunctionCall(
      List<? extends AParameterDeclaration> pFunctionParameters,
      List<? extends AExpression> pCallArguments) {
    Iterator<? extends AParameterDeclaration> parameterIt = pFunctionParameters.iterator();
    Iterator<? extends AExpression> argumentIt = pCallArguments.iterator();
    ImmutableSet.Builder<String> inputs = ImmutableSet.builder();
    while (parameterIt.hasNext()) {
      AParameterDeclaration paramDecl = parameterIt.next();
      if (!argumentIt.hasNext()) {
        inputs.add(paramDecl.getQualifiedName());
      }
    }
    return InputState.forInputs(inputs.build());
  }
}
