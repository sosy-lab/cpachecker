// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * A copy of a CFA (e.g., with analysis.algorithm.copyCFA) consists of new nodes and edges, so it
 * needs its own {@link AstCfaRelation}, which resolves them to the program through {@link
 * org.sosy_lab.cpachecker.cfa.CfaCopyOrigins}. Otherwise, e.g., no waypoints can be exported into a
 * witness for a counterexample on the copy.
 */
public class AstCfaRelationCfaCopyTest {

  private static final String PROGRAM =
      """
      extern int __VERIFIER_nondet_int(void);
      int main() {
        int x = __VERIFIER_nondet_int();
        while (x > 0) {
          if (x == 5) {
            x = 0;
          }
          x--;
        }
        return 0;
      }
      """;

  private CFA original;
  private CFA copy;

  @Before
  public void setUp() throws Exception {
    original = TestCfaUtils.makeCfaFromString(PROGRAM);
    copy =
        ImmutableCFA.copyOf(
            original, TestUtils.configurationForTest().build(), LogManager.createTestLogManager());
  }

  @Test
  public void copyHasOwnRelation() {
    assertThat(copy.getAstCfaRelation()).isNotNull();
    assertThat(copy.getAstCfaRelation()).isNotSameInstanceAs(original.getAstCfaRelation());
  }

  @Test
  public void ifStructureFoundForConditionEdgeOfCopy() {
    CFAEdge condition = TestCfaUtils.getEdge("[x == 5]", copy);

    IfElement ifElement =
        copy.getAstCfaRelation().getIfStructureForConditionEdge(condition).orElseThrow();
    assertThat(ifElement.getConditionElement().edges()).contains(condition);
    assertThat(ifElement.getThenEdges()).isNotEmpty();
    assertThat(CFAUtils.allEdges(copy).toSet()).containsAtLeastElementsIn(ifElement.getThenEdges());
  }

  @Test
  public void iterationStructureFoundForLoopHeadOfCopy() {
    CFANode loopHead = TestCfaUtils.getEdge("[x > 0]", copy).getPredecessor();

    IterationElement loop =
        copy.getAstCfaRelation().getTightestIterationStructureForNode(loopHead).orElseThrow();
    ImmutableSet<CFAEdge> bodyEdges = loop.getBody().edges();
    assertThat(bodyEdges).isNotEmpty();
    assertThat(CFAUtils.allEdges(copy).toSet()).containsAtLeastElementsIn(bodyEdges);
  }

  @Test
  public void variablesInScopeKnownForNodesOfCopy() {
    CFANode loopHead = TestCfaUtils.getEdge("[x > 0]", copy).getPredecessor();

    assertThat(copy.getAstCfaRelation().getAstLocalVariablesInScopeByCfaNode(loopHead))
        .isNotEmpty();
  }

  @Test
  public void nodesAndEdgesOfCopyStandForTheProgram() {
    CFAEdge conditionInCopy = TestCfaUtils.getEdge("[x == 5]", copy);
    CFAEdge conditionInProgram = TestCfaUtils.getEdge("[x == 5]", original);
    AstCfaRelation relation = copy.getAstCfaRelation();

    assertThat(relation.programEdgeOf(conditionInCopy)).isSameInstanceAs(conditionInProgram);
    assertThat(relation.programNodeOf(conditionInCopy.getPredecessor()))
        .isSameInstanceAs(conditionInProgram.getPredecessor());
  }

  @Test
  public void copyOfCopyStandsForTheProgram() throws Exception {
    CFA copyOfCopy =
        ImmutableCFA.copyOf(
            copy, TestUtils.configurationForTest().build(), LogManager.createTestLogManager());
    CFAEdge condition = TestCfaUtils.getEdge("[x == 5]", copyOfCopy);
    AstCfaRelation relation = copyOfCopy.getAstCfaRelation();

    assertThat(relation.programEdgeOf(condition))
        .isSameInstanceAs(TestCfaUtils.getEdge("[x == 5]", original));
    assertThat(relation.getIfStructureForConditionEdge(condition)).isPresent();
    assertThat(relation.getAstLocalVariablesInScopeByCfaNode(condition.getPredecessor()))
        .isNotEmpty();
  }

  @Test
  public void originalRelationStillRefersToOriginalCfa() {
    CFAEdge condition = TestCfaUtils.getEdge("[x == 5]", original);

    IfElement ifElement =
        original.getAstCfaRelation().getIfStructureForConditionEdge(condition).orElseThrow();
    assertThat(CFAUtils.allEdges(original).toSet())
        .containsAtLeastElementsIn(ifElement.getThenEdges());
  }
}
