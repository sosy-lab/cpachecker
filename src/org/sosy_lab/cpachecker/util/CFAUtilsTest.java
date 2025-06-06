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

  private void fullExpressionAtCorrectPosition(
      CFA pCFA,
      String pStringsToIdentifyEdge,
      int pExpectedStartingLine,
      int pExpectedStartColumnInLine,
      int pExpectedEndingLine,
      int pExpectedEndColumnInLine) {
    AstCfaRelation astCfaRelation = pCFA.getAstCfaRelation();
    CFAEdge edge = TestDataTools.getEdge(pStringsToIdentifyEdge, pCFA);
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
    fullExpressionAtCorrectPosition(cfa, "t = 2", 39, 12, 39, 17);
    fullExpressionAtCorrectPosition(cfa, "{a(1, 1)}", 39, 3, 39, 10);
    fullExpressionAtCorrectPosition(cfa, "{g(2, 2)}", 42, 3, 42, 48);
    fullExpressionAtCorrectPosition(cfa, "{g(1, 1)}", 43, 3, 43, 55);
    fullExpressionAtCorrectPosition(cfa, "arr[0] = 1", 44, 3, 44, 48);
    fullExpressionAtCorrectPosition(cfa, "arr[0] = 0", 45, 3, 45, 45);
    fullExpressionAtCorrectPosition(cfa, "{d(1, 2)}", 47, 7, 47, 25);
    fullExpressionAtCorrectPosition(cfa, "{g(3, 3)}", 47, 7, 47, 25);
    fullExpressionAtCorrectPosition(cfa, "{g(3, 0)}", 48, 7, 48, 19);
    fullExpressionAtCorrectPosition(cfa, "t = 4", 49, 5, 49, 20);
    fullExpressionAtCorrectPosition(cfa, "{g(2, 3)}", 49, 5, 49, 20);
    fullExpressionAtCorrectPosition(cfa, "{d(3, 2)}", 50, 14, 50, 21);
    fullExpressionAtCorrectPosition(cfa, "{g(0, 1)}", 52, 7, 52, 19);
    fullExpressionAtCorrectPosition(cfa, "{a(3, 2)}", 56, 11, 56, 40);
    fullExpressionAtCorrectPosition(cfa, "{a(2, 3)}", 56, 11, 56, 40);
    fullExpressionAtCorrectPosition(cfa, "{a(4, 1)}", 56, 11, 56, 40);
    fullExpressionAtCorrectPosition(cfa, "{a(1, 4)}", 58, 3, 58, 19);
    fullExpressionAtCorrectPosition(cfa, "{a(5, 6)}", 60, 11, 63, 4);
    fullExpressionAtCorrectPosition(cfa, "arr[2] = 0", 65, 3, 67, 4);
    fullExpressionAtCorrectPosition(cfa, "rec: p = rec(0);}", 71, 12, 71, 18);
    fullExpressionAtCorrectPosition(cfa, "rec: p = rec(2);}", 72, 4, 72, 14);
    fullExpressionAtCorrectPosition(cfa, "rec: m = rec(1);}", 73, 12, 73, 45);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_0 = a(5, 4);}", 74, 12, 74, 24);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_1 = rec(3);}", 75, 12, 75, 23);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_2 = rec(4);}", 76, 4, 76, 19);
    fullExpressionAtCorrectPosition(cfa, "rec: __CPAchecker_TMP_3 = rec(2);}", 77, 12, 81, 5);
    fullExpressionAtCorrectPosition(cfa, "{rec(x - 2)}", 82, 11, 82, 21);
    fullExpressionAtCorrectPosition(cfa, "[v > w]", 85, 27, 85, 32);
    fullExpressionAtCorrectPosition(cfa, "v - w", 85, 43, 85, 48);
    fullExpressionAtCorrectPosition(cfa, "v - 1", 85, 66, 85, 71);
    fullExpressionAtCorrectPosition(cfa, "a = 0", 88, 11, 88, 12);
    fullExpressionAtCorrectPosition(cfa, "b = 1", 88, 22, 88, 23);
    fullExpressionAtCorrectPosition(cfa, "c = 3", 88, 22, 88, 23);
    fullExpressionAtCorrectPosition(cfa, "a = 3", 88, 32, 88, 37);
    fullExpressionAtCorrectPosition(cfa, "b = 0", 88, 39, 88, 44);
    fullExpressionAtCorrectPosition(cfa, "[a > 0]", 89, 7, 89, 36);
    fullExpressionAtCorrectPosition(cfa, "[b == 0]", 89, 7, 89, 36);
    fullExpressionAtCorrectPosition(cfa, "[c == 3]", 89, 7, 89, 36);
    fullExpressionAtCorrectPosition(cfa, "c = 2", 89, 39, 89, 44);
    fullExpressionAtCorrectPosition(cfa, "d = a + b", 89, 54, 89, 59);
    fullExpressionAtCorrectPosition(cfa, "c = 1", 89, 68, 89, 73);
    fullExpressionAtCorrectPosition(cfa, "[b < 2]", 90, 25, 90, 39);
    fullExpressionAtCorrectPosition(cfa, "[c > 0]", 90, 25, 90, 39);
    fullExpressionAtCorrectPosition(cfa, "c = 4", 91, 3, 91, 8);
    fullExpressionAtCorrectPosition(cfa, "e = 2", 95, 11, 95, 12);
    fullExpressionAtCorrectPosition(cfa, "f = 1", 96, 46, 96, 47);
    fullExpressionAtCorrectPosition(cfa, "g = 0", 97, 51, 97, 52);
    fullExpressionAtCorrectPosition(cfa, "[e > f]", 98, 7, 98, 84);
    fullExpressionAtCorrectPosition(cfa, "[g == 0]", 98, 7, 98, 84);
    fullExpressionAtCorrectPosition(cfa, "e = 3;", 99, 5, 99, 62);
    fullExpressionAtCorrectPosition(cfa, "p = 0", 101, 18, 101, 19);
    fullExpressionAtCorrectPosition(cfa, "[p < 2]", 101, 44, 101, 71);
    fullExpressionAtCorrectPosition(cfa, "f + g", 105, 34, 105, 71);
    fullExpressionAtCorrectPosition(cfa, "{a(h, k)}", 108, 30, 108, 43);
    fullExpressionAtCorrectPosition(cfa, "f + 1", 111, 15, 111, 20);
    fullExpressionAtCorrectPosition(cfa, "g = 1", 112, 7, 112, 24);
    fullExpressionAtCorrectPosition(cfa, "g < 3", 112, 19, 112, 24);
    fullExpressionAtCorrectPosition(cfa, "l = 4", 113, 5, 113, 17);
    fullExpressionAtCorrectPosition(cfa, "l = 2", 113, 5, 113, 17);
    fullExpressionAtCorrectPosition(cfa, "i = 4", 115, 18, 115, 19);
    fullExpressionAtCorrectPosition(cfa, "j = 5", 115, 18, 115, 19);
    fullExpressionAtCorrectPosition(cfa, "i > 1", 115, 29, 115, 41);
    fullExpressionAtCorrectPosition(cfa, "j > 1", 115, 36, 115, 41);
    fullExpressionAtCorrectPosition(cfa, "i + 3", 116, 13, 116, 25);
    fullExpressionAtCorrectPosition(cfa, "j + 1", 116, 20, 116, 25);
    fullExpressionAtCorrectPosition(cfa, "f >= 0", 119, 11, 119, 25);
    fullExpressionAtCorrectPosition(cfa, "g >= 0", 119, 19, 119, 25);
    fullExpressionAtCorrectPosition(cfa, "f < 5", 119, 31, 119, 43);
    fullExpressionAtCorrectPosition(cfa, "g < 5", 119, 38, 119, 43);
    fullExpressionAtCorrectPosition(cfa, "p = 10", 126, 3, 126, 5);
    fullExpressionAtCorrectPosition(cfa, "q + 1", 130, 3, 130, 4);
    fullExpressionAtCorrectPosition(cfa, "[q < p]", 131, 3, 133, 4);
    fullExpressionAtCorrectPosition(cfa, "[p > 0]", 144, 5, 146, 6);
    fullExpressionAtCorrectPosition(cfa, "p = 5", 149, 9, 151, 10);
    fullExpressionAtCorrectPosition(cfa, "p - q", 137, 5, 141, 6);
    fullExpressionAtCorrectPosition(cfa, "p + 3", 154, 3, 156, 4);
  }

  @Test
  @SuppressWarnings("DistinctVarargsChecker")
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
