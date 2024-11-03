// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.Optional;
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
  private static CFAEdge getEdge(String pStringsInEdge, CFA pCFA) {
    return Iterables.getOnlyElement(
        CFAUtils.allEdges(pCFA).filter(edge -> edge.toString().contains(pStringsInEdge)));
  }

  private void testFullExpression(
      CFA pCFA,
      String pStringsToIdentifyEdge,
      int pExpectedStartingLine,
      int pExpectedStartColumnInLine,
      int pExpectedEndingLine,
      int pExpectedEndColumnInLine) {
    AstCfaRelation astCfaRelation = pCFA.getAstCfaRelation();
    CFAEdge edge = getEdge(pStringsToIdentifyEdge, pCFA);
    Optional<FileLocation> optionalExpressionLocation =
        CFAUtils.getClosestFullExpression(edge, astCfaRelation);
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
   * @throws Exception in case the parsing of the program fails
   */
  @Test
  public void testFullExpressionStartPosition() throws Exception {
    String programName = "full-expression.c";
    TestResults results = parseProgram(programName);
    CFA cfa = results.getCheckerResult().getCfa();
    assertThat(cfa).isNotNull();

    // The expected end position is sometimes wrong. This is on purpose in order to catch
    // regressions if something is done on our frontend. The comment after the expected value
    // denotes the correct expected value
    testFullExpression(cfa, "x + y", 18, 10, 18, 11 /* 15 */);
    testFullExpression(cfa, "x = 1", 10, 11, 10, 12);
    testFullExpression(cfa, "y = 1", 11, 11, 11, 12);
    testFullExpression(cfa, "x = 2", 12, 3, 12, 9 /* 8*/);
    testFullExpression(cfa, "y = 2", 13, 3, 13, 9 /* 8*/);
    testFullExpression(cfa, "[x != 0]", 14, 10, 14, 26);
    testFullExpression(cfa, "[y != 0]", 14, 10, 14, 26);
    testFullExpression(cfa, "z + w", 21, 30, 21, 31 /* 35 */);
    testFullExpression(cfa, "{f()}", 24, 3, 24, 6);
    testFullExpression(cfa, "{g(1, 2)}", 25, 3, 25, 10);
    testFullExpression(cfa, "[j < 0]", 29, 8, 29, 24 /* 23 */);
    testFullExpression(cfa, "[i == 0]", 29, 8, 29, 24 /* 23 */);
    testFullExpression(cfa, "[i < 10]", 29, 25, 29, 41);
    testFullExpression(cfa, "[j == 0]", 29, 25, 29, 41);
    testFullExpression(cfa, "[i < 5]", 29, 43, 29, 58);
    testFullExpression(cfa, "[i != 0]", 29, 43, 29, 58);
    testFullExpression(cfa, "[s != q]", 35, 11, 35, 17 /* 35 */);
    testFullExpression(cfa, "s == 1", 35, 11, 35, 35);
    testFullExpression(cfa, "q == 2", 35, 11, 35, 35);
    testFullExpression(cfa, "l = 0", 36, 11, 36, 14);
    testFullExpression(cfa, " rec: p = rec(0);}", 40, 12, 40, 18);
    testFullExpression(cfa, "rec: p = rec(2);}", 41, 4, 41, 14);
    testFullExpression(cfa, "{rec(x - 2)}", 42, 11, 42, 21);
  }
}
