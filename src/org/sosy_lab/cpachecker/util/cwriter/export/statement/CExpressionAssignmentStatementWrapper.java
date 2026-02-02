// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

/**
 * A wrapper for a {@link CExpressionAssignmentStatement} so that {@link CExportExpression} can be
 * used as a right-hand side in the assignment. Example:
 *
 * <pre>{@code int x = y && z;}</pre>
 */
public record CExpressionAssignmentStatementWrapper(
    CExpressionAssignmentStatement expressionAssignmentStatement, CExportExpression rightHandSide)
    implements CExportStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return expressionAssignmentStatement.getLeftHandSide()
        + " = "
        + rightHandSide.toASTString(pAAstNodeRepresentation)
        + ";";
  }
}
