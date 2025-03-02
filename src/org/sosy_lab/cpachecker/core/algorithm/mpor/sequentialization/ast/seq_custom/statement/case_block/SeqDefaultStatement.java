// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents the default case block statement where the original {@link CFAEdge}s require no
 * specific handling and their (substituted) code is placed directly into the case block.
 */
public class SeqDefaultStatement implements SeqCaseBlockStatement {

  private final CStatementEdge edge;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  protected SeqDefaultStatement(
      CStatementEdge pEdge, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    edge = pEdge;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  protected SeqDefaultStatement(
      CStatementEdge pEdge, CLeftHandSide pPcLeftHandSide, CExpression pTargetPc) {

    edge = pEdge;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return edge.getCode() + SeqSyntax.SPACE + pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @NonNull
  @Override
  public SeqDefaultStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqDefaultStatement(edge, pcLeftHandSide, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
