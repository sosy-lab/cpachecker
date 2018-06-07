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

import com.google.common.truth.Truth;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;

public class FileScopeImplTest {

  private static String fileName = "fileScope.js";
  private FileScope fileScope;

  @Before
  public void init() {
    fileScope = new FileScopeImpl(fileName);
  }

  @Test
  public void testGetFileName() {
    Truth.assertThat(fileScope.getFileName()).isEqualTo(fileName);
  }

  @Test
  public void testGetNameOfScope() {
    Truth.assertThat(fileScope.getNameOfScope()).isEqualTo("");
  }

  @Test
  public void testQualifiedNameOfScope() {
    Truth.assertThat(fileScope.qualifiedNameOfScope()).isEqualTo("");
  }

  @Test
  public void testQualifiedVariableNameOf() {
    final String variableName = "variableName";
    Truth.assertThat(fileScope.qualifiedVariableNameOf(variableName)).isEqualTo(variableName);
  }

  @Test
  public void testQualifiedFunctionNameOf() {
    final String functionName = "functionName";
    Truth.assertThat(fileScope.qualifiedFunctionNameOf(functionName)).isEqualTo(functionName);
  }

  @Test
  public void testHasParentScope() {
    Truth.assertThat(fileScope.hasParentScope()).isFalse();
  }

  @Test
  public void testGetParentScope() {
    Truth.assertThat(fileScope.getParentScope()).isNull();
  }

  @Test
  public void testGetFileScope() {
    Truth.assertThat(fileScope.getFileScope()).isEqualTo(fileScope);
  }

  @Test
  public void testManagementOfDeclarations() {
    final JSVariableDeclaration declaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY, false, "name", "originalName", "qualified::name", null);
    Truth.assertThat(fileScope.findDeclaration(declaration.getOrigName()))
        .isEqualTo(Optional.empty());
    fileScope.addDeclaration(declaration);
    Truth.assertThat(fileScope.findDeclaration(declaration.getOrigName()))
        .isEqualTo(Optional.of(declaration));
  }
}
