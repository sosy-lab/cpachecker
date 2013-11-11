/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.forwarding.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.sign.SignState.SIGN;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;


public class SignTransferRelation extends ForwardingTransferRelation<SignState, SingletonPrecision> {

  @SuppressWarnings("unused")
  private LogManager logger;

  private interface SignAssignmentStrategy {
    Optional<SIGN> computeSign(CRightHandSide pRightExpr, Map<String, SIGN> pSignMap) throws CPATransferException;
  }

  private final SignAssignmentStrategy VISITOR = new SignAssignmentStrategy() {
    @Override
    public Optional<SIGN> computeSign(CRightHandSide pRightExpr, Map<String, SIGN> pSignMap)
        throws CPATransferException {
      return pRightExpr.accept(new SignCExpressionVisitor(edge, pSignMap));
    }
  };

  private final SignAssignmentStrategy ZERO = new SignAssignmentStrategy() {
    @Override
    public Optional<SIGN> computeSign(CRightHandSide pRightExpr, Map<String, SIGN> pSignMap)
        throws CPATransferException {
      return Optional.of(SIGN.ZERO);
    }
  };

  public SignTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }


  @Override
  protected SignState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    // TODO implement as well as other ForwardingTransferRelation methods that throw new AssertionError(NOT_IMPLEMENTED)
    // e.g. if does not change state like assume statement return getState();
    return null;
  }

  @Override
  protected SignState handleDeclarationEdge(ADeclarationEdge pCfaEdge, IADeclaration pDecl) throws CPATransferException {
    if(!(pDecl instanceof AVariableDeclaration)) {
      return state;
    }
    AVariableDeclaration decl = (AVariableDeclaration)pDecl;
    String scopedId = decl.isGlobal() ? decl.getName() : functionName + "::" + decl.getName();
    IAInitializer init = decl.getInitializer();
    if(init instanceof AInitializerExpression) {
      return handleDeclaration(scopedId, ((AInitializerExpression)init).getExpression(), VISITOR);
    }
    // default sign is zero
    // TODO since it is C, we better assume it may have any value here
    return handleDeclaration(scopedId, ((AInitializerExpression)init).getExpression(), ZERO);
  }

  private SignState handleDeclaration(String pId, IAExpression pInit, SignAssignmentStrategy pStrategy)
      throws CPATransferException {
    if(pInit instanceof ARightHandSide) {
      return handleAssignmentToVariable(pId, pInit, pStrategy);
    }
    throw new UnrecognizedCodeException("unhandled initializer expression", edge);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, IAStatement pStatement) throws CPATransferException {
    // expression is a binary expressionm e.g. a = b.
    if(pStatement instanceof IAssignment) {
      return handleAssignment((IAssignment)pStatement);
    }
    throw new UnrecognizedCodeException("only assignments are supported at this time", edge);
  }

  private SignState handleAssignment(IAssignment pAssignExpr)
      throws CPATransferException {
    IAExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if(left instanceof AIdExpression) {
      String pId = getScopedVariableName(left);
      return handleAssignmentToVariable(pId, pAssignExpr.getRightHandSide(), VISITOR);
    }
    throw new UnrecognizedCodeException("left operand has to be an id expression", edge);
  }

  private SignState handleAssignmentToVariable(String pId, IARightHandSide pRightExpr, SignAssignmentStrategy pSignStrategy)
    throws CPATransferException {
    Set<Map<String, SIGN>> possibleSigns = new HashSet<>();
    if(pRightExpr instanceof CRightHandSide) {
      CRightHandSide right = (CRightHandSide)pRightExpr;
      // If there are no sign assumptions within the state, create new assumption
      if(state.getPossibleSigns().isEmpty()) {
        Optional<SIGN> result = pSignStrategy.computeSign(right, ImmutableMap.<String, SIGN>of());
        if(result.isPresent()) {
          ImmutableMap<String, SIGN> varMap = ImmutableMap.of(pId, result.get());
          possibleSigns.add(varMap);
        } else {
          return state;
        }
      }
      // Otherwise update all given sign assumptions
      for(Map<String, SIGN> signMap : state.getPossibleSigns()) {
        Builder<String, SIGN> mapBuilder = ImmutableMap.builder();
        Optional<SIGN> result = pSignStrategy.computeSign(right, signMap);
        if(result.isPresent()) {
          mapBuilder.put(pId, result.get());
        }
        for(String key : signMap.keySet()) {
          if(!key.equals(pId)) {
            mapBuilder.put(key, signMap.get(key));
          }
        }
        possibleSigns.add(mapBuilder.build());
      }
      return new SignState(possibleSigns);
    }
    throw new UnrecognizedCodeException("unhandled righthandside expression", edge);
  }

  private String getScopedVariableName(IAExpression pVariableName) {
    if (isGlobal(pVariableName)) {
      return pVariableName.toASTString();
    }
    return functionName + "::" + pVariableName.toASTString();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
