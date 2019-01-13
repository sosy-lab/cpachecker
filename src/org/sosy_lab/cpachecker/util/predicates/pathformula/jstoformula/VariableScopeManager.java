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

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.Scope;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Management of the scope of JavaScript variables.
 *
 * <p>To <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-10.3.1">resolve an
 * identifier</a> (of a variable) in ECMAScript the <a
 * href="https://www.ecma-international.org/ecma-262/5.1/#sec-10.2">Lexical Environment</a> of the
 * running <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-10.3">Execution Context</a>
 * is needed. If the Lexical Environment has no binding of the identifier, the identifier is looked
 * up in the outer environment reference of the Lexical Environment.
 *
 * <p>This chain of nested environments and the lookup has to be formula encoded. Therefore, each
 * variable is associated with a so-called scope (sort of an ID of the environment). Since
 * environments might be nested, a scope is associated with a so-called scope stack, which is an
 * array formula that contains all scopes of the (nested environments) chain. The scope of the
 * environment of the current execution context is encoded as a special indexed variables (see
 * {@link #getCurrentScope()}). On every function call the current scope has to be updated (see
 * {@link #createCurrentScope()}) to encode the change of the running execution context. To lookup
 * the scope of a declaration, the {@link Scope#getNestingLevel() nesting level} of the declaration
 * is used as index on the scope stack of the current scope.
 */
class VariableScopeManager extends ManagerWithEdgeContext {
  private final FunctionDeclaration<IntegerFormula> scopeOfDeclaration;
  private final FunctionDeclaration<ArrayFormula<IntegerFormula, IntegerFormula>>
      scopeStackDeclaration;
  private final IntegerFormula mainScope;
  private final ArrayFormula<IntegerFormula, IntegerFormula> globalScopeStack;
  private final FunctionDeclaration<IntegerFormula> varDeclaration;

  VariableScopeManager(final EdgeManagerContext pCtx) {
    super(pCtx);
    scopeOfDeclaration = ffmgr.declareUF("scopeOf", SCOPE_TYPE, VARIABLE_TYPE);
    scopeStackDeclaration = ffmgr.declareUF("scopeStack", SCOPE_STACK_TYPE, SCOPE_TYPE);
    mainScope = fmgr.makeNumber(SCOPE_TYPE, 0);
    globalScopeStack = afmgr.makeArray("globalScopeStack", SCOPE_STACK_TYPE);
    varDeclaration = ffmgr.declareUF("var", VARIABLE_TYPE, SCOPE_TYPE, VARIABLE_TYPE);
  }

  /**
   * Get the scope (formula) of a (function or variable) declaration.
   *
   * @param pDeclaration The (function or variable) declaration to get the scope of.
   * @return Scope (formula) of the passed (function or variable) declaration.
   */
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
    return afmgr.select(
        scopeStack(getCurrentScope()), ifmgr.makeNumber(scope.getNestingLevel()));
  }

  IntegerFormula scopeOf(final IntegerFormula pFunctionObject) {
    return ffmgr.callUF(scopeOfDeclaration, pFunctionObject);
  }

  ArrayFormula<IntegerFormula, IntegerFormula> scopeStack(final IntegerFormula pScope) {
    return ffmgr.callUF(scopeStackDeclaration, pScope);
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
    return var(scopeOf(pDeclaration), ctx.varMgr.makeVariable(pDeclaration.getQualifiedName()));
  }

  ArrayFormula<IntegerFormula, IntegerFormula> getGlobalScopeStack() {
    return globalScopeStack;
  }

  /**
   * Creates formula encoding of a JavaScript variable. It is used as encoding of variable
   * identifiers in <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-12.2">Variable
   * Statement</a> and <a
   * href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.1.2">Identifier Reference</a>.
   *
   * @param pScope Formula encoding of the scope object of the variable declaration (see {@link
   *     VariableScopeManager}).
   * @param pVariable Variable formula created using {@link VariableManager}.
   * @return Formula encoding of a (scoped) JavaScript variable.
   */
  IntegerFormula var(final IntegerFormula pScope, final IntegerFormula pVariable) {
    return ffmgr.callUF(varDeclaration, pScope, pVariable);
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
  void updateIndicesOfOtherScopeVariables(final JSSimpleDeclaration pVariableDeclaration) {
    if (pVariableDeclaration.getScope().isGlobalScope()) {
      return;
    }
    final String variableName = pVariableDeclaration.getQualifiedName();
    final List<Long> scopeIds =
        functionScopeManager.getScopeIds(
            pVariableDeclaration.getScope().getFunctionDeclaration().getQualifiedName());
    ctx.constraints.addConstraint(
        bfmgr.and(
            scopeIds
                .stream()
                .map(
                    (pScopeId) ->
                        bfmgr.or(
                            fmgr.makeEqual(
                                fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                scopeOf(pVariableDeclaration)),
                            fmgr.makeEqual(
                                var(
                                    fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                    ctx.varMgr.makePreviousVariable(variableName)),
                                var(
                                    fmgr.makeNumber(SCOPE_TYPE, pScopeId),
                                    ctx.varMgr.makeVariable(variableName)))))
                .collect(Collectors.toList())));
  }
}
