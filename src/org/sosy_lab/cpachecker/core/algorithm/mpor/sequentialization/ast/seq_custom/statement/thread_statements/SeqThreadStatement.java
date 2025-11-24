// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

// TODO further divide this into thread, function, ... interfaces
// TODO its probably better to use an abstract class here for default implementations and attributes
//  (each statement has a target pc, expression, replacement, ...)
// TODO also add CloneableStatement so that we dont always throw an Exception
/**
 * Please ensure that constructors are package-private (see {@link SeqThreadStatementBuilder} and
 * constructors used for cloning are {@code private}.
 */
public interface SeqThreadStatement extends SeqStatement {

  /** The set of underlying {@link SubstituteEdge}s used to create this statement. */
  ImmutableSet<SubstituteEdge> getSubstituteEdges();

  /** After linking, a statement may not have a target {@code pc}, hence optional. */
  Optional<Integer> getTargetPc();

  Optional<SeqBlockLabelStatement> getTargetGoto();

  /** The list of statements injected to the {@code pc} write. */
  ImmutableList<SeqInjectedStatement> getInjectedStatements();

  /**
   * This function should only be called when finalizing (i.e. pruning) {@link
   * SeqThreadStatementClause}s.
   */
  SeqThreadStatement cloneWithTargetPc(int pTargetPc);

  SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel);

  /**
   * Clones this statement and replaces all existing statements with {@code
   * pReplacingInjectedStatements}.
   */
  SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements);

  /**
   * Clones this statement and adds the {@code pAppendingInjectedStatements} to the already existing
   * injected statements as a suffix.
   */
  SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendingInjectedStatements);

  boolean isLinkable();

  /**
   * A statement may synchronize threads, e.g. with mutex locks, pthread_join, etc. via assumptions,
   * forcing us to e.g. not link the statements, otherwise a thread may terminate pre-emptively.
   */
  boolean synchronizesThreads();

  /** Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;} */
  boolean onlyWritesPc();
}
