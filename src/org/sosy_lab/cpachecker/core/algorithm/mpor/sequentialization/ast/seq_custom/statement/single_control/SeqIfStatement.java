// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * An {@code if (*expression*) { *statements* }} statement with an optional {@code else if
 * (*expression*) { *statements* }} branch.
 */
public class SeqIfStatement implements SeqSingleControlStatement {

  private final CExpression ifExpression;

  private final ImmutableList<CStatement> ifStatements;

  /** The optional {@code else if (*expression*))} */
  private final Optional<CExpression> elseIfExpression;

  private final Optional<ImmutableList<CStatement>> elseStatements;

  /**
   * Use this constructor for an {@code if (...) { ... }} statement without any {@code else} branch.
   */
  public SeqIfStatement(CExpression pIfExpression, ImmutableList<CStatement> pIfStatements) {
    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    ifExpression = pIfExpression;
    ifStatements = pIfStatements;
    elseIfExpression = Optional.empty();
    elseStatements = Optional.empty();
  }

  /** Use this constructor for an {@code if (...) { ... } else { ... }} statement. */
  public SeqIfStatement(
      CExpression pIfExpression,
      ImmutableList<CStatement> pIfStatements,
      ImmutableList<CStatement> pElseStatements) {

    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    checkArgument(!pElseStatements.isEmpty(), "pElseStatements needs at least one element");
    ifExpression = pIfExpression;
    ifStatements = pIfStatements;
    elseIfExpression = Optional.empty();
    elseStatements = Optional.of(pElseStatements);
  }

  /** Use this constructor for an {@code if (...) { ... } else if (...) { ... }} statement. */
  public SeqIfStatement(
      CExpression pIfExpression,
      ImmutableList<CStatement> pIfStatements,
      CExpression pElseIfExpression,
      ImmutableList<CStatement> pElseStatements) {

    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    checkArgument(!pElseStatements.isEmpty(), "pElseStatements needs at least one element");
    ifExpression = pIfExpression;
    ifStatements = pIfStatements;
    elseIfExpression = Optional.of(pElseIfExpression);
    elseStatements = Optional.of(pElseStatements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(SingleControlStatementType.IF.buildControlFlowPrefix(ifExpression));
    ifStatements.forEach(ifStatement -> joiner.add(ifStatement.toASTString()));

    if (elseStatements.isEmpty()) {
      // if (...) { ... }
      joiner.add(SeqSyntax.CURLY_BRACKET_RIGHT);

    } else {
      if (elseIfExpression.isEmpty()) {
        // if (...) { ... } else { ... }
        joiner.add(
            SeqStringUtil.wrapInCurlyBracketsOutwards(
                SingleControlStatementType.ELSE.getKeyword()));

      } else {
        // if (...) { ... } else if (...) { ... }
        joiner.add(
            SingleControlStatementType.ELSE_IF.buildControlFlowPrefix(
                elseIfExpression.orElseThrow()));
      }
      elseStatements
          .orElseThrow()
          .forEach(elseStatement -> joiner.add(elseStatement.toASTString()));
      joiner.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    }
    return joiner.toString();
  }
}
