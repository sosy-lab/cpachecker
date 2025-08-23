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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class StatementLinker {

  /** Links commuting clauses by replacing {@code pc} writes with {@code goto} statements. */
  protected static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> link(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      PointerAssignments pPointerAssignments) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rLinked =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      // collect IDs of targets that result in valid links.
      // these clauses must not be directly reachable, and their labels are pruned later
      ImmutableSet.Builder<Integer> linkedTargetIds = ImmutableSet.builder();
      ImmutableList<SeqThreadStatementClause> linkedClauses =
          linkCommutingClausesWithGotos(
              pOptions, pClauses.get(thread), pPointerAssignments, linkedTargetIds);
      ImmutableList<SeqThreadStatementClause> merged =
          mergeNotDirectlyReachableStatements(linkedClauses, linkedTargetIds.build());
      rLinked.putAll(thread, merged);
    }
    return rLinked.build();
  }

  // Inject Gotos ==================================================================================

  private static ImmutableList<SeqThreadStatementClause> linkCommutingClausesWithGotos(
      MPOROptions pOptions,
      ImmutableList<SeqThreadStatementClause> pClauses,
      PointerAssignments pPointerAssignments,
      ImmutableSet.Builder<Integer> pLinkedTargetIds) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);

    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : block.getStatements()) {
          newStatements.add(
              linkStatements(
                  pOptions,
                  statement,
                  pLinkedTargetIds,
                  labelClauseMap,
                  labelBlockMap,
                  pPointerAssignments));
        }
        newBlocks.add(block.cloneWithStatements(newStatements.build()));
      }
      rNewClauses.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rNewClauses.build();
  }

  /**
   * Links the target statements of {@code pCurrentStatement}, if applicable i.e. if the target
   * statement is guaranteed to commute.
   */
  private static SeqThreadStatement linkStatements(
      MPOROptions pOptions,
      SeqThreadStatement pCurrentStatement,
      ImmutableSet.Builder<Integer> pLinkedTargetIds,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      PointerAssignments pPointerAssignments) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      if (isValidLink(
          pOptions, pCurrentStatement, newTarget, pLabelBlockMap, pPointerAssignments)) {
        pLinkedTargetIds.add(newTarget.id);
        return pCurrentStatement.cloneWithTargetGoto(newTarget.getFirstBlock().getLabel());
      }
    }
    return pCurrentStatement;
  }

  // Helpers =======================================================================================

  /** Checks if {@code pStatement} and {@code pTarget} can be linked via {@code goto}. */
  private static boolean isValidLink(
      MPOROptions pOptions,
      SeqThreadStatement pStatement,
      SeqThreadStatementClause pTarget,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      PointerAssignments pPointerAssignments) {

    SeqThreadStatementBlock targetBlock = pTarget.getFirstBlock();
    return pStatement.isLinkable()
        // if the target is a loop start, then backward loop goto must be enabled for linking
        && (!targetBlock.isLoopStart() || !pOptions.noBackwardLoopGoto)
        // do not link atomic blocks, this is handled by AtomicBlockMerger
        && !(targetBlock.startsAtomicBlock() || targetBlock.startsInAtomicBlock())
        // thread synchronization statements must be directly reachable (via pc) -> no linking
        && !SeqThreadStatementUtil.anySynchronizesThreads(pTarget.getAllStatements())
        // only consider global accesses if not ignored
        && !(!canIgnoreGlobal(pTarget)
            && GlobalVariableFinder.hasGlobalAccess(
                pLabelBlockMap, pPointerAssignments, targetBlock));
  }

  /**
   * Whether we can ignore the local/global property of {@code pClause}, e.g. unlocks of global
   * mutexes.
   */
  private static boolean canIgnoreGlobal(SeqThreadStatementClause pTarget) {
    // TODO there should be more checks, this only holds if the only global access is the mutex,
    //  what if there are other global accesses?
    // TODO only holds without pthread_mutex_trylock (i.e. unlock does not guarantee commute
    //  anymore) -> remove if adding pthread_mutex_trylock support
    if (pTarget.getFirstBlock().getFirstStatement() instanceof SeqMutexUnlockStatement) {
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
      // if i == 0, then add clause even if it is not directly reachable, otherwise it is pruned
      if (i != 0 && pLinkedTargetIds.contains(clause.id)) {
        temporary.addAll(0, clause.getBlocks());
      } else {
        // still need all mergedBlocks here
        temporary.addAll(0, clause.getMergedBlocks());
        SeqThreadStatementClause mergedClause =
            clause.cloneWithMergedBlocks(ImmutableList.copyOf(temporary));
        rMerged.addFirst(mergedClause);
        temporary.clear();
      }
    }
    return ImmutableList.copyOf(rMerged);
  }
}
