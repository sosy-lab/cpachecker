// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public final class SeqThreadCreationStatement extends CSeqThreadStatement {

  /**
   * The assignment of the parameter given in the {@code pthread_create} call. This is present if
   * the start_routine has exactly one parameter, even if the parameter is not used.
   */
  private final Optional<FunctionParameterAssignment> startRoutineArgAssignment;

  private final CLeftHandSide createdThreadPc;

  private final Optional<ImmutableList<SeqBitVectorAssignmentStatement>> bitVectorInitializations;

  SeqThreadCreationStatement(
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      CLeftHandSide pPcLeftHandSide,
      CLeftHandSide pCreatedThreadPc,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThreadPc = pCreatedThreadPc;
    bitVectorInitializations = Optional.empty();
  }

  private SeqThreadCreationStatement(
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      CLeftHandSide pPcLeftHandSide,
      CLeftHandSide pCreatedThreadPc,
      Optional<ImmutableList<SeqBitVectorAssignmentStatement>> pBitVectorInitializations,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThreadPc = pCreatedThreadPc;
    bitVectorInitializations = pBitVectorInitializations;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement createdThreadPcAssignmentStatement =
        ProgramCounterVariables.buildPcAssignmentStatement(
            createdThreadPc, ProgramCounterVariables.INIT_PC);
    StringJoiner bitVectorInitializationString = new StringJoiner(SeqSyntax.SPACE);
    if (bitVectorInitializations.isPresent()) {
      for (SeqBitVectorAssignmentStatement initialization :
          bitVectorInitializations.orElseThrow()) {
        bitVectorInitializationString.add(initialization.toASTString());
      }
    }
    String injectedStatementsString =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    String startRoutineArgAssignmentString =
        buildStartRoutineArgAssignmentString(startRoutineArgAssignment)
            .orElse(SeqSyntax.EMPTY_STRING);

    return Joiner.on(SeqSyntax.SPACE)
        .join(
            startRoutineArgAssignmentString,
            bitVectorInitializationString,
            createdThreadPcAssignmentStatement.toASTString(),
            injectedStatementsString);
  }

  private static Optional<String> buildStartRoutineArgAssignmentString(
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment) {

    if (pStartRoutineArgAssignment.isPresent()) {
      CExpressionAssignmentStatement assignment =
          pStartRoutineArgAssignment.orElseThrow().toExpressionAssignmentStatement();
      return Optional.of(assignment.toASTString());
    }
    return Optional.empty();
  }

  @Override
  public SeqThreadCreationStatement withTargetPc(int pTargetPc) {
    return new SeqThreadCreationStatement(
        startRoutineArgAssignment,
        pcLeftHandSide,
        createdThreadPc,
        bitVectorInitializations,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqThreadCreationStatement(
        startRoutineArgAssignment,
        pcLeftHandSide,
        createdThreadPc,
        bitVectorInitializations,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadCreationStatement(
        startRoutineArgAssignment,
        pcLeftHandSide,
        createdThreadPc,
        bitVectorInitializations,
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
