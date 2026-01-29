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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;

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
    ImmutableList<CExportStatement> precedingStatements,
    ImmutableMap<CExportExpression, ? extends CExportStatement> statements)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner switchCase = new StringJoiner(SeqSyntax.NEWLINE);
    // first build preceding statements
    for (CExportStatement precedingStatement : precedingStatements) {
      switchCase.add(precedingStatement.toASTString(pAAstNodeRepresentation));
    }

    // add switch (expression) ...
    switchCase.add(
        Joiner.on(SeqSyntax.SPACE)
            .join(
                "switch",
                SeqStringUtil.wrapInBrackets(switchExpression.toASTString()),
                SeqSyntax.CURLY_BRACKET_LEFT));

    // add all cases
    for (var entry : statements.entrySet()) {
      CExportStatement statement = entry.getValue();
      String casePrefix = "case " + entry.getKey().toASTString(pAAstNodeRepresentation) + ":";
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
}
