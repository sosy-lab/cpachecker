/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.NUMBER_TYPE;

import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBracketPropertyAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Formula management of assignments to variables and object properties. */
class AssignmentManager extends ManagerWithEdgeContext {

  AssignmentManager(final EdgeManagerContext pCtx) {
    super(pCtx);
  }

  /**
   * Creates formula for the given assignment.
   *
   * @param lhs the left-hand-side of the assignment
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   */
  BooleanFormula makeAssignment(final JSLeftHandSide lhs, final JSRightHandSide rhs)
      throws UnrecognizedCodeException {
    return makeAssignment(lhs, rhs, ctx);
  }

  BooleanFormula makeAssignment(
      final JSLeftHandSide lhs, final JSRightHandSide rhs, final EdgeManagerContext rhsCtx)
      throws UnrecognizedCodeException {
    final TypedValue r = rhsCtx.exprMgr.makeExpression(rhs);
    if (lhs instanceof JSIdExpression) {
      return makeAssignment((JSIdExpression) lhs, r);
    } else if (lhs instanceof JSBracketPropertyAccess) {
      return makeAssignment((JSBracketPropertyAccess) lhs, r);
    } else if (lhs instanceof JSFieldAccess) {
      return makeAssignment((JSFieldAccess) lhs, r);
    }
    throw new UnrecognizedCodeException(
        "Unimplemented left-hand-side in assignment", ctx.edge, lhs);
  }

  private BooleanFormula makeAssignment(
      final JSBracketPropertyAccess pPropertyAccess, final TypedValue pRhsValue)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfObjectExpression(pPropertyAccess.getObjectExpression());
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    // If the length property of an array is updated, it is stored in these fields and the resulting
    // fields will be assigned to this variable.
    ArrayFormula<FloatingPointFormula, IntegerFormula> objectFields =
        ctx.objMgr.getObjectFields(objectId);
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final IntegerFormula field;
    final FloatingPointFormula propertyNameFormula;
    if (propertyNameExpression instanceof JSStringLiteralExpression) {
      final String propertyName = ((JSStringLiteralExpression) propertyNameExpression).getValue();
      field = ctx.objMgr.makeFieldVariable(propertyName);
      propertyNameFormula = strMgr.getStringFormula(propertyName);
      // TODO check if integer string (otherwise do not set/update `length` property)
      if (StringFormulaManager.isNumberString(propertyName)) {
        objectFields =
            updateArrayLengthProperty(
                objectId, objectFields, fpfmgr.makeNumber(propertyName, NUMBER_TYPE));
      }
    } else {
      field = ctx.objMgr.makeFieldVariable();
      final TypedValue typedPropertyNameValue = ctx.exprMgr.makeExpression(propertyNameExpression);
      propertyNameFormula = valConv.toStringFormula(typedPropertyNameValue);
      final IntegerFormula propertyNameType = typedPropertyNameValue.getType();
      if (propertyNameType.equals(typeTags.NUMBER)) {
        objectFields =
            updateArrayLengthProperty(
                objectId, objectFields, (FloatingPointFormula) typedPropertyNameValue.getValue());
      } else if (propertyNameType.equals(typeTags.STRING)) {
        objectFields =
            updateArrayLengthProperty(
                objectId,
                objectFields,
                valConv.stringToNumber((FloatingPointFormula) typedPropertyNameValue.getValue()));
      } else {
        // variable
        final IntegerFormula variable = (IntegerFormula) typedPropertyNameValue.getValue();
        objectFields =
            bfmgr.ifThenElse(
                fmgr.makeEqual(propertyNameType, typeTags.NUMBER),
                updateArrayLengthProperty(
                    objectId, objectFields, typedVarValues.numberValue(variable)),
                bfmgr.ifThenElse(
                    fmgr.makeEqual(propertyNameType, typeTags.STRING),
                    updateArrayLengthProperty(
                        objectId,
                        objectFields,
                        valConv.stringToNumber(typedVarValues.stringValue(variable))),
                    objectFields));
      }
    }
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(field));
    ctx.objMgr.setObjectFields(objectId, afmgr.store(objectFields, propertyNameFormula, field));
    return makeAssignment(field, pRhsValue);
  }

  private ArrayFormula<FloatingPointFormula, IntegerFormula> updateArrayLengthProperty(
      final IntegerFormula pObjectId,
      final ArrayFormula<FloatingPointFormula, IntegerFormula> pObjectFields,
      final FloatingPointFormula pNumberIndexFormula) {
    final FloatingPointFormula lengthPropertyNameFormula = strMgr.getStringFormula("length");
    final IntegerFormula oldLengthVariable =
        (IntegerFormula) ctx.propMgr.accessField(pObjectId, lengthPropertyNameFormula).getValue();
    final IntegerFormula newLengthVariable = ctx.objMgr.makeFieldVariable("length");
    final FloatingPointFormula newLength =
        fpfmgr.add(pNumberIndexFormula, fpfmgr.makeNumber(1, NUMBER_TYPE));
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(newLengthVariable));
    ctx.constraints.addConstraint(makeNumberAssignment(newLengthVariable, newLength));
    // TODO check if object is an array (otherwise do not set/update `length` property)
    // TODO check if index is positive int < 2^32
    // TODO check NaN and Infinity
    return afmgr.store(
        pObjectFields,
        lengthPropertyNameFormula,
        bfmgr.ifThenElse(
            fpfmgr.greaterOrEquals(typedVarValues.numberValue(oldLengthVariable), newLength),
            oldLengthVariable,
            newLengthVariable));
  }

  private BooleanFormula makeAssignment(final JSFieldAccess pLhs, final TypedValue pRhsValue)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfFieldAccess(pLhs);
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final String fieldName = pLhs.getFieldName();
    final IntegerFormula field = ctx.objMgr.makeFieldVariable(fieldName);
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(field));
    ctx.objMgr.setObjectFields(
        objectId,
        afmgr.store(
            ctx.objMgr.getObjectFields(objectId), strMgr.getStringFormula(fieldName), field));
    return makeAssignment(field, pRhsValue);
  }

  @Nonnull
  private BooleanFormula makeAssignment(final JSIdExpression pLhs, final TypedValue pRhsValue) {
    final JSSimpleDeclaration declaration;
    final EdgeManagerContext varCtx;
    if (pLhs.getDeclaration() != null) {
      declaration = pLhs.getDeclaration();
      varCtx = ctx;
    } else {
      logger.logOnce(
          Level.WARNING,
          "Assignment to undeclared variable '" + pLhs + "' (" + pLhs.getFileLocation() + ")");
      declaration = globalDeclarationsMgr.get(pLhs);
      varCtx = ctx.copy("main");
    }
    final IntegerFormula l = varCtx.scopeMgr.declareScopedVariable(declaration);
    varCtx.scopeMgr.updateIndicesOfOtherScopeVariables(declaration);
    return makeAssignment(l, pRhsValue);
  }



  @Nonnull
  BooleanFormula makeAssignment(final IntegerFormula pLeft, final TypedValue pRight) {
    final IntegerFormula rType = pRight.getType();
    if (rType.equals(typeTags.BOOLEAN)) {
      return bfmgr.and(
          fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.BOOLEAN),
          fmgr.makeEqual(typedVarValues.booleanValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.FUNCTION)) {
      return bfmgr.and(
          fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.FUNCTION),
          fmgr.makeEqual(typedVarValues.functionValue(pLeft), pRight.getValue()),
          fmgr.makeEqual(typedVarValues.objectValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.NUMBER)) {
      return makeNumberAssignment(pLeft, (FloatingPointFormula) pRight.getValue());
    }
    if (rType.equals(typeTags.OBJECT)) {
      return bfmgr.and(
          fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.OBJECT),
          fmgr.makeEqual(typedVarValues.objectValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.STRING)) {
      return bfmgr.and(
          fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.STRING),
          fmgr.makeEqual(typedVarValues.stringValue(pLeft), pRight.getValue()));
    }
    return fmgr.makeAnd(
        fmgr.assignment(typedVarValues.typeof(pLeft), pRight.getType()),
        bfmgr.or(
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.BOOLEAN),
                fmgr.makeEqual(typedVarValues.booleanValue(pLeft), valConv.toBoolean(pRight))),
            bfmgr.and(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.FUNCTION),
                fmgr.makeEqual(typedVarValues.functionValue(pLeft), valConv.toFunction(pRight)),
                fmgr.makeEqual(typedVarValues.objectValue(pLeft), valConv.toObject(pRight))),
            makeNumberAssignment(pLeft, valConv.toNumber(pRight)),
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.OBJECT),
                fmgr.makeEqual(typedVarValues.objectValue(pLeft), valConv.toObject(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.STRING),
                fmgr.makeEqual(typedVarValues.stringValue(pLeft), valConv.toStringFormula(pRight))),
            fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.UNDEFINED)));
  }

  @Nonnull
  private BooleanFormula makeNumberAssignment(
      final IntegerFormula pLeft, final FloatingPointFormula pRightValue) {
    return bfmgr.and(
        fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.NUMBER),
        fmgr.assignment(typedVarValues.numberValue(pLeft), pRightValue));
  }
}
