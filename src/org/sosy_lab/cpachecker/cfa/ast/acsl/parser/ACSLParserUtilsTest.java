// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.junit.Test;

public class ACSLParserUtilsTest {
  @Test
  public void testStripCommentMarker() {
    String lineComment = "//@ assert a == 20;";
    String lineCommentExpected = "assert a == 20;";
    String lineCommentStripped = ACSLParserUtils.stripCommentMarker(lineComment);
    assert lineCommentStripped.equals(lineCommentExpected);
  }

  @Test
  public void testStripBlockComment() {
    String blockComment =
"""
/*@
ensures x = 0;
assumes /true;
ensures !(x < 0);
*/""";
    String blockCommentExpected =
"""
ensures x = 0;
assumes /true;
ensures !(x < 0);
""";

    String blockCommentStripped = ACSLParserUtils.stripCommentMarker(blockComment);
    assert blockCommentStripped.equals(blockCommentExpected);
  }
}
