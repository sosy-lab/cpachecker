// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

public class SeqReachErrorStatement implements SeqCaseBlockStatement {

  public SeqReachErrorStatement() {}

  @Override
  public String toASTString() {
    return Sequentialization.inputReachErrorDummy;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.empty();
  }
}