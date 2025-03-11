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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqCaseBlockInjectedStatement;

// TODO further divide this into thread, function, ... interfaces
// TODO its probably better to use an abstract class here for default implementations and attributes
//  (each statement has a target pc, expression, replacement, ...)
// TODO also add CloneableStatement so that we dont always throw an Exception
/**
 * Please ensure that constructors are package-private (see {@link SeqCaseBlockStatementBuilder} and
 * constructors used for cloning are {@code private}.
 */
public interface SeqCaseBlockStatement extends SeqStatement {

  /** After concatenation, a statement may not have a target {@code pc}, hence optional. */
  Optional<Integer> getTargetPc();

  /** The list of statements injected to the {@code pc} write. */
  ImmutableList<SeqCaseBlockInjectedStatement> getInjectedStatements();

  ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements();

  /** This function should only be called when finalizing (i.e. pruning) {@link SeqCaseClause}s. */
  SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc);

  SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements);

  /**
   * This function should be called when applying Partial Order Reduction to {@link SeqCaseClause}s,
   * i.e. when concatenating statements and replacing {@code pc} writes.
   */
  SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements);

  // TODO this is equivalent to whether cloneWithConcatenatedStatements throws an Exception
  boolean isConcatenable();

  /** Whether this statement enters a critical section (e.g. mutex or atomic locks). */
  boolean isCriticalSectionStart();

  /** Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;} */
  boolean onlyWritesPc();
}
