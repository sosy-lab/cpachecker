// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;

/**
 * Represents the assignment of a parameter given to a function to an injected parameter variable in
 * the sequentialization.
 *
 * <p>E.g. {@code __MPOR_SEQ__THREAD0_PARAM_q = GLOBAL_queue; }
 */
public class SeqParameterAssignStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  private final Optional<CLeftHandSide> pcLeftHandSide;

  private final Optional<Integer> targetPc;

  // TODO better make this encompass multiple statements i.e. assigns with a non-optional
  //  targetPc. we can still create an inner class here with only one assignment then we can remove
  //  the optional entirely.
  protected SeqParameterAssignStatement(
      CExpressionAssignmentStatement pAssign,
      Optional<CLeftHandSide> pPcLeftHandSide,
      Optional<Integer> pTargetPc) {

    // the presence of pThreadId and pTargetPc must be equivalent
    checkArgument(pPcLeftHandSide.isEmpty() || pTargetPc.isPresent());
    checkArgument(pTargetPc.isEmpty() || pPcLeftHandSide.isPresent());

    assign = pAssign;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    if (pcLeftHandSide.isPresent() && targetPc.isPresent()) {
      CExpressionAssignmentStatement pcWrite =
          SeqExpressionAssignmentStatement.buildPcWrite(
              pcLeftHandSide.orElseThrow(), targetPc.orElseThrow());
      return assign.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString();
    } else if (pcLeftHandSide.isEmpty() && targetPc.isEmpty()) {
      return assign.toASTString();
    } else {
      throw new IllegalArgumentException("presence of threadId and targetPc must be equivalent");
    }
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @NonNull
  @Override
  public SeqParameterAssignStatement cloneWithTargetPc(int pTargetPc) {
    checkArgument(targetPc.isPresent(), "cannot replace empty targetPc");
    return new SeqParameterAssignStatement(assign, pcLeftHandSide, Optional.of(pTargetPc));
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }
}
