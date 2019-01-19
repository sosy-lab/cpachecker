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
import org.sosy_lab.java_smt.api.BooleanFormula;
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
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final IntegerFormula field;
    final IntegerFormula propertyNameFormula;
    if (propertyNameExpression instanceof JSStringLiteralExpression) {
      final String propertyName = ((JSStringLiteralExpression) propertyNameExpression).getValue();
      field = ctx.objMgr.makeFieldVariable(propertyName);
      propertyNameFormula = strMgr.getStringFormula(propertyName);
    } else {
      field = ctx.objMgr.makeFieldVariable();
      propertyNameFormula =
          valConv.toStringFormula(ctx.exprMgr.makeExpression(propertyNameExpression));
    }
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(field));
    ctx.objMgr.setObjectFields(
        objectId, afmgr.store(ctx.objMgr.getObjectFields(objectId), propertyNameFormula, field));
    return makeAssignment(field, pRhsValue);
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
      return bfmgr.and(
          fmgr.assignment(typedVarValues.typeof(pLeft), typeTags.NUMBER),
          fmgr.makeEqual(typedVarValues.numberValue(pLeft), pRight.getValue()));
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
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.FUNCTION),
                fmgr.makeEqual(typedVarValues.functionValue(pLeft), valConv.toFunction(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.NUMBER),
                fmgr.makeEqual(typedVarValues.numberValue(pLeft), valConv.toNumber(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.OBJECT),
                fmgr.makeEqual(typedVarValues.objectValue(pLeft), valConv.toObject(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.STRING),
                fmgr.makeEqual(typedVarValues.stringValue(pLeft), valConv.toStringFormula(pRight))),
            fmgr.makeEqual(typedVarValues.typeof(pLeft), typeTags.UNDEFINED)));
  }
}
