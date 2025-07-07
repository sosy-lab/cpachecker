// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public class SeqThreadCreationStatement implements SeqThreadStatement {

  /**
   * The assignment of the parameter given in the {@code pthread_create} call. This is always
   * present, even if the parameter is not actually used.
   */
  private final FunctionParameterAssignment parameterAssignment;

  public final MPORThread createdThread;

  private final MPORThread creatingThread;

  private final Optional<ImmutableList<SeqBitVectorAssignmentStatement>> bitVectorInitializations;

  private final PcVariables pcVariables;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqThreadCreationStatement(
      FunctionParameterAssignment pParameterAssignment,
      MPORThread pCreatedThread,
      MPORThread pCreatingThread,
      PcVariables pPcVariables,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    parameterAssignment = pParameterAssignment;
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
      FunctionParameterAssignment pParameterAssignment,
      MPORThread pCreatedThread,
      MPORThread pCreatingThread,
      Optional<ImmutableList<SeqBitVectorAssignmentStatement>> pBitVectorInitializations,
      PcVariables pPcVariables,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    parameterAssignment = pParameterAssignment;
    createdThread = pCreatedThread;
    creatingThread = pCreatingThread;
    bitVectorInitializations = pBitVectorInitializations;
    pcVariables = pPcVariables;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  public SeqThreadCreationStatement cloneWithBitVectorAssignments(
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    return new SeqThreadCreationStatement(
        parameterAssignment,
        createdThread,
        creatingThread,
        Optional.of(pBitVectorAssignments),
        pcVariables,
        substituteEdges,
        targetPc,
        targetGoto,
        injectedStatements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement createdPcWrite =
        SeqStatementBuilder.buildPcWrite(
            pcVariables.getPcLeftHandSide(createdThread.id), Sequentialization.INIT_PC);
    StringBuilder bitVectors = new StringBuilder();
    if (bitVectorInitializations.isPresent()) {
      for (SeqBitVectorAssignmentStatement initialization :
          bitVectorInitializations.orElseThrow()) {
        bitVectors.append(initialization.toASTString()).append(SeqSyntax.SPACE);
      }
    }
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcVariables.getPcLeftHandSide(creatingThread.id),
            targetPc,
            targetGoto,
            injectedStatements);
    return parameterAssignment.statement.toASTString()
        + SeqSyntax.SPACE
        + bitVectors
        + createdPcWrite.toASTString()
        + SeqSyntax.SPACE
        + targetStatements;
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
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
        parameterAssignment,
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
        parameterAssignment,
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
        parameterAssignment,
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
        parameterAssignment,
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
