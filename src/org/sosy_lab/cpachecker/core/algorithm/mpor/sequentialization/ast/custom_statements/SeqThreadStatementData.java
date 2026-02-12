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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

/**
 * A record to keep the data that is linked to every {@link SeqThreadStatement}.
 *
 * @param type The {@link SeqThreadStatementType} of this statement.
 * @param substituteEdges The set of {@link SubstituteEdge}s created from the input programs {@link
 *     CFA} that this statement represents.
 * @param threadId The ID of the thread that executes this statement.
 * @param pcLeftHandSide The {@link CLeftHandSide} that is written to when updating the pc, e.g.
 *     {@code pc0 = 42;}.
 * @param targetPc The value assigned to {@code pcLeftHandSide}, e.g. {@code 42} in {@code pc0 =
 *     42;}, used only if there is no {@code targetGoto}
 * @param targetGoto The {@code goto stmt;} statement, used only if there is no {@code targetPc}.
 * @param instrumentation The list of {@link SeqInstrumentation}s, includes e.g. partial order
 *     reduction instrumentation. The instrumentation is updated dynamically during the
 *     sequentialization process and is only converted to {@link CExportStatement} once no more
 *     dynamic updates occur.
 * @param ifExpression The {@link CExpression} used in a {@link CAssumeEdge}, can only be present if
 *     this data instance is tied to {@link SeqThreadStatementType#ASSUME}
 */
public record SeqThreadStatementData(
    SeqThreadStatementType type,
    ImmutableSet<SubstituteEdge> substituteEdges,
    int threadId,
    CLeftHandSide pcLeftHandSide,
    Optional<Integer> targetPc,
    Optional<Integer> targetGoto,
    ImmutableList<SeqInstrumentation> instrumentation,
    Optional<CExpression> ifExpression) {

  public SeqThreadStatementData {
    checkArgument(
        targetPc.isPresent() ^ targetGoto.isPresent(),
        "Either targetPc or targetGoto must be present (exclusive or).");
    checkArgument(
        ifExpression.isEmpty() || type.equals(SeqThreadStatementType.ASSUME),
        "If the ifExpression is present, then type must be SeqThreadStatementType.ASSUME");
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    return new SeqThreadStatementData(
        pType,
        pSubstituteEdges,
        pThreadId,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of(),
        Optional.empty());
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      SubstituteEdge pSubstituteEdge,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    return new SeqThreadStatementData(
        pType,
        ImmutableSet.of(pSubstituteEdge),
        pThreadId,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of(),
        Optional.empty());
  }

  /**
   * Clones this data with the given pc. This function should only be called when finalizing (i.e.
   * pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatementData withTargetPc(int pTargetPc) {
    if (type.equals(SeqThreadStatementType.THREAD_EXIT)) {
      checkArgument(
          pTargetPc == ProgramCounterVariables.EXIT_PC,
          "%s should only be cloned with exit pc %s",
          SeqThreadStatementType.THREAD_EXIT,
          ProgramCounterVariables.EXIT_PC);
    }
    return new SeqThreadStatementData(
        type,
        substituteEdges,
        threadId,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        instrumentation,
        ifExpression);
  }

  /**
   * Clones this data with the given label. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatementData withTargetGoto(int pTargetGoto) {
    return new SeqThreadStatementData(
        type,
        substituteEdges,
        threadId,
        pcLeftHandSide,
        Optional.empty(),
        Optional.of(pTargetGoto),
        instrumentation,
        ifExpression);
  }

  /**
   * Clones this data and replaces all existing statements with {@code pInstrumentation}. This is
   * necessary when a {@link SeqInstrumentation} contains a goto or pc that is replaced, e.g. when
   * consecutive labels are enabled.
   */
  public SeqThreadStatementData withInstrumentation(
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    return new SeqThreadStatementData(
        type,
        substituteEdges,
        threadId,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        pInstrumentation,
        ifExpression);
  }
}
