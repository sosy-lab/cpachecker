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
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqAtomicStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class AtomicBlockBuilder {

  /**
   * Builds atomic blocks for {@code pCaseClauses} by adding {@code goto} statements when
   * encountering an atomic_begin, until an atomic_end is encountered.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> build(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rWithBlocks =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();
      ImmutableMap<Integer, SeqThreadStatementClause> labelMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      ImmutableList<SeqThreadStatementClause> withGotos = injectAtomicGotos(clauses, labelMap);
      rWithBlocks.put(thread, mergeAtomicBlocks(pOptions, withGotos));
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
        newStatements.add(recursivelyInjectAtomicGotos(statement, pLabelMap));
      }
      rWithGotos.add(clause.cloneWithBlockStatements(newStatements.build()));
    }
    return rWithGotos.build();
  }

  private static SeqThreadStatement recursivelyInjectAtomicGotos(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelMap) {

    // if there are concatenated statements, replace target pc there too
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqThreadStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement concatenatedStatement : concatenatedStatements) {
          if (concatenatedStatement instanceof SeqAtomicEndStatement) {
            // atomic_end -> stop replacing, need pc update and context switch
            newStatements.add(concatenatedStatement);
          } else {
            newStatements.add(recursivelyInjectAtomicGotos(concatenatedStatement, pLabelMap));
          }
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC && pLabelMap.containsKey(targetPc)) {
        SeqThreadStatementClause targetClause = Objects.requireNonNull(pLabelMap.get(targetPc));
        SeqThreadStatement firstStatement = targetClause.block.getFirstStatement();
        // only add goto when the target starts in an atomic block
        if (SeqThreadStatementUtil.startsInAtomicBlock(firstStatement)) {
          return pCurrentStatement.cloneWithTargetGoto(
              targetClause.block.getGotoLabel().getLabelName());
        }
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  private static ImmutableList<SeqThreadStatementClause> mergeAtomicBlocks(
      MPOROptions pOptions, ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rMerged = ImmutableList.builder();
    Set<SeqThreadStatementClause> visited = new HashSet<>();
    for (int i = 0; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (visited.add(clause)) {
        if (clause.block.startsInAtomicBlock()) {
          // if the predecessor starts an atomic block, include it, otherwise it was added already
          int fromIndex = pClauses.get(i - 1).block.startsAtomicBlock() ? i - 1 : i;
          int toIndex = findFirstExclusiveIndexNotInAtomicBlock(i, pClauses, visited);
          SeqAtomicStatementBlock newBlock =
              new SeqAtomicStatementBlock(collectStatementBlocks(fromIndex, toIndex, pClauses));
          rMerged.add(clause.cloneWithAtomicBlock(newBlock));
        } else {
          // prevent duplicate atomic_begin
          // or add if the very first statement starts atomic, but only when concatenating
          if ((i == 0 && pOptions.porConcat) || !clause.block.startsAtomicBlock()) {
            rMerged.add(clause);
          }
        }
      }
    }
    return rMerged.build();
  }

  private static int findFirstExclusiveIndexNotInAtomicBlock(
      int pFrom,
      ImmutableList<SeqThreadStatementClause> pClauses,
      Set<SeqThreadStatementClause> pVisited) {

    for (int i = pFrom; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (!clause.block.startsInAtomicBlock()) {
        return i;
      }
      pVisited.add(clause);
    }
    return pClauses.size(); // if the rest is atomic block, return last index (exclusive)
  }

  private static ImmutableList<SeqThreadStatementBlock> collectStatementBlocks(
      int pFrom, // inclusive
      int pTo, // exclusive
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementBlock> rCollected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses.subList(pFrom, pTo)) {
      assert clause.block instanceof SeqThreadStatementBlock;
      rCollected.add((SeqThreadStatementBlock) clause.block);
    }
    return rCollected.build();
  }
}
