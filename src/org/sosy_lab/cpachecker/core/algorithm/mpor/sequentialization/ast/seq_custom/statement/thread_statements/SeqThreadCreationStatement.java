// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CFAEdgeSubstitute;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public class SeqThreadCreationStatement implements SeqThreadStatement {

  private final MPOROptions options;

  /**
   * The assignment of the parameter given in the {@code pthread_create} call. This is present if
   * the start_routine has exactly one parameter, even if the parameter is not used.
   */
  private final Optional<FunctionParameterAssignment> startRoutineArgAssignment;

  public final MPORThread createdThread;

  private final MPORThread creatingThread;

  private final Optional<ImmutableList<SeqBitVectorAssignmentStatement>> bitVectorInitializations;

  private final ProgramCounterVariables pcVariables;

  private final ImmutableSet<CFAEdgeSubstitute> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqThreadCreationStatement(
      MPOROptions pOptions,
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      MPORThread pCreatedThread,
      MPORThread pCreatingThread,
      ProgramCounterVariables pPcVariables,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      int pTargetPc) {

    options = pOptions;
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThread = pCreatedThread;
    creatingThread = pCreatingThread;
    bitVectorInitializations = Optional.empty();
    pcVariables = pPcVariables;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqThreadCreationStatement(
      MPOROptions pOptions,
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      MPORThread pCreatedThread,
      MPORThread pCreatingThread,
      Optional<ImmutableList<SeqBitVectorAssignmentStatement>> pBitVectorInitializations,
      ProgramCounterVariables pPcVariables,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThread = pCreatedThread;
    creatingThread = pCreatingThread;
    bitVectorInitializations = pBitVectorInitializations;
    pcVariables = pPcVariables;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement createdPcWrite =
        SeqStatementBuilder.buildPcWrite(
            pcVariables.getPcLeftHandSide(createdThread.getId()), Sequentialization.INIT_PC);
    StringJoiner bitVectorInitializationString = new StringJoiner(SeqSyntax.SPACE);
    if (bitVectorInitializations.isPresent()) {
      for (SeqBitVectorAssignmentStatement initialization :
          bitVectorInitializations.orElseThrow()) {
        bitVectorInitializationString.add(initialization.toASTString());
      }
    }
    String injectedStatementsString =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options,
            pcVariables.getPcLeftHandSide(creatingThread.getId()),
            targetPc,
            targetGoto,
            injectedStatements);
    String startRoutineArgAssignmentString =
        buildStartRoutineArgAssignmentString(startRoutineArgAssignment)
            .orElse(SeqSyntax.EMPTY_STRING);

    return Joiner.on(SeqSyntax.SPACE)
        .join(
            startRoutineArgAssignmentString,
            bitVectorInitializationString,
            createdPcWrite.toASTString(),
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
  public ImmutableSet<CFAEdgeSubstitute> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqThreadCreationStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqThreadCreationStatement(
        options,
        startRoutineArgAssignment,
        createdThread,
        creatingThread,
        bitVectorInitializations,
        pcVariables,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqThreadCreationStatement(
        options,
        startRoutineArgAssignment,
        createdThread,
        creatingThread,
        bitVectorInitializations,
        pcVariables,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    return new SeqThreadCreationStatement(
        options,
        startRoutineArgAssignment,
        createdThread,
        creatingThread,
        bitVectorInitializations,
        pcVariables,
        substituteEdges,
        targetPc,
        targetGoto,
        pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return new SeqThreadCreationStatement(
        options,
        startRoutineArgAssignment,
        createdThread,
        creatingThread,
        bitVectorInitializations,
        pcVariables,
        substituteEdges,
        targetPc,
        targetGoto,
        SeqThreadStatementUtil.appendInjectedStatements(this, pAppendedInjectedStatements));
  }

  @Override
  public boolean isLinkable() {
    return true;
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
