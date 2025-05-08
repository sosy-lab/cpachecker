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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class StatementConcatenator {

  /** Concatenates commuting clauses by replacing {@code pc} writes with {@code goto} statements. */
  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> concat(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      // collect IDs of targets that result in valid concatenations.
      // these clauses must not be directly reachable, and their labels are pruned later
      ImmutableSet.Builder<Integer> clauseTargets = ImmutableSet.builder();
      ImmutableList<SeqThreadStatementClause> concatenatedCases =
          concatenateCommutingClausesWithGotos(entry.getValue(), clauseTargets);
      ImmutableList<SeqThreadStatementClause> merged =
          mergeNotDirectlyReachableStatements(clauseTargets.build(), concatenatedCases);
      rConcatenated.put(entry.getKey(), merged);
    }
    return rConcatenated.buildOrThrow();
  }

  // Inject Gotos ==================================================================================

  private static ImmutableList<SeqThreadStatementClause> concatenateCommutingClausesWithGotos(
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      ImmutableSet.Builder<Integer> pClauseTargets) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pCaseClauses);

    for (int i = 0; i < pCaseClauses.size(); i++) {
      SeqThreadStatementClause clause = pCaseClauses.get(i);
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(
            concatenateStatements(
                i == 0, clause.isGlobal, statement, labelValueMap, pClauseTargets));
      }
      rNewCaseClauses.add(clause.cloneWithBlockStatements(newStatements.build()));
    }
    return rNewCaseClauses.build();
  }

  /**
   * Concatenates the target statements of {@code pCurrentStatement}, if applicable i.e. if the
   * target statement is guaranteed to commute.
   *
   * @param pIsFirstConcat for the very first concat, we can concat global statements
   */
  private static SeqThreadStatement concatenateStatements(
      final boolean pIsFirstConcat,
      final boolean pIsGlobal,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap,
      ImmutableSet.Builder<Integer> pClauseTargets) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      if (validConcatenation(pIsFirstConcat, pIsGlobal, pCurrentStatement, newTarget)) {
        pClauseTargets.add(newTarget.id);
        return pCurrentStatement.cloneWithTargetGoto(newTarget.block.getGotoLabel());
      }
    }
    return pCurrentStatement;
  }

  // Helpers =======================================================================================

  /** Checks if {@code pStatement} and {@code pTarget} can be concatenated via {@code goto}. */
  private static boolean validConcatenation(
      final boolean pIsFirstConcat,
      final boolean pIsGlobal,
      SeqThreadStatement pStatement,
      SeqThreadStatementClause pTarget) {

    // enforce that the concatenation depends only on the target, not the statement
    /*checkArgument(
    pStatement.isConcatenable(),
    "pStatement of class %s is not concatenable",
    pStatement.getClass());*/
    return pStatement.isConcatenable()
        && !pTarget.isCriticalSectionStart()
        // do not concatenate atomic blocks, this is handled by AtomicBlockBuilder
        && !(pTarget.block.startsAtomicBlock() || pTarget.block.startsInAtomicBlock())
        // only consider global if not ignored
        && !(!canIgnoreGlobal(pTarget, pIsFirstConcat, pIsGlobal) && pTarget.isGlobal);
  }

  /**
   * Whether we can ignore the local/global property of {@code pCaseClause}, e.g. unlocks of global
   * mutexes.
   */
  private static boolean canIgnoreGlobal(
      SeqThreadStatementClause pCaseClause, boolean pIsFirstConcat, boolean pIsGlobal) {

    // global loop heads are not concat but must be directly reachable -> loops can be interleaved
    if (pCaseClause.isLoopStart) {
      return false;
    }
    // TODO only holds without pthread_mutex_trylock (i.e. unlock does not guarantee commute
    //  anymore) -> remove if adding pthread_mutex_trylock support
    if (pCaseClause.block.getFirstStatement() instanceof SeqMutexUnlockStatement) {
      return true;
    }
    // the first concatenation in the thread can also concatenate global, if it is not global itself
    return pIsFirstConcat && !pIsGlobal;
  }

  private static ImmutableList<SeqThreadStatementClause> mergeNotDirectlyReachableStatements(
      ImmutableSet<Integer> pCollectedTargetIds, ImmutableList<SeqThreadStatementClause> pClauses) {

    // use lists to add at list start
    List<SeqThreadStatementClause> rMerged = new ArrayList<>();
    List<SeqThreadStatementBlock> collected = new ArrayList<>();
    // in reverse, merge not directly reachable blocks to first directly reachable
    for (int i = pClauses.size() - 1; i >= 0; i--) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (pCollectedTargetIds.contains(clause.id)) {
        collected.add(0, clause.block);
      } else {
        SeqThreadStatementClause mergedClause =
            clause.cloneWithMergedBlocks(ImmutableList.copyOf(collected));
        rMerged.add(0, mergedClause);
        collected = new ArrayList<>();
      }
    }
    return ImmutableList.copyOf(rMerged);
  }
}
