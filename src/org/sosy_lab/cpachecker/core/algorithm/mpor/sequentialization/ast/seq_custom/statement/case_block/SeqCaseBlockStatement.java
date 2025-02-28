// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

// TODO further divide this into thread, function, ... interfaces
public interface SeqCaseBlockStatement extends SeqStatement {

  Optional<Integer> getTargetPc();

  Optional<CIdExpression> getTargetPcExpression();

  /**
   * This function should only be called when finalizing (i.e. pruning) {@link SeqCaseClause}s. The
   * target {@code pc} may be a {@code RETURN_PC}, thus we need a {@link CIdExpression} instead of
   * an {@code int}.
   */
  @NonNull SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc) throws UnrecognizedCodeException;

  /**
   * This function should only be called when finalizing (i.e. pruning) {@link SeqCaseClause}s. The
   * target {@code pc} may be a {@code RETURN_PC}, thus we need a {@link CIdExpression} instead of
   * an {@code int}.
   */
  @NonNull SeqCaseBlockStatement cloneWithTargetPc(CIdExpression pTargetPc)
      throws UnrecognizedCodeException;

  // TODO this can be removed later when getting rid of POR assumptions
  /**
   * Whether this statement is guaranteed to write a pc, e.g. {@code pc[i] = 42;} Used for POR
   * assumptions.
   */
  boolean alwaysWritesPc();

  /** Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;} */
  boolean onlyWritesPc();
}
