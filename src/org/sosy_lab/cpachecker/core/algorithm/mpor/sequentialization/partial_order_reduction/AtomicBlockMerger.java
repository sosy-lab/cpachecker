// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class AtomicBlockMerger {

  /**
   * Builds atomic blocks for {@code clauses} by adding {@code goto} statements when encountering an
   * atomic_begin, until an atomic_end is encountered.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> merge(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rWithBlocks =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      ImmutableList<SeqThreadStatementClause> withGotos = injectAtomicGotos(clauses, labelBlockMap);
      rWithBlocks.putAll(thread, mergeAtomicBlocks(withGotos));
    }
    return rWithBlocks.build();
  }

  private static ImmutableList<SeqThreadStatementClause> injectAtomicGotos(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithGotos = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
      // at this stage, mergedBlocks is empty, we only require the first block
      SeqThreadStatementBlock firstBlock = clause.getFirstBlock();
      for (CSeqThreadStatement statement : firstBlock.getStatements()) {
        newStatements.add(injectAtomicGotosIntoStatement(statement, pLabelBlockMap));
      }
      SeqThreadStatementBlock newBlock = firstBlock.withStatements(newStatements.build());
      rWithGotos.add(clause.withFirstBlock(newBlock));
    }
    return rWithGotos.build();
  }

  private static CSeqThreadStatement injectAtomicGotosIntoStatement(
      CSeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pCurrentStatement.isTargetPcValid()) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqThreadStatementBlock targetBlock = Objects.requireNonNull(pLabelBlockMap.get(targetPc));
      CSeqThreadStatement firstStatement = targetBlock.getFirstStatement();
      // only add goto when the target starts in an atomic block
      if (SeqThreadStatementUtil.startsInAtomicBlock(firstStatement)) {
        return pCurrentStatement.withTargetGoto(targetBlock.getLabel());
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
          int exclusiveTo = findFirstExclusiveIndexNotInAtomicBlock(i, pClauses, visited);
          // start at i + 1 so that atomic_begin is not inside mergedBlocks, but block itself
          ImmutableList<SeqThreadStatementBlock> newMergedBlocks =
              collectAllBlocksFromTo(i + 1, exclusiveTo, pClauses);
          rMerged.add(clause.withMergedBlocks(newMergedBlocks));
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
    return pClause.getFirstBlock().startsAtomicBlock()
        || pClause.getFirstBlock().startsInAtomicBlock()
        // allowed if the next statement starts in an atomic block, regardless of previous statement
        // this can happen e.g. with atomic_ends that are reached conditionally
        // -> multiple paths that may not be consecutive in clauses
        || (next.isPresent() && next.orElseThrow().getFirstBlock().startsInAtomicBlock());
  }

  private static int findFirstExclusiveIndexNotInAtomicBlock(
      int pFrom,
      ImmutableList<SeqThreadStatementClause> pClauses,
      Set<SeqThreadStatementClause> pVisited) {

    for (int i = pFrom; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      // if not in first, consider whether clause starts in atomic block
      if (i != pFrom && !clause.getFirstBlock().startsInAtomicBlock()) {
        return i;
      }
      pVisited.add(clause);
    }
    return pClauses.size(); // if the rest is atomic block, return last index (exclusive)
  }

  private static ImmutableList<SeqThreadStatementBlock> collectAllBlocksFromTo(
      int pFrom, // inclusive
      int pTo, // exclusive
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementBlock> rCollected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses.subList(pFrom, pTo)) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rCollected.add(block);
      }
    }
    return rCollected.build();
  }
}
