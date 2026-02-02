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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;

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

  // Multi Control Statement Suffix ================================================================

  public static Optional<String> tryBuildBlockSuffix(
      MPOROptions pOptions,
      Optional<CLabelStatement> pNextThreadLabel,
      ImmutableList<CSeqThreadStatement> pStatements,
      AAstNodeRepresentation pAAstNodeRepresentation) {

    // if all statements have a 'goto', then the suffix is never reached
    if (SeqThreadStatementUtil.allHaveTargetGoto(pStatements)) {
      return Optional.empty();
    }
    // if the bit vector evaluation is empty, 'abort();' is called and the suffix is never reached
    if (SeqThreadStatementUtil.anyContainsEmptyBitVectorEvaluationExpression(pStatements)) {
      return Optional.empty();
    }
    return Optional.of(
        buildBlockSuffixByControlStatementEncoding(
            pOptions, pNextThreadLabel, pAAstNodeRepresentation));
  }

  private static String buildBlockSuffixByControlStatementEncoding(
      MPOROptions pOptions,
      Optional<CLabelStatement> pNextThreadLabel,
      AAstNodeRepresentation pAAstNodeRepresentation) {

    // use control encoding of the statement since we append the suffix to the statement
    return switch (pOptions.controlEncodingStatement()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build suffix for control encoding " + pOptions.controlEncodingStatement());
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN -> {
        if (pOptions.loopUnrolling()) {
          // with loop unrolling (and separate thread functions) enabled, always return to main()
          yield "return" + SeqSyntax.SEMICOLON;
        }
        // if this is not the last thread, add "goto T{next_thread_ID};"
        if (pNextThreadLabel.isPresent()) {
          yield new CGotoStatement(pNextThreadLabel.orElseThrow())
              .toASTString(pAAstNodeRepresentation);
        }
        // otherwise, continue i.e. go to next loop iteration
        yield "continue" + SeqSyntax.SEMICOLON;
      }
      // for switch cases, add additional "break;" after each block, because SeqSwitchStatement
      // only adds "break;" after an entire clause i.e. after the last block
      case SWITCH_CASE -> "break" + SeqSyntax.SEMICOLON;
    };
  }

  // Wrap / Append Methods =========================================================================

  /** Returns {@code /* pString * /} without the last whitespace (Javadoc doesn't allow it ...) */
  public static String wrapInBlockComment(String pString) {
    return SeqComment.COMMENT_BLOCK_BEGIN + pString + SeqComment.COMMENT_BLOCK_END;
  }

  /** Returns ""pString"" */
  public static String wrapInQuotationMarks(String pString) {
    return SeqSyntax.QUOTATION_MARK + pString + SeqSyntax.QUOTATION_MARK;
  }

  /** Returns "(pString)" */
  public static String wrapInBrackets(String pString) {
    return SeqSyntax.BRACKET_LEFT + pString + SeqSyntax.BRACKET_RIGHT;
  }

  /** Returns "pString {" */
  public static String appendCurlyBracketLeft(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  // AST Nodes =====================================================================================

  /** {@link CType#toString()} yields a trailing white space, this function strips it. */
  public static String getTypeName(CType pType) {
    return pType.toString().strip();
  }

  /**
   * If {@link CVariableDeclaration#toASTString()} yields {@code int x = 42;} then this method
   * yields {@code int x;}.
   */
  public static String getVariableDeclarationASTStringWithoutInitializer(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return buildStorageClassNameAndTypeASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + ";";
  }

  /**
   * If {@link CVariableDeclaration#toASTString()} yields {@code extern int x = 42;} then this
   * method yields {@code x = 42;}. Note that the initializer does not have to be present.
   */
  public static String getVariableDeclarationASTStringWithoutStorageClassAndType(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return buildNameASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + buildInitializerASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + ";";
  }

  private static String buildStorageClassNameAndTypeASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return pVariableDeclaration.getCStorageClass().toASTString()
        + pVariableDeclaration
            .getType()
            .toASTString(buildNameASTString(pVariableDeclaration, pAAstNodeRepresentation));
  }

  private static String buildNameASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return switch (pAAstNodeRepresentation) {
      case DEFAULT -> pVariableDeclaration.getName();
      case QUALIFIED -> pVariableDeclaration.getQualifiedName().replace("::", "__");
      case ORIGINAL_NAMES -> pVariableDeclaration.getOrigName();
    };
  }

  private static String buildInitializerASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    if (pVariableDeclaration.getInitializer() != null) {
      return " = " + pVariableDeclaration.getInitializer().toASTString(pAAstNodeRepresentation);
    }
    return "";
  }

  // Hexadecimal Format ============================================================================

  public static String hexFormat(int pLength, BigInteger pBigInteger) {
    return String.format("%0" + pLength + "x", pBigInteger);
  }
}
