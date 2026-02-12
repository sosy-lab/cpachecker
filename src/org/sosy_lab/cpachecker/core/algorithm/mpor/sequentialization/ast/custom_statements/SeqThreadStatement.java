// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

/** A statement executed by a thread simulation in the sequentialization. */
public record SeqThreadStatement(
    SeqThreadStatementData data, ImmutableList<CExportStatement> exportStatements)
    implements SeqExportStatement {

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
   * additional {@link SeqInstrumentation}s.
   */
  public boolean isOnlyPcWrite() {
    // the only case where a statement writes only 'pc' is when it is a blank statement without
    // any injected statement
    return data.type().equals(SeqThreadStatementType.GHOST_ONLY)
        && data.instrumentation().isEmpty();
  }

  /**
   * Returns either the target {@code pc} or the number of the target {@code goto} label, whichever
   * is present.
   */
  public int getTargetNumber() {
    return data.targetPc().isPresent()
        ? data.targetPc().orElseThrow()
        : data.targetGoto().orElseThrow();
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
  public SeqThreadStatement withTargetGoto(int pTargetGoto) {
    return new SeqThreadStatement(data.withTargetGoto(pTargetGoto), exportStatements);
  }

  /**
   * Clones this statement and replaces all existing statements with {@code pInstrumentation}. This
   * is necessary e.g. when a specific combination of already added {@link SeqInstrumentation} can
   * be simplified.
   *
   * <p>The instrumentation is thus kept separate from the {@link CExportStatement}s of this
   * statement, but instead kept inside {@link SeqThreadStatementData} so that it easier to observe
   * the specific combination of {@link SeqInstrumentation}s.
   */
  public SeqThreadStatement withInstrumentation(
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    return new SeqThreadStatement(data.withInstrumentation(pInstrumentation), exportStatements);
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    checkState(
        data.targetPc().isPresent() || data.targetGoto().isPresent(),
        "Either targetPc or targetGoto must be present.");

    // first build the CExportStatements of the SeqInjectedStatement
    ImmutableList<SeqInstrumentation> preparedInstrumentation =
        data.targetPc().isPresent()
            ? SeqThreadStatementUtil.prepareInstrumentationByTargetPc(
                data.pcLeftHandSide(), data.targetPc().orElseThrow(), data.instrumentation())
            : SeqThreadStatementUtil.prepareInstrumentationByTargetGoto(
                data.threadId(), data.targetGoto().orElseThrow(), data.instrumentation());

    ImmutableList<CExportStatement> injectedExportStatements =
        transformedImmutableListCopy(preparedInstrumentation, i -> checkNotNull(i).statement());

    return ImmutableList.<CExportStatement>builder()
        .addAll(exportStatements)
        .addAll(injectedExportStatements)
        .build();
  }
}
