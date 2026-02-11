// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

/** A statement executed by a thread simulation in the sequentialization. */
public record SeqThreadStatement(
    SeqThreadStatementData data, ImmutableList<CExportStatement> exportStatements) {

  /**
   * Returns true if the target {@code pc} is present and not equal to {@link
   * ProgramCounterVariables#EXIT_PC}, i.e. if it actually targets another statement.
   */
  public boolean isTargetPcValid() {
    return data.targetPc().filter(pc -> pc != ProgramCounterVariables.EXIT_PC).isPresent();
  }

  /**
   * Returns true if the target {@code pc} is present and equal to {@link
   * ProgramCounterVariables#EXIT_PC}, i.e. if it terminates a thread.
   */
  public boolean isTargetPcExit() {
    return data.targetPc().filter(pc -> pc == ProgramCounterVariables.EXIT_PC).isPresent();
  }

  /**
   * Whether this statement consists only of a {@code pc} write, e.g. {@code pc[i] = 42;}, and no
   * additional {@link SeqInjectedStatement}s.
   */
  public boolean isOnlyPcWrite() {
    // the only case where a statement writes only 'pc' is when it is a blank statement without
    // any injected statement
    return data.type().equals(SeqThreadStatementType.GHOST_ONLY)
        && data.injectedStatements().isEmpty();
  }

  /**
   * Returns either the target {@code pc} or the number of the target {@link
   * SeqBlockLabelStatement}, whichever is present.
   */
  public int getTargetNumber() {
    return data.targetPc().isPresent()
        ? data.targetPc().orElseThrow()
        : data.targetGoto().orElseThrow().labelNumber();
  }

  /**
   * Clones this statement with the given pc. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqThreadStatement(data.withTargetPc(pTargetPc), exportStatements);
  }

  /**
   * Clones this statement with the given label. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatement withTargetGoto(SeqBlockLabelStatement pTargetGoto) {
    return new SeqThreadStatement(data.withTargetGoto(pTargetGoto), exportStatements);
  }

  /**
   * Clones this statement and replaces all existing statements with {@code pInjectedStatements}.
   * This is necessary when a {@link SeqInjectedStatement} contains a goto or pc that is replaced,
   * e.g. when consecutive labels are enabled.
   */
  public SeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadStatement(
        data.withInjectedStatements(pInjectedStatements), exportStatements);
  }

  /**
   * Appends the {@link SeqInjectedStatement} to the {@link CExportStatement}s of this statement and
   * returns them. This should be done after the injected statements are finalized, i.e., after all
   * instrumentations, links, and {@code pc} updates were performed.
   */
  ImmutableList<CExportStatement> appendInjectedStatementsToExportStatements() {
    checkState(
        data.targetPc().isPresent() || data.targetGoto().isPresent(),
        "Either targetPc or targetGoto must be present.");

    // first build the CExportStatements of the SeqInjectedStatement
    ImmutableList<SeqInjectedStatement> preparedInjectedStatements =
        data.targetPc().isPresent()
            ? SeqThreadStatementUtil.prepareInjectedStatementsByTargetPc(
                data.pcLeftHandSide(), data.targetPc().orElseThrow(), data.injectedStatements())
            : SeqThreadStatementUtil.prepareInjectedStatementsByTargetGoto(
                data.targetGoto().orElseThrow(), data.injectedStatements());

    ImmutableList<CExportStatement> injectedExportStatements =
        preparedInjectedStatements.stream()
            .flatMap(injected -> injected.toCExportStatements().stream())
            .collect(ImmutableList.toImmutableList());

    return ImmutableList.<CExportStatement>builder()
        .addAll(exportStatements)
        .addAll(injectedExportStatements)
        .build();
  }
}
