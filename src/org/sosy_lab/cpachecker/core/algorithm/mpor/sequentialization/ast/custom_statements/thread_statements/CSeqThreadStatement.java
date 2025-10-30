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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/**
 * Statements that convert {@link CFAEdge}s to {@link String}s for the output program, {@code
 * abstract} so that it centralizes fields and methods for all these statements.
 *
 * <p>Please ensure that constructors are package-private (see {@link SeqThreadStatementBuilder} and
 * constructors used for cloning are {@code private}.
 */
public abstract class CSeqThreadStatement implements SeqStatement {

  final MPOROptions options;

  final ImmutableSet<SubstituteEdge> substituteEdges;

  /**
   * The {@link CLeftHandSide} that is written to when updating the pc, e.g. {@code pc0 = 42;} for
   * thread 0.
   */
  final CLeftHandSide pcLeftHandSide;

  final Optional<Integer> targetPc;

  final Optional<SeqBlockLabelStatement> targetGoto;

  final ImmutableList<SeqInjectedStatement> injectedStatements;

  CSeqThreadStatement(
      MPOROptions pOptions,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    substituteEdges = pSubstituteEdges;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  /** The set of underlying {@link SubstituteEdge}s used to create this statement. */
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  /**
   * The value that is written to the pc, e.g. {@code pc = 42}. After linking, a statement may not
   * have a target {@code pc}, hence optional.
   */
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  /**
   * The label of the goto, e.g. {@code goto label;}. This may only present after linking or merging
   * atomic blocks.
   */
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  /** The list of statements injected to the {@code pc} write. */
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  /**
   * Clones the statement with the given pc. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public abstract CSeqThreadStatement withTargetPc(int pTargetPc);

  /**
   * Clones the statement with the given label. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public abstract CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel);

  /**
   * Clones this statement and replaces all existing statements with {@code pInjectedStatements}.
   * This is necessary when a {@link SeqInjectedStatement} contains a goto or pc that is replaced,
   * e.g. when consecutive labels are enabled.
   */
  public abstract CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements);

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
