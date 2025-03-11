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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class PartialOrderReducer {

  // TODO add bit vectors for each thread store which global variables (up to 128 supported) are
  //  accessed in the next case

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concatenateCommutingCases(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConcatenated.put(entry.getKey(), concatenateCommutingCases(entry.getValue()));
    }
    return rConcatenated.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> concatenateCommutingCases(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> newCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<Integer> concatenated = new HashSet<>();
    Set<Integer> duplicated = new HashSet<>();
    for (SeqCaseClause caseClause : pCaseClauses) {
      // prevent start in already concatenated clauses, otherwise they are duplicated
      if (concatenated.add(caseClause.id)) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(statement, concatenated, duplicated, labelValueMap));
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build(), Terminator.CONTINUE);
        SeqCaseClause clone = caseClause.cloneWithBlock(newBlock);
        newCaseClauses.add(clone);
      }
    }
    ImmutableList<SeqCaseClause> rNewCaseClauses = newCaseClauses.build();
    // we filter out case clauses that were visited twice during concatenation
    return rNewCaseClauses.stream()
        .filter(
            caseClause ->
                concatenated.contains(caseClause.id) && !duplicated.contains(caseClause.id))
        .collect(ImmutableList.toImmutableList());
  }

  private static SeqCaseBlockStatement recursivelyConcatenateStatements(
      SeqCaseBlockStatement pCurrentStatement,
      Set<Integer> pConcatenated,
      Set<Integer> pDuplicated,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (validIntTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      if (validConcatenation(pCurrentStatement, newTarget)) {
        // if the target id was seen before, add it to duplicate
        if (!pConcatenated.add(newTarget.id)) {
          pDuplicated.add(newTarget.id);
        }
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement targetStatement : newTarget.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(
                  targetStatement, pConcatenated, pDuplicated, pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    return pCurrentStatement;
  }

  /**
   * Checks if {@code pStatement} and {@code pTarget} can be concatenated. No concatenation when:
   *
   * <ul>
   *   <li>{@code pTarget} contains {@code pStatement} as a statement in its {@link SeqCaseBlock}
   *       (to prevent loops when recursively concatenating)
   *   <li>{@code pTarget} is global (its commutativity is not guaranteed)
   *   <li>{@code pTarget} is a loop head (it must be directly reachable)
   *   <li>{@code pTarget} is not guaranteed to update a {@code pc}, e.g. {@code pthread_mutex_lock}
   *       (must be directly reachable to continue simulation when thread halts)
   * </ul>
   */
  private static boolean validConcatenation(
      SeqCaseBlockStatement pStatement, SeqCaseClause pTarget) {

    // TODO optimize by adding traces and checking if there is at least one global access

    // TODO think about: do mutex unlocks always commute, even to global mutex objects?
    //  technically no if we also consider stuff like pthread_mutex_trylock != 0 etc.

    return pStatement.isConcatenable()
        // these are sorted by performance impact in descending order for short circuit evaluation
        && !(pTarget.isGlobal
            // TODO add support for loop heads by adding goto loop_heads
            || pTarget.isLoopStart
            || !pTarget.alwaysUpdatesPc()
            || pTarget.block.statements.contains(pStatement));
  }

  private static boolean validIntTargetPc(Optional<Integer> pTargetPc) {
    if (pTargetPc.isPresent()) {
      int targetPc = pTargetPc.orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        return true;
      }
    }
    return false;
  }
}
