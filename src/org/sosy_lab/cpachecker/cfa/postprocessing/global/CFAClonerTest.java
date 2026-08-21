// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.OptionalInt;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaCloneRelation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests the relation between cloned functions and their originals that {@link CFACloner} creates.
 */
public class CFAClonerTest {

  private static final String PROGRAM =
      """
      typedef unsigned long pthread_t;
      extern int pthread_create(
          pthread_t *pThread, void *pAttr, void *(*pStartRoutine)(void *), void *pArg);
      extern int pthread_join(pthread_t pThread, void **pValue);

      int x = 0;

      void helper(void) {}

      void *writer(void *pArg) {
        helper();
        x = 1;
        return 0;
      }

      int main(void) {
        pthread_t t;
        pthread_create(&t, 0, writer, 0);
        if (x == 1) {
          return 1;
        }
        pthread_join(t, 0);
        return 0;
      }
      """;

  private static final String CLONED_WRITER = CFACloner.getFunctionName("writer", 1);

  private CFA cfa;
  private CfaCloneRelation cloneRelation;

  @Before
  public void createClonedCfa() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .setOption("cfa.useCFACloningForMultiThreadedPrograms", "true")
            .setOption("cfa.cfaCloner.numberOfCopies", "2")
            .build();
    cfa = TestCfaUtils.makeCFA(config, PROGRAM);
    cloneRelation = cfa.getCloneRelation();
  }

  /** Returns the line at which the given code snippet occurs in {@link #PROGRAM}. */
  private static int lineOf(String pSnippet) {
    ImmutableList<String> lines = ImmutableList.copyOf(PROGRAM.split("\n", -1));
    for (int line = 1; line <= lines.size(); line++) {
      if (lines.get(line - 1).contains(pSnippet)) {
        return line;
      }
    }
    throw new AssertionError("snippet not found in program: " + pSnippet);
  }

  /** Returns the edges of the given function, i.e., all edges leaving one of its nodes. */
  private FluentIterable<CFAEdge> edgesOfFunction(String pFunctionName) {
    return CFAUtils.allEdges(cfa)
        .filter(edge -> edge.getPredecessor().getFunctionName().equals(pFunctionName));
  }

  /** Returns the only edge of the given AST element that stems from the source code. */
  private CFAEdge onlyStatementEdgeOf(ASTElement pElement) {
    return FluentIterable.from(pElement.edges())
        .filter(edge -> !edge.getRawStatement().isEmpty())
        .stream()
        .reduce(
            (first, second) -> {
              throw new AssertionError("more than one statement edge: " + first + ", " + second);
            })
        .orElseThrow();
  }

  private ASTElement statementElementAt(String pSnippet) {
    return cfa.getAstCfaRelation()
        .getTightestStatementForStarting(lineOf(pSnippet), OptionalInt.empty())
        .orElseThrow();
  }

  @Test
  public void testFunctionsAreCloned() {
    assertThat(cfa.getAllFunctionNames()).contains(CLONED_WRITER);
    assertThat(cloneRelation.isEmpty()).isFalse();
  }

  @Test
  public void testNodesOfClonedFunctionAreTracked() {
    ImmutableList<CFANode> clonedNodes =
        cfa.nodes().stream()
            .filter(node -> node.getFunctionName().equals(CLONED_WRITER))
            .collect(ImmutableList.toImmutableList());
    assertThat(clonedNodes).isNotEmpty();

    for (CFANode node : clonedNodes) {
      CFANode original = cloneRelation.getOriginalNode(node);
      assertThat(original).isNotSameInstanceAs(node);
      assertThat(original.getFunctionName()).isEqualTo("writer");
    }
  }

  @Test
  public void testEdgesOfClonedFunctionAreTracked() {
    ImmutableList<CFAEdge> clonedEdges =
        edgesOfFunction(CLONED_WRITER)
            // the edges of the supergraph are created after the cloning
            .filter(
                edge ->
                    !(edge instanceof FunctionCallEdge
                        || edge instanceof FunctionReturnEdge
                        || edge instanceof FunctionSummaryEdge))
            .toList();
    assertThat(clonedEdges).isNotEmpty();

    for (CFAEdge edge : clonedEdges) {
      CFAEdge original = cloneRelation.getOriginalEdge(edge);
      assertThat(original).isNotSameInstanceAs(edge);
      assertThat(original.getPredecessor().getFunctionName()).isEqualTo("writer");
      assertThat(original.getRawStatement()).isEqualTo(edge.getRawStatement());
      assertThat(original.getFileLocation()).isEqualTo(edge.getFileLocation());
    }
  }

  @Test
  public void testEdgesOfOriginalFunctionAreNoClones() {
    for (CFAEdge edge : edgesOfFunction("writer")) {
      assertThat(cloneRelation.getOriginalEdge(edge)).isSameInstanceAs(edge);
      assertThat(cloneRelation.getOriginalNode(edge.getPredecessor()))
          .isSameInstanceAs(edge.getPredecessor());
    }
  }

  @Test
  public void testClonedEdgeOriginatesFromEdgeOfAstElement() {
    CFAEdge originalEdge = onlyStatementEdgeOf(statementElementAt("x = 1;"));
    ImmutableList<CFAEdge> clonedEdges =
        edgesOfFunction(CLONED_WRITER)
            .filter(edge -> edge.getRawStatement().equals(originalEdge.getRawStatement()))
            .toList();
    assertThat(clonedEdges).hasSize(1);

    assertThat(CFAUtils.originatesFrom(clonedEdges.get(0), originalEdge, cloneRelation)).isTrue();
    assertThat(CFAUtils.originatesFrom(originalEdge, originalEdge, cloneRelation)).isTrue();
  }

  /**
   * The statement edge of a function call is replaced by a function call edge when the supergraph
   * is built, so it can only be matched via the nodes of the clone relation.
   */
  @Test
  public void testClonedFunctionCallEdgeOriginatesFromEdgeOfAstElement() {
    CFAEdge originalEdge = onlyStatementEdgeOf(statementElementAt("helper();"));
    ImmutableList<FunctionCallEdge> clonedCallEdges =
        edgesOfFunction(CLONED_WRITER).filter(FunctionCallEdge.class).toList();
    assertThat(clonedCallEdges).hasSize(1);

    assertThat(CFAUtils.originatesFrom(clonedCallEdges.get(0), originalEdge, cloneRelation))
        .isTrue();
  }

  @Test
  public void testClonedEdgeDoesNotOriginateFromUnrelatedEdge() {
    CFAEdge unrelatedEdge = onlyStatementEdgeOf(statementElementAt("helper();"));
    ImmutableList<CFAEdge> clonedEdges =
        edgesOfFunction(CLONED_WRITER)
            .filter(edge -> edge.getRawStatement().equals("x = 1;"))
            .toList();
    assertThat(clonedEdges).hasSize(1);

    assertThat(CFAUtils.originatesFrom(clonedEdges.get(0), unrelatedEdge, cloneRelation)).isFalse();
  }
}
