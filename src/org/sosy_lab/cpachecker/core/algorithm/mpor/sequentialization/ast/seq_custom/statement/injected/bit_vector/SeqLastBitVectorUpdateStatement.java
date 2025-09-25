// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqLastBitVectorUpdateStatement implements SeqInjectedStatement {

  private final CExpressionAssignmentStatement lastThreadUpdate;

  private final ImmutableList<CExpressionAssignmentStatement> lastBitVectorUpdates;

  public SeqLastBitVectorUpdateStatement(
      CExpressionAssignmentStatement pLastThreadUpdate,
      ImmutableList<CExpressionAssignmentStatement> pLastBitVectorUpdates) {

    lastThreadUpdate = pLastThreadUpdate;
    lastBitVectorUpdates = pLastBitVectorUpdates;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> rLines = ImmutableList.builder();
    rLines.add(lastThreadUpdate.toASTString());
    for (CExpressionAssignmentStatement lastBitVectorUpdate : lastBitVectorUpdates) {
      rLines.add(lastBitVectorUpdate.toASTString());
    }
    return SeqStringUtil.joinWithNewlines(rLines.build());
  }
}
