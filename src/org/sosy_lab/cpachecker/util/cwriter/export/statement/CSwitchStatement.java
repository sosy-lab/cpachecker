// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

/**
 * Represents the entirety of a switch statement. Note that every {@code case} statement is followed
 * by a {@code break;} so that no fall through happens. Example with an {@code int expression}
 * between {@code 0} and {@code 2}:
 *
 * <pre>{@code
 * switch (expression) {
 *    case 0:
 *       ...
 *       break;
 *    case 1:
 *       ...
 *       break;
 *    case 2:
 *       ...
 *       break;
 * }
 * }</pre>
 *
 * <p>For most verifiers, the {@link CSwitchStatement} is a good choice because it scales well with
 * the number of statements. For CBMC however, the {@link CBinarySearchTreeStatement} performed
 * better.
 */
public final class CSwitchStatement extends CMultiControlStatement {

  private record CSwitchCaseStatement(
      CExportExpression expression, ImmutableList<? extends CExportStatement> statements)
      implements CExportStatement {

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
        throws UnrecognizedCodeException {

      StringJoiner caseStatement = new StringJoiner(System.lineSeparator());
      caseStatement.add("case " + expression.toASTString(pAAstNodeRepresentation) + ":");
      for (CExportStatement statement : statements) {
        caseStatement.add(statement.toASTString(pAAstNodeRepresentation));
      }
      caseStatement.add("break;");
      return caseStatement.toString();
    }
  }

  private final CExpression switchExpression;

  public CSwitchStatement(
      CExpression pSwitchExpression,
      ImmutableListMultimap<CExportExpression, ? extends CExportStatement> pStatements) {

    super(pStatements);
    switchExpression = pSwitchExpression;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner switchCase = new StringJoiner(System.lineSeparator());

    // add switch (expression) ...
    switchCase.add("switch (" + switchExpression.toASTString(pAAstNodeRepresentation) + ")");
    ImmutableList.Builder<CExportStatement> caseStatements = ImmutableList.builder();
    // add all case expression: stmt1; ... break;
    for (CExportExpression expression : statements.keySet()) {
      caseStatements.add(new CSwitchCaseStatement(expression, statements.get(expression)));
    }
    switchCase.add(
        new CCompoundStatement(caseStatements.build()).toASTString(pAAstNodeRepresentation));
    return switchCase.toString();
  }
}
