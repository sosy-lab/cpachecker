// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqInjectedBitVectorStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqStringUtil {

  /** The amount of spaces in a tab, adjust as desired. */
  public static final int TAB_SIZE = 3;

  /** This value - 1 is the max expected label number used for whitespace padding alignment. */
  public static final int MAX_ALIGN = 4;

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  public static String buildControlFlowSuffixByEncoding(MPOROptions pOptions) {
    return switch (pOptions.controlFlowEncoding) {
      // tests showed that using break in switch is more efficient than continue, despite the loop
      case SWITCH_CASE -> SeqToken._break + SeqSyntax.SEMICOLON;
      case BINARY_IF_TREE -> SeqToken._continue + SeqSyntax.SEMICOLON;
    };
  }

  /** Builds a whitespace aligner based on the number of digits in {@code pNumber}. */
  public static String buildSpaceAlign(int pNumber) {
    int numberLength = String.valueOf(pNumber).length();
    int padding = numberLength % MAX_ALIGN;
    return SeqSyntax.SPACE.repeat(MAX_ALIGN - padding);
  }

  /** Returns {@code /* pString * /} without the last whitespace (Javadoc doesn't allow it ...) */
  public static String wrapInBlockComment(String pString) {
    return SeqComment.COMMENT_BLOCK_BEGIN + pString + SeqComment.COMMENT_BLOCK_END;
  }

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

  public static String wrapInCurlyInwardsWithNewlines(
      String pString, int pBeginTabs, int pEndTabs) {

    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + prependTabsWithNewline(pBeginTabs, pString)
        + prependTabsWithoutNewline(pEndTabs, SeqSyntax.CURLY_BRACKET_RIGHT);
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

  public static String hexFormat(int pLength, BigInteger pBigInteger) {
    return String.format("%0" + pLength + "x", pBigInteger);
  }

  /**
   * This returns either a {@code pc} write of the form {@code pc[i] = n;} including injected
   * statements, if present.
   */
  public static String buildTargetStatements(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockGotoLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    // TODO add some restrictions here

    // TODO we should add some newlines here...
    StringBuilder statements = new StringBuilder();

    if (pTargetPc.isPresent()) {
      CExpressionAssignmentStatement pcWrite =
          SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPc.orElseThrow());
      statements.append(pcWrite.toASTString());

      // we inject after the pc write, in case the injections are goto that require updated pc
      ImmutableList<SeqInjectedStatement> pruned = pruneInjectedStatements(pInjectedStatements);
      for (SeqInjectedStatement injectedStatement : orderInjectedStatements(pruned)) {
        statements.append(SeqSyntax.SPACE).append(injectedStatement.toASTString());
      }

    } else if (pTargetGoto.isPresent()) {
      SeqGotoStatement gotoStatement = new SeqGotoStatement(pTargetGoto.orElseThrow());
      statements.append(gotoStatement.toASTString());
    }
    return statements.toString();
  }

  /**
   * Prunes unnecessary injected statements, e.g. when bit vectors are updated but never evaluated
   * due to the direct {@code goto}. Only the last bit vector update, before an actual evaluation,
   * is necessary.
   */
  private static ImmutableList<SeqInjectedStatement> pruneInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    Set<SeqInjectedStatement> pruned = new HashSet<>();
    for (SeqInjectedStatement outerInjected : pInjectedStatements) {
      if (outerInjected instanceof SeqBitVectorEvaluationStatement evaluation) {
        if (evaluation.isOnlyGoto()) {
          // if the evaluation is only goto, prune all unnecessary bit vector assignments
          pruned.addAll(
              pInjectedStatements.stream()
                  .filter(s -> s instanceof SeqBitVectorAssignmentStatement)
                  .collect(ImmutableList.toImmutableList()));
        }
      }
    }
    return pInjectedStatements.stream()
        .filter(s -> !pruned.contains(s))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> rOrdered = ImmutableList.builder();
    ImmutableList.Builder<SeqInjectedStatement> leftOver = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqInjectedBitVectorStatement bitVectorStatement) {
        // other bit vector statements placed last (more expensive than r < K (cf. threadLoops))
        leftOver.add(bitVectorStatement);
      } else {
        rOrdered.add(injectedStatement);
      }
    }
    rOrdered.addAll(leftOver.build());
    return rOrdered.build();
  }
}
