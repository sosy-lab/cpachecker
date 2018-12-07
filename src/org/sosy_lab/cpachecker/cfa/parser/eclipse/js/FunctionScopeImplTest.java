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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;

public class FunctionScopeImplTest {

  private FunctionScope functionScope;
  private Scope parentScope;
  private JSFunctionDeclaration functionDeclaration;
  private List<JSParameterDeclaration> parameters;
  private static final String name = "functionName";
  private static final String originalName = "functionOriginalName";
  private FileScope parentFileScope;
  private JSVariableDeclaration varAlreadyDeclaredInParentScope;

  @Before
  public void init() {
    varAlreadyDeclaredInParentScope =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            mock(org.sosy_lab.cpachecker.cfa.ast.js.Scope.class),
            "varAlreadyDeclaredInParentScope",
            "varAlreadyDeclaredInParentScope",
            "varAlreadyDeclaredInParentScope",
            null);
    parentFileScope = mock(FileScope.class);
    when(parentFileScope.qualifiedNameOfScope()).thenReturn("");
    parentScope =
        new Scope() {
          @Override
          public Scope getParentScope() {
            return parentFileScope;
          }

          @Nonnull
          @Override
          public String getNameOfScope() {
            return "parentScopeName";
          }

          @Override
          public void addDeclaration(final JSSimpleDeclaration pDeclaration) {
            throw new RuntimeException("Unexpected call");
          }

          @Override
          public Optional<? extends JSSimpleDeclaration> findDeclaration(final String pIdentifier) {
            return pIdentifier.equals(varAlreadyDeclaredInParentScope.getOrigName())
                ? Optional.of(varAlreadyDeclaredInParentScope)
                : Optional.empty();
          }
        };
    parameters =
        Collections.unmodifiableList(
            Arrays.asList(
                new JSParameterDeclaration(FileLocation.DUMMY, "p0"),
                new JSParameterDeclaration(FileLocation.DUMMY, "p1")));
    functionDeclaration =
        new JSFunctionDeclaration(
            FileLocation.DUMMY,
            org.sosy_lab.cpachecker.cfa.ast.js.Scope.GLOBAL,
            name,
            originalName,
            name,
            parameters);
    functionScope = new FunctionScopeImpl(parentScope, functionDeclaration);
  }

  @Test
  public void testConstructorSetQualifiedNameOfParameters() {
    Truth.assertThat(parameters.get(0).getQualifiedName())
        .isEqualTo("parentScopeName.functionName::p0");
    Truth.assertThat(parameters.get(1).getQualifiedName())
        .isEqualTo("parentScopeName.functionName::p1");
  }

  @Test
  public void testGetFunctionDeclaration() {
    Truth.assertThat(functionScope.getFunctionDeclaration()).isEqualTo(functionDeclaration);
  }

  @Test
  public void testGetNameOfScope() {
    Truth.assertThat(functionScope.getNameOfScope()).isEqualTo(name);
  }

  @Test
  public void testQualifiedNameOfScope() {
    Truth.assertThat(functionScope.qualifiedNameOfScope())
        .isEqualTo("parentScopeName.functionName");
  }

  @Test
  public void testQualifiedVariableNameOf() {
    Truth.assertThat(functionScope.qualifiedVariableNameOf("variableName"))
        .isEqualTo("parentScopeName.functionName::variableName");
  }

  @Test
  public void testQualifiedFunctionNameOf() {
    Truth.assertThat(functionScope.qualifiedFunctionNameOf("innerFunctionName"))
        .isEqualTo("parentScopeName.functionName.innerFunctionName");
  }

  @Test
  public void testHasParentScope() {
    Truth.assertThat(functionScope.hasParentScope()).isTrue();
  }

  @Test
  public void testGetParentScope() {
    Truth.assertThat(functionScope.getParentScope()).isEqualTo(parentScope);
  }

  @Test
  public void testGetFileScope() {
    Truth.assertThat(functionScope.getFileScope()).isEqualTo(parentFileScope);
  }

  @Test
  public void testThisBinding() {
    Truth.assertThat(functionScope.findDeclaration("this"))
        .isEqualTo(Optional.of(functionDeclaration.getThisVariableDeclaration()));
  }

  @Test
  public void testManagementOfDeclarations() {
    Truth.assertThat(functionScope.findDeclaration(varAlreadyDeclaredInParentScope.getOrigName()))
        .isEqualTo(Optional.of(varAlreadyDeclaredInParentScope));
    for (final JSParameterDeclaration parameter : parameters) {
      Truth.assertThat(functionScope.findDeclaration(parameter.getOrigName()))
          .isEqualTo(Optional.of(parameter));
    }
    final JSVariableDeclaration declaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            mock(org.sosy_lab.cpachecker.cfa.ast.js.Scope.class),
            "name",
            "originalName",
            "qualified::name",
            null);
    Truth.assertThat(functionScope.findDeclaration(declaration.getOrigName()))
        .isEqualTo(Optional.empty());
    functionScope.addDeclaration(declaration);
    Truth.assertThat(functionScope.findDeclaration(declaration.getOrigName()))
        .isEqualTo(Optional.of(declaration));
    // check shadowing of variable declared in parent scope
    final JSVariableDeclaration shadowVarAlreadyDeclaredInParentScope =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            mock(org.sosy_lab.cpachecker.cfa.ast.js.Scope.class),
            "shadowedVar",
            varAlreadyDeclaredInParentScope.getOrigName(),
            "qualified::shadowedVar",
            null);
    functionScope.addDeclaration(shadowVarAlreadyDeclaredInParentScope);
    // varAlreadyDeclaredInParentScope should be shadowed by shadowVarAlreadyDeclaredInParentScope
    Truth.assertThat(functionScope.findDeclaration(varAlreadyDeclaredInParentScope.getOrigName()))
        .isEqualTo(Optional.of(shadowVarAlreadyDeclaredInParentScope));
  }
}
