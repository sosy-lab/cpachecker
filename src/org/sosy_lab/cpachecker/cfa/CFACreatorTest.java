// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  JMethodEntryNode N1;
  @Mock
  JMethodEntryNode N2;
  @Mock
  JMethodEntryNode N3;
  @Mock
  JMethodEntryNode N4;
  @Mock
  JMethodEntryNode N5;

  private Map<String, FunctionEntryNode> cfa;

  @Before
  public void init() {

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
  }

  @Test
  public void testGetJavaMainMethodSourceFileIsClasspathAndMainFunctionWithParameters()
      throws InvalidConfigurationException {
    String sourceFile = "test/programs/java/CallTests";
    String mainFunction = "pack5.CallTests_true_assert.main_String[]";
    FunctionEntryNode result =
        CFACreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  @Test
  public void testGetJavaMainMethodForSameNameMethodsWithDifferentParameters()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "callTests_true_assert";

    assertThat(
        CFACreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N3);

    mainFunction = "callTests_true_assert_int";
    assertThat(
        CFACreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N4);

    mainFunction = "callTests_true_assert_int_int";
    assertThat(
        CFACreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa))
        .isEqualTo(N5);
  }

  @Test
  public void testGetJavaMainMethodWithTwoSimilarNamedMethods()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "main";

    FunctionEntryNode result =
        CFACreator.getJavaMainMethod(
            new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  private JMethodDeclaration createFunctionDefinition(
      String classPath, String methodName, String parametersSubString) {
    String name = classPath + "_" + methodName;
    List<String> parameters;
    if (!parametersSubString.isEmpty()) {
      parameters = Arrays.asList(parametersSubString.split("_"));
    } else {
      parameters = ImmutableList.of();
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

  private ImmutableMap<String, FunctionEntryNode> buildExampleCfa(JMethodEntryNode... nodeArray) {

    ImmutableMap.Builder<String, FunctionEntryNode> result = ImmutableMap.builder();

    for (JMethodEntryNode node : nodeArray) {
      result.put(node.getFunctionDefinition().getName(), node);
    }

    return result.build();
  }
}
