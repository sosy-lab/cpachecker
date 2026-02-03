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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

/**
 * Represents a statement that simulates calls to {@code pthread_exit}, where the second parameter
 * {@code void* retval} is assigned to the respective threads intermediate exit variable. This
 * intermediate variable can then be accessed by threads calling {@code pthread_join} on this
 * exiting thread.
 */
public final class SeqThreadExitStatement extends CSeqThreadStatement {

  private final FunctionReturnValueAssignment returnValueAssignment;

  SeqThreadExitStatement(
      FunctionReturnValueAssignment pReturnValueAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    returnValueAssignment = pReturnValueAssignment;
  }

  private SeqThreadExitStatement(
      FunctionReturnValueAssignment pReturnValueAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    returnValueAssignment = pReturnValueAssignment;
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    return buildExportStatements(new CStatementWrapper(returnValueAssignment.statement()));
  }

  @Override
  public SeqThreadExitStatement withTargetPc(int pTargetPc) {
    checkArgument(
        pTargetPc == ProgramCounterVariables.EXIT_PC,
        "%s should only be cloned with exit pc %s",
        this.getClass().getSimpleName(),
        ProgramCounterVariables.EXIT_PC);
    return new SeqThreadExitStatement(
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
}
