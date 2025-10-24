// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

// TODO further divide this into thread, function, ... interfaces
// TODO its probably better to use an abstract class here for default implementations and attributes
//  (each statement has a target pc, expression, replacement, ...)
// TODO also add CloneableStatement so that we dont always throw an Exception
/**
 * Please ensure that constructors are package-private (see {@link SeqThreadStatementBuilder} and
 * constructors used for cloning are {@code private}.
 */
public abstract class ASeqThreadStatement implements SeqStatement {

  final MPOROptions options;

  final ImmutableSet<SubstituteEdge> substituteEdges;

  final ImmutableList<SeqInjectedStatement> injectedStatements;

  ASeqThreadStatement(
      MPOROptions pOptions,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    substituteEdges = pSubstituteEdges;
    injectedStatements = pInjectedStatements;
  }

  /** The set of underlying {@link SubstituteEdge}s used to create this statement. */
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  /** The list of statements injected to the {@code pc} write. */
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  /** After linking, a statement may not have a target {@code pc}, hence optional. */
  public abstract Optional<Integer> getTargetPc();

  public abstract Optional<SeqBlockLabelStatement> getTargetGoto();

  /**
   * This function should only be called when finalizing (i.e. pruning) {@link
   * SeqThreadStatementClause}s.
   */
  public abstract ASeqThreadStatement cloneWithTargetPc(int pTargetPc);

  public abstract ASeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel);

  /**
   * Clones this statement and replaces all existing statements with {@code
   * pReplacingInjectedStatements}.
   */
  public abstract ASeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements);

  /**
   * Clones this statement and adds the {@code pAppendingInjectedStatements} to the already existing
   * injected statements as a suffix.
   */
  public abstract ASeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendingInjectedStatements);

  /**
   * Whether this statement can be linked to its target statement. This is false e.g. for statements
   * that terminate a thread.
   */
  public abstract boolean isLinkable();

  /**
   * A statement may synchronize threads, e.g. with mutex locks, pthread_join, etc. via assumptions,
   * forcing us to e.g. not link the statements, otherwise a thread may terminate pre-emptively.
   */
  public abstract boolean synchronizesThreads();

  /** Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;} */
  public abstract boolean onlyWritesPc();
}
