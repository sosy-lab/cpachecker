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
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

enum InvariantsTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException {

    InvariantsElement element = (InvariantsElement)pElement;

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case FunctionReturnEdge:
    case ReturnStatementEdge:
      break;

    case AssumeEdge:
      element = handleAssume(element, (AssumeEdge)edge);
      break;

    case DeclarationEdge:
      element = handleDeclaration(element, (DeclarationEdge)edge);
      break;

    case FunctionCallEdge:
      element = handleFunctionCall(element, (FunctionCallEdge)edge);
      break;

    case StatementEdge:
      element = handleStatement(element, (StatementEdge)edge);
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

  private InvariantsElement handleAssume(InvariantsElement element, AssumeEdge edge) throws UnrecognizedCCodeException {

    // handle special case "a == i" where i is an integer literal
    IASTExpression exp = edge.getExpression();
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = (IASTBinaryExpression)exp;

      if (binExp.getOperator() == BinaryOperator.EQUALS) {
        IASTExpression operand1 = binExp.getOperand1();
        if (operand1 instanceof IASTIdExpression) {
          String var = getVarName((IASTIdExpression)operand1, edge);
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

  private InvariantsElement handleDeclaration(InvariantsElement element, DeclarationEdge edge) throws UnrecognizedCCodeException {
    if (!(edge.getDeclaration() instanceof IASTVariableDeclaration)) {

      return element;
    }
    IASTVariableDeclaration decl = (IASTVariableDeclaration)edge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = edge.getSuccessor().getFunctionName() + "::" + varName;
    }

    SimpleInterval value = SimpleInterval.infinite();
    if (decl.getInitializer() != null && decl.getInitializer() instanceof IASTInitializerExpression) {
      IASTExpression init = ((IASTInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);
    }

    return element.copyAndSet(varName, value);
  }

  private InvariantsElement handleFunctionCall(InvariantsElement element, FunctionCallEdge edge) throws UnrecognizedCCodeException {

    InvariantsElement newElement = element;
    List<String> formalParams = edge.getSuccessor().getFunctionParameterNames();
    List<IASTExpression> actualParams = edge.getArguments();

    for (Pair<String, IASTExpression> param : Pair.zipList(formalParams, actualParams)) {
      IASTExpression actualParam = param.getSecond();

      SimpleInterval value = actualParam.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);

      if (actualParam instanceof IASTIdExpression) {
        String var = getVarName((IASTIdExpression)actualParam, edge);
        value = element.get(var);
      }

      String formalParam = scope(param.getFirst(), edge.getSuccessor().getFunctionName());
      newElement = newElement.copyAndSet(formalParam, value);
    }

    return newElement;
  }

  private InvariantsElement handleStatement(InvariantsElement element, StatementEdge edge) throws UnrecognizedCCodeException {

    if (edge.getStatement() instanceof IASTAssignment) {
      IASTAssignment assignment = (IASTAssignment)edge.getStatement();

      IASTExpression leftHandSide = assignment.getLeftHandSide();
      if (leftHandSide instanceof IASTIdExpression) {
        // a = ...

        String varName = getVarName((IASTIdExpression)leftHandSide, edge);

        IASTRightHandSide rightHandSide = assignment.getRightHandSide();
        SimpleInterval rightHandValue = rightHandSide.accept(SimpleRightHandSideValueVisitor.VISITOR_INSTANCE);

        // the line above handles all "easy" assignments like a = 5
        // now handle the special case "a = a + i" where is a literal

        if (rightHandSide instanceof IASTBinaryExpression) {
          IASTBinaryExpression binExp = (IASTBinaryExpression)rightHandSide;

          if (binExp.getOperator() == BinaryOperator.PLUS) {
            IASTExpression operand1 = binExp.getOperand1();
            if (operand1 instanceof IASTIdExpression) {
              String rightHandVar = getVarName((IASTIdExpression)operand1, edge);

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

        } else if (rightHandSide instanceof IASTIdExpression) {
          // special case "a = b"
          String var = getVarName((IASTIdExpression)rightHandSide, edge);
          rightHandValue = element.get(var);
        }

        element = element.copyAndSet(varName, rightHandValue);

      } else {
        throw new UnrecognizedCCodeException("unknown left-hand side of assignment", edge, leftHandSide);
      }

    }

    return element;
  }

  private static String getVarName(IASTIdExpression var, CFAEdge edge) throws UnrecognizedCCodeException {
    String varName = var.getName();
    if (var.getDeclaration() != null) {
      IASTSimpleDeclaration decl = var.getDeclaration();

      if (!(decl instanceof IASTDeclaration || decl instanceof IASTParameterDeclaration)) {
        throw new UnrecognizedCCodeException("unknown variable declaration", edge, var);
      }

      if (decl instanceof IASTDeclaration && ((IASTDeclaration)decl).isGlobal()) {

      } else {
        varName = scope(varName, edge.getPredecessor().getFunctionName());
      }
    }
    return varName;
  }

  private static String scope(String var, String function) {
    return function + "::" + var;
  }

  private static class SimpleRightHandSideValueVisitor extends DefaultExpressionVisitor<SimpleInterval, UnrecognizedCCodeException>
                                                       implements RightHandSideVisitor<SimpleInterval, UnrecognizedCCodeException> {

    private static SimpleRightHandSideValueVisitor VISITOR_INSTANCE = new SimpleRightHandSideValueVisitor();

    @Override
    protected SimpleInterval visitDefault(IASTExpression pExp) {
      return SimpleInterval.infinite();
    }

    @Override
    public SimpleInterval visit(IASTFunctionCallExpression pIastFunctionCallExpression) {
      return visitDefault(null);
    }

    @Override
    public SimpleInterval visit(IASTIntegerLiteralExpression pE) {
      return SimpleInterval.singleton(pE.getValue());
    }

    @Override
    public SimpleInterval visit(IASTCharLiteralExpression pE) {
      return SimpleInterval.singleton(BigInteger.valueOf(pE.getCharacter()));
    }

    @Override
    public SimpleInterval visit(IASTCastExpression pE) throws UnrecognizedCCodeException {
      SimpleInterval operand = pE.getOperand().accept(this);
      return operand;
    }

    @Override
    public SimpleInterval visit(IASTUnaryExpression pE) throws UnrecognizedCCodeException {

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
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}