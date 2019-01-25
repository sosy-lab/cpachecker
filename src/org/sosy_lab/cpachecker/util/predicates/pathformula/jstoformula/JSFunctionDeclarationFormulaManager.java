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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.FUNCTION_DECLARATION_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.FUNCTION_TYPE;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Management of formula encoding of function objects and their association with a function
 * declaration, which is required to resolve a dynamic function call (call of a function object).
 * The association is accomplished using an uninterpreted function formula called <code>
 * declarationOf</code>.
 *
 * @see org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression
 * @see org.sosy_lab.cpachecker.cfa.parser.eclipse.js.UnknownFunctionCallerDeclarationBuilder
 */
class JSFunctionDeclarationFormulaManager extends ManagerWithEdgeContext {
  private final FunctionDeclaration<IntegerFormula> declarationOfDeclaration;

  JSFunctionDeclarationFormulaManager(final EdgeManagerContext pCtx) {
    super(pCtx);
    declarationOfDeclaration =
        ffmgr.declareUF("declarationOf", FUNCTION_DECLARATION_TYPE, FUNCTION_TYPE);
  }

  private IntegerFormula declarationOf(final IntegerFormula pFunctionObject) {
    return ffmgr.callUF(declarationOfDeclaration, pFunctionObject);
  }

  BooleanFormula declareFunction(final JSFunctionDeclaration pDeclaration)
      throws UnrecognizedCodeException {
    final IntegerFormula functionObjectId = createFunctionObject(pDeclaration);
    final IntegerFormula funObjVar = ctx.scopeMgr.declareScopedVariable(pDeclaration);
    return bfmgr.and(
        fmgr.assignment(typedVarValues.typeof(funObjVar), typeTags.FUNCTION),
        fmgr.makeEqual(typedVarValues.functionValue(funObjVar), functionObjectId),
        fmgr.makeEqual(typedVarValues.objectValue(funObjVar), functionObjectId),
        fmgr.makeEqual(ctx.scopeMgr.scopeOf(functionObjectId), ctx.scopeMgr.getCurrentScope()),
        fmgr.makeEqual(declarationOf(functionObjectId), getFunctionDeclarationId(pDeclaration)));
  }

  @Nonnull
  private IntegerFormula createFunctionObject(final JSFunctionDeclaration pDeclaration)
      throws UnrecognizedCodeException {
    // TODO implement without creating CFA expressions
    return (IntegerFormula)
        ctx.objMgr
            .createObject(
                new JSObjectLiteralExpression(
                    FileLocation.DUMMY,
                    ImmutableList.of(
                        new JSObjectLiteralField(
                            "prototype",
                            new JSObjectLiteralExpression(
                                FileLocation.DUMMY, Collections.emptyList())),
                        new JSObjectLiteralField(
                            "length",
                            new JSIntegerLiteralExpression(
                                FileLocation.DUMMY,
                                BigInteger.valueOf(pDeclaration.getParameters().size()))))))
            .getValue();
  }

  TypedValue makeDeclaredBy(final JSDeclaredByExpression pDeclaredByExpression) {
    final JSSimpleDeclaration variableDeclaration =
        pDeclaredByExpression.getIdExpression().getDeclaration();
    assert variableDeclaration != null;
    return tvmgr.createBooleanValue(
        fmgr.makeEqual(
            declarationOf(
                typedVarValues.functionValue(ctx.scopeMgr.scopedVariable(variableDeclaration))),
            getFunctionDeclarationId(pDeclaredByExpression.getJsFunctionDeclaration())));
  }

  @Nonnull
  private IntegerFormula getFunctionDeclarationId(final JSFunctionDeclaration pDeclaration) {
    return fmgr.makeNumber(FUNCTION_DECLARATION_TYPE, functionDeclarationIds.get(pDeclaration));
  }
}
