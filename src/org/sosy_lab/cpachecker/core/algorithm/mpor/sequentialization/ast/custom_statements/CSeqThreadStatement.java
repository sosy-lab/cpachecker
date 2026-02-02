// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

/**
 * Statements that convert {@link CFAEdge}s to {@link String}s for the output program, {@code
 * abstract} so that it centralizes fields and methods for all these statements.
 *
 * <p>Please ensure that constructors are package-private (see {@link SeqThreadStatementBuilder})
 * and constructors used for cloning are {@code private}.
 */
public abstract sealed class CSeqThreadStatement implements SeqExportStatement
    permits SeqAssumeStatement,
        SeqAtomicBeginStatement,
        SeqAtomicEndStatement,
        SeqGhostOnlyStatement,
        SeqCondSignalStatement,
        SeqCondWaitStatement,
        SeqConstCpaCheckerTmpStatement,
        SeqDefaultStatement,
        SeqLocalVariableDeclarationWithInitializerStatement,
        SeqMutexLockStatement,
        SeqMutexUnlockStatement,
        SeqParameterAssignmentStatement,
        SeqReturnValueAssignmentStatement,
        SeqRwLockRdLockStatement,
        SeqRwLockUnlockStatement,
        SeqRwLockWrLockStatement,
        SeqThreadCreationStatement,
        SeqThreadExitStatement,
        SeqThreadJoinStatement {

  final ImmutableSet<SubstituteEdge> substituteEdges;

  /**
   * The {@link CLeftHandSide} that is written to when updating the pc, e.g. {@code pc0 = 42;} for
   * thread 0.
   */
  final CLeftHandSide pcLeftHandSide;

  final Optional<Integer> targetPc;

  final Optional<SeqBlockLabelStatement> targetGoto;

  final ImmutableList<SeqInjectedStatement> injectedStatements;

  /**
   * Use this constructor for the initialization, when there is no target goto {@link
   * SeqBlockLabelStatement} and no {@link SeqInjectedStatement}s.
   */
  CSeqThreadStatement(
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      CLeftHandSide pPcLeftHandSide,
      Integer pTargetPc) {

    substituteEdges = pSubstituteEdges;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  /**
   * Use this constructor to clone with target pc, target goto {@link SeqBlockLabelStatement}, or
   * {@link SeqInjectedStatement}s.
   */
  CSeqThreadStatement(
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    // XOR that one must be present, one must be empty
    checkArgument(
        pTargetPc.isPresent() ^ pTargetGoto.isPresent(),
        "either targetPc or targetLabel must be present (exclusive or)");
    substituteEdges = pSubstituteEdges;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  /**
   * Returns true if the target {@code pc} is present and not equal to {@link
   * ProgramCounterVariables#EXIT_PC}, i.e. if it actually targets another statement.
   */
  public boolean isTargetPcValid() {
    return targetPc.filter(pc -> pc != ProgramCounterVariables.EXIT_PC).isPresent();
  }

  /**
   * Returns true if the target {@code pc} is present and equal to {@link
   * ProgramCounterVariables#EXIT_PC}, i.e. if it terminates a thread.
   */
  public boolean isTargetPcExit() {
    return targetPc.filter(pc -> pc == ProgramCounterVariables.EXIT_PC).isPresent();
  }

  /**
   * Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;}, and no
   * additional {@link SeqInjectedStatement}s.
   */
  public boolean isOnlyPcWrite() {
    // the only case where a statement writes only 'pc' is when it is a blank statement without
    // any injected statement
    return this instanceof SeqGhostOnlyStatement && injectedStatements.isEmpty();
  }

  /**
   * Returns either the target {@code pc} or the number of the target {@link
   * SeqBlockLabelStatement}, whichever is present.
   */
  public int getTargetNumber() {
    return targetPc.isPresent() ? targetPc.orElseThrow() : targetGoto.orElseThrow().labelNumber();
  }

  /** The set of underlying {@link SubstituteEdge}s used to create this statement. */
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  static ImmutableList<CExportStatement> convertInjectedStatementsToCExportStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<CExportStatement> exportStatements = ImmutableList.builder();
    for (SeqInjectedStatement injected : pInjectedStatements) {
      exportStatements.addAll(injected.toCExportStatements());
    }
    return exportStatements.build();
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
}
