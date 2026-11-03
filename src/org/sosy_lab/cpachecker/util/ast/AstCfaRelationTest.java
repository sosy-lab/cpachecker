// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class AstCfaRelationTest {

  private static final String PROGRAMS = "test/programs/simple/witness_validation/";

  /**
   * The call to {@code f} is only executed in one branch of the conditional expression, so a state
   * at the call does not describe the start of the statement.
   */
  @Test
  public void statementStartingAtNodeInsideConditionalExpression() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "call-in-conditional-expression.c");
    CFANode call =
        Iterables.getOnlyElement(CFAUtils.allEdges(cfa).filter(FunctionCallEdge.class))
            .getPredecessor();
    CFANode statementStart =
        TestCfaUtils.getEdge("b = __VERIFIER_nondet_int();", cfa).getSuccessor();

    assertThat(cfa.getAstCfaRelation().getStatementStartingAtNode(call)).isEmpty();
    assertThat(cfa.getAstCfaRelation().getStatementStartingAtNode(statementStart)).isPresent();
  }
}
