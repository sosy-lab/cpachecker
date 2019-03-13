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
package org.sosy_lab.cpachecker.cpa.harness;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.harness.PointerFunctionExtractor;

@Options(prefix = "cpa.harness")
public class HarnessTransferRelation
    extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final Set<AFunctionDeclaration> unimplementedPointerReturnTypeFunctions;

  public HarnessTransferRelation(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this, HarnessTransferRelation.class);
    logger = new LogManagerWithoutDuplicates(pLogger);
    unimplementedPointerReturnTypeFunctions =
        PointerFunctionExtractor.getExternUnimplementedPointerReturnTypeFunctions(pCFA);
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pEdge)
          throws CPATransferException {

    HarnessState state = (HarnessState) pState;
    HarnessState result = (HarnessState) pState;

    if (!(pEdge.getFileLocation().getNiceFileName() == "")) {
      return Collections.singleton(result);
    }
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        result = handleAssumeEdge(state, (CAssumeEdge) pEdge);
        break;
      case BlankEdge:
        break;
      case CallToReturnEdge:
        break;
      case DeclarationEdge:
        result = handleDeclarationEdge(state, (CDeclarationEdge) pEdge);
        break;
      case FunctionCallEdge:
        result = handleFunctionCallEdge(state, (CFunctionCallEdge) pEdge);
        break;
      case FunctionReturnEdge:
        result = handleFunctionReturnEdge(state, (CFunctionReturnEdge) pEdge);
        break;
      case ReturnStatementEdge:
        result = handleReturnStatementEdge(state, (CReturnStatementEdge) pEdge);
        break;
      case StatementEdge:
        result = handleStatementEdge(state, (CStatementEdge) pEdge);
        break;
      default: {
        throw new UnrecognizedCCodeException("Unrecognized CFA edge.", pEdge);
      }
    }
    return Collections.singleton(result);
  }

  private HarnessState handleAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    CExpression expression = pEdge.getExpression();
    boolean modifier = pEdge.getTruthAssumption();
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;
      CExpression operand1 = binaryExpression.getOperand1();
      CType operand1Type = operand1.getExpressionType();
      if (operand1Type instanceof CPointerType) {
        BinaryOperator binaryOperator = binaryExpression.getOperator();
        if ((binaryOperator == BinaryOperator.EQUALS && modifier)
            || (binaryOperator == BinaryOperator.NOT_EQUALS && !modifier)) {
          return handleEqualityAssumeEdge(pState, pEdge);
        }
      }
    }
    return pState;
  }

  private HarnessState handleEqualityAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    CBinaryExpression binaryExpression = (CBinaryExpression) pEdge.getExpression();
    CExpression operand1 = binaryExpression.getOperand1();
    CExpression operand2 = binaryExpression.getOperand2();
    if (operand1 instanceof CIdExpression && operand2 instanceof CIdExpression) {
      // TODO: handle cases where operands are not id expressions, such as *p == *q, p.i == q.i
      CIdExpression operand1IdExpression = (CIdExpression) operand1;
      String operand1Name = operand1IdExpression.getName();
      CIdExpression operand2IdExpression = (CIdExpression) operand2;
      String operand2Name = operand2IdExpression.getName();
      HarnessState newState = pState.merge(operand1Name, operand2Name);
      return newState;
    }
    return pState;
  }

  private HarnessState handleInequalityAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    return pState;
  }

  private HarnessState handleReturnStatementEdge(HarnessState pState, CReturnStatementEdge pEdge)
      throws UnrecognizedCCodeException {
    if (!pEdge.getExpression().isPresent()) {
      return pState;
    }
    return pState;
  }

  private HarnessState handleFunctionReturnEdge(HarnessState pState, CFunctionReturnEdge pEdge) {
    return pState;
  }

  private HarnessState handleFunctionCallEdge(HarnessState pState, CFunctionCallEdge pEdge) {
    HarnessState newState = pState;
    CFunctionCall functionCall = pEdge.getSummaryEdge().getExpression();
    CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
    CFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    return pState;
  }

  private boolean declarationHasPointerType(CDeclarationEdge pDeclaration) {
    return (pDeclaration.getDeclaration().getType() instanceof CPointerType);
  }

  private boolean declarationHasArrayType(CDeclarationEdge pDeclaration) {
    return (pDeclaration.getDeclaration().getType() instanceof CArrayType);
  }

  private boolean isVariableDeclaration(CDeclarationEdge pDeclaration) {
    return pDeclaration.getDeclaration() instanceof CVariableDeclaration;
  }

  private HarnessState handleDeclarationEdge(HarnessState pState, CDeclarationEdge pEdge) {
    if (!isVariableDeclaration(pEdge)) {
      return pState;
    }
    if (declarationHasPointerType(pEdge)) {
      return handlePointerDeclaration(pState, pEdge);
    } else if (declarationHasArrayType(pEdge)) {
      return handleArrayDeclaration(pState, pEdge);
    } else {
      return handleSimpleDeclaration(pState, pEdge);
    }
  }

  private HarnessState handleSimpleDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    return pState.addMemoryLocation(declaration);
  }

  private HarnessState handlePointerDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    MemoryLocation sourceLocation = new MemoryLocation(declaration.getName());
    HarnessState newState = pState.addMemoryLocation(sourceLocation);
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      return handlePointerDeclarationWithInitializer(newState, sourceLocation, initializer);
    }
    return newState;
  }

  private HarnessState handleArrayDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    MemoryLocation sourceLocation = new MemoryLocation(declaration.getName());
    HarnessState newState = pState.addMemoryLocation(sourceLocation);
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      return handleArrayDeclarationWithInitializer(
          newState,
          sourceLocation,
          initializer);
    }
    return newState;
  }

  private HarnessState handlePointerDeclarationWithInitializer(
      HarnessState pState,
      MemoryLocation pSourceLocation,
      CInitializer pInitializer) {
    return handlePointerAssignment(pSourceLocation, pState, pInitializer);
  }

  private HarnessState handleArrayDeclarationWithInitializer(
      HarnessState pState,
      MemoryLocation pSourceLocation,
      CInitializer pInitializer) {
    return handlePointerAssignment(pSourceLocation, pState, pInitializer);
  }

  private HarnessState handlePointerAssignment(
      MemoryLocation pLeftLocation,
      HarnessState pState,
      CInitializer pInitializer) {
    return pState.addPointsToInformation(pLeftLocation, (CExpression) pInitializer);
  }

  private HarnessState handlePointerAssignment(HarnessState pState, CFAEdge pEdge) {
    HarnessState newState = pState.updatePointerTarget((CAssignment) pEdge);
    return newState;
  }


  private HarnessState handleStatementEdge(HarnessState pState, CStatementEdge pEdge) {
    CStatement statement = pEdge.getStatement();
    if (statement instanceof CFunctionCallStatement) {
      CFunctionCallStatement functionCallStatement = (CFunctionCallStatement) statement;
      CFunctionCallExpression functionCallExpression =
          functionCallStatement.getFunctionCallExpression();
      CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (isExternFunction(functionCallExpression)) {
        List<CExpression> functionParameters = functionCallExpression.getParameterExpressions();
        List<CExpression> functionParametersOfPointerType =
            functionParameters.stream()
                .filter(cExpression -> (cExpression.getExpressionType() instanceof CPointerType))
                .collect(Collectors.toList());
        HarnessState newState = pState.addExternallyKnownLocations(functionParametersOfPointerType);
        return newState;
      }
    }
    if (statement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) statement;
      if (statement instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement functionCallAssignment =
            (CFunctionCallAssignmentStatement) assignment;
        CFunctionCallExpression functionCallExpression =
            functionCallAssignment.getFunctionCallExpression();
        CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        if (isSystemMemoryAllocation(functionNameExpression)) {
          MemoryLocation newMemoryLocation = new MemoryLocation();
          CLeftHandSide lhs = functionCallAssignment.getLeftHandSide();
          HarnessState newState = pState.addPointsToInformation(lhs, newMemoryLocation);
          return newState;
        }
        if (isExternFunction(functionCallExpression)) {
          MemoryLocation newMemoryLocation = new MemoryLocation(false);
          CLeftHandSide lhs = functionCallAssignment.getLeftHandSide();
          if (lhs instanceof CIdExpression) {
            HarnessState newState =
                pState.addPointsToInformation(lhs, newMemoryLocation)
                    .addFunctionCall(functionNameExpression, newMemoryLocation);
            return newState;
          }
          return pState;
        }
      }
    }
    return pState;
  }

  private boolean isSystemMemoryAllocation(CExpression pFunctionNameExpression) {
    if (pFunctionNameExpression instanceof CIdExpression) {
      String functionName = ((CIdExpression) pFunctionNameExpression).getName();
      return functionName.equals("malloc")
          || functionName.equals("alloca")
          || functionName.equals("kmalloc")
          || functionName.equals("__kmalloc");
    } else {
      return false;
    }
  }

  private boolean isExternFunction(CFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration functionDeclaration = pFunctionCallExpression.getDeclaration();
    return unimplementedPointerReturnTypeFunctions.contains(functionDeclaration);

  }
}