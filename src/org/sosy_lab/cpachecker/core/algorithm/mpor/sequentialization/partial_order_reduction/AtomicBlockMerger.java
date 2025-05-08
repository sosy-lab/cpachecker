// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class AtomicBlockMerger {

  /**
   * Builds atomic blocks for {@code pCaseClauses} by adding {@code goto} statements when
   * encountering an atomic_begin, until an atomic_end is encountered.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> merge(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rWithBlocks =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();
      ImmutableMap<Integer, SeqThreadStatementClause> labelMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      ImmutableList<SeqThreadStatementClause> withGotos = injectAtomicGotos(clauses, labelMap);
      rWithBlocks.put(thread, mergeAtomicBlocks(withGotos));
    }
    return rWithBlocks.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> injectAtomicGotos(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelMap) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithGotos = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(injectAtomicGotosIntoStatement(statement, pLabelMap));
      }
      rWithGotos.add(clause.cloneWithBlockStatements(newStatements.build()));
    }
    return rWithGotos.build();
  }

  private static SeqThreadStatement injectAtomicGotosIntoStatement(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelMap) {

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC && pLabelMap.containsKey(targetPc)) {
        SeqThreadStatementClause targetClause = Objects.requireNonNull(pLabelMap.get(targetPc));
        SeqThreadStatement firstStatement = targetClause.block.getFirstStatement();
        // only add goto when the target starts in an atomic block
        if (SeqThreadStatementUtil.startsInAtomicBlock(firstStatement)) {
          return pCurrentStatement.cloneWithTargetGoto(targetClause.block.getGotoLabel());
        }
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  private static ImmutableList<SeqThreadStatementClause> mergeAtomicBlocks(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rMerged = ImmutableList.builder();
    Set<SeqThreadStatementClause> visited = new HashSet<>();
    for (int i = 0; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (visited.add(clause)) {
        if (isAtomicMergeAllowed(clause, pClauses, i)) {
          // when beginning atomic, merge all blocks until encountering atomic end
          int toIndex = findFirstExclusiveIndexNotInAtomicBlock(i, pClauses, visited);
          // start at following index, since we clone with adding, not replacing
          ImmutableList<SeqThreadStatementBlock> newMergedBlocks =
              collectAllBlocksAndMergedBlocks(i + 1, toIndex, pClauses);
          rMerged.add(clause.cloneWithAddedMergedBlocks(newMergedBlocks));
        } else {
          rMerged.add(clause);
        }
      }
    }
    return rMerged.build();
  }

  private static boolean isAtomicMergeAllowed(
      SeqThreadStatementClause pClause,
      ImmutableList<SeqThreadStatementClause> pClauses,
      int pCurrentIndex) {

    int nextIndex = pCurrentIndex + 1;
    Optional<SeqThreadStatementClause> next =
        nextIndex < pClauses.size() ? Optional.of(pClauses.get(nextIndex)) : Optional.empty();
    // always allowed when starting atomic block or starting in atomic block
    return pClause.block.startsAtomicBlock()
        || pClause.block.startsInAtomicBlock()
        // allowed if the next statement starts in an atomic block, regardless of previous statement
        // this can happen e.g. with atomic_ends that are reached conditionally
        // -> multiple paths that may not be consecutive in pClauses
        || (next.isPresent() && next.orElseThrow().block.startsInAtomicBlock());
  }

  private static int findFirstExclusiveIndexNotInAtomicBlock(
      int pFrom,
      ImmutableList<SeqThreadStatementClause> pClauses,
      Set<SeqThreadStatementClause> pVisited) {

    for (int i = pFrom; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      // if not in first, consider whether clause starts in atomic block
      if (i != pFrom && !clause.block.startsInAtomicBlock()) {
        return i;
      }
      pVisited.add(clause);
    }
    return pClauses.size(); // if the rest is atomic block, return last index (exclusive)
  }

  private static ImmutableList<SeqThreadStatementBlock> collectAllBlocksAndMergedBlocks(
      int pFrom, // inclusive
      int pTo, // exclusive
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementBlock> rCollected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses.subList(pFrom, pTo)) {
      rCollected.add(clause.block);
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        rCollected.add(mergedBlock);
      }
    }
    return rCollected.build();
  }
}
