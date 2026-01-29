// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.collect.ImmutableListMultimap;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;

/**
 * Represents the entirety of a switch statement. Note that every statement is followed by a {@code
 * break;} so that no fall through happens.
 *
 * <p>Example: {@code switch(a) { case b: ...; break; case c: ...; break; } }
 */
public final class SeqSwitchStatement extends SeqMultiControlStatement {

  private final CExpression switchExpression;

  public SeqSwitchStatement(
      CExpression pSwitchExpression,
      ImmutableListMultimap<CExportExpression, ? extends CExportStatement> pStatements) {

    super(pStatements);
    switchExpression = pSwitchExpression;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner switchCase = new StringJoiner(SeqSyntax.NEWLINE);

    // add switch (expression) ...
    switchCase.add("switch (" + switchExpression.toASTString(pAAstNodeRepresentation) + ") {");

    // add all case expression: stmt1; ... break;
    for (CExportExpression expression : statements.keySet()) {
      switchCase.add("case " + expression.toASTString(pAAstNodeRepresentation) + ":");
      for (CExportStatement caseStatement : statements.get(expression)) {
        switchCase.add(caseStatement.toASTString(pAAstNodeRepresentation));
      }
      switchCase.add("break;");
    }
    switchCase.add("}");
    return switchCase.toString();
  }
}
