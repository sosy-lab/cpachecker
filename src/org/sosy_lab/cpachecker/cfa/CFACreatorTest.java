/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class CFACreatorTest {

  @Mock
  CFACreator cfaCreator;

  @Mock
  JMethodEntryNode N1;
  @Mock
  JMethodEntryNode N2;
  @Mock
  JMethodEntryNode N3;
  @Mock
  JMethodEntryNode N4;
  @Mock
  JMethodEntryNode N5;

  private TreeMap<String, FunctionEntryNode> cfa;

  @Before
  public void init() throws InvalidConfigurationException {

    MockitoAnnotations.initMocks(this);
    JMethodDeclaration functionDefinition1 =
        createFunctionDefinition("pack5.CallTests_true_assert", "main", "String[]");
    when(N1.getFunctionDefinition()).thenReturn(functionDefinition1);
    JMethodDeclaration functionDefinition2 =
        createFunctionDefinition("pack5.CallTests_true_assert", "main2", "String[]");
    when(N2.getFunctionDefinition()).thenReturn(functionDefinition2);
    JMethodDeclaration functionDefinition3 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "");
    when(N3.getFunctionDefinition()).thenReturn(functionDefinition3);
    JMethodDeclaration functionDefinition4 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "int");
    when(N4.getFunctionDefinition()).thenReturn(functionDefinition4);
    JMethodDeclaration functionDefinition5 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "int_int");
    when(N5.getFunctionDefinition()).thenReturn(functionDefinition5);
    cfa = buildExampleCfa(N1, N2, N3, N4, N5);

    when(cfaCreator.getJavaMainMethod(anyList(), anyString(), anyMap())).thenCallRealMethod();
  }

  @Test
  public void testGetJavaMainMethodSourceFileIsClasspathAndMainFunctionWithParameters()
      throws InvalidConfigurationException {
    String sourceFile = "test/programs/java/CallTests";
    String mainFunction = "pack5.CallTests_true_assert.main_String[]";
    FunctionEntryNode result =
        cfaCreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  @Test
  public void testGetJavaMainMethodForSameNameMethodsWithDifferentParameters()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "callTests_true_assert";

    assertThat(
        cfaCreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N3);
    mainFunction = "callTests_true_assert_int";
    assertThat(
        cfaCreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N4);

    mainFunction = "callTests_true_assert_int_int";
    assertThat(
        cfaCreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N5);
  }

  @Test
  public void testGetJavaMainMethodWithTwoSimilarNamedMethods()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "main";

    FunctionEntryNode result =
        cfaCreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  private JMethodDeclaration createFunctionDefinition(
      String classPath, String methodName, String parametersSubString) {
    String name = classPath + "_" + methodName;
    List<String> parameters;
    if (!parametersSubString.equals("")) {
      parameters = Arrays.asList(parametersSubString.split("_"));
    } else {
      parameters = Collections.emptyList();
    }
    List<JParameterDeclaration> jParameterDeclarations = new ArrayList<>(parameters.size());
    for (String parameter : parameters) {
      jParameterDeclarations.add(
          new JParameterDeclaration(
              mock(FileLocation.class), mock(JType.class), parameter, "stub", false));
    }
    if (!parametersSubString.isEmpty()) {
      name = name + "_" + parametersSubString;
    }

    return new JConstructorDeclaration(
        FileLocation.DUMMY,
        null,
        name,
        methodName,
        jParameterDeclarations,
        VisibilityModifier.PUBLIC,
        false,
        createDeclaringClassMock(classPath));
  }

  private JClassType createDeclaringClassMock(String classPath) {
    String simpleClassName;
    int indexOfLastDot = classPath.lastIndexOf(".");
    if (indexOfLastDot >= 0) {
      simpleClassName = classPath.substring(indexOfLastDot);
    } else {
      simpleClassName = classPath;
    }

    JClassType declaringClass = mock(JClassType.class);
    when(declaringClass.getName()).thenReturn(classPath);
    when(declaringClass.getSimpleName()).thenReturn(simpleClassName);
    return declaringClass;
  }

  private TreeMap<String, FunctionEntryNode> buildExampleCfa(JMethodEntryNode... nodeArray) {

    TreeMap<String, FunctionEntryNode> result = new TreeMap<>();

    for (JMethodEntryNode node : nodeArray) {
      result.put(node.getFunctionDefinition().getName(), node);
    }

    return result;
  }
}
