/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import java.math.BigDecimal;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedJSCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ExpressionToFormulaVisitor
    implements JSRightHandSideVisitor<TypedValue, UnrecognizedJSCodeException> {

  private final JSToFormulaConverter conv;
  private final CFAEdge       edge;
  private final String        function;
  private final Constraints   constraints;
  protected final FormulaManagerView mgr;
  protected final SSAMapBuilder ssa;

  public ExpressionToFormulaVisitor(
      JSToFormulaConverter pJSToFormulaConverter,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      Constraints pConstraints) {

    conv = pJSToFormulaConverter;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pConstraints;
    mgr = pFmgr;
  }

  @Override
  public TypedValue visit(final JSFunctionCallExpression pFunctionCallExpression)
      throws UnrecognizedJSCodeException {
    throw new UnrecognizedJSCodeException("Not implemented yet", pFunctionCallExpression);
  }

  @Override
  public TypedValue visit(final JSBinaryExpression pBinaryExpression)
      throws UnrecognizedJSCodeException {
    final TypedValue leftOperand = visit(pBinaryExpression.getOperand1());
    final TypedValue rightOperand = visit(pBinaryExpression.getOperand2());
    switch (pBinaryExpression.getOperator()) {
      case EQUAL_EQUAL_EQUAL:
        return conv.tvmgr.createBooleanValue(makeEqual(leftOperand, rightOperand));
      case NOT_EQUAL_EQUAL:
        return conv.tvmgr.createBooleanValue(mgr.makeNot(makeEqual(leftOperand, rightOperand)));
      case PLUS:
        return conv.tvmgr.createNumberValue(
            conv.fpfmgr.add(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case MINUS:
        return conv.tvmgr.createNumberValue(
            conv.fpfmgr.subtract(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case TIMES:
        return conv.tvmgr.createNumberValue(
            conv.fpfmgr.multiply(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case DIVIDE:
        return conv.tvmgr.createNumberValue(
            conv.fpfmgr.divide(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case REMAINDER:
        return conv.tvmgr.createNumberValue(makeRemainder(leftOperand, rightOperand));
      case LESS:
        return conv.tvmgr.createBooleanValue(
            conv.fpfmgr.lessThan(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case LESS_EQUALS:
        return conv.tvmgr.createBooleanValue(
            conv.fpfmgr.lessOrEquals(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case GREATER:
        return conv.tvmgr.createBooleanValue(
            conv.fpfmgr.greaterThan(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      case GREATER_EQUALS:
        return conv.tvmgr.createBooleanValue(
            conv.fpfmgr.greaterOrEquals(conv.toNumber(leftOperand), conv.toNumber(rightOperand)));
      default:
        throw new UnrecognizedJSCodeException("Not implemented yet", pBinaryExpression);
    }
  }

  @Nonnull
  private FloatingPointFormula makeRemainder(
      final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final FloatingPointFormulaManagerView f = conv.fpfmgr;
    final FloatingPointFormula dividend = conv.toNumber(pLeftOperand);
    final FloatingPointFormula divisor = conv.toNumber(pRightOperand);
    return conv.bfmgr.ifThenElse(
        conv.bfmgr.or(
            f.isNaN(dividend), f.isNaN(divisor), f.isInfinity(dividend), f.isZero(divisor)),
        f.makeNaN(Types.NUMBER_TYPE),
        conv.bfmgr.ifThenElse(
            conv.bfmgr.or(f.isInfinity(divisor), f.isZero(dividend)), dividend, dividend));
  }

  @Nonnull
  private BooleanFormula makeEqual(final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    // TODO null and string
    final IntegerFormula leftType = pLeftOperand.getType();
    final IntegerFormula rightType = pRightOperand.getType();
    return mgr.makeAnd(
        mgr.makeEqual(leftType, rightType),
        conv.bfmgr.or(
            mgr.makeEqual(conv.typeTags.UNDEFINED, leftType),
            conv.bfmgr.and(
                mgr.makeEqual(conv.typeTags.NUMBER, leftType),
                mgr.makeNot(conv.fpfmgr.isNaN(conv.toNumber(pLeftOperand))),
                mgr.makeNot(conv.fpfmgr.isNaN(conv.toNumber(pRightOperand))),
                mgr.makeEqual(conv.toNumber(pLeftOperand), conv.toNumber(pRightOperand))),
            mgr.makeAnd(
                mgr.makeEqual(conv.typeTags.BOOLEAN, leftType),
                mgr.makeEqual(conv.toBoolean(pLeftOperand), conv.toBoolean(pRightOperand)))));
  }

  @Override
  public TypedValue visit(final JSStringLiteralExpression pStringLiteralExpression) {
    return conv.tvmgr.createStringValue(pStringLiteralExpression.getValue());
  }

  @Override
  public TypedValue visit(final JSFloatLiteralExpression pLiteral) {
    return makeNumber(pLiteral.getValue());
  }

  @Nonnull
  private TypedValue makeNumber(final BigDecimal pValue) {
    return conv.tvmgr.createNumberValue(
        conv.fpfmgr.makeNumber(pValue, FormulaType.getDoublePrecisionFloatingPointType()));
  }

  @Override
  public TypedValue visit(final JSUnaryExpression pUnaryExpression)
      throws UnrecognizedJSCodeException {
    final TypedValue operand = visit(pUnaryExpression.getOperand());
    switch (pUnaryExpression.getOperator()) {
      case NOT:
        return conv.tvmgr.createBooleanValue(mgr.makeNot(conv.toBoolean(operand)));
      case PLUS:
        return conv.tvmgr.createNumberValue(conv.toNumber(operand));
      case MINUS:
        return conv.tvmgr.createNumberValue(mgr.makeNegate(conv.toNumber(operand)));
      case VOID:
        return conv.tvmgr.getUndefinedValue();
      default:
        throw new UnrecognizedJSCodeException("Not implemented yet", pUnaryExpression);
    }
  }

  @Override
  public TypedValue visit(final JSIntegerLiteralExpression pIntegerLiteralExpression) {
    return makeNumber(new BigDecimal(pIntegerLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSBooleanLiteralExpression pBooleanLiteralExpression) {
    return conv.tvmgr.createBooleanValue(
        conv.bfmgr.makeBoolean(pBooleanLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSNullLiteralExpression pNullLiteralExpression) {
    return conv.tvmgr.getNullValue();
  }

  @Override
  public TypedValue visit(final JSUndefinedLiteralExpression pUndefinedLiteralExpression) {
    return conv.tvmgr.getUndefinedValue();
  }

  @Override
  public TypedValue visit(final JSThisExpression pThisExpression)
      throws UnrecognizedJSCodeException {
    throw new UnrecognizedJSCodeException("Not implemented yet", pThisExpression);
  }

  @Override
  public TypedValue visit(final JSIdExpression pIdExpression) throws UnrecognizedJSCodeException {
    final JSSimpleDeclaration declaration = pIdExpression.getDeclaration();
    if (declaration == null) {
      return handlePredefined(pIdExpression);
    }
    final IntegerFormula variable =
        conv.makeVariable(declaration.getQualifiedName(), pIdExpression.getExpressionType(), ssa);
    return new TypedValue(conv.typedValues.typeof(variable), variable);
  }

  private TypedValue handlePredefined(final JSIdExpression pIdExpression)
      throws UnrecognizedJSCodeException {
    final String name = pIdExpression.getName();
    switch (name) {
      case "Infinity":
        return conv.tvmgr.createNumberValue(conv.fpfmgr.makePlusInfinity(Types.NUMBER_TYPE));
      case "NaN":
        return conv.tvmgr.createNumberValue(conv.fpfmgr.makeNaN(Types.NUMBER_TYPE));
      default:
        throw new UnrecognizedJSCodeException(
            "Variable without declaration is not defined on global object", pIdExpression);
    }
  }
}
