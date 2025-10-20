// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqStringUtil {

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  /** Returns the number of lines, i.e. the amount of \n + 1 in pString. */
  public static int countLines(String pString) {
    if (isNullOrEmpty(pString)) {
      return 0;
    }
    return newlineSplitter.splitToList(pString).size();
  }

  public static ImmutableList<String> splitOnNewline(String pString) {
    return ImmutableList.copyOf(newlineSplitter.split(pString));
  }

  public static String joinWithNewlines(ImmutableList<String> pStrings) {
    return String.join("\n", pStrings);
  }

  // From CAstNodes ================================================================================

  /** Return the list of {@link String} for {@code pAstNodes}. */
  public static ImmutableList<String> buildLinesOfCodeFromCAstNodes(
      ImmutableList<? extends CAstNode> pAstNodes) {

    ImmutableList.Builder<String> rStrings = ImmutableList.builder();
    for (CAstNode astNode : pAstNodes) {
      rStrings.add(astNode.toASTString());
    }
    return rStrings.build();
  }

  // Multi Control Statement Suffix ================================================================

  public static Optional<String> tryBuildBlockSuffix(
      MPOROptions pOptions,
      Optional<MPORThread> pNextThread,
      ImmutableMap<MPORThread, SeqThreadLabelStatement> pThreadLabels,
      ImmutableList<SeqThreadStatement> pStatements)
      throws UnrecognizedCodeException {

    if (SeqThreadStatementUtil.allHaveTargetGoto(pStatements)) {
      return Optional.empty();
    }
    if (SeqThreadStatementUtil.anyContainsEmptyBitVectorEvaluationExpression(pStatements)) {
      return Optional.empty();
    }
    return Optional.of(
        buildBlockSuffixByControlStatementEncoding(pOptions, pNextThread, pThreadLabels));
  }

  private static String buildBlockSuffixByControlStatementEncoding(
      MPOROptions pOptions,
      Optional<MPORThread> pNextThread,
      ImmutableMap<MPORThread, SeqThreadLabelStatement> pThreadLabels)
      throws UnrecognizedCodeException {

    // use control encoding of the statement since we append the suffix to the statement
    return switch (pOptions.controlEncodingStatement) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build suffix for control encoding " + pOptions.controlEncodingStatement);
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN -> {
        if (pOptions.loopUnrolling) {
          // with loop unrolling enabled, always return to main()
          yield SeqToken.RETURN_KEYWORD + SeqSyntax.SEMICOLON;
        }
        if (pOptions.isThreadLabelRequired()) {
          // if this is not the last thread, add goto T{next_thread_ID}, otherwise continue
          if (pNextThread.isPresent()) {
            SeqThreadLabelStatement nextLabel = pThreadLabels.get(pNextThread.orElseThrow());
            SeqGotoStatement gotoStatement = new SeqGotoStatement(nextLabel);
            yield gotoStatement.toASTString();
          }
        }
        yield SeqToken.CONTINUE_KEYWORD + SeqSyntax.SEMICOLON;
      }
      // it is best to always use break; for switch cases, tests showed it is more efficient
      case SWITCH_CASE -> SeqToken.BREAK_KEYWORD + SeqSyntax.SEMICOLON;
    };
  }

  // String from ASTNodes ==========================================================================

  public static String buildStringFromSeqASTNodes(ImmutableList<SeqASTNode> pSeqASTNodes)
      throws UnrecognizedCodeException {

    StringJoiner rString = new StringJoiner(SeqSyntax.NEWLINE);
    for (SeqASTNode seqASTNode : pSeqASTNodes) {
      rString.add(seqASTNode.toASTString());
    }
    return rString.toString();
  }

  public static String buildStringFromCAstNodes(ImmutableList<CAstNode> pCASTNodes) {
    StringJoiner rString = new StringJoiner(SeqSyntax.NEWLINE);
    for (CAstNode cASTNode : pCASTNodes) {
      rString.add(cASTNode.toASTString());
    }
    return rString.toString();
  }

  // Comments ======================================================================================

  /** Returns {@code /* pString * /} without the last whitespace (Javadoc doesn't allow it ...) */
  public static String wrapInBlockComment(String pString) {
    return SeqComment.COMMENT_BLOCK_BEGIN + pString + SeqComment.COMMENT_BLOCK_END;
  }

  // Quotation Marks ===============================================================================

  /** Returns ""pString"" */
  public static String wrapInQuotationMarks(String pString) {
    return SeqSyntax.QUOTATION_MARK + pString + SeqSyntax.QUOTATION_MARK;
  }

  // Brackets ======================================================================================

  /** Returns "(pString)" */
  public static String wrapInBrackets(String pString) {
    return SeqSyntax.BRACKET_LEFT + pString + SeqSyntax.BRACKET_RIGHT;
  }

  // Curly Brackets ================================================================================

  /** Returns "{ pString }" */
  public static String wrapInCurlyBracketsInwards(String pString) {
    return Joiner.on(SeqSyntax.SPACE)
        .join(SeqSyntax.CURLY_BRACKET_LEFT, pString, SeqSyntax.CURLY_BRACKET_RIGHT);
  }

  /** Returns "} pString {" */
  public static String wrapInCurlyBracketsOutwards(String pString) {
    return Joiner.on(SeqSyntax.SPACE)
        .join(SeqSyntax.CURLY_BRACKET_RIGHT, pString, SeqSyntax.CURLY_BRACKET_LEFT);
  }

  /** Returns "pString {" */
  public static String appendCurlyBracketLeft(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** {@link CType#toString()} yields a trailing white space, this function strips it. */
  public static String getTypeName(CType pType) {
    return pType.toString().strip();
  }

  public static String hexFormat(int pLength, BigInteger pBigInteger) {
    return String.format("%0" + pLength + "x", pBigInteger);
  }
}
