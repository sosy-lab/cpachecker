// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlExpressionEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * An {@code if (*expression*) { *statements* }} statement with an optional {@code else if
 * (*expression*) { *statements* }} branch.
 */
public class SeqIfStatement implements SeqStatement {

  private final CExpression ifExpression;

  private final ImmutableList<CStatement> ifStatements;

  /** The optional {@code else if (*expression*))} */
  private final Optional<CExpression> elseIfExpression;

  private final ImmutableList<CStatement> elseStatements;

  SeqIfStatement(
      CExpression pIfExpression,
      ImmutableList<CStatement> pIfStatements,
      Optional<CExpression> pElseIfExpression,
      ImmutableList<CStatement> pElseStatements) {

    checkArgument(!pIfStatements.isEmpty(), "pIfStatements needs at least one element");
    checkArgument(
        pElseIfExpression.isEmpty() || !pElseStatements.isEmpty(),
        "if there is a pElseIfExpression, then pElseStatements needs at least one element");
    ifExpression = pIfExpression;
    ifStatements = pIfStatements;
    elseIfExpression = pElseIfExpression;
    elseStatements = pElseStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(buildIfExpressionString(ifExpression));
    ifStatements.forEach(ifStatement -> joiner.add(ifStatement.toASTString()));

    if (elseStatements.isEmpty()) {
      // if (...) { ... }
      joiner.add(SeqSyntax.CURLY_BRACKET_RIGHT);

    } else {
      if (elseIfExpression.isEmpty()) {
        // if (...) { ... } else { ... }
        joiner.add(
            SeqStringUtil.wrapInCurlyBracketsOutwards(
                SingleControlExpressionEncoding.ELSE.keyword));

      } else {
        // if (...) { ... } else if (...) { ... }
        joiner.add(
            Joiner.on(SeqSyntax.SPACE)
                .join(
                    SeqSyntax.CURLY_BRACKET_RIGHT,
                    SingleControlExpressionEncoding.ELSE.keyword,
                    buildIfExpressionString(elseIfExpression.orElseThrow())));
      }
      elseStatements.forEach(elseStatement -> joiner.add(elseStatement.toASTString()));
      joiner.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    }
    return joiner.toString();
  }

  private static String buildIfExpressionString(CExpression pIfExpression) {
    return Joiner.on(SeqSyntax.SPACE)
        .join(
            SingleControlExpressionEncoding.IF.keyword,
            SeqStringUtil.wrapInBrackets(pIfExpression.toASTString()),
            SeqSyntax.CURLY_BRACKET_LEFT);
  }
}
