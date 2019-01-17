/*
 *  CPAchecker is a tool for configurable software verification.
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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.Scope;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Management of formula encoding of access to a property of a JavaScript object. */
class PropertyAccessManager extends ManagerWithEdgeContext {

  /**
   * The <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.6.2">internal property
   * [[Prototype]]</a> is represented as regular field called <a
   * href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto">__proto__</a>
   * which is still supported by all common JavaScript engines (<a
   * href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto#Browser_compatibility">as
   * of 4. September 2018</a>).
   */
  private final IntegerFormula prototypeField;

  PropertyAccessManager(final EdgeManagerContext pCtx) {
    super(pCtx);
    prototypeField = strMgr.getStringFormula("__proto__");
  }

  /**
   * Look up field on the prototype chain (recursively) of an object.
   *
   * @param prototypeChainDepth The depth of the current prototype in the prototype chain.
   * @param pPrototypeField The prototype field (internal property) of the object.
   * @param pFieldName The field name (formula of string constant) to look up.
   * @return The typed value of the field on the prototype chain.
   */
  private TypedValue lookUpOnPrototypeChain(
      final int prototypeChainDepth,
      final IntegerFormula pPrototypeField,
      final IntegerFormula pFieldName) {
    final TypedValue undefined = tvmgr.getUndefinedValue();
    if (prototypeChainDepth > jsOptions.maxPrototypeChainLength) {
      return undefined;
    }
    final IntegerFormula prototypeObjectId = typedVarValues.objectValue(pPrototypeField);
    final ArrayFormula<IntegerFormula, IntegerFormula> prototypeFields =
        ctx.objMgr.getObjectFields(prototypeObjectId);
    final IntegerFormula fieldOnPrototype = afmgr.select(prototypeFields, pFieldName);
    final BooleanFormula hasNoParentPrototype = ctx.objMgr.markFieldAsNotSet(pPrototypeField);
    final BooleanFormula isFieldOnPrototypeNotSet = ctx.objMgr.markFieldAsNotSet(fieldOnPrototype);
    final TypedValue parentPrototype =
        lookUpOnPrototypeChain(
            prototypeChainDepth + 1, afmgr.select(prototypeFields, prototypeField), pFieldName);
    return new TypedValue(
        bfmgr.ifThenElse(
            hasNoParentPrototype,
            undefined.getType(),
            bfmgr.ifThenElse(
                isFieldOnPrototypeNotSet,
                parentPrototype.getType(),
                typedVarValues.typeof(fieldOnPrototype))),
        bfmgr.ifThenElse(
            hasNoParentPrototype,
            undefined.getValue(),
            bfmgr.ifThenElse(
                isFieldOnPrototypeNotSet, parentPrototype.getValue(), fieldOnPrototype)));
  }

  /**
   * Get typed value of a field on an object.
   *
   * @param pObjectId The object (value) to look up the field.
   * @param pFieldName The field name (formula of string constant) to look up.
   * @return Typed value of the field on the object or the field inherited by the prototype chain of
   *     the object.
   */
  TypedValue accessField(final IntegerFormula pObjectId, final IntegerFormula pFieldName) {
    final ArrayFormula<IntegerFormula, IntegerFormula> fields =
        ctx.objMgr.getObjectFields(pObjectId);
    final IntegerFormula field = afmgr.select(fields, pFieldName);
    final BooleanFormula isObjectFieldNotSet = ctx.objMgr.markFieldAsNotSet(field);
    final TypedValue typedValueOnPrototypeChain =
        lookUpOnPrototypeChain(1, afmgr.select(fields, prototypeField), pFieldName);
    return new TypedValue(
        bfmgr.ifThenElse(
            isObjectFieldNotSet,
            typedValueOnPrototypeChain.getType(),
            typedVarValues.typeof(field)),
        bfmgr.ifThenElse(isObjectFieldNotSet, typedValueOnPrototypeChain.getValue(), field));
  }

  JSSimpleDeclaration getObjectDeclarationOfFieldAccess(final JSFieldAccess ppFieldAccess)
      throws UnrecognizedCodeException {
    return getObjectDeclarationOfObjectExpression(ppFieldAccess.getObject());
  }

  private long tmpVariableCount = 0;

  private String generateTemporaryVariableName() {
    ++tmpVariableCount;
    return "tmp" + tmpVariableCount;
  }

  JSSimpleDeclaration getObjectDeclarationOfObjectExpression(final JSExpression pObjectExpression)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration;
    if (pObjectExpression instanceof JSIdExpression) {
      objectDeclaration = ((JSIdExpression) pObjectExpression).getDeclaration();
      assert objectDeclaration != null
          : "Object '"
              + pObjectExpression
              + "' is not declared ("
              + pObjectExpression.getFileLocation()
              + ")";
    } else {
      final String temporaryVariableName = generateTemporaryVariableName();
      objectDeclaration =
          new JSVariableDeclaration(
              FileLocation.DUMMY,
              Scope.GLOBAL,
              temporaryVariableName,
              temporaryVariableName,
              temporaryVariableName,
              new JSInitializerExpression(FileLocation.DUMMY, pObjectExpression));
      ctx.constraints.addConstraint(
          ctx.assignmentMgr.makeAssignment(
              ctx.scopeMgr.declareScopedVariable(objectDeclaration),
              ctx.exprMgr.makeExpression(pObjectExpression)));
    }
    return objectDeclaration;
  }
}
