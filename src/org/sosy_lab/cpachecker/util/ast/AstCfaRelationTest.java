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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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

  /** The edges of the body of {@code step} include the return edge from {@code aux}. */
  @Test
  public void nodeForStatementLocationOfBodyWithCall() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "parameter-at-start-of-body.c");
    CFANode bodyStart = cfa.getAstCfaRelation().getNodeForStatementLocation(20, 1).orElseThrow();

    assertThat(bodyStart.getFunctionName()).isEqualTo("step");
  }

  /** A condition edge of two if statements belongs to the inner one, whose condition it is. */
  @Test
  public void ifStructureForConditionEdgeOfNestedIfStatements() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "if-in-statement-expression-in-condition.c");
    CFAEdge innerCondition = TestCfaUtils.getEdge("[x > 5]", cfa);

    IfElement ifElement =
        cfa.getAstCfaRelation().getIfStructureForConditionEdge(innerCondition).orElseThrow();
    assertThat(ifElement.getCompleteElement().location().getNodeLength())
        .isEqualTo("if (x > 5) t = 1;".length());
  }

  /** The call in the second operand of {@code &&} is inside the condition. */
  @Test
  public void nodesBetweenConditionWithCallsAndBranches() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "branching-at-condition-with-calls.c");
    CFAEdge abort = TestCfaUtils.getEdge("{abort();}", cfa);
    CFAEdge secondOperandTrue = TestCfaUtils.getEdge("[!(__CPAchecker_TMP_1 == 0)]", cfa);

    IfElement ifElement =
        cfa.getAstCfaRelation().getIfStructureForConditionEdge(secondOperandTrue).orElseThrow();
    assertThat(ifElement.getNodesBetweenConditionAndThenBranch())
        .containsExactly(abort.getPredecessor());
    assertThat(ifElement.getNodesBetweenConditionAndElseBranch())
        .containsExactly(secondOperandTrue.getSuccessor());
  }

  /** The assume edges of the condition tell which node enters the empty body of the loop. */
  @Test
  public void nodesBetweenConditionAndEmptyLoopBody() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "empty-loop-body-and-constant-operand.c");
    CFAEdge enterBody = TestCfaUtils.getEdge("[i < n]", cfa);
    CFAEdge exit = TestCfaUtils.getEdge("[!(i < n)]", cfa);

    IterationElement loop =
        cfa.getAstCfaRelation()
            .getTightestIterationStructureForNode(enterBody.getPredecessor())
            .orElseThrow();
    assertThat(loop.getNodesBetweenConditionAndBody()).containsExactly(enterBody.getSuccessor());
    assertThat(loop.getNodesBetweenConditionAndExit()).containsExactly(exit.getSuccessor());
  }

  /** The constant operand {@code 0} of {@code ||} leads to the else branch. */
  @Test
  public void nodesBetweenConditionWithConstantOperandAndBranches() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "empty-loop-body-and-constant-operand.c");
    CFAEdge lastOperandTrue = TestCfaUtils.getEdge("[i == 2]", cfa);
    CFAEdge afterIf = TestCfaUtils.getEdge("return 0;", cfa);

    IfElement ifElement =
        cfa.getAstCfaRelation().getIfStructureForConditionEdge(lastOperandTrue).orElseThrow();
    assertThat(ifElement.getNodesBetweenConditionAndThenBranch())
        .containsExactly(lastOperandTrue.getSuccessor());
    assertThat(ifElement.getNodesBetweenConditionAndElseBranch())
        .containsExactly(afterIf.getPredecessor());
  }

  /** The else branch starts after the negated condition also if it is an empty block. */
  @Test
  public void nodesBeforeEmptyElseBranch() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "if-with-empty-else.c");
    CFAEdge negatedCondition = TestCfaUtils.getEdge("[!(x > 5)]", cfa);

    IfElement ifElement =
        cfa.getAstCfaRelation().getIfStructureForConditionEdge(negatedCondition).orElseThrow();
    assertThat(ifElement.getNodesBetweenConditionAndElseBranch())
        .containsExactly(negatedCondition.getSuccessor());
  }
}
