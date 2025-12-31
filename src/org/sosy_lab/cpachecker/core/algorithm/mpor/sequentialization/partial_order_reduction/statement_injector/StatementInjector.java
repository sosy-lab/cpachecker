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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
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
    BitVectorVariables bitVectorVariables,
    MemoryModel memoryModel,
    SequentializationUtils utils) {

  public ImmutableList<SeqThreadStatementClause> injectStatementsIntoClauses()
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : clauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(injectStatementsIntoBlock(block));
      }
      rInjected.add(clause.withBlocks(newBlocks.build()));
    }
    return rInjected.build();
  }

  private SeqThreadStatementBlock injectStatementsIntoBlock(SeqThreadStatementBlock pBlock)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(injectStatementsIntoStatement(statement));
    }
    return pBlock.withStatements(newStatements.build());
  }

  private CSeqThreadStatement injectStatementsIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    if (options.reduceUntilConflict()) {
      ReduceUntilConflictInjector reduceUntilConflictInjector =
          new ReduceUntilConflictInjector(
              options,
              otherThreads,
              labelClauseMap,
              labelBlockMap,
              bitVectorVariables,
              memoryModel,
              utils);
      pStatement =
          reduceUntilConflictInjector.injectUntilConflictReductionIntoStatement(pStatement);
    }
    if (options.reduceLastThreadOrder()) {
      ReduceLastThreadOrderInjector reduceLastThreadOrderInjector =
          new ReduceLastThreadOrderInjector(
              options,
              otherThreads.size() + 1,
              activeThread,
              labelClauseMap,
              labelBlockMap,
              bitVectorVariables,
              memoryModel,
              utils);
      pStatement =
          reduceLastThreadOrderInjector.injectLastThreadOrderReductionIntoStatement(
              pStatement, labelClauseMap);
    }
    if (options.reduceIgnoreSleep()) {
      // this needs to be last, it collects the prior injections
      ReduceIgnoreSleepInjector reduceIgnoreSleepInjector =
          new ReduceIgnoreSleepInjector(
              options, activeThread, otherThreads, labelClauseMap, bitVectorVariables, utils);
      pStatement = reduceIgnoreSleepInjector.injectIgnoreSleepReductionIntoStatement(pStatement);
    }
    // always inject bit vector assignments after evaluations i.e. reductions
    BitVectorAssignmentInjector bitVectorAssignmentInjector =
        new BitVectorAssignmentInjector(
            options, activeThread, labelClauseMap, labelBlockMap, bitVectorVariables, memoryModel);
    pStatement = bitVectorAssignmentInjector.injectBitVectorAssignmentsIntoStatement(pStatement);
    return pStatement;
  }

  // boolean helpers ===============================================================================

  /**
   * Checks whether bit vector injections are allowed, i.e. if they do not result in interleaving
   * loss.
   */
  static boolean isReductionAllowed(MPOROptions pOptions, SeqThreadStatementClause pTarget) {
    // if the target starts with a thread synchronization (i.e. assume), do not inject
    return !SeqThreadStatementUtil.anySynchronizesThreads(pTarget.getAllStatements())
        // check based on pOptions if the target is a loop head that must remain separate
        && !SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, pTarget);
  }
}
