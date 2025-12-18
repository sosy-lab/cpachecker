// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents the assignments of a function return value, e.g. {@code x = fib(5);} where {@code fib}
 * has a return statement {@code return fibNumber;} then we create a statement {@code x =
 * fibNumber;}.
 */
public final class SeqReturnValueAssignmentStatement extends CSeqThreadStatement {

  private final CExpressionAssignmentStatement assignment;

  SeqReturnValueAssignmentStatement(
      ReductionOrder pReductionOrder,
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pReductionOrder, pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    assignment = pAssignment;
  }

  private SeqReturnValueAssignmentStatement(
      ReductionOrder pReductionOrder,
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(
        pReductionOrder,
        pSubstituteEdges,
        pPcLeftHandSide,
        pTargetPc,
        pTargetGoto,
        pInjectedStatements);
    assignment = pAssignment;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            reductionOrder, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return assignment.toASTString() + SeqSyntax.SPACE + injected;
  }

  @Override
  public CSeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqReturnValueAssignmentStatement(
        reductionOrder,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqReturnValueAssignmentStatement(
        reductionOrder,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqReturnValueAssignmentStatement(
        reductionOrder,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }
}
