// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
      ImmutableList<CExportStatement> pLoopStatements) {

    super(pCondition, pLoopStatements);
    counterDeclaration = pCounterDeclaration;
    iterationUpdate = pIterationUpdate;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner("\n");

    StringJoiner forLoopHeader = new StringJoiner(" ");
    forLoopHeader.add("for");

    StringJoiner innerJoiner = new StringJoiner(" ");
    // build the variable declaration, it already contains a ';' suffix
    innerJoiner.add(counterDeclaration.toASTString(pAAstNodeRepresentation));
    innerJoiner.add(condition.toASTString(pAAstNodeRepresentation) + ";");
    // exclude the semicolon in the assignment statement
    innerJoiner.add(iterationUpdate.toASTString(pAAstNodeRepresentation).replace(";", ""));

    forLoopHeader.add("(" + innerJoiner + ")");
    joiner.add(forLoopHeader.toString());
    joiner.add(buildLoopStatements(pAAstNodeRepresentation));

    return joiner.toString();
  }
}
