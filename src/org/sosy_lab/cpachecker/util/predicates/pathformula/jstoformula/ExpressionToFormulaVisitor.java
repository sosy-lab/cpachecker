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
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSArrayLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBracketPropertyAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.JSObjectFormulaManager.JSObjectFormulaManagerWithContext;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ExpressionToFormulaVisitor
    implements JSExpressionFormulaManager,
        JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> {

  // TODO this option should be removed as soon as NaN and float interpolation can be used together
  private boolean useNaN;

  private final JSToFormulaConverter conv;
  private final CFAEdge       edge;
  private final String        function;
  private final Constraints   constraints;
  protected final FormulaManagerView mgr;
  private final ErrorConditions errorConditions;
  protected final SSAMapBuilder ssa;
  private final JSObjectFormulaManagerWithContext ofmgr;

  public ExpressionToFormulaVisitor(
      JSToFormulaConverter pJSToFormulaConverter,
      final boolean pUseNaN,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      Constraints pConstraints,
      final ErrorConditions pErrorConditions) {

    useNaN = pUseNaN;
    conv = pJSToFormulaConverter;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pConstraints;
    mgr = pFmgr;
    errorConditions = pErrorConditions;
    ofmgr = conv.ofmgr.withContext(ssa, constraints, this);
  }

  @Override
  public TypedValue visit(final JSFunctionCallExpression pFunctionCallExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException(
        "JSFunctionCallExpression not implemented yet", pFunctionCallExpression);
  }

  @Override
  public TypedValue visit(final JSExpression pExpression) throws UnrecognizedCodeException {
    return JSRightHandSideVisitor.super.visit(pExpression);
  }

  @Override
  public TypedValue visit(final JSBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    final TypedValue leftOperand = visit(pBinaryExpression.getOperand1());
    final TypedValue rightOperand = visit(pBinaryExpression.getOperand2());
    switch (pBinaryExpression.getOperator()) {
      case EQUALS:
        // Treat == like === until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
      case EQUAL_EQUAL_EQUAL:
        return conv.tvmgr.createBooleanValue(makeEqual(leftOperand, rightOperand));
      case NOT_EQUALS:
        // Treat != like !== until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
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
        throw new UnrecognizedCodeException(
            "JSBinaryExpression not implemented yet", pBinaryExpression);
    }
  }

  @Nonnull
  private FloatingPointFormula makeRemainder(
      final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final FloatingPointFormulaManagerView f = conv.fpfmgr;
    final FloatingPointFormula dividend = conv.toNumber(pLeftOperand);
    final FloatingPointFormula divisor = conv.toNumber(pRightOperand);
    final BooleanFormula nanCase =
        useNaN ? conv.bfmgr.or(f.isNaN(dividend), f.isNaN(divisor)) : conv.bfmgr.makeFalse();
    return conv.bfmgr.ifThenElse(
        conv.bfmgr.or(nanCase, f.isInfinity(dividend), f.isZero(divisor)),
        f.makeNaN(Types.NUMBER_TYPE),
        conv.bfmgr.ifThenElse(
            conv.bfmgr.or(f.isInfinity(divisor), f.isZero(dividend)), dividend, dividend));
  }

  @Nonnull
  private BooleanFormula makeEqual(final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    // TODO strings and functions
    final IntegerFormula leftType = pLeftOperand.getType();
    final IntegerFormula rightType = pRightOperand.getType();
    final BooleanFormula nanCase =
        useNaN
            ? conv.bfmgr.and(
                mgr.makeNot(conv.fpfmgr.isNaN(conv.toNumber(pLeftOperand))),
                mgr.makeNot(conv.fpfmgr.isNaN(conv.toNumber(pRightOperand))))
            : conv.bfmgr.makeTrue();
    return mgr.makeAnd(
        mgr.makeEqual(leftType, rightType),
        conv.bfmgr.or(
            mgr.makeEqual(conv.typeTags.UNDEFINED, leftType),
            conv.bfmgr.and(
                mgr.makeEqual(conv.typeTags.NUMBER, leftType),
                nanCase,
                mgr.makeEqual(conv.toNumber(pLeftOperand), conv.toNumber(pRightOperand))),
            mgr.makeAnd(
                mgr.makeEqual(conv.typeTags.BOOLEAN, leftType),
                mgr.makeEqual(conv.toBoolean(pLeftOperand), conv.toBoolean(pRightOperand))),
            mgr.makeAnd(
                mgr.makeEqual(conv.typeTags.OBJECT, leftType),
                mgr.makeEqual(conv.toObject(pLeftOperand), conv.toObject(pRightOperand))),
            mgr.makeAnd(
                mgr.makeEqual(conv.typeTags.STRING, leftType),
                mgr.makeEqual(
                    conv.toStringFormula(pLeftOperand), conv.toStringFormula(pRightOperand)))));
  }

  @Override
  public TypedValue visit(final JSStringLiteralExpression pStringLiteralExpression) {
    return conv.tvmgr.createStringValue(conv.getStringFormula(pStringLiteralExpression.getValue()));
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
      throws UnrecognizedCodeException {
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
        throw new UnrecognizedCodeException(
            "JSUnaryExpression not implemented yet", pUnaryExpression);
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
  public TypedValue visit(final JSObjectLiteralExpression pObjectLiteralExpression)
      throws UnrecognizedCodeException {
    return ofmgr.createObject(pObjectLiteralExpression);
  }

  @Override
  public TypedValue visit(final JSArrayLiteralExpression pArrayLiteralExpression)
      throws UnrecognizedCodeException {
    final IntegerFormula objectId = conv.ofmgr.createObjectId();
    final TypedValue objectValue = conv.tvmgr.createObjectValue(objectId);
    // TODO assign elements to new array object
    final List<JSExpression> elements = pArrayLiteralExpression.getElements();
    final JSObjectLiteralField lengthField =
        new JSObjectLiteralField(
            "length",
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(elements.size())));
    ofmgr.setObjectFields(objectId, Collections.singletonList(lengthField));
    return objectValue;
  }

  @Override
  public TypedValue visit(final JSUndefinedLiteralExpression pUndefinedLiteralExpression) {
    return conv.tvmgr.getUndefinedValue();
  }

  @Override
  public TypedValue visit(final JSThisExpression pThisExpression) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("JSThisExpression not implemented yet", pThisExpression);
  }

  @Override
  public TypedValue visit(final JSDeclaredByExpression pDeclaredByExpression) {
    assert pDeclaredByExpression.getIdExpression().getDeclaration() != null;
    final IntegerFormula variable =
        conv.scopedVariable(
            function, pDeclaredByExpression.getIdExpression().getDeclaration(), ssa);
    final IntegerFormula functionDeclarationId =
        mgr.makeNumber(
            Types.FUNCTION_DECLARATION_TYPE,
            conv.functionDeclarationIds.get(pDeclaredByExpression.getJsFunctionDeclaration()));
    return conv.tvmgr.createBooleanValue(
        mgr.makeEqual(
            conv.declarationOf(conv.typedValues.functionValue(variable)), functionDeclarationId));
  }

  @Override
  public TypedValue visit(final JSIdExpression pIdExpression) throws UnrecognizedCodeException {
    final JSSimpleDeclaration declaration = pIdExpression.getDeclaration();
    if (declaration == null) {
      return handlePredefined(pIdExpression);
    } else if (declaration instanceof JSFunctionDeclaration) {
      final JSFunctionDeclaration functionDeclaration = (JSFunctionDeclaration) declaration;
      final IntegerFormula functionDeclarationId =
          mgr.makeNumber(Types.FUNCTION_TYPE, conv.functionDeclarationIds.get(functionDeclaration));
      final IntegerFormula functionValueFormula =
          functionDeclaration.isGlobal()
              ? functionDeclarationId
              : conv.scopedVariable(function, functionDeclaration, ssa);
      constraints.addConstraint(
          mgr.makeEqual(conv.declarationOf(functionValueFormula), functionDeclarationId));
      // TODO function might be declared outside of current function
      constraints.addConstraint(
          mgr.makeEqual(conv.scopeOf(functionValueFormula), conv.getCurrentScope(function, ssa)));
      return conv.tvmgr.createFunctionValue(functionValueFormula);
    }
    final IntegerFormula variable = conv.scopedVariable(function, declaration, ssa);
    return new TypedValue(conv.typedValues.typeof(variable), variable);
  }

  @Override
  public TypedValue visit(final JSFieldAccess pFieldAccess) throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        conv.getObjectDeclarationOfFieldAccess(
            pFieldAccess, edge, function, ssa, constraints, errorConditions);
    final IntegerFormula objectId =
        conv.typedValues.objectValue(conv.scopedVariable(function, objectDeclaration, ssa));
    final IntegerFormula fieldName = conv.getStringFormula(pFieldAccess.getFieldName());
    return new FieldAccessToTypedValue(conv, ofmgr).accessField(objectId, fieldName);
  }

  @Override
  public TypedValue visit(final JSBracketPropertyAccess pPropertyAccess)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        conv.getObjectDeclarationOfObjectExpression(
            pPropertyAccess.getObjectExpression(),
            edge,
            function,
            ssa,
            constraints,
            errorConditions);
    final IntegerFormula objectId =
        conv.typedValues.objectValue(conv.scopedVariable(function, objectDeclaration, ssa));
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final TypedValue propertyNameValue = visit(propertyNameExpression);
    final IntegerFormula fieldName =
        (propertyNameExpression instanceof JSStringLiteralExpression)
            ? conv.getStringFormula(((JSStringLiteralExpression) propertyNameExpression).getValue())
            : conv.toStringFormula(propertyNameValue);
    final FieldAccessToTypedValue f = new FieldAccessToTypedValue(conv, ofmgr);
    final TypedValue propertyValue = f.accessField(objectId, fieldName);
    final TypedValue lengthProperty = f.accessField(objectId, conv.getStringFormula("length"));
    // TODO check if object is an array (otherwise `length` is no indicator if property is defined)
    final BooleanFormula isUndefinedArrayElementIndex =
        conv.bfmgr.and(
            mgr.makeEqual(propertyNameValue.getType(), conv.typeTags.NUMBER),
            conv.fpfmgr.greaterOrEquals(
                conv.toNumber(propertyNameValue), conv.toNumber(lengthProperty)));
    return conv.tvmgr.ifThenElse(
        isUndefinedArrayElementIndex, conv.tvmgr.getUndefinedValue(), propertyValue);
  }

  private TypedValue handlePredefined(final JSIdExpression pIdExpression)
      throws UnrecognizedCodeException {
    final String name = pIdExpression.getName();
    switch (name) {
      case "Infinity":
        return conv.tvmgr.createNumberValue(conv.fpfmgr.makePlusInfinity(Types.NUMBER_TYPE));
      case "NaN":
        if (!useNaN) {
          conv.logger.log(Level.WARNING, "NaN is used in "
              + pIdExpression.getFileLocation() + " even though cpa.predicate.js.useNaN=false");
        }
        return conv.tvmgr.createNumberValue(conv.fpfmgr.makeNaN(Types.NUMBER_TYPE));
      default:
        throw new UnrecognizedCodeException(
            "Variable without declaration is not defined on global object", pIdExpression);
    }
  }

}
