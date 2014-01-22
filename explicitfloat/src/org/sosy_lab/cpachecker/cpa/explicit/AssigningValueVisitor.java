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
package org.sosy_lab.cpachecker.cpa.explicit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * Visitor that derives further information from an assume edge
 */
public class AssigningValueVisitor extends ExplicitExpressionValueVisitor {

  private final ExplicitTransferRelation explicitTransferRelation;
  private ExplicitState assignableState;
  protected boolean truthValue = false;

  public AssigningValueVisitor(ExplicitTransferRelation pExplicitTransferRelation, ExplicitState assignableState, boolean truthValue) {
    super(pExplicitTransferRelation.getState(), pExplicitTransferRelation.getFunctionName(), pExplicitTransferRelation.machineModel, pExplicitTransferRelation.logger, pExplicitTransferRelation.getEdge());
    explicitTransferRelation = pExplicitTransferRelation;
    this.assignableState = assignableState;
    this.truthValue = truthValue;
  }

  private IAExpression unwrap(IAExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    if (expression instanceof AUnaryExpression) {
      AUnaryExpression exp = (AUnaryExpression)expression;
      if (exp.getOperator() == UnaryOperator.NOT) { // TODO why only C-UnaryOperator?
        expression = exp.getOperand();
        truthValue = !truthValue;

        expression = unwrap(expression);
      }
    }

    if (expression instanceof CCastExpression) {
      CCastExpression exp = (CCastExpression)expression;
      expression = exp.getOperand();

      expression = unwrap(expression);
    }

    return expression;
  }

  @Override
  public NumberContainer visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
    BinaryOperator binaryOperator   = pE.getOperator();

    CExpression lVarInBinaryExp  = pE.getOperand1();

    lVarInBinaryExp = (CExpression) unwrap(lVarInBinaryExp);

    CExpression rVarInBinaryExp  = pE.getOperand2();

    NumberContainer leftValue                  = lVarInBinaryExp.accept(this);
    NumberContainer rightValue                 = rVarInBinaryExp.accept(this);

    if ((binaryOperator == BinaryOperator.EQUALS && truthValue) || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue)) {
      if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {
        MemoryLocation leftVariableLocation = getMemoryLocation(lVarInBinaryExp);
        assignableState.assignConstant(leftVariableLocation, rightValue);
      }

      else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
        MemoryLocation rightVariableName = getMemoryLocation(rVarInBinaryExp);
        assignableState.assignConstant(rightVariableName, leftValue);
      }
    }

    if (explicitTransferRelation.initAssumptionVars) {
      // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
      // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
      if ((binaryOperator == BinaryOperator.NOT_EQUALS && truthValue)
          || (binaryOperator == BinaryOperator.EQUALS && !truthValue)) {
        if (leftValue == null &&
            (rightValue.bigDecimalValue().compareTo(new BigDecimal(0)) == 0)
            && isAssignable(lVarInBinaryExp)) {
          MemoryLocation leftVariableName = getMemoryLocation(lVarInBinaryExp);
          assignableState.assignConstant(leftVariableName, new NumberContainer(1L));
        }

        else if (rightValue == null &&
            (leftValue.bigDecimalValue().compareTo(new BigDecimal(0)) == 0)
            && isAssignable(rVarInBinaryExp)) {
          MemoryLocation rightVariableName = getMemoryLocation(rVarInBinaryExp);
          assignableState.assignConstant(rightVariableName, new NumberContainer(1L));
        }
      }
    }
    return super.visit(pE);
  }

  @Override
  public Long visit(JBinaryExpression pE) {
    JBinaryExpression.BinaryOperator binaryOperator   = pE.getOperator();

    JExpression lVarInBinaryExp  = pE.getOperand1();

    lVarInBinaryExp = (JExpression) unwrap(lVarInBinaryExp);

    JExpression rVarInBinaryExp  = pE.getOperand2();

    Long leftValue                  = lVarInBinaryExp.accept(this);
    Long rightValue                 = rVarInBinaryExp.accept(this);

    if ((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue) || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {
      if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {

        String leftVariableName = explicitTransferRelation.getScopedVariableName(lVarInBinaryExp, explicitTransferRelation.getFunctionName());
        // TODO for java
        //assignableState.assignConstant(leftVariableName, rightValue);
      } else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
        String rightVariableName = explicitTransferRelation.getScopedVariableName(rVarInBinaryExp, explicitTransferRelation.getFunctionName());
        //assignableState.assignConstant(rightVariableName, leftValue);

      }
    }

    if (explicitTransferRelation.initAssumptionVars) {
      // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
      // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
      if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
          || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {
        if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
          String leftVariableName = explicitTransferRelation.getScopedVariableName(lVarInBinaryExp, explicitTransferRelation.getFunctionName());
          // TODO for java
          //assignableState.assignConstant(leftVariableName, 1L);

        }

        else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
          String rightVariableName = explicitTransferRelation.getScopedVariableName(rVarInBinaryExp, explicitTransferRelation.getFunctionName());
          //assignableState.assignConstant(rightVariableName, 1L);
        }
      }
    }
    return super.visit(pE);
  }

  protected MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
    ExplicitExpressionValueVisitor v = explicitTransferRelation.getVisitor();
    assert pLValue instanceof CLeftHandSide;
    return checkNotNull(v.evaluateMemoryLocation(pLValue));
  }

  protected boolean isAssignable(JExpression expression) {

    boolean result = false;

    if (expression instanceof JIdExpression) {

      JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

      if (decl == null) {
        result = false;
      } else if (decl instanceof JFieldDeclaration) {
        result = ((JFieldDeclaration) decl).isStatic();
      } else {
        result = true;
      }
    }

    return result;
  }



  protected boolean isAssignable(CExpression expression) throws UnrecognizedCCodeException  {

    if (expression instanceof CIdExpression) {
      return true;
    }

    if (expression instanceof CFieldReference || expression instanceof CArraySubscriptExpression) {
      ExplicitExpressionValueVisitor evv = explicitTransferRelation.getVisitor();
      return evv.canBeEvaluated(expression);
    }

    return false;
  }
}