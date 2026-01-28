// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqLastBitVectorUpdateStatement(
    CExpressionAssignmentStatement lastThreadUpdate,
    ImmutableList<CExpressionAssignmentStatement> lastBitVectorUpdates)
    implements SeqInjectedStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(lastThreadUpdate.toASTString(pAAstNodeRepresentation));
    for (CExpressionAssignmentStatement lastBitVectorUpdate : lastBitVectorUpdates) {
      joiner.add(lastBitVectorUpdate.toASTString(pAAstNodeRepresentation));
    }
    return joiner.toString();
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
