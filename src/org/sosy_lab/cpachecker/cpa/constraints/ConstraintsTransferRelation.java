/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Transfer relation for Symbolic Execution Analysis.
 */
@Options(prefix="cpa.constraints")
public class ConstraintsTransferRelation
    extends ForwardingTransferRelation<ConstraintsState, ConstraintsState, SingletonPrecision> {

  private static final String AMPERSAND = "&";

  private final LogManager logger;

  public ConstraintsTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected ConstraintsState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends AExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) {
    return state;
  }

  @Override
  protected ConstraintsState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge,
      FunctionSummaryEdge pFunctionCallEdge, AFunctionCall pSummaryExpression, String pCallerFunctionName) {
    return state;
  }

  @Override
  protected ConstraintsState handleStatementEdge(AStatementEdge pCfaEdge, AStatement pStatement) {
    if (pStatement instanceof JAssignment) {
      JExpression leftHandSide = ((JAssignment) pStatement).getLeftHandSide();

      assert leftHandSide instanceof JIdExpression;
      String identifier = ((JIdExpression) leftHandSide).getName();

      new ExpressionToFormulaVisitor(functionName).addAlias(identifier);

    } else if (pStatement instanceof CAssignment) {
      CExpression leftHandSide = ((CAssignment) pStatement).getLeftHandSide();

      assert leftHandSide instanceof CIdExpression || leftHandSide instanceof CPointerExpression;
      String identifier;

      if (leftHandSide instanceof CPointerExpression) {
        leftHandSide = ((CPointerExpression) leftHandSide).getOperand();

        identifier = AMPERSAND + ((CIdExpression) leftHandSide).getName();
      } else {

        identifier = ((CIdExpression) leftHandSide).getName();
      }

      new ExpressionToFormulaVisitor(functionName).addAlias(identifier);
    }

    return state;
  }

  @Override
  protected ConstraintsState handleReturnStatementEdge(AReturnStatementEdge pCfaEdge) {
    return state;
  }

  @Override
  protected ConstraintsState handleFunctionSummaryEdge(FunctionSummaryEdge pCfaEdge) {
    return state;
  }

  @Override
  protected ConstraintsState handleDeclarationEdge(ADeclarationEdge pCfaEdge, ADeclaration pDeclaration)
      throws CPATransferException {
    String identifier = pDeclaration.getName();

    new ExpressionToFormulaVisitor(functionName).addAlias(identifier);

    return state;
  }

  @Override
  protected ConstraintsState handleAssumption(AssumeEdge pCfaEdge, AExpression pExpression, boolean pTruthAssumption) {
    ConstraintsState newState = ConstraintsState.copyOf(state);

    try {
      Optional<Constraint> newConstraint = createConstraint(pExpression, pTruthAssumption);

      if (newConstraint.isPresent()) {
        newState.addConstraint(newConstraint.get());
      }
    } catch (UnrecognizedCodeException e) {
      logger.logUserException(Level.WARNING, e, pCfaEdge.getFileLocation().toString());
    }

    return newState;
  }

  private Optional<Constraint> createConstraint(AExpression pExpression, boolean pTruthAssumption)
      throws UnrecognizedCodeException {
    Optional<Constraint> result;

    if (pExpression instanceof JBinaryExpression) {
      result = createConstraint((JBinaryExpression) pExpression, pTruthAssumption);

    } else if (pExpression instanceof CBinaryExpression) {
      result = createConstraint((CBinaryExpression) pExpression, pTruthAssumption);

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }

    return result;
  }

  private Optional<Constraint> createConstraint(JBinaryExpression pExpression, boolean pTruthAssumption)
      throws UnrecognizedCodeException {

    final ConstraintFactory factory = ConstraintFactory.getInstance(functionName);

    JExpression leftOperand = pExpression.getOperand1();
    JExpression rightOperand = pExpression.getOperand2();

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = factory.createPositiveConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    } else {
      constraint = factory.createNegativeConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(CBinaryExpression pExpression, boolean pTruthAssumption)
      throws UnrecognizedCodeException {

    final ConstraintFactory factory = ConstraintFactory.getInstance(functionName);

    CExpression leftOperand = pExpression.getOperand1();
    CExpression rightOperand = pExpression.getOperand2();

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = factory.createPositiveConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    } else {
      constraint = factory.createNegativeConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    }

    return Optional.fromNullable(constraint);
  }


  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement, List<AbstractState> pElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    assert pElement instanceof ConstraintsState;

    return null;
  }
}
