// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CBitVectorLiteralExpression;

public record SeqBitVectorAssignmentStatement(
    CIdExpression variable, CBitVectorLiteralExpression value) implements SeqInjectedStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return variable.toASTString(pAAstNodeRepresentation)
        + SeqSyntax.SPACE
        + SeqSyntax.EQUALS
        + SeqSyntax.SPACE
        + value.toASTString()
        + SeqSyntax.SEMICOLON;
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return true;
  }
}
