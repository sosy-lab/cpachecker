// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class CFAUtilsTest {

  private TestResults parseProgram(String pProgramName) throws Exception {
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption("--config", "config/generateCFA.properties")
            .build();

    String test_dir = "test/programs/cfa-ast-relation/";
    Path program = Path.of(test_dir, pProgramName);

    return CPATestRunner.run(config, program.toString(), Level.FINEST);
  }

  /** Get the edge from the CFA that contains the given string. */
  private static CFAEdge getEdge(String pStringsInEdge, CFA pCFA) {
    return Iterables.getOnlyElement(
        CFAUtils.allEdges(pCFA).filter(edge -> edge.toString().contains(pStringsInEdge)));
  }

  private void fullExpressionAtCorrectPosition(
      CFA pCFA,
      String pStringsToIdentifyEdge,
      int pExpectedStartingLine,
      int pExpectedStartColumnInLine,
      int pExpectedEndingLine,
      int pExpectedEndColumnInLine) {
    AstCfaRelation astCfaRelation = pCFA.getAstCfaRelation();
    CFAEdge edge = getEdge(pStringsToIdentifyEdge, pCFA);
    assertThat(edge instanceof CCfaEdge).isTrue();

    Optional<FileLocation> optionalExpressionLocation =
        CFAUtils.getClosestFullExpression((CCfaEdge) edge, astCfaRelation);
    assertThat(optionalExpressionLocation).isPresent();

    FileLocation expressionLocation = optionalExpressionLocation.orElseThrow();
    assertThat(expressionLocation.getStartingLineNumber()).isEqualTo(pExpectedStartingLine);
    assertThat(expressionLocation.getStartColumnInLine()).isEqualTo(pExpectedStartColumnInLine);
    assertThat(expressionLocation.getEndingLineNumber()).isEqualTo(pExpectedEndingLine);
    assertThat(expressionLocation.getEndColumnInLine()).isEqualTo(pExpectedEndColumnInLine);
  }

  /**
   * Test that the results from the function {@link CFAUtils#getClosestFullExpression} match those
   * of a full expression as defined in §6.8 (4) of the C11 standard.
   *
   * <p>Be aware that these tests are not exhaustive, some possible improvements are documented as
   * <a href="https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1260">issue 1260</a>
   *
   * @throws Exception in case the parsing of the program fails
   */
  @Test
  public void testFullExpression() throws Exception {
    String programName = "full-expression.c";
    TestResults results = parseProgram(programName);
    CFA cfa = results.getCheckerResult().getCfa();
    assertThat(cfa).isNotNull();

    fullExpressionAtCorrectPosition(cfa, "x + y", 18, 10, 18, 15);
    fullExpressionAtCorrectPosition(cfa, "x = 1", 10, 11, 10, 12);
    fullExpressionAtCorrectPosition(cfa, "y = 1", 11, 11, 11, 12);
    fullExpressionAtCorrectPosition(cfa, "x = 2", 12, 3, 12, 8);
    fullExpressionAtCorrectPosition(cfa, "y = 2", 13, 3, 13, 8);
    fullExpressionAtCorrectPosition(cfa, "[x != 0]", 14, 10, 14, 26);
    fullExpressionAtCorrectPosition(cfa, "[y != 0]", 14, 10, 14, 26);
    fullExpressionAtCorrectPosition(cfa, "z + w", 21, 30, 21, 35);
    fullExpressionAtCorrectPosition(cfa, "{f()}", 24, 3, 24, 6);
    fullExpressionAtCorrectPosition(cfa, "{g(1, 2)}", 25, 3, 25, 10);
    fullExpressionAtCorrectPosition(cfa, "[j < 0]", 29, 8, 29, 23);
    fullExpressionAtCorrectPosition(cfa, "[i == 0]", 29, 8, 29, 23);
    fullExpressionAtCorrectPosition(cfa, "[i < 10]", 29, 25, 29, 41);
    fullExpressionAtCorrectPosition(cfa, "[j == 0]", 29, 25, 29, 41);
    fullExpressionAtCorrectPosition(cfa, "[i < 5]", 29, 43, 29, 58);
    fullExpressionAtCorrectPosition(cfa, "[i != 0]", 29, 43, 29, 58);
    fullExpressionAtCorrectPosition(cfa, "[s != q]", 35, 11, 35, 35);
    fullExpressionAtCorrectPosition(cfa, "s == 1", 35, 11, 35, 35);
    fullExpressionAtCorrectPosition(cfa, "q == 2", 35, 11, 35, 35);
    fullExpressionAtCorrectPosition(cfa, "l = 0", 36, 11, 36, 14);
    fullExpressionAtCorrectPosition(cfa, " rec: p = rec(0);}", 40, 12, 40, 18);
    fullExpressionAtCorrectPosition(cfa, "rec: p = rec(2);}", 41, 4, 41, 14);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_0 = rec(3);}", 42, 12, 42, 23);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_1 = rec(4);}", 43, 4, 43, 19);
    fullExpressionAtCorrectPosition(cfa, "{rec(x - 2)}", 44, 11, 44, 21);
  }

  @Test
  public void testDisconnectedBase() {
    Set<CFANode> nodes = ImmutableSet.of(CFANode.newDummyCFANode(), CFANode.newDummyCFANode());
    assertThat(CFAUtils.isConnected(nodes)).isFalse();
  }

  @Test
  public void testDisconnectedComponentsSimple() {
    CFAEdge edge =
        new BlankEdge(
            "dummy",
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode(),
            "dummy");
    CFACreationUtils.addEdgeUnconditionallyToCFA(edge);

    Set<CFANode> nodes =
        FluentIterable.from(CFAUtils.nodes(edge)).append(CFANode.newDummyCFANode()).toSet();
    assertThat(CFAUtils.isConnected(nodes)).isFalse();
  }

  @Test
  public void testDisconnectedComponents() {
    CFANode node1 = CFANode.newDummyCFANode();
    CFANode node2 = CFANode.newDummyCFANode();
    CFANode node3 = CFANode.newDummyCFANode();
    CFANode node4 = CFANode.newDummyCFANode();

    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();

    edges
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node2, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node3, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node3, CFANode.newDummyCFANode(), "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node4, CFANode.newDummyCFANode(), "dummy"));

    for (CFAEdge edge : edges.build()) {
      CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    }

    Set<CFANode> nodes =
        FluentIterable.from(edges.build()).transformAndConcat(CFAUtils::nodes).toSet();
    assertThat(CFAUtils.isConnected(nodes)).isFalse();
  }

  @Test
  public void testConnectedComponentsBase() {
    CFAEdge edge =
        new BlankEdge(
            "dummy",
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode(),
            "dummy");
    CFACreationUtils.addEdgeUnconditionallyToCFA(edge);

    Set<CFANode> nodes = ImmutableSet.copyOf(CFAUtils.nodes(edge));
    assertThat(CFAUtils.isConnected(nodes)).isTrue();
  }

  @Test
  public void testConnectedComponentsSelfLoop() {
    CFANode node = CFANode.newDummyCFANode();
    CFAEdge edge = new BlankEdge("dummy", FileLocation.DUMMY, node, node, "dummy");
    CFACreationUtils.addEdgeUnconditionallyToCFA(edge);

    Set<CFANode> nodes = ImmutableSet.copyOf(CFAUtils.nodes(edge));
    assertThat(CFAUtils.isConnected(nodes)).isTrue();
  }

  @Test
  public void testConnectedComponentsBranching() {
    CFANode node1 = CFANode.newDummyCFANode();
    CFANode node2 = CFANode.newDummyCFANode();
    CFANode node3 = CFANode.newDummyCFANode();
    CFANode node4 = CFANode.newDummyCFANode();

    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();

    edges
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node2, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node3, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node3, node4, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node3, CFANode.newDummyCFANode(), "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node4, CFANode.newDummyCFANode(), "dummy"));

    for (CFAEdge edge : edges.build()) {
      CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    }

    Set<CFANode> nodes =
        FluentIterable.from(edges.build()).transformAndConcat(CFAUtils::nodes).toSet();
    assertThat(CFAUtils.isConnected(nodes)).isTrue();
  }

  @Test
  public void testConnectedComponentsMultiReachability() {
    CFANode node1 = CFANode.newDummyCFANode();
    CFANode node2 = CFANode.newDummyCFANode();
    CFANode node3 = CFANode.newDummyCFANode();
    CFANode node4 = CFANode.newDummyCFANode();

    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();

    edges
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node2, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node2, node3, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node2, node4, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node1, node3, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node3, node4, "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node3, CFANode.newDummyCFANode(), "dummy"))
        .add(new BlankEdge("dummy", FileLocation.DUMMY, node4, CFANode.newDummyCFANode(), "dummy"));

    for (CFAEdge edge : edges.build()) {
      CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    }

    Set<CFANode> nodes =
        FluentIterable.from(edges.build()).transformAndConcat(CFAUtils::nodes).toSet();
    assertThat(CFAUtils.isConnected(nodes)).isTrue();
  }
}
