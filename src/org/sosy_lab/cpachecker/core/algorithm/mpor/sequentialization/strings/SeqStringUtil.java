// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqStringUtil {

  /** The amount of spaces in a tab, adjust as desired. */
  public static final int TAB_SIZE = 2;

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  /** Returns ""pString"" */
  public static String wrapInQuotationMarks(String pString) {
    return SeqSyntax.QUOTATION_MARK + pString + SeqSyntax.QUOTATION_MARK;
  }

  /** Returns "{ pString }" */
  public static String wrapInCurlyInwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns "} pString {" */
  public static String wrapInCurlyOutwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString {" */
  public static String appendOpeningCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString }" */
  public static String appendClosingCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns pString with the specified amount of tabs as prefix and a new line \n as suffix. */
  public static String prependTabsWithNewline(int pTabs, String pString) {
    return prependTabsWithoutNewline(pTabs, pString) + SeqSyntax.NEWLINE;
  }

  /** Returns pString with the specified amount of tabs as prefix. */
  public static String prependTabsWithoutNewline(int pTabs, String pString) {
    return buildTab(pTabs) + pString;
  }

  public static String buildTab(int pTabs) {
    return repeat(SeqSyntax.SPACE, pTabs * TAB_SIZE);
  }

  public static String repeat(String pString, int pAmount) {
    return pString.repeat(Math.max(0, pAmount));
  }

  public static Iterable<String> splitOnNewline(String pString) {
    return newlineSplitter.split(pString);
  }

  /** {@link CType#toString()} yields a trailing white space, this function strips it. */
  public static String getTypeName(CType pType) {
    return pType.toString().strip();
  }

  /** Returns the number of lines, i.e. the amount of \n + 1 in pString. */
  public static int countLines(String pString) {
    if (isNullOrEmpty(pString)) {
      return 0;
    }
    return newlineSplitter.splitToList(pString).size();
  }

  /**
   * This returns either a {@code pc} write of the form:
   *
   * <ul>
   *   <li>{@code pc[i] = n;}
   *   <li>{@code pc[i] = RETURN_PC}
   * </ul>
   *
   * Or the strings of concatenated statements, if present.
   */
  public static String buildTargetStatements(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<CExpression> pTargetPcExpression,
      Optional<ImmutableList<SeqCaseBlockStatement>> pConcatenatedStatements) {

    // TODO this logic could be expanded that only one is present
    checkArgument(
        pTargetPc.isPresent()
            || pTargetPcExpression.isPresent()
            || pConcatenatedStatements.isPresent(),
        "either pTargetPc or pTargetPcExpression or pConcatenatedStatements must be present");
    checkArgument(
        pTargetPc.isEmpty() || pTargetPcExpression.isEmpty() || pConcatenatedStatements.isPresent(),
        "either pTargetPc or pTargetPcExpression or pConcatenatedStatements must be empty");
    if (pTargetPc.isPresent()) {
      return SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPc.orElseThrow())
          .toASTString();
    } else if (pTargetPcExpression.isPresent()) {
      return SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPcExpression.orElseThrow())
          .toASTString();
    } else {
      // TODO we should add some newlines here...
      StringBuilder statements = new StringBuilder();
      // this includes statements that were concatenated before
      for (SeqCaseBlockStatement statement : pConcatenatedStatements.orElseThrow()) {
        if (statement instanceof SeqAssumeStatement assumeStatement) {
          if (assumeStatement.controlFlowStatement.type.equals(SeqControlFlowStatementType.ELSE)) {
            // append additional space before 'else { ... }'
            statements.append(SeqSyntax.SPACE).append(statement.toASTString());
            continue; // other control flow statements are appended as is (see below)
          }
        }
        statements.append(statement.toASTString());
      }
      return statements.toString();
    }
  }
}
