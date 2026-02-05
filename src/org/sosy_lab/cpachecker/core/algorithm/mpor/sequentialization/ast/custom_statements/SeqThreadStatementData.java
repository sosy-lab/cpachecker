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

/**
 * A record to keep the data that is linked to every {@link SeqThreadStatement}.
 *
 * @param substituteEdges The set of {@link SubstituteEdge}s created from the input programs {@link
 *     CFA} that this statement represents.
 * @param pcLeftHandSide The {@link CLeftHandSide} that is written to when updating the pc, e.g.
 *     {@code pc0 = 42;}.
 * @param targetPc The value assigned to {@code pcLeftHandSide}, e.g. {@code 42} in {@code pc0 =
 *     42;}, used only if there is no {@code targetGoto}
 * @param targetGoto The {@code goto stmt;} statement, used only if there is no {@code targetPc}.
 * @param injectedStatements The list of {@link SeqInjectedStatement}. May includes e.g. partial
 *     order reduction instrumentation.
 * @param ifExpression The {@link CExpression} used in a {@link CAssumeEdge}, can only be present if
 *     this data instance is tied to {@link SeqThreadStatementType#ASSUME}
 */
public record SeqThreadStatementData(
    SeqThreadStatementType type,
    ImmutableSet<SubstituteEdge> substituteEdges,
    CLeftHandSide pcLeftHandSide,
    Optional<Integer> targetPc,
    Optional<SeqBlockLabelStatement> targetGoto,
    ImmutableList<SeqInjectedStatement> injectedStatements,
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
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    return new SeqThreadStatementData(
        pType,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of(),
        Optional.empty());
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    return new SeqThreadStatementData(
        pType,
        ImmutableSet.of(pSubstituteEdge),
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
        pcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements,
        ifExpression);
  }

  /**
   * Clones this data with the given label. This function should only be called when finalizing
   * (i.e. pruning) {@link SeqThreadStatementClause}s.
   */
  public SeqThreadStatementData withTargetGoto(SeqBlockLabelStatement pTargetGoto) {
    return new SeqThreadStatementData(
        type,
        substituteEdges,
        pcLeftHandSide,
        Optional.empty(),
        Optional.of(pTargetGoto),
        injectedStatements,
        ifExpression);
  }

  /**
   * Clones this data and replaces all existing statements with {@code pInjectedStatements}. This is
   * necessary when a {@link SeqInjectedStatement} contains a goto or pc that is replaced, e.g. when
   * consecutive labels are enabled.
   */
  public SeqThreadStatementData withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadStatementData(
        type,
        substituteEdges,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        pInjectedStatements,
        ifExpression);
  }
}
