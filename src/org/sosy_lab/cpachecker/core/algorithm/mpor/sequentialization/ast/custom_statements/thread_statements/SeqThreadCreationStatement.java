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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public class SeqThreadCreationStatement extends ASeqThreadStatement {

  /**
   * The assignment of the parameter given in the {@code pthread_create} call. This is present if
   * the start_routine has exactly one parameter, even if the parameter is not used.
   */
  private final Optional<FunctionParameterAssignment> startRoutineArgAssignment;

  private final CLeftHandSide createdThreadPc;

  private final Optional<ImmutableList<SeqBitVectorAssignmentStatement>> bitVectorInitializations;

  SeqThreadCreationStatement(
      MPOROptions pOptions,
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      CLeftHandSide pPcLeftHandSide,
      CLeftHandSide pCreatedThreadPc,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(
        pOptions,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of());
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThreadPc = pCreatedThreadPc;
    bitVectorInitializations = Optional.empty();
  }

  private SeqThreadCreationStatement(
      MPOROptions pOptions,
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      CLeftHandSide pPcLeftHandSide,
      CLeftHandSide pCreatedThreadPc,
      Optional<ImmutableList<SeqBitVectorAssignmentStatement>> pBitVectorInitializations,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    startRoutineArgAssignment = pStartRoutineArgAssignment;
    createdThreadPc = pCreatedThreadPc;
    bitVectorInitializations = pBitVectorInitializations;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement createdPcWrite =
        SeqStatementBuilder.buildPcWrite(createdThreadPc, Sequentialization.INIT_PC);
    StringJoiner bitVectorInitializationString = new StringJoiner(SeqSyntax.SPACE);
    if (bitVectorInitializations.isPresent()) {
      for (SeqBitVectorAssignmentStatement initialization :
          bitVectorInitializations.orElseThrow()) {
        bitVectorInitializationString.add(initialization.toASTString());
      }
    }
    String injectedStatementsString =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
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
  public SeqThreadCreationStatement withTargetPc(int pTargetPc) {
    return new SeqThreadCreationStatement(
        options,
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
  public ASeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqThreadCreationStatement(
        options,
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
  public ASeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadCreationStatement(
        options,
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

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
