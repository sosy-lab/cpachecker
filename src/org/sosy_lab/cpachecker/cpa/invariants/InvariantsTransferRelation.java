/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

enum InvariantsTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException {

    InvariantsState element = (InvariantsState)pElement;

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case FunctionReturnEdge:
    case ReturnStatementEdge:
      break;

    case AssumeEdge:
      element = handleAssume(element, (CAssumeEdge)edge);
      break;

    case DeclarationEdge:
      element = handleDeclaration(element, (CDeclarationEdge)edge);
      break;

    case FunctionCallEdge:
      element = handleFunctionCall(element, (CFunctionCallEdge)edge);
      break;

    case StatementEdge:
      element = handleStatement(element, (CStatementEdge)edge);
      break;

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    if (element == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(element);
    }
  }

  private InvariantsState handleAssume(InvariantsState element, CAssumeEdge edge) throws UnrecognizedCCodeException {

    // handle special case "a == i" where i is an integer literal
    CExpression exp = edge.getExpression();
    if (exp instanceof CBinaryExpression) {
      CBinaryExpression binExp = (CBinaryExpression)exp;

      if (binExp.getOperator() == BinaryOperator.EQUALS) {
        CExpression operand1 = binExp.getOperand1();
        if (operand1 instanceof CIdExpression) {
          String var = getVarName((CIdExpression)operand1, edge);
          SimpleInterval varValue = element.get(var);

          SimpleInterval value = binExp.getOperand2().accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);
          // value may be either a single value or (-INF, +INF)

          if (value.isSingleton()) {

            if (edge.getTruthAssumption()) {
              if (!varValue.intersectsWith(value)) {
                // not a possible edge
                return null;
              } else {
                element = element.copyAndSet(var, value);
              }

            } else {
              // negated edge
              if (varValue.equals(value)) {
                // not a possible edge
                return null;
              } else {
                // don't change interval
              }
            }

          } else {
            assert !value.hasLowerBound() && !value.hasUpperBound();
          }
        }
      }
    }

    return element;
  }

  private InvariantsState handleDeclaration(InvariantsState element, CDeclarationEdge edge) throws UnrecognizedCCodeException {
    if (!(edge.getDeclaration() instanceof CVariableDeclaration)) {

      return element;
    }
    CVariableDeclaration decl = (CVariableDeclaration)edge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = edge.getSuccessor().getFunctionName() + "::" + varName;
    }

    SimpleInterval value = SimpleInterval.infinite();
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);
    }

    return element.copyAndSet(varName, value);
  }

  private InvariantsState handleFunctionCall(InvariantsState element, CFunctionCallEdge edge) throws UnrecognizedCCodeException {

    InvariantsState newElement = element;
    List<String> formalParams = edge.getSuccessor().getFunctionParameterNames();
    List<CExpression> actualParams = edge.getArguments();

    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();

      SimpleInterval value = actualParam.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);

      if (actualParam instanceof CIdExpression) {
        String var = getVarName((CIdExpression)actualParam, edge);
        value = element.get(var);
      }

      String formalParam = scope(param.getFirst(), edge.getSuccessor().getFunctionName());
      newElement = newElement.copyAndSet(formalParam, value);
    }

    return newElement;
  }

  private InvariantsState handleStatement(InvariantsState element, CStatementEdge edge) throws UnrecognizedCCodeException {

    if (edge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment)edge.getStatement();

      CExpression leftHandSide = assignment.getLeftHandSide();
      if (leftHandSide instanceof CIdExpression) {
        // a = ...

        String varName = getVarName((CIdExpression)leftHandSide, edge);

        CRightHandSide rightHandSide = assignment.getRightHandSide();
        SimpleInterval rightHandValue = rightHandSide.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);

        // the line above handles all "easy" assignments like a = 5
        // now handle the special case "a = a + i" where is a literal

        if (rightHandSide instanceof CBinaryExpression) {
          CBinaryExpression binExp = (CBinaryExpression)rightHandSide;

          if (binExp.getOperator() == BinaryOperator.PLUS) {
            CExpression operand1 = binExp.getOperand1();
            if (operand1 instanceof CIdExpression) {
              String rightHandVar = getVarName((CIdExpression)operand1, edge);

              if (varName.equals(rightHandVar)) {
                // now we are sure it's really "a = a + ..."

                rightHandValue = element.get(varName);
                SimpleInterval incrementValue = binExp.getOperand2().accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);

                if (incrementValue.containsPositive()) {
                  rightHandValue = rightHandValue.extendToPositiveInfinity();
                }
                if (incrementValue.containsNegative()) {
                  rightHandValue = rightHandValue.extendToNegativeInfinity();
                }
              }
            }
          }

        } else if (rightHandSide instanceof CIdExpression) {
          // special case "a = b"
          String var = getVarName((CIdExpression)rightHandSide, edge);
          rightHandValue = element.get(var);
        }

        element = element.copyAndSet(varName, rightHandValue);

      } else {
        throw new UnrecognizedCCodeException("unknown left-hand side of assignment", edge, leftHandSide);
      }

    }

    return element;
  }

  private static String getVarName(CIdExpression var, CFAEdge edge) throws UnrecognizedCCodeException {
    String varName = var.getName();
    if (var.getDeclaration() != null) {
      CSimpleDeclaration decl = var.getDeclaration();

      if (!(decl instanceof CDeclaration || decl instanceof CParameterDeclaration)) {
        throw new UnrecognizedCCodeException("unknown variable declaration", edge, var);
      }

      if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {

      } else {
        varName = scope(varName, edge.getPredecessor().getFunctionName());
      }
    }
    return varName;
  }

  private static String scope(String var, String function) {
    return function + "::" + var;
  }

  private static class SimpleRightHandSideValueVisitor extends DefaultCExpressionVisitor<SimpleInterval, UnrecognizedCCodeException>
                                                       implements CRightHandSideVisitor<SimpleInterval, UnrecognizedCCodeException> {

    private static SimpleRightHandSideValueVisitor VISITOR_INSTANCE = new SimpleRightHandSideValueVisitor();

    @Override
    protected SimpleInterval visitDefault(CExpression pExp) {
      return SimpleInterval.infinite();
    }

    @Override
    public SimpleInterval visit(CFunctionCallExpression pIastFunctionCallExpression) {
      return visitDefault(null);
    }

    @Override
    public SimpleInterval visit(CIntegerLiteralExpression pE) {
      return SimpleInterval.singleton(pE.getValue());
    }

    @Override
    public SimpleInterval visit(CCharLiteralExpression pE) {
      return SimpleInterval.singleton(BigInteger.valueOf(pE.getCharacter()));
    }

    @Override
    public SimpleInterval visit(CCastExpression pE) throws UnrecognizedCCodeException {
      SimpleInterval operand = pE.getOperand().accept(this);
      return operand;
    }

    @Override
    public SimpleInterval visit(CUnaryExpression pE) throws UnrecognizedCCodeException {

      switch (pE.getOperator()) {
      case MINUS:
        SimpleInterval operand = pE.getOperand().accept(this);
        return operand.negate();

      default:
        return super.visit(pE);
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}