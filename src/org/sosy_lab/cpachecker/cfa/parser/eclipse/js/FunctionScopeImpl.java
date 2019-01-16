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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;

class FunctionScopeImpl implements FunctionScope {

  private final Scope parentScope;
  private final JSFunctionDeclaration functionDeclaration;
  private final LogManager logger;

  /** Declarations in the body of the function mapped by their original name. */
  private final Map<String, JSSimpleDeclaration> localDeclarations = new HashMap<>();

  FunctionScopeImpl(
      @Nonnull final Scope pParentScope,
      @Nonnull final JSFunctionDeclaration pFunctionDeclaration,
      final LogManager pLogger) {
    parentScope = pParentScope;
    functionDeclaration = pFunctionDeclaration;
    logger = pLogger;
    setQualifiedNameOfParameters();
    setScopeOfParameters();
  }

  /**
   * The qualified name of a parameter depends on the function scope. Parameters belong to the
   * function declaration, which is required by the function scope. Thereby, the qualified name of a
   * parameter can only be set after the function scope is created.
   */
  private void setQualifiedNameOfParameters() {
    for (final JSParameterDeclaration parameterDeclaration : functionDeclaration.getParameters()) {
      final String parameterName = parameterDeclaration.getName();
      assert !getParentScope().findDeclaration(parameterName).isPresent()
          : "Parameter name " + parameterName + " may not shadow identifier of parent scope";
      parameterDeclaration.setQualifiedName(qualifiedVariableNameOf(parameterName));
    }
  }

  private void setScopeOfParameters() {
    for (final JSParameterDeclaration parameterDeclaration : functionDeclaration.getParameters()) {
      final org.sosy_lab.cpachecker.cfa.ast.js.Scope cfaScope = ScopeConverter.toCFAScope(this);
      parameterDeclaration.setScope(cfaScope);
    }
  }

  @Override
  public Scope getParentScope() {
    return parentScope;
  }

  @Override
  public void addDeclaration(@Nonnull final JSSimpleDeclaration pDeclaration) {
    final String origName = pDeclaration.getOrigName();
    if (localDeclarations.containsKey(origName)) {
      logger.log(
          Level.WARNING, "Duplicate declaration " + origName + " in " + qualifiedNameOfScope());
    } else {
      localDeclarations.put(origName, pDeclaration);
    }
  }

  @Override
  public Optional<? extends JSSimpleDeclaration> findDeclaration(
      @Nonnull final String pIdentifier) {
    if (pIdentifier.equals("this")) {
      return Optional.of(functionDeclaration.getThisVariableDeclaration());
    }
    final Optional<? extends JSSimpleDeclaration> localDeclaration =
        findLocalDeclaration(pIdentifier);
    if (localDeclaration.isPresent()) {
      return localDeclaration;
    }
    final Optional<? extends JSSimpleDeclaration> parameterDeclaration =
        findParameterDeclaration(pIdentifier);
    if (parameterDeclaration.isPresent()) {
      return parameterDeclaration;
    }
    return parentScope.findDeclaration(pIdentifier);
  }

  /**
   * Find declaration of variable declared in this function.
   *
   * @param pIdentifier The original name as in the source code to analyze.
   * @return Only present if declaration is found.
   */
  private Optional<? extends JSSimpleDeclaration> findLocalDeclaration(
      @Nonnull final String pIdentifier) {
    return Optional.ofNullable(localDeclarations.get(pIdentifier));
  }

  private Optional<? extends JSSimpleDeclaration> findParameterDeclaration(
      @Nonnull final String pIdentifier) {
    return functionDeclaration
        .getParameters()
        .stream()
        .filter(
            pJSParameterDeclaration -> pJSParameterDeclaration.getOrigName().equals(pIdentifier))
        .findFirst();
  }

  @Override
  public JSFunctionDeclaration getFunctionDeclaration() {
    return functionDeclaration;
  }
}
