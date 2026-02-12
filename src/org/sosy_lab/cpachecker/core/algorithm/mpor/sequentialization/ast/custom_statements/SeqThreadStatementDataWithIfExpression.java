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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;

/**
 * A special case for a {@link SeqThreadStatementData} with a {@link CExportExpression} used in an
 * {@link CIfStatement} to represent {@link CAssumeEdge}s from the input {@link CFA}.
 */
public final class SeqThreadStatementDataWithIfExpression extends SeqThreadStatementData {

  private final CExpression ifExpression;

  /**
   * Returns a new {@link SeqThreadStatementDataWithIfExpression} instance.
   *
   * @param pType The {@link SeqThreadStatementType} of this statement.
   * @param pSubstituteEdges The set of {@link SubstituteEdge}s created from the input programs
   *     {@link CFA} that this statement represents.
   * @param pThreadId The ID of the thread that executes this statement.
   * @param pPcLeftHandSide The {@link CLeftHandSide} of the thread simulation that executes the
   *     underlying statement. The {@link CLeftHandSide} is written to when updating the pc, e.g.
   *     {@code pc0 = 42;}.
   * @param pIfExpression The {@link CExpression} used in a {@link CAssumeEdge}, can only be present
   *     if this data instance is tied to {@link SeqThreadStatementType#ASSUME}
   */
  public SeqThreadStatementDataWithIfExpression(
      SeqThreadStatementType pType,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pThreadId,
      CLeftHandSide pPcLeftHandSide,
      CExpression pIfExpression) {

    super(pType, pSubstituteEdges, pThreadId, pPcLeftHandSide);
    checkArgument(
        pType.equals(SeqThreadStatementType.ASSUME), "pType must be SeqThreadStatementType.ASSUME");
    ifExpression = pIfExpression;
  }

  public CExpression getIfExpression() {
    return ifExpression;
  }
}
