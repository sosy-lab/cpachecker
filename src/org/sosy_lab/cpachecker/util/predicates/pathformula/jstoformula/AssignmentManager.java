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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.SCOPE_TYPE;

import java.util.List;
import java.util.stream.Collectors;
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class AssignmentManager extends ManagerWithEdgeContext {

  AssignmentManager(final EdgeManagerContext pCtx) {
    super(pCtx);
  }

  IntegerFormula buildLvalueTerm(final JSSimpleDeclaration pDeclaration) {
    return gctx.typedValues.var(
        ctx.scopeMgr.scopeOf(pDeclaration),
        ctx.varMgr.makeFreshVariable(pDeclaration.getQualifiedName()));
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
        gctx.typedValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final IntegerFormula field;
    final IntegerFormula propertyNameFormula;
    if (propertyNameExpression instanceof JSStringLiteralExpression) {
      final String propertyName = ((JSStringLiteralExpression) propertyNameExpression).getValue();
      field = ctx.objMgr.makeFieldVariable(propertyName);
      propertyNameFormula = gctx.strMgr.getStringFormula(propertyName);
    } else {
      field = ctx.objMgr.makeFieldVariable();
      propertyNameFormula =
          gctx.valConv.toStringFormula(ctx.exprMgr.makeExpression(propertyNameExpression));
    }
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(field));
    ctx.objMgr.setObjectFields(
        objectId,
        gctx.afmgr.store(ctx.objMgr.getObjectFields(objectId), propertyNameFormula, field));
    return makeAssignment(field, pRhsValue);
  }

  private BooleanFormula makeAssignment(final JSFieldAccess pLhs, final TypedValue pRhsValue)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfFieldAccess(pLhs);
    final IntegerFormula objectId =
        gctx.typedValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final String fieldName = pLhs.getFieldName();
    final IntegerFormula field = ctx.objMgr.makeFieldVariable(fieldName);
    ctx.constraints.addConstraint(ctx.objMgr.markFieldAsSet(field));
    ctx.objMgr.setObjectFields(
        objectId,
        gctx.afmgr.store(
            ctx.objMgr.getObjectFields(objectId), gctx.strMgr.getStringFormula(fieldName), field));
    return makeAssignment(field, pRhsValue);
  }

  @Nonnull
  private BooleanFormula makeAssignment(final JSIdExpression pLhs, final TypedValue pRhsValue) {
    final JSSimpleDeclaration declaration = pLhs.getDeclaration();
    assert declaration != null;
    final IntegerFormula l = buildLvalueTerm(declaration);
    updateIndicesOfOtherScopeVariables(declaration);
    return makeAssignment(l, pRhsValue);
  }

  /**
   * Indices of other scope variables have to be updated on every assignment.
   *
   * <p>If a function f(p) is called the first time a scope s0 is created and variables/parameters
   * are associated with this scope like (var s0 f::p@2). On the second call of f(p) another scope
   * s1 is created and the index of p is incremented, e.g. p=3, and p is associated to s1 by (var s1
   * f::p@3). However, if p of the first scope is captured in a closure then it would be addressed
   * by (var s0 f::p@3) instead of (var s0 f::p@2) since the index of p has changed due to the other
   * call of f. To work around, indices of the same variable in other scopes are updated too, when a
   * value is assigned to the variable. Since, p is assigned a value on the second call of f(p)
   * using (var s1 f::p@3), the index of p in s0 has to be updated by (= (var s0 f::p@2) (var s0
   * f::p@3)).
   */
  private void updateIndicesOfOtherScopeVariables(final JSSimpleDeclaration pVariableDeclaration) {
    if (pVariableDeclaration.getScope().isGlobalScope()) {
      return;
    }
    final String variableName = pVariableDeclaration.getQualifiedName();
    final List<Long> scopeIds =
        gctx.functionScopeManager.getScopeIds(
            pVariableDeclaration.getScope().getFunctionDeclaration().getQualifiedName());
    ctx.constraints.addConstraint(
        gctx.bfmgr.and(
            scopeIds
                .stream()
                .map(
                    (pScopeId) ->
                        gctx.bfmgr.or(
                            gctx.fmgr.makeEqual(
                                gctx.fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                ctx.scopeMgr.scopeOf(pVariableDeclaration)),
                            gctx.fmgr.makeEqual(
                                gctx.typedValues.var(
                                    gctx.fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                    ctx.varMgr.makePreviousVariable(variableName)),
                                gctx.typedValues.var(
                                    gctx.fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                    ctx.varMgr.makeVariable(variableName)))))
                .collect(Collectors.toList())));
  }

  @Nonnull
  BooleanFormula makeAssignment(final IntegerFormula pLeft, final TypedValue pRight) {
    final BooleanFormulaManagerView bfmgr = gctx.bfmgr;
    final FormulaManagerView fmgr = gctx.fmgr;
    final TypeTags typeTags = gctx.typeTags;
    final TypedValues typedValues = gctx.typedValues;

    final IntegerFormula rType = pRight.getType();
    if (rType.equals(typeTags.BOOLEAN)) {
      return bfmgr.and(
          fmgr.assignment(typedValues.typeof(pLeft), typeTags.BOOLEAN),
          fmgr.makeEqual(typedValues.booleanValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.FUNCTION)) {
      return bfmgr.and(
          fmgr.assignment(typedValues.typeof(pLeft), typeTags.FUNCTION),
          fmgr.makeEqual(typedValues.functionValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.NUMBER)) {
      return bfmgr.and(
          fmgr.assignment(typedValues.typeof(pLeft), typeTags.NUMBER),
          fmgr.makeEqual(typedValues.numberValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.OBJECT)) {
      return bfmgr.and(
          fmgr.assignment(typedValues.typeof(pLeft), typeTags.OBJECT),
          fmgr.makeEqual(typedValues.objectValue(pLeft), pRight.getValue()));
    }
    if (rType.equals(typeTags.STRING)) {
      return bfmgr.and(
          fmgr.assignment(typedValues.typeof(pLeft), typeTags.STRING),
          fmgr.makeEqual(typedValues.stringValue(pLeft), pRight.getValue()));
    }
    final ValueConverterManager valConv = gctx.valConv;
    return fmgr.makeAnd(
        fmgr.assignment(typedValues.typeof(pLeft), pRight.getType()),
        bfmgr.or(
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.BOOLEAN),
                fmgr.makeEqual(typedValues.booleanValue(pLeft), valConv.toBoolean(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.FUNCTION),
                fmgr.makeEqual(typedValues.functionValue(pLeft), valConv.toFunction(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.NUMBER),
                fmgr.makeEqual(typedValues.numberValue(pLeft), valConv.toNumber(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.OBJECT),
                fmgr.makeEqual(typedValues.objectValue(pLeft), valConv.toObject(pRight))),
            fmgr.makeAnd(
                fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.STRING),
                fmgr.makeEqual(typedValues.stringValue(pLeft), valConv.toStringFormula(pRight))),
            fmgr.makeEqual(typedValues.typeof(pLeft), typeTags.UNDEFINED)));
  }
}
