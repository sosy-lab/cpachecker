// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/** A class to keep the data that is linked to every {@link SeqThreadStatement}. */
public class SeqThreadStatementData {

  private final SeqThreadStatementType type;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final int threadId;

  private final CLeftHandSide pcLeftHandSide;

  /**
   * Returns a new {@link SeqThreadStatementData} instance.
   *
   * @param pType The {@link SeqThreadStatementType} of this statement.
   * @param pSubstituteEdges The set of {@link SubstituteEdge}s created from the input programs
   *     {@link CFA} that this statement represents.
   * @param pThreadId The ID of the thread that executes this statement.
   * @param pPcLeftHandSide The {@link CLeftHandSide} of the thread simulation that executes the
   *     underlying statement. The {@link CLeftHandSide} is written to when updating the pc, e.g.
   *     {@code pc0 = 42;}.
   */
  public SeqThreadStatementData(
      SeqThreadStatementType pType,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide) {

    type = pType;
    substituteEdges = pSubstituteEdges;
    threadId = pThreadId;
    pcLeftHandSide = pPcLeftHandSide;
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqThreadStatementData(pType, pSubstituteEdges, pThreadId, pPcLeftHandSide);
  }

  public static SeqThreadStatementData of(
      SeqThreadStatementType pType,
      SubstituteEdge pSubstituteEdge,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqThreadStatementData(
        pType, ImmutableSet.of(pSubstituteEdge), pThreadId, pPcLeftHandSide);
  }

  public SeqThreadStatementType getType() {
    return type;
  }

  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  public int getThreadId() {
    return threadId;
  }

  public CLeftHandSide getPcLeftHandSide() {
    return pcLeftHandSide;
  }
}
