// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class PrecisionScopeVariablesTest {

  private static CFA cfa;
  private static PrecisionScopeVariables variables;

  @BeforeClass
  public static void createCfa() throws Exception {
    cfa = TestCfaUtils.makeCfaFromFile("test/programs/precision-exchange/scopes.c");
    variables = new PrecisionScopeVariables(cfa);
  }

  private static Map<String, String> qualifiedNames(
      ImmutableMap<String, AbstractSimpleDeclaration> pVariables) {
    return Maps.transformValues(pVariables, AbstractSimpleDeclaration::getQualifiedName);
  }

  @Test
  public void staticLocalVariableIsNoGlobalVariable() {
    assertThat(qualifiedNames(variables.globalVariables()))
        .containsExactly("g", "g", "shadowed", "shadowed");
  }

  @Test
  public void returnVariableIsNoVariableOfFunction() {
    assertThat(qualifiedNames(variables.variablesIn("square")))
        .containsExactly("v", "square::v", "g", "g", "shadowed", "shadowed");
  }

  @Test
  public void staticLocalVariableIsVariableOfItsFunction() {
    assertThat(qualifiedNames(variables.variablesIn("counter")))
        .containsExactly("calls", "static__counter__calls", "g", "g", "shadowed", "shadowed");
  }

  @Test
  public void nameOfSeveralVariablesOfFunctionDenotesNone() {
    // main declares three variables i, and its variable shadowed hides the global one
    assertThat(qualifiedNames(variables.variablesIn("main")))
        .containsExactly("shadowed", "main::shadowed__1", "p", "main::p", "g", "g");
  }

  @Test
  public void variablesInScopeAtNodeHaveTheirNamesInInputProgram() {
    CFANode inBlock = TestCfaUtils.getEdge("p.y = i__2;", cfa).getPredecessor();

    assertThat(qualifiedNames(variables.variablesAt(inBlock).orElseThrow()))
        .containsExactly(
            "i", "main::i__2", "shadowed", "main::shadowed__1", "p", "main::p", "g", "g");
  }

  @Test
  public void staticLocalVariableInScopeOnlyInItsFunction() {
    CFANode inCounter =
        TestCfaUtils.getEdge("return static__counter__calls;", cfa).getPredecessor();

    assertThat(qualifiedNames(variables.variablesAt(inCounter).orElseThrow()))
        .containsExactly("calls", "static__counter__calls", "g", "g", "shadowed", "shadowed");
  }
}
