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

public class StatementLinker {

  /** Links commuting clauses by replacing {@code pc} writes with {@code goto} statements. */
  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> link(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rLinked =
        ImmutableMap.builder();
    for (var entry : pClauses.entrySet()) {
      // collect IDs of targets that result in valid links.
      // these clauses must not be directly reachable, and their labels are pruned later
      ImmutableSet.Builder<Integer> linkedTargetIds = ImmutableSet.builder();
      ImmutableList<SeqThreadStatementClause> linkedClauses =
          linkCommutingClausesWithGotos(entry.getValue(), linkedTargetIds);
      ImmutableList<SeqThreadStatementClause> merged =
          mergeNotDirectlyReachableStatements(linkedClauses, linkedTargetIds.build());
      rLinked.put(entry.getKey(), merged);
    }
    return rLinked.buildOrThrow();
  }

  // Inject Gotos ==================================================================================

  private static ImmutableList<SeqThreadStatementClause> linkCommutingClausesWithGotos(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableSet.Builder<Integer> pLinkedTargetIds) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);

    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(linkStatements(statement, pLinkedTargetIds, labelClauseMap));
      }
      rNewClauses.add(
          clause.cloneWithBlock(clause.block.cloneWithStatements(newStatements.build())));
    }
    return rNewClauses.build();
  }

  /**
   * Links the target statements of {@code pCurrentStatement}, if applicable i.e. if the target
   * statement is guaranteed to commute.
   */
  private static SeqThreadStatement linkStatements(
      SeqThreadStatement pCurrentStatement,
      ImmutableSet.Builder<Integer> pLinkedTargetIds,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      if (validLink(pCurrentStatement, newTarget)) {
        pLinkedTargetIds.add(newTarget.id);
        return pCurrentStatement.cloneWithTargetGoto(newTarget.block.getGotoLabel());
      }
    }
    return pCurrentStatement;
  }

  // Helpers =======================================================================================

  /** Checks if {@code pStatement} and {@code pTarget} can be linked via {@code goto}. */
  private static boolean validLink(
      SeqThreadStatement pStatement, SeqThreadStatementClause pTarget) {

    return pStatement.isLinkable()
        // do not link atomic blocks, this is handled by AtomicBlockMerger
        && !(pTarget.block.startsAtomicBlock() || pTarget.block.startsInAtomicBlock())
        // only consider global if not ignored
        && !(!canIgnoreGlobal(pTarget) && pTarget.isGlobal);
  }

  /**
   * Whether we can ignore the local/global property of {@code pClause}, e.g. unlocks of global
   * mutexes.
   */
  private static boolean canIgnoreGlobal(SeqThreadStatementClause pTarget) {
    // TODO only holds without pthread_mutex_trylock (i.e. unlock does not guarantee commute
    //  anymore) -> remove if adding pthread_mutex_trylock support
    if (pTarget.block.getFirstStatement() instanceof SeqMutexUnlockStatement) {
      return true;
    }
    return false;
  }

  private static ImmutableList<SeqThreadStatementClause> mergeNotDirectlyReachableStatements(
      ImmutableList<SeqThreadStatementClause> pClauses, ImmutableSet<Integer> pLinkedTargetIds) {

    // use lists to add at list start
    List<SeqThreadStatementClause> rMerged = new ArrayList<>();
    List<SeqThreadStatementBlock> temporary = new ArrayList<>();
    // in reverse, merge not directly reachable blocks to first directly reachable
    for (int i = pClauses.size() - 1; i >= 0; i--) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (clause.block.startsAtomicBlock()) {
        // atomic blocks are merged by AtomicBlockMerger already, add as is
        rMerged.add(0, clause);
      } else {
        // if i == 0, then add clause even if it is not directly reachable, otherwise it is pruned
        if (i != 0 && pLinkedTargetIds.contains(clause.id)) {
          temporary.add(0, clause.block);
          temporary.addAll(0, clause.mergedBlocks);
        } else {
          // still need all mergedBlocks here
          temporary.addAll(0, clause.mergedBlocks);
          SeqThreadStatementClause mergedClause =
              clause.cloneWithMergedBlocks(ImmutableList.copyOf(temporary));
          rMerged.add(0, mergedClause);
          temporary.clear();
        }
      }
    }
    return ImmutableList.copyOf(rMerged);
  }
}
