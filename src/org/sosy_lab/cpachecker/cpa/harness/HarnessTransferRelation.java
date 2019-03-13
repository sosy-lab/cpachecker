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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

@Options(prefix = "cpa.harness")
public class HarnessTransferRelation
    extends SingleEdgeTransferRelation {


  private final LogManager logger;
  private final ImmutableSet<String> externPointerFunctions;

  public HarnessTransferRelation(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this, HarnessTransferRelation.class);
    logger = new LogManagerWithoutDuplicates(pLogger);
    externPointerFunctions = extractExternPointerFunctions(pCFA);
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pEdge)
          throws CPATransferException {

    HarnessState state = (HarnessState) pState;
    HarnessState result = (HarnessState) pState;

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        result = handleAssumeEdge(state, (CAssumeEdge) pEdge);
        break;
      case BlankEdge:
        break;
      case CallToReturnEdge:
        break;
      case DeclarationEdge:
        handleDeclarationEdge(state, (CDeclarationEdge) pEdge);
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
    /*
     * TODO handle implicit struct/union equality where it can be inferred that bar() returns q from
     * knowing that q.i = someInt; foo(q); p = bar(); p.i == someInt;
     */
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

    // check if function is external, and takes pointer parameters, in that case add that parameter
    // to array
    // if it is external but no pointer params, do nothing
    // if it is internal, and has pointer params, update pointers with assignments of
    // qualified param names to their param values
    // how to check if it is extern? check if it has any code associated with it
    // as there is no storage class attribute for function declarations

    /*
     * List<CParameterDeclaration> formalParams = pEdge.getSuccessor().getFunctionParameters();
     * List<CExpression> actualParams = pEdge.getArguments(); int limit =
     * Math.min(formalParams.size(), actualParams.size()); formalParams =
     * FluentIterable.from(formalParams).limit(limit).toList(); actualParams =
     * FluentIterable.from(actualParams).limit(limit).toList();
     *
     * Handle the mapping of arguments to formal parameters for (Pair<CParameterDeclaration,
     * CExpression> param : Pair .zipList(formalParams, actualParams)) { CExpression actualParam =
     * param.getSecond(); CParameterDeclaration formalParam = param.getFirst(); MemoryLocation
     * location = new MemoryLocation(formalParam); newState = handleAssignment(newState, location,
     * pState.getLocation(actualParam)); } return newState;
     */
  }

  private HarnessState handleDeclarationEdge(HarnessState pState, CDeclarationEdge pEdge) {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pState;
    }
    CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
    CInitializer initializer = declaration.getInitializer();
    if (initializer != null) {
      String declarationName = declaration.getQualifiedName();
      MemoryLocation newMemoryLocation = new MemoryLocation(declarationName);
      return handleWithInitializer(pState, newMemoryLocation, declaration.getType(), initializer);
    }
    HarnessState newState = pState.addPointerDeclaration(declaration);
    return newState;
  }

  private HarnessState handleWithInitializer(
      HarnessState pState,
      MemoryLocation pLocation,
      CType pType,
      CInitializer pInitializer) {
    return pState;
  }

  private HarnessState handleStatementEdge(HarnessState pState, CStatementEdge pEdge) {
    CStatement statement = pEdge.getStatement();
    if (statement instanceof CFunctionCallStatement) {
      CFunctionCallStatement functionCallStatement = (CFunctionCallStatement) statement;
      CFunctionCallExpression functionCallExpression =
          functionCallStatement.getFunctionCallExpression();
      CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (isExternFunction(functionNameExpression)) {
        List<CExpression> functionParameters = functionCallExpression.getParameterExpressions();
        List<CExpression> functionParametersOfPointerType =
            functionParameters.stream()
                .filter(cExpression -> (cExpression.getExpressionType() instanceof CPointerType))
                .collect(Collectors.toList());
        HarnessState newState = pState.addExternallyKnownLocations(functionParametersOfPointerType);
        HarnessState.relevantFunctions.add(functionCallExpression.getDeclaration());
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
        if (isExternFunction(functionNameExpression)) {
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

  private boolean isExternFunction(CExpression pFunctionNameExpression) {
    if (pFunctionNameExpression instanceof CIdExpression) {
      String functionName = ((CIdExpression) pFunctionNameExpression).getName();
      return externPointerFunctions.contains(functionName);
    } else {
      return false;
    }
  }

  private ImmutableSet<String> extractExternPointerFunctions(CFA pCFA) {
    Set<String> foundExternPointerFunctions = new HashSet<>();
    CFAVisitor externalFunctionCollector = new CFAVisitor() {

      private CFA cfa = pCFA;

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitEdge(CFAEdge pEdge) {
        if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
          ADeclaration declaration = declarationEdge.getDeclaration();
          if (declaration instanceof AFunctionDeclaration) {
            AFunctionDeclaration functionDeclaration = (AFunctionDeclaration) declaration;
            if (!cfa.getAllFunctionNames().contains(functionDeclaration.getName())) {
              boolean headIsEmpty = (cfa.getFunctionHead(declaration.getQualifiedName()) == null);

              boolean hasPointerParameter =
                  functionDeclaration.getParameters()
                      .stream()
                      .map(o -> o.getType())
                      .filter(type -> type instanceof CPointerType)
                      .findFirst()
                      .isPresent();
              if (hasPointerParameter && headIsEmpty) {
                foundExternPointerFunctions.add(functionDeclaration.getName());
                // HarnessState.relevantFunctions.add((CFunctionDeclaration) functionDeclaration);
              }
              boolean hasPointerReturnType = (functionDeclaration.getType().getReturnType() instanceof CPointerType);
              if (hasPointerReturnType && headIsEmpty) {
                foundExternPointerFunctions.add(functionDeclaration.getName());
                // HarnessState.relevantFunctions.add((CFunctionDeclaration) functionDeclaration);
              }
            }
          }
        }
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(pCFA.getMainFunction(), externalFunctionCollector);
    ImmutableSet<String> res = ImmutableSet.copyOf(foundExternPointerFunctions);
    return res;
  }


}