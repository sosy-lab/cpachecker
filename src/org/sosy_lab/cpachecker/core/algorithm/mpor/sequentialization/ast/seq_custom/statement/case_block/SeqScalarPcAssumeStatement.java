// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;

public class SeqScalarPcAssumeStatement implements SeqCaseBlockStatement {

  private final SeqStatement statement;

  public SeqScalarPcAssumeStatement(SeqStatement pStatement) {
    statement = pStatement;
  }

  @Override
  public String toASTString() {
    return statement.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    throw new UnsupportedOperationException("SeqScalarPcAssumeStatement do not have a target pc");
  }

  @NonNull
  @Override
  public SeqScalarPcAssumeStatement cloneWithTargetPc(int pTargetPc) {
    throw new UnsupportedOperationException("SeqScalarPcAssumeStatement cannot be cloned");
  }

  @Override
  public boolean alwaysUpdatesPc() {
    throw new UnsupportedOperationException("SeqScalarPcAssumeStatement are not part of POR");
  }
}
