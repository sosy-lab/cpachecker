// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

/**
 * Used to export loop statements such as {@code for (int i = 0; i < N; i++) { ... }} that can be
 * used for finite loops.
 */
public final class CForLoopStatement extends CLoopStatement {

  private final CVariableDeclaration counterDeclaration;

  private final CExpressionAssignmentStatement iterationUpdate;

  public CForLoopStatement(
      CVariableDeclaration pCounterDeclaration,
      CExportExpression pCondition,
      CExpressionAssignmentStatement pIterationUpdate,
      CCompoundStatement pCompoundStatement) {

    super(pCondition, pCompoundStatement);
    counterDeclaration = pCounterDeclaration;
    iterationUpdate = pIterationUpdate;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());

    StringJoiner innerJoiner = new StringJoiner(" ");
    // build the variable declaration, it already contains a ';' suffix
    innerJoiner.add(counterDeclaration.toASTString(pAAstNodeRepresentation));
    innerJoiner.add(getCondition().toASTString(pAAstNodeRepresentation) + ";");
    // exclude the semicolon in the assignment statement
    innerJoiner.add(iterationUpdate.toASTString(pAAstNodeRepresentation).replace(";", ""));

    joiner.add("for (" + innerJoiner + ")");
    joiner.add(getBody().toASTString(pAAstNodeRepresentation));

    return joiner.toString();
  }
}
