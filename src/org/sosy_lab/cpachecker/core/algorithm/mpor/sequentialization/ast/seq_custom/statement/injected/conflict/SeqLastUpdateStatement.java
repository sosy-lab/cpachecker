// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqLastUpdateStatement implements SeqInjectedStatement {

  private final CExpressionAssignmentStatement lastThreadUpdate;

  private final ImmutableList<CExpressionAssignmentStatement> lastBitVectorUpdates;

  public SeqLastUpdateStatement(
      CExpressionAssignmentStatement pLastThreadUpdate,
      ImmutableList<CExpressionAssignmentStatement> pLastBitVectorUpdates) {

    lastThreadUpdate = pLastThreadUpdate;
    lastBitVectorUpdates = pLastBitVectorUpdates;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rLines = ImmutableList.builder();
    rLines.add(LineOfCode.of(lastThreadUpdate.toASTString()));
    for (CExpressionAssignmentStatement lastBitVectorUpdate : lastBitVectorUpdates) {
      rLines.add(LineOfCode.of(lastBitVectorUpdate.toASTString()));
    }
    return LineOfCodeUtil.buildString(rLines.build());
  }
}
