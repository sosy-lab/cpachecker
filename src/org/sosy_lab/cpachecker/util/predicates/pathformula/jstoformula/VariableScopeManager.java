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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.SCOPE_STACK_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.SCOPE_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.VARIABLE_TYPE;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.Scope;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class VariableScopeManager extends ManagerWithEdgeContext {
  private final FunctionDeclaration<IntegerFormula> scopeOfDeclaration;
  private final FunctionDeclaration<ArrayFormula<IntegerFormula, IntegerFormula>>
      scopeStackDeclaration;
  private final IntegerFormula mainScope;
  private final ArrayFormula<IntegerFormula, IntegerFormula> globalScopeStack;

  VariableScopeManager(final EdgeManagerContext pCtx) {
    super(pCtx);
    scopeOfDeclaration = ctx.conv.ffmgr.declareUF("scopeOf", SCOPE_TYPE, VARIABLE_TYPE);
    scopeStackDeclaration = ctx.conv.ffmgr.declareUF("scopeStack", SCOPE_STACK_TYPE, SCOPE_TYPE);
    mainScope = ctx.conv.fmgr.makeNumber(SCOPE_TYPE, 0);
    globalScopeStack = ctx.conv.afmgr.makeArray("globalScopeStack", SCOPE_STACK_TYPE);
  }

  @Nonnull
  IntegerFormula scopeOf(final JSSimpleDeclaration pDeclaration) {
    final Scope scope = pDeclaration.getScope();
    if (scope.isGlobalScope()) {
      return mainScope;
    }
    assert !ctx.function.equals("main")
        : pDeclaration.getQualifiedName()
            + " has nesting level of "
            + scope.getNestingLevel()
            + " in main function";
    return ctx.conv.afmgr.select(
        scopeStack(getCurrentScope()), ctx.conv.ifmgr.makeNumber(scope.getNestingLevel()));
  }

  IntegerFormula scopeOf(final IntegerFormula pFunctionObject) {
    return ctx.conv.ffmgr.callUF(scopeOfDeclaration, pFunctionObject);
  }

  ArrayFormula<IntegerFormula, IntegerFormula> scopeStack(final IntegerFormula pScope) {
    return ctx.conv.ffmgr.callUF(scopeStackDeclaration, pScope);
  }

  IntegerFormula getCurrentScope() {
    return ctx.function.equals("main")
        ? mainScope
        : ctx.varMgr.makeVariable(ctx.function + "_currentScope");
  }

  @Nonnull
  IntegerFormula createCurrentScope() {
    assert !ctx.function.equals("main");
    return ctx.varMgr.makeFreshVariable(ctx.function + "_currentScope");
  }

  IntegerFormula scopedVariable(final JSSimpleDeclaration pDeclaration) {
    return typedValues.var(
        scopeOf(pDeclaration), ctx.varMgr.makeVariable(pDeclaration.getQualifiedName()));
  }

  ArrayFormula<IntegerFormula, IntegerFormula> getGlobalScopeStack() {
    return globalScopeStack;
  }
}
