// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

/**
 * A statement executed by a thread simulation in the sequentialization.
 *
 * <p>The fields in this class are separate from the {@code exportStatements} because they are
 * dynamically updated during the sequentialization process. These dynamic updates include merging
 * atomic blocks, linking commuting statements or making the label numbers of statements
 * consecutive, based on the specified {@link MPOROptions}. Meanwhile, the {@code exportStatements}
 * are only created once based on the input programs {@link CFA}.
 *
 * <p>Once the data in this class is finalized, it is converted to {@link CExportStatement}s and
 * placed together with the {@code exportStatements} to create the exported program.
 *
 * @param data The data that all statements must contain, e.g., their {@link
 *     SeqThreadStatementType}.
 * @param targetPc The value assigned to a threads {@code pc}, e.g. {@code 42} in {@code pc0 = 42;},
 *     used only if there is no {@code targetGoto}.
 * @param targetGoto The {@code goto stmt;} statement, used only if there is no {@code targetPc}.
 * @param instrumentation The list of {@link SeqInstrumentation}s, includes e.g. partial order
 *     reduction instrumentation. The instrumentation is updated dynamically during the
 *     sequentialization process and is only converted to {@link CExportStatement} once no more
 *     dynamic updates occur.
 * @param exportStatements The list of {@link CExportStatement} as created from the input {@link
 *     CFA}.
 */
public record SeqThreadStatement(
    SeqThreadStatementData data,
    Optional<Integer> targetPc,
    Optional<Integer> targetGoto,
    ImmutableList<SeqInstrumentation> instrumentation,
    ImmutableList<CExportStatement> exportStatements)
    implements SeqExportStatement {

  public SeqThreadStatement {
    checkArgument(
        targetPc.isPresent() ^ targetGoto.isPresent(),
        "Either targetPc or targetGoto must be present (exclusive or).");
  }

  public static SeqThreadStatement of(
      SeqThreadStatementData pData,
      int pTargetPc,
      ImmutableList<CExportStatement> pExportStatements) {
    // the targetGoto and instrumentation are always empty on initialization
    return new SeqThreadStatement(
        pData, Optional.of(pTargetPc), Optional.empty(), ImmutableList.of(), pExportStatements);
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
   * additional {@link SeqInstrumentation}s.
   */
  public boolean isOnlyPcWrite() {
    // the only case where a statement writes only 'pc' is when it is a blank statement without
    // any injected statement
    return data.getType().equals(SeqThreadStatementType.GHOST_ONLY) && instrumentation.isEmpty();
  }

  /**
   * Returns either the target {@code pc} or the number of the target {@code goto} label, whichever
   * is present.
   */
  public int getTargetNumber() {
    return targetPc.isPresent() ? targetPc.orElseThrow() : targetGoto.orElseThrow();
  }

  /**
   * Clones this statement with the given pc. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatement withTargetPc(int pTargetPc) {
    if (data.getType().equals(SeqThreadStatementType.THREAD_EXIT)) {
      checkArgument(
          pTargetPc == ProgramCounterVariables.EXIT_PC,
          "%s should only be cloned with exit pc %s",
          SeqThreadStatementType.THREAD_EXIT,
          ProgramCounterVariables.EXIT_PC);
    }
    return new SeqThreadStatement(
        data, Optional.of(pTargetPc), Optional.empty(), instrumentation, exportStatements);
  }

  /**
   * Clones this statement with the given label. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatement withTargetGoto(int pTargetGoto) {
    return new SeqThreadStatement(
        data, Optional.empty(), Optional.of(pTargetGoto), instrumentation, exportStatements);
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

    return new SeqThreadStatement(data, targetPc, targetGoto, pInstrumentation, exportStatements);
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    checkState(
        targetPc.isPresent() || targetGoto.isPresent(),
        "Either targetPc or targetGoto must be present.");

    // first build the CExportStatements of the SeqInjectedStatement
    ImmutableList<SeqInstrumentation> preparedInstrumentation =
        targetPc.isPresent()
            ? SeqThreadStatementUtil.prepareInstrumentationByTargetPc(
                data.getPcLeftHandSide(), targetPc.orElseThrow(), instrumentation)
            : SeqThreadStatementUtil.prepareInstrumentationByTargetGoto(
                data.getThreadId(), targetGoto.orElseThrow(), instrumentation);

    ImmutableList<CExportStatement> injectedExportStatements =
        transformedImmutableListCopy(preparedInstrumentation, i -> checkNotNull(i).statement());

    return ImmutableList.<CExportStatement>builder()
        .addAll(exportStatements)
        .addAll(injectedExportStatements)
        .build();
  }
}
