// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A class to create a compound / block statement, wrapped in curly brackets. We use {@link String}
 * to also accept comments.
 *
 * <p>{@code { statementA; statementB; ... }}
 */
public record SeqCompoundStatement(ImmutableList<String> statements)
    implements SeqSingleControlStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String statementString = Joiner.on(SeqSyntax.NEWLINE).join(statements);
    return Joiner.on(SeqSyntax.NEWLINE)
        .join(SeqSyntax.CURLY_BRACKET_LEFT, statementString, SeqSyntax.CURLY_BRACKET_RIGHT);
  }
}
