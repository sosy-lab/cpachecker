// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/**
 * A record to keep the data that is linked to every {@link SeqThreadStatement}.
 *
 * @param type The {@link SeqThreadStatementType} of this statement.
 * @param substituteEdges The set of {@link SubstituteEdge}s created from the input programs {@link
 *     CFA} that this statement represents.
 * @param threadId The ID of the thread that executes this statement.
 * @param pcLeftHandSide The {@link CLeftHandSide} of the thread simulation that executes the
 *     underlying statement. The {@link CLeftHandSide} is written to when updating the pc, e.g.
 *     {@code pc0 = 42;}.
 * @param ifExpression The {@link CExpression} used in a {@link CAssumeEdge}, can only be present if
 *     this data instance is tied to {@link SeqThreadStatementType#ASSUME}
 */
public record SeqThreadStatementData(
    SeqThreadStatementType type,
    ImmutableSet<SubstituteEdge> substituteEdges,
    int threadId,
    CLeftHandSide pcLeftHandSide,
    Optional<CExpression> ifExpression) {

  public SeqThreadStatementData {
    checkArgument(
        ifExpression.isEmpty() || type.equals(SeqThreadStatementType.ASSUME),
        "If the ifExpression is present, then type must be SeqThreadStatementType.ASSUME");
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqThreadStatementData(
        pType, pSubstituteEdges, pThreadId, pPcLeftHandSide, Optional.empty());
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      SubstituteEdge pSubstituteEdge,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqThreadStatementData(
        pType, ImmutableSet.of(pSubstituteEdge), pThreadId, pPcLeftHandSide, Optional.empty());
  }
}
