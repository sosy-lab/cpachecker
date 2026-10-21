// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class AstCfaRelationScopeTest {

  private static final String PROGRAMS = "test/programs/simple/witness_validation/";

  private static FluentIterable<AbstractSimpleDeclaration> variablesInScope(
      CFA pCfa, CFANode pNode) {
    return pCfa.getAstCfaRelation().getVariablesAndParametersInScope(pNode).orElseThrow();
  }

  @Test
  public void variableOfForLoopNotInScopeAfterLoop() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "two-loops.c");
    CFANode afterLoop = TestCfaUtils.getEdge("[!(i < 2)]", cfa).getSuccessor();

    assertThat(variablesInScope(cfa, afterLoop).transform(AbstractSimpleDeclaration::getOrigName))
        .doesNotContain("i");
    // The CFA builder gives the node the scope of the loop
    assertThat(
            FluentIterable.from(
                    cfa.getAstCfaRelation().getBuilderLocalVariablesInScopeByCfaNode(afterLoop))
                .transform(AbstractSimpleDeclaration::getOrigName))
        .contains("i");
  }

  @Test
  public void renamedVariableHasNameOfInputProgram() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "two-loops.c");
    CFANode loopBody = TestCfaUtils.getEdge("[i__1 < 2]", cfa).getSuccessor();

    assertThat(cfa.getAstCfaRelation().getNameInInputProgram(loopBody, "i__1")).isEqualTo("i");
  }

  @Test
  public void hiddenVariableNotInScope() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "shadowed-variable.c");
    CFANode inBlock = TestCfaUtils.getEdge("data__1 = data__1 + 1;", cfa).getPredecessor();

    assertThat(
            variablesInScope(cfa, inBlock)
                .filter(variable -> variable.getOrigName().equals("data"))
                .transform(AbstractSimpleDeclaration::getName))
        .containsExactly("data__1");
  }

  @Test
  public void staticLocalVariableInScopeOnce() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "static-local-variable.c");
    CFANode beforeReturn =
        TestCfaUtils.getEdge("return static__count__calls;", cfa).getPredecessor();

    assertThat(
            variablesInScope(cfa, beforeReturn)
                .filter(variable -> variable.getOrigName().equals("calls")))
        .hasSize(1);
  }
}
