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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;

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
  CFACreator cfaCreator;

  private TreeMap<String, FunctionEntryNode> cfa;

  @Before
  public void init() throws InvalidConfigurationException {
    MockitoAnnotations.initMocks(this);
    cfa = buildExampleCfa();

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
  public void
  testGetJavaMainMethodForSameNameMethodsWithDifferentParametersThrowsExceptionIfParametersNotDefined() {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "CallTests_true_assert";
    assertThrows(
        InvalidConfigurationException.class,
        () ->
            cfaCreator.getJavaMainMethod(
                new ArrayList<>(ImmutableList.of(sourceFile)), mainFunction, cfa));
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

  private TreeMap<String, FunctionEntryNode> buildExampleCfa() {

    return new TreeMap<>(
        ImmutableMap.of(
            "pack5.CallTests_true_assert_main_String[]", N1,
            "pack5.CallTests_true_assert_main2_String[]", N2,
            "pack5.CallTests_true_assert_CallTests_true_assert", N3,
            "pack5.CallTests_true_assert_CallTests_true_assert_int", N4));
  }
}
