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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.harness.PointerFunctionExtractor;

@Options(prefix = "cpa.harness")
public class HarnessTransferRelation
    extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final Set<AFunctionDeclaration> unimplementedPointerReturnTypeFunctions;
  private final Set<AFunctionDeclaration> relevantFunctions;
  CFA cfa;

  public HarnessTransferRelation(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this, HarnessTransferRelation.class);
    logger = new LogManagerWithoutDuplicates(pLogger);
    unimplementedPointerReturnTypeFunctions =
        PointerFunctionExtractor.getExternUnimplementedPointerReturnTypeFunctions(pCFA);
    relevantFunctions = PointerFunctionExtractor.getRelevantFunctions(pCFA);
    cfa = pCFA;
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
        throw new UnrecognizedCodeException("Unrecognized CFA edge.", pEdge);
      }
    }
    return Collections.singleton(result);
  }

  private HarnessState handleAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    CExpression expression = pEdge.getExpression();
    boolean modifier = pEdge.getTruthAssumption();
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;
      CExpression firstOperand = binaryExpression.getOperand1();
      CType firstOperandExpressionType = firstOperand.getExpressionType();
      boolean isPointerComparison = firstOperandExpressionType instanceof CPointerType;
      if (isPointerComparison) {
        BinaryOperator binaryOperator = binaryExpression.getOperator();
        if ((binaryOperator == BinaryOperator.EQUALS && modifier)
            || (binaryOperator == BinaryOperator.NOT_EQUALS && !modifier)) {
          return handlePointerTypeEqualityAssumeEdge(pState, pEdge);
        }
        else if ((binaryOperator == BinaryOperator.NOT_EQUALS && modifier)
            || (binaryOperator == BinaryOperator.EQUALS && !modifier)) {
          return handlePointerTypeInequalityAssumeEdge(pState, pEdge);
        }
      }
    }
    return pState;
  }

  private HarnessState handlePointerTypeEqualityAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    CBinaryExpression binaryExpression = (CBinaryExpression) pEdge.getExpression();
    CExpression operand1 = binaryExpression.getOperand1();
    CExpression operand2 = binaryExpression.getOperand2();
    HarnessState newState = pState.addPointerTypeEqualityAssumption(operand1, operand2);
    return newState;
  }

  private HarnessState
      handlePointerTypeInequalityAssumeEdge(HarnessState pState, CAssumeEdge pEdge) {
    return pState;
  }

  private HarnessState handleReturnStatementEdge(HarnessState pState, CReturnStatementEdge pEdge) {
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
    return newState;
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
    } else if (hasStructTypeWithPointerField(pEdge)) {
      return handleRelevantTypeStructDeclaration(pState, pEdge);
    }
    return pState;
  }

  private HarnessState
      handleRelevantTypeStructDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CDeclaration declaration = pEdge.getDeclaration();
    CVariableDeclaration structVariableDeclaration = (CVariableDeclaration) declaration;
    CInitializer initializer = structVariableDeclaration.getInitializer();
    if (initializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) initializer;
      List<CInitializer> initializers = initializerList.getInitializers();
      for (CInitializer fieldInitializer : initializers) {
        if (fieldInitializer instanceof CDesignatedInitializer) {
          CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) fieldInitializer;
          CInitializer rhs = designatedInitializer.getRightHandSide();
          if (rhs instanceof CInitializerExpression) {
            CInitializerExpression initializerExpression = (CInitializerExpression) rhs;
            CExpression expression = initializerExpression.getExpression();
            CType expressionType = expression.getExpressionType();
            if (expressionType instanceof CPointerType) {
              if (expression instanceof CIdExpression
                  || (expression instanceof CFunctionCallExpression
                      && isExternFunction((CFunctionCallExpression) expression))) {
                return handleStructDeclarationWithPointerFieldWithInitializer(pState, pEdge);
              }
            }
          }
        }
      }
    }
    return pState;
  }

  private HarnessState handleStructDeclarationWithPointerFieldWithInitializer(
      HarnessState pState,
      CDeclarationEdge pEdge) {

    HarnessState newState =
        pState.handleStructDeclarationWithPointerFieldWithInitializer(pState, pEdge);
    return newState;

  }

  private boolean hasStructTypeWithPointerField(CDeclarationEdge pEdge) {
    boolean hasPointerMembers = false;
    boolean isStruct = false;
    CDeclaration declaration = pEdge.getDeclaration();
    CType declarationType = declaration.getType();
    if (declarationType instanceof CElaboratedType) {
      CElaboratedType elaboratedDeclarationType = (CElaboratedType) declarationType;
      if (elaboratedDeclarationType.getKind() == ComplexTypeKind.STRUCT) {
        isStruct = true;
        CCompositeType realDeclarationType =
            (CCompositeType) elaboratedDeclarationType.getRealType();
        List<CCompositeTypeMemberDeclaration> memberList = realDeclarationType.getMembers();
        hasPointerMembers =
            memberList.stream()
            .filter(memberDeclaration -> memberDeclaration.getType() instanceof CPointerType)
            .findFirst()
            .isPresent();
      }
    }
    return hasPointerMembers && isStruct;
  }

  private HarnessState handlePointerDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    String name = declaration.getQualifiedName();
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      return pState.addPointerVariableInitialization(name, initializer);
    }
    return pState;
  }

  private HarnessState handleArrayDeclaration(HarnessState pState, CDeclarationEdge pEdge) {
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    String lhs = declaration.getQualifiedName();
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      return pState.handleArrayDeclarationWithInitializer(
          lhs,
          initializer);
    }
    return pState;
  }

  private HarnessState handleStatementEdge(HarnessState pState, CStatementEdge pEdge) {
    CStatement statement = pEdge.getStatement();
    if (statement instanceof CFunctionCallStatement) {
      CFunctionCallStatement functionCallStatement = (CFunctionCallStatement) statement;
      CFunctionCallExpression functionCallExpression =
          functionCallStatement.getFunctionCallExpression();
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
          CLeftHandSide lhs = functionCallAssignment.getLeftHandSide();
          CRightHandSide rhs = functionCallAssignment.getRightHandSide();
          HarnessState newState = pState.addPointerVariableAssignment(lhs, rhs);
          return newState;
        }
        if (isExternFunction(functionCallExpression)) {
          CLeftHandSide lhs = functionCallAssignment.getLeftHandSide();
          HarnessState newState =
              pState
                  .addPointerVariableToUndefinedFunctionCallAssignment(lhs, functionCallExpression);
          return newState;
        }
      }
    }
    return pState;
  }

  private boolean isSystemMemoryAllocationCall(CInitializer pInitializer) {
    if (pInitializer instanceof CInitializerExpression) {
      CInitializerExpression initializerExpression = (CInitializerExpression) pInitializer;
      CExpression expression = initializerExpression.getExpression();
      if (expression instanceof CFunctionCallExpression) {
        CFunctionCallExpression functionCallExpression = (CFunctionCallExpression) expression;
        CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        return isSystemMemoryAllocation(functionNameExpression);
      }
    }
    return false;
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
    return relevantFunctions.contains(functionDeclaration);

  }



}