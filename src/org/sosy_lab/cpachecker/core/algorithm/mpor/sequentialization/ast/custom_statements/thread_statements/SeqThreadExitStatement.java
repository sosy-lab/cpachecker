// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_exit}, where the second parameter
 * {@code void* retval} is assigned to the respective threads intermediate exit variable. This
 * intermediate variable can then be accessed by threads calling {@code pthread_join} on this
 * exiting thread.
 */
public final class SeqThreadExitStatement extends CSeqThreadStatement {

  private final FunctionReturnValueAssignment returnValueAssignment;

  SeqThreadExitStatement(
      MPOROptions pOptions,
      FunctionReturnValueAssignment pReturnValueAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    returnValueAssignment = pReturnValueAssignment;
  }

  private SeqThreadExitStatement(
      MPOROptions pOptions,
      FunctionReturnValueAssignment pReturnValueAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    returnValueAssignment = pReturnValueAssignment;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return returnValueAssignment.statement().toASTString() + SeqSyntax.SPACE + injected;
  }

  @Override
  public SeqThreadExitStatement withTargetPc(int pTargetPc) {
    checkArgument(
        pTargetPc == Sequentialization.EXIT_PC,
        "reach_errors should only be cloned with exit pc %s",
        Sequentialization.EXIT_PC);
    return new SeqThreadExitStatement(
        options,
        returnValueAssignment,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        // no need to update injected labels, no goto at end of thread
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have target goto");
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadExitStatement(
        options,
        returnValueAssignment,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return false;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
