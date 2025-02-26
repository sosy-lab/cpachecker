// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;

public class LineOfCodeUtil {

  /** Create and return the {@link String} for {@code pLinesOfCode}. */
  public static String buildString(ImmutableList<LineOfCode> pLinesOfCode) {
    StringBuilder rString = new StringBuilder();
    for (LineOfCode lineOfCode : pLinesOfCode) {
      rString.append(lineOfCode.toString());
    }
    return rString.toString();
  }

  /**
   * Create and return the {@link ImmutableList} for {@code pString} that is split on newlines and
   * preserves leading {@link LineOfCode#tabs} and adds {@code pAdditionalTabs}.
   *
   * <p>This function adds additional leading whitespaces if the amount of leading whitespaces is
   * not a multiple of {@link SeqStringUtil#TAB_SIZE}.
   */
  public static ImmutableList<LineOfCode> buildLinesOfCode(int pAdditionalTabs, String pString) {
    ImmutableList.Builder<LineOfCode> rLinesOfCode = ImmutableList.builder();
    for (String line : SeqStringUtil.splitOnNewline(pString)) {
      int leadingSpaces = line.length() - line.stripLeading().length();
      int tabs = (int) Math.ceil((double) leadingSpaces / SeqStringUtil.TAB_SIZE);
      rLinesOfCode.add(LineOfCode.of(tabs + pAdditionalTabs, line.trim()));
    }
    return rLinesOfCode.build();
  }

  /**
   * Create and return the {@link ImmutableList} for {@code pString} that is split on newlines and
   * preserves leading {@link LineOfCode#tabs}.
   *
   * <p>This function adds additional leading whitespaces if the amount of leading whitespaces is
   * not a multiple of {@link SeqStringUtil#TAB_SIZE}.
   */
  public static ImmutableList<LineOfCode> buildLinesOfCode(String pString) {
    return buildLinesOfCode(0, pString);
  }

  /** Return the list of {@link LineOfCode} for pAstNodes. */
  public static <T extends CAstNode> ImmutableList<LineOfCode> buildLinesOfCode(
      ImmutableList<T> pAstNodes) {

    ImmutableList.Builder<LineOfCode> rLinesOfCode = ImmutableList.builder();
    for (T astNode : pAstNodes) {
      rLinesOfCode.addAll(buildLinesOfCode(astNode.toASTString()));
    }
    return rLinesOfCode.build();
  }
}
