// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents the entirety of a switch statement. Note that every statement is followed by a {@code
 * break;} so that no fall through happens.
 *
 * <p>Example: {@code switch(a) { case b: ...; break; case c: ...; break; } }
 *
 * @param statements No restriction to literal expressions as keys because e.g. {@code case 1 + 2:}
 *     i.e. a {@link CBinaryExpression} is allowed in C.
 */
public record SeqSwitchStatement(
    CExpression switchExpression,
    ImmutableList<String> precedingStatements,
    ImmutableMap<CExpression, ? extends SeqStatement> statements)
    implements SeqMultiControlStatement {

  private static final String SWITCH_KEYWORD = "switch";

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner switchCase = new StringJoiner(SeqSyntax.NEWLINE);
    // first build preceding statements
    precedingStatements.forEach(statement -> switchCase.add(statement));
    // add switch (expression) ...
    switchCase.add(
        Joiner.on(SeqSyntax.SPACE)
            .join(
                SWITCH_KEYWORD,
                SeqStringUtil.wrapInBrackets(switchExpression.toASTString()),
                SeqSyntax.CURLY_BRACKET_LEFT));
    // add all cases
    for (var entry : statements.entrySet()) {
      SeqStatement statement = entry.getValue();
      String casePrefix = buildCasePrefix(entry.getKey());
      String suffix = "";
      if (statement instanceof SeqMultiControlStatement) {
        // for inner multi control statements, add "break;" suffix. for clauses this is not
        // necessary, because each block within the clause has its own "break;" suffix
        suffix = "break" + SeqSyntax.SEMICOLON;
      }
      switchCase.add(casePrefix + statement.toASTString() + suffix);
    }
    switchCase.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return switchCase.toString();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.SWITCH_CASE;
  }

  private static String buildCasePrefix(CExpression pExpression) {
    return "case" + SeqSyntax.SPACE + pExpression.toASTString() + SeqSyntax.COLON;
  }
}
