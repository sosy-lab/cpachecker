// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.thread_sync;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqSyncUpdateStatement implements SeqInjectedStatement {

  private final CExpressionAssignmentStatement syncUpdate;

  public SeqSyncUpdateStatement(CExpressionAssignmentStatement pSyncUpdate) {
    syncUpdate = pSyncUpdate;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return syncUpdate.toASTString();
  }
}
