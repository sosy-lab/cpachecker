// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

// TODO further divide this into thread, function, ... interfaces
// TODO its probably better to use an abstract class here for default implementations and attributes
//  (each statement has a target pc, expression, replacement, ...)
/**
 * Please ensure that constructors are package-private (see {@link SeqCaseBlockStatementBuilder} and
 * constructors used for cloning are {@code private}.
 */
public interface SeqCaseBlockStatement extends SeqStatement {

  Optional<Integer> getTargetPc();

  Optional<CExpression> getTargetPcExpression();

  Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements();

  /**
   * This function should only be called when finalizing (i.e. pruning) {@link SeqCaseClause}s. The
   * target {@code pc} may be a {@code RETURN_PC}, thus we need a {@link CExpression} instead of an
   * {@code int}.
   */
  SeqCaseBlockStatement cloneWithTargetPc(CExpression pTargetPc) throws UnrecognizedCodeException;

  /**
   * This function should be called when applying Partial Order Reduction to {@link SeqCaseClause}s,
   * i.e. when concatenating statements and replacing {@code pc} writes.
   */
  SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements)
      throws UnrecognizedCodeException;

  boolean isConcatenable();

  /**
   * Whether this statement is guaranteed to write a pc, e.g. {@code pc[i] = 42;} Used for POR
   * assumptions.
   */
  boolean alwaysWritesPc();

  /** Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;} */
  boolean onlyWritesPc();
}
