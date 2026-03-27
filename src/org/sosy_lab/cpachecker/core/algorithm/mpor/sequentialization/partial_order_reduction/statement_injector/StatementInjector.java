// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record StatementInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableSet<MPORThread> otherThreads,
    ImmutableList<SeqThreadStatementClause> clauses,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    GhostElements ghostElements,
    MachineModel machineModel,
    Optional<MemoryModel> memoryModel,
    SequentializationUtils utils) {

  public ImmutableList<SeqThreadStatementClause> injectStatementsIntoClauses()
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> newClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : clauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : block.getStatements()) {
          newStatements.add(injectStatementsIntoStatement(statement));
        }
        newBlocks.add(block.withStatements(newStatements.build()));
      }
      newClauses.add(clause.withBlocks(newBlocks.build()));
    }
    return newClauses.build();
  }

  private SeqThreadStatement injectStatementsIntoStatement(SeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    // always place executeSingleActiveThreadFirst instrumentation first, because it is very cheap
    if (options.executeSingleActiveThreadFirst()) {
      SingleActiveThreadFirstInjector singleActiveThreadFirstInjector =
          new SingleActiveThreadFirstInjector(
              options, labelClauseMap, utils.binaryExpressionBuilder());
      pStatement =
          singleActiveThreadFirstInjector.injectSingleActiveThreadFirstReduction(pStatement);
    }
    // then place executeThreadsUntilConflict, because if the reduction succeeds then the
    // subsequent ghost element updates are unnecessary
    if (options.executeThreadsUntilConflict()) {
      ExecuteUntilConflictInjector executeUntilConflictInjector =
          new ExecuteUntilConflictInjector(
              options,
              otherThreads,
              labelClauseMap,
              labelBlockMap,
              ghostElements.bitVectorVariables().orElseThrow(),
              machineModel,
              memoryModel.orElseThrow(),
              utils);
      pStatement =
          executeUntilConflictInjector.injectUntilConflictReductionIntoStatement(pStatement);
    }
    if (options.executeCommutingThreadsFirst()) {
      CommutingThreadsFirstInjector commutingThreadsFirstInjector =
          new CommutingThreadsFirstInjector(
              options, activeThread, otherThreads, labelClauseMap, ghostElements, utils);
      pStatement = commutingThreadsFirstInjector.tryInjectSyncUpdateIntoStatement(pStatement);
    }
    if (options.abortCommutingContextSwitches()) {
      AbortCommutingContextSwitchesInjector abortCommutingContextSwitches =
          new AbortCommutingContextSwitchesInjector(
              options,
              otherThreads.size() + 1,
              activeThread,
              labelClauseMap,
              labelBlockMap,
              ghostElements.bitVectorVariables().orElseThrow(),
              machineModel,
              memoryModel.orElseThrow(),
              utils);
      pStatement =
          abortCommutingContextSwitches.injectLastUpdatesIntoStatement(pStatement, labelClauseMap);
    }
    if (ghostElements.bitVectorVariables().isPresent()) {
      // always inject bit vector assignments after evaluations i.e. reductions
      BitVectorAssignmentInjector bitVectorAssignmentInjector =
          new BitVectorAssignmentInjector(
              options,
              activeThread,
              labelClauseMap,
              labelBlockMap,
              ghostElements.bitVectorVariables().orElseThrow(),
              machineModel,
              memoryModel.orElseThrow());
      pStatement = bitVectorAssignmentInjector.injectBitVectorAssignmentsIntoStatement(pStatement);
    }
    return pStatement;
  }
}
