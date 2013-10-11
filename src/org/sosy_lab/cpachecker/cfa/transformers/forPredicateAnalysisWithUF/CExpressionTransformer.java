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
package org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class CExpressionTransformer extends DefaultCExpressionVisitor<CAstNode, UnrecognizedCCodeException>
                             implements CExpressionVisitor<CAstNode, UnrecognizedCCodeException> {

  CExpressionTransformer(final boolean transformPointerArithmetic,
                         final boolean transformArrows,
                         final boolean transformStarAmper,
                         final boolean transformFuncitonPointers) {
    this.transformPointerArithmetic = transformPointerArithmetic;
    this.transformArrows = transformArrows;
    this.transformStarAmper = transformStarAmper;
    this.transformFuncitonPointers = transformFuncitonPointers;
  }

  @Override
  public CArraySubscriptExpression visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    final CExpression oldArrayExpression = e.getArrayExpression();
    final CExpression oldSubscriptExpression = e.getSubscriptExpression();
    final CExpression arrayExpression = (CExpression) oldArrayExpression.accept(this);
    final CExpression subscriptExpression = (CExpression) oldSubscriptExpression.accept(this);

    return arrayExpression == oldArrayExpression && subscriptExpression == oldSubscriptExpression ? e :
           new CArraySubscriptExpression(e.getFileLocation(),
                                         e.getExpressionType(),
                                         arrayExpression,
                                         subscriptExpression);

  }

  @Override
  public CExpression visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    final CExpression oldOperand1 = e.getOperand1();
    final CExpression oldOperand2 = e.getOperand2();
    final CExpression operand1 = (CExpression) oldOperand1.accept(this);
    final CExpression operand2 = (CExpression) oldOperand2.accept(this);
    final CType type1 = operand1.getExpressionType().getCanonicalType();
    final CType type2 = operand2.getExpressionType().getCanonicalType();
    final FileLocation fileLocation = e.getFileLocation();
    switch (e.getOperator()) {
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
      break;
    case DIVIDE:
    case MODULO:
    case MULTIPLY:
      break;
    case EQUALS:
    case NOT_EQUALS:
    case GREATER_EQUAL:
    case GREATER_THAN:
    case LESS_EQUAL:
    case LESS_THAN:
      break;
    case MINUS:
      if (!(type1 instanceof CPointerType) && !(type2 instanceof CPointerType)) { // just subtraction e.g. 6 - 7

      } else if (!(type2 instanceof CPointerType)) { // operand1 is a pointer => transform `-' into array subscript
        final CUnaryExpression minusOperand2 = new CUnaryExpression(operand2.getFileLocation(),
                                                                    operand2.getExpressionType(),
                                                                    operand2, UnaryOperator.MINUS);
        final CPointerType resultType = (CPointerType) type1;
        // &operand1[-operand2]
        return new CUnaryExpression(fileLocation,
                                    resultType,
                                    new CArraySubscriptExpression(fileLocation,
                                                                  resultType.getType(),
                                                                  operand1,
                                                                  minusOperand2),
                                    UnaryOperator.AMPER);
      } else if (type1 instanceof CPointerType) {
        if (type1.equals(type2)) { // Pointer subtraction => (operand1 - operand2) / sizeof(operand1)
          final CType resultType = e.getExpressionType();
          final CBinaryExpression difference = operand1 == oldOperand1 && operand2 == oldOperand2 ? e :
            new CBinaryExpression(fileLocation,
                                  resultType,
                                  operand1,
                                  operand2,
                                  BinaryOperator.MINUS);
          final CTypeIdExpression sizeofOperand1 = new CTypeIdExpression(fileLocation,
                                                                         resultType,
                                                                         TypeIdOperator.SIZEOF,
                                                                         type1);
          return new CBinaryExpression(fileLocation,
                                       resultType,
                                       difference,
                                       sizeofOperand1,
                                       BinaryOperator.DIVIDE);
        } else {
          throw new UnrecognizedCCodeException("Can't subtract pointers of different types", currentEdge, e);
        }
      } else {
        throw new UnrecognizedCCodeException("Can't subtract pointer from a non-pointer", currentEdge, e);
      }
      break;
    case PLUS:
      if (!(type1 instanceof CPointerType) && !(type2 instanceof CPointerType)) { // just addition e.g. 6 + 7

      } else if (!(type2 instanceof CPointerType)) { // operand1 is a pointer => transform `+' into array subscript
        final CPointerType resultType = (CPointerType) type1;
        // &operand1[operand2]
        return new CUnaryExpression(fileLocation,
                                    resultType,
                                    new CArraySubscriptExpression(fileLocation,
                                                                  resultType.getType(),
                                                                  operand1,
                                                                  operand2),
                                    UnaryOperator.AMPER);
      } else if (!(type1 instanceof CPointerType)) { // operand2 is a pointer => transform `+' into array subscript
        final CPointerType resultType = (CPointerType) type2;
        //&operand2[operand1]
        return new CUnaryExpression(fileLocation,
                                    resultType,
                                    new CArraySubscriptExpression(fileLocation,
                                                                  resultType.getType(),
                                                                  operand2,
                                                                  operand1),
                                    UnaryOperator.AMPER);
      } else {
        throw new UnrecognizedCCodeException("Can't add pointers", currentEdge, e);
      }
      break;
    default:
      break;
    }

    return operand1 == oldOperand1 && operand2 == oldOperand2 ? e :
           new CBinaryExpression(fileLocation,
                                 e.getExpressionType(),
                                 operand1,
                                 operand2,
                                 e.getOperator());
  }

  @Override
  public CCastExpression visit(final CCastExpression e) throws UnrecognizedCCodeException {
    final CExpression oldOperand = e.getOperand();
    final CExpression operand = (CExpression) oldOperand.accept(this);

    return operand == oldOperand ? e :
           new CCastExpression(e.getFileLocation(),
                               e.getExpressionType(),
                               operand,
                               e.getType());
  }

  @Override
  public CFieldReference visit(final CFieldReference e) throws UnrecognizedCCodeException {
    final CExpression oldFieldOwner = e.getFieldOwner();
    final CExpression fieldOwner = (CExpression) oldFieldOwner.accept(this);
    if (e.isPointerDereference()) { // transform p->f into (*p).f
      return new CFieldReference(e.getFileLocation(),
                                 e.getExpressionType(),
                                 e.getFieldName(),
                                 new CUnaryExpression(e.getFileLocation(),
                                                      ((CPointerType) e.getFieldOwner().getExpressionType()).getType(),
                                                      e.getFieldOwner(),
                                                      UnaryOperator.STAR),
                                 false);
    } else {
      return fieldOwner == oldFieldOwner ? e :
             new CFieldReference(e.getFileLocation(),
                                 e.getExpressionType(),
                                 e.getFieldName(),
                                 fieldOwner,
                                 false);
    }
  }

  @Override
  public CIdExpression visit(final CIdExpression e) throws UnrecognizedCCodeException {
    return e;
  }

  @Override
  public CExpression visitDefault(final CExpression e) throws UnrecognizedCCodeException {
    return e;
  }

  @Override
  public CTypeIdExpression visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    switch (e.getOperator()) {
    case ALIGNOF:
    case SIZEOF:
    case TYPEOF:
      return e;
    case TYPEID: // For C language it's the just the same as TYPEOF
      break;
    }
    return new CTypeIdExpression(e.getFileLocation(), // This is for TYPEID case, to prevent error message
        e.getExpressionType(),
        TypeIdOperator.TYPEOF,
        e.getType());
  }

  @Override
  public CTypeIdInitializerExpression visit(final CTypeIdInitializerExpression e)
  throws UnrecognizedCCodeException {
    final CInitializer oldInitializer = e.getInitializer();
    final CInitializer initializer = (CInitializer) oldInitializer.accept(initializerTransformer);

    return initializer == oldInitializer ? e :
           new CTypeIdInitializerExpression(e.getFileLocation(),
                                            e.getExpressionType(),
                                            initializer,
                                            e.getType());
  }

  @Override
  public CExpression visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    // Detect expression of the form
    // "* &v", "* ((t *) &v)", and "* f" where f is a function pointer
    if (e.getOperator() == UnaryOperator.STAR) {
      if (e.getOperand() instanceof CUnaryExpression &&
          ((CUnaryExpression) e.getOperand()).getOperator() == UnaryOperator.AMPER) {
        // "* &v" -> "v"
        return (CExpression) ((CUnaryExpression) e.getOperand()).getOperand().accept(this);
      } /* else if (e.getOperand() instanceof CCastExpression &&
                 ((CCastExpression) e.getOperand()).getOperand() instanceof CUnaryExpression &&
                 ((CUnaryExpression) ((CCastExpression) e.getOperand()).getOperand()).getOperator() ==
                   UnaryOperator.AMPER) {
        // "* ((t *)) &v)" -> "(t) v"
        return new CCastExpression(e.getFileLocation(),
                                   e.getExpressionType(),
                                   (CExpression)
                                   ((CUnaryExpression) ((CCastExpression) e.getOperand()).getOperand()).getOperand()
                                                                                                       .accept(this),
                                   ((CCastExpression) e.getOperand()).getType());
      } */ else if (e.getOperand().getExpressionType() instanceof CPointerType &&
                 ((CPointerType)e.getOperand().getExpressionType()).getType() instanceof CFunctionType) {
        // "* f" -> "f"
        return (CExpression) e.getOperand().accept(this);
      }
    }

    final CExpression oldOperand = e.getOperand();
    final CExpression operand = (CExpression) oldOperand.accept(this);

    switch (e.getOperator()) {
    case AMPER:
      if (e.getOperand().getExpressionType() instanceof CFunctionType) {
        return (CExpression) e.getOperand().accept(this); // &f -> f
      }
      break;
    case MINUS:
    case PLUS:
      break;
    case NOT:
      break;
    case SIZEOF:
      break;
    case STAR:
      break;
    case TILDE:
      break;
    }

    return operand == oldOperand ? e :
           new CUnaryExpression(e.getFileLocation(),
                                e.getExpressionType(),
                                operand,
                                e.getOperator());
  }

  public void setCurrentEdge(final CFAEdge e) {
    currentEdge = e;
  }

  public CInitializerVisitor<CAstNode, UnrecognizedCCodeException> getInitializerTransformer() {
    return initializerTransformer;
  }

  private final boolean transformPointerArithmetic;
  private final boolean transformArrows;
  private final boolean transformStarAmper;
  private final boolean transformFuncitonPointers;

  private CFAEdge currentEdge = null;
  private final CInitializerVisitor<CAstNode, UnrecognizedCCodeException> initializerTransformer =
                new CInitializerTransformer(this);
}
