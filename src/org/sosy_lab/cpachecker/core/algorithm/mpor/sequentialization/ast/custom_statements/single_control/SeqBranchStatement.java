// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * An {@code if (*expression*) { *statements* }} statement with an optional {@code else if
 * (*expression*) { *statements* }} branch.
 */
public final class SeqBranchStatement implements SeqSingleControlStatement {

  private enum BranchType {
    IF("if"),
    ELSE("else");

    private final String keyword;

    BranchType(String pKeyword) {
      keyword = pKeyword;
    }

    private String getKeyword() {
      return keyword;
    }
  }

  private final String ifExpression;

  private final SeqCompoundStatement ifCompoundStatement;

  private final Optional<SeqCompoundStatement> elseCompoundStatement;

  private final Optional<SeqBranchStatement> elseBranchStatement;

  /**
   * Use this constructor for an {@code if (...) { ... }} statement without any {@code else} branch.
   */
  public SeqBranchStatement(String pIfExpression, ImmutableList<String> pIfStatements) {
    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    ifExpression = pIfExpression;
    ifCompoundStatement = new SeqCompoundStatement(pIfStatements);
    elseCompoundStatement = Optional.empty();
    elseBranchStatement = Optional.empty();
  }

  /** Use this constructor for an {@code if (...) { ... } else { ... }} statement. */
  public SeqBranchStatement(
      String pIfExpression,
      ImmutableList<String> pIfStatements,
      ImmutableList<String> pElseStatements) {

    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    checkArgument(!pElseStatements.isEmpty(), "pElseStatements needs at least one element");
    ifExpression = pIfExpression;
    ifCompoundStatement = new SeqCompoundStatement(pIfStatements);
    elseCompoundStatement = Optional.of(new SeqCompoundStatement(pElseStatements));
    elseBranchStatement = Optional.empty();
  }

  /** Use this constructor for an {@code if (...) { ... } else if (...) { ... }} statement. */
  public SeqBranchStatement(
      String pIfExpression,
      ImmutableList<String> pIfStatements,
      SeqBranchStatement pElseBranchStatement) {

    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    ifExpression = pIfExpression;
    ifCompoundStatement = new SeqCompoundStatement(pIfStatements);
    elseCompoundStatement = Optional.empty();
    elseBranchStatement = Optional.of(pElseBranchStatement);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    checkArgument(
        elseCompoundStatement.isEmpty() || elseBranchStatement.isEmpty(),
        "elseCompoundStatement and/or elseBranchStatement must be empty");

    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    // if (...) { ... }
    joiner.add(buildIfPrefix(ifExpression));
    joiner.add(ifCompoundStatement.toASTString());

    // if (...) { ... } else { ... }
    if (elseCompoundStatement.isPresent()) {
      joiner.add(buildElsePrefix());
      joiner.add(elseCompoundStatement.orElseThrow().toASTString());
    }

    // if (...) { ... } else if (...) { ... }
    if (elseBranchStatement.isPresent()) {
      joiner.add(buildElsePrefix());
      ImmutableList<String> elseBranchString =
          SeqStringUtil.splitOnNewline(elseBranchStatement.orElseThrow().toASTString());
      SeqCompoundStatement elseBranchCompoundStatement = new SeqCompoundStatement(elseBranchString);
      joiner.add(elseBranchCompoundStatement.toASTString());
    }

    return joiner.toString();
  }

  private static String buildIfPrefix(String pExpression) {
    return BranchType.IF.getKeyword() + SeqSyntax.SPACE + SeqStringUtil.wrapInBrackets(pExpression);
  }

  private String buildElsePrefix() {
    return SeqSyntax.SPACE + BranchType.ELSE.getKeyword() + SeqSyntax.SPACE;
  }
}
