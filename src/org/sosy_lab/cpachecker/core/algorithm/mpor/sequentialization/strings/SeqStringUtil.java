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
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqInjectedBitVectorStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict.SeqLastUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqStringUtil {

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  public static String buildSuffixByMultiControlStatementEncoding(MPOROptions pOptions) {

    // use control encoding of the statement since we append the suffix to the statement
    return switch (pOptions.controlEncodingStatement) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build suffix for control encoding " + pOptions.controlEncodingStatement);
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN -> {
        if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
          yield SeqToken._continue + SeqSyntax.SEMICOLON;
        } else {
          yield SeqSyntax.EMPTY_STRING;
        }
      }
      case SWITCH_CASE ->
          // tests showed: using break in switch is more efficient than continue, despite the loop
          SeqToken._break + SeqSyntax.SEMICOLON;
    };
  }

  /** Returns {@code /* pString * /} without the last whitespace (Javadoc doesn't allow it ...) */
  public static String wrapInBlockComment(String pString) {
    return SeqComment.COMMENT_BLOCK_BEGIN + pString + SeqComment.COMMENT_BLOCK_END;
  }

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
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns "} pString {" */
  public static String wrapInCurlyBracketsOutwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString {" */
  public static String appendCurlyBracketRight(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
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

  public static String buildEmptyFunctionDefinitionFromDeclaration(
      CFunctionDeclaration pDeclaration) {

    StringBuilder declaration = new StringBuilder();
    declaration.append(pDeclaration.getType().getReturnType().toASTString(""));
    declaration.append(SeqSyntax.SPACE);
    declaration.append(pDeclaration.getOrigName());
    // add parameters either with original or generic name, if declaration without names
    declaration.append(SeqSyntax.BRACKET_LEFT);
    for (int i = 0; i < pDeclaration.getParameters().size(); i++) {
      CParameterDeclaration parameter = pDeclaration.getParameters().get(i);
      declaration
          .append(parameter.getType().getCanonicalType().toASTString(""))
          .append(SeqSyntax.SPACE);
      if (parameter.getName().isEmpty()) {
        declaration.append(
            SeqNameUtil.buildParameterNameForEmptyFunctionDefinition(pDeclaration, i));
      } else {
        declaration.append(parameter.getOrigName());
      }
      if (i != pDeclaration.getParameters().size() - 1) {
        declaration.append(SeqSyntax.COMMA).append(SeqSyntax.SPACE);
      }
    }
    declaration.append(SeqSyntax.BRACKET_RIGHT);
    // no body, only {}. the parser still accepts it, even with e.g. int return type
    declaration.append(SeqSyntax.SPACE);
    declaration.append(SeqSyntax.CURLY_BRACKET_LEFT);
    declaration.append(SeqSyntax.CURLY_BRACKET_RIGHT);
    return declaration.toString();
  }

  // TODO move this out of this class, maybe into StatementUtil?
  /**
   * This returns either a {@code pc} write of the form {@code pc[i] = n;} including injected
   * statements, if present.
   */
  public static String buildTargetStatements(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    // TODO add some restrictions here

    // TODO we should add some newlines here...
    StringBuilder statements = new StringBuilder();

    if (pTargetPc.isPresent()) {
      // first create pruned statements
      ImmutableList<SeqInjectedStatement> pruned = pruneInjectedStatements(pInjectedStatements);
      // create the pc write
      CExpressionAssignmentStatement pcWrite =
          SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPc.orElseThrow());
      boolean emptyBitVectorEvaluation =
          SeqThreadStatementUtil.containsEmptyBitVectorEvaluationExpression(pruned);
      // with empty bit vector evaluations, place pc write before injections, otherwise info is lost
      if (emptyBitVectorEvaluation) {
        statements.append(pcWrite.toASTString()).append(SeqSyntax.SPACE);
      }
      // add all injected statements in the correct order
      ImmutableList<SeqInjectedStatement> ordered = orderInjectedStatements(pruned);
      for (int i = 0; i < ordered.size(); i++) {
        SeqInjectedStatement injectedStatement = ordered.get(i);
        statements.append(injectedStatement.toASTString());
        if (i != ordered.size() - 1) {
          // append space to all statements except last
          statements.append(SeqSyntax.SPACE);
        }
      }
      // for non-empty bit vector evaluations, place pc write after injections for optimization
      if (!emptyBitVectorEvaluation) {
        if (!ordered.isEmpty()) {
          statements.append(SeqSyntax.SPACE);
        }
        statements.append(pcWrite.toASTString());
      }

    } else if (pTargetGoto.isPresent()) {
      SeqGotoStatement gotoStatement = new SeqGotoStatement(pTargetGoto.orElseThrow());
      for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
        if (injectedStatement instanceof SeqCountUpdateStatement) {
          // count updates are included, even with target gotos
          statements.append(injectedStatement.toASTString());
        }
      }
      statements.append(gotoStatement.toASTString());
    }
    return statements.toString();
  }

  private static ImmutableList<SeqInjectedStatement> pruneInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    Set<SeqInjectedStatement> pruned = new HashSet<>();
    if (SeqThreadStatementUtil.containsEmptyBitVectorEvaluationExpression(pInjectedStatements)) {
      // prune all bit vector assignments if the evaluation expression is empty
      pruned.addAll(
          pInjectedStatements.stream()
              .filter(s -> s instanceof SeqBitVectorAssignmentStatement)
              .collect(ImmutableSet.toImmutableSet()));
    }
    return pInjectedStatements.stream()
        .filter(i -> !pruned.contains(i))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> rOrdered = ImmutableList.builder();
    ImmutableList.Builder<SeqInjectedStatement> leftOver = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqInjectedBitVectorStatement bitVectorStatement) {
        // bit vector statements are last (more expensive than r < K)
        leftOver.add(bitVectorStatement);
      }
    }
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqLastUpdateStatement lastUpdateStatement) {
        // last updates are always last, i.e. placed where pc and bv updates are
        leftOver.add(lastUpdateStatement);
      }
    }
    rOrdered.addAll(
        pInjectedStatements.stream()
            .filter(stmt -> !leftOver.build().contains(stmt))
            .collect(ImmutableList.toImmutableList()));
    rOrdered.addAll(leftOver.build());
    return rOrdered.build();
  }
}
