// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class CFAUtilsTest {

  private final Level logLevel = Level.FINEST;

  private TestResults parseProgram(String pProgramName) throws Exception {
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption("--config", "config/generateCFA.properties")
            .build();

    String test_dir = "test/programs/cfa-ast-relation/";
    Path program = Path.of(test_dir, pProgramName);

    return CPATestRunner.run(config, program.toString(), logLevel);
  }

  /**
   * Small helper function to get the edge from the CFA that contains the given strings.
   *
   * <p>DO NOT USE THIS IF YOU DO NOT KNOW EXACTLY WHAT CFA YOU ARE WORKING WITH!!!!!!!!!
   */
  private static CFAEdge getEdge(Set<String> pStringsInEdge, CFA pCFA) {
    for (CFAEdge edge : CFAUtils.allEdges(pCFA)) {
      if (FluentIterable.from(pStringsInEdge)
          .allMatch(string -> edge.toString().contains(string))) {
        return edge;
      }
    }
    assert_().fail();
    return null;
  }

  private void testFullExpression(
      String pProgramName,
      Set<String> pStringsToIdentifyEdge,
      int pExpectedStartingLineInOrigin,
      int pExpectedStartColumnInLine)
      throws Exception {
    TestResults results = parseProgram(pProgramName);
    CFA cfa = results.getCheckerResult().getCfa();
    assertThat(cfa).isNotNull();

    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    CFAEdge edge = getEdge(pStringsToIdentifyEdge, cfa);
    Optional<FileLocation> optionalExpressionLocation =
        CFAUtils.getClosestFullExpression(edge, astCfaRelation);
    assertThat(optionalExpressionLocation).isPresent();
    FileLocation expressionLocation = optionalExpressionLocation.orElseThrow();
    assertThat(expressionLocation.getStartingLineInOrigin())
        .isEqualTo(pExpectedStartingLineInOrigin);
    assertThat(expressionLocation.getStartColumnInLine()).isEqualTo(pExpectedStartColumnInLine);
  }

  /**
   * Test that the results from the function {@link CFAUtils#getClosestFullExpression} match those
   * of a full expression as defined in §6.8 (4) of the C11 standard.
   *
   * @throws Exception in case the parsing of the program fails
   */
  @Test
  public void testFullExpressionStartPosition() throws Exception {
    testFullExpression("full-expression.c", ImmutableSet.of("x + y"), 10, 10);
    testFullExpression("full-expression.c", ImmutableSet.of("x = 1"), 2, 11);
    testFullExpression("full-expression.c", ImmutableSet.of("y = 1"), 3, 11);
    testFullExpression("full-expression.c", ImmutableSet.of("x = 2"), 4, 3);
    testFullExpression("full-expression.c", ImmutableSet.of("y = 2"), 5, 3);
    testFullExpression("full-expression.c", ImmutableSet.of("x != 0"), 6, 10);
    testFullExpression("full-expression.c", ImmutableSet.of("y != 0"), 6, 10);
    testFullExpression("full-expression.c", ImmutableSet.of("z + w"), 13, 30);
    testFullExpression("full-expression.c", ImmutableSet.of("f()"), 16, 3);
    testFullExpression("full-expression.c", ImmutableSet.of("g(1, 2)"), 17, 3);
    testFullExpression("full-expression.c", ImmutableSet.of("j < 0"), 21, 8);
    testFullExpression("full-expression.c", ImmutableSet.of("i == 0"), 21, 8);
    testFullExpression("full-expression.c", ImmutableSet.of("i < 10"), 21, 25);
    testFullExpression("full-expression.c", ImmutableSet.of("j == 0"), 21, 25);
    testFullExpression("full-expression.c", ImmutableSet.of("i < 5"), 21, 43);
    testFullExpression("full-expression.c", ImmutableSet.of("i != 0"), 21, 43);
    testFullExpression("full-expression.c", ImmutableSet.of("s != q"), 27, 11);
    testFullExpression("full-expression.c", ImmutableSet.of("s == 1"), 27, 11);
    testFullExpression("full-expression.c", ImmutableSet.of("q == 2"), 27, 11);
    testFullExpression("full-expression.c", ImmutableSet.of("l = 0"), 28, 11);
  }
}
