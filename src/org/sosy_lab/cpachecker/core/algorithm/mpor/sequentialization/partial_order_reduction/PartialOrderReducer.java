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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class PartialOrderReducer {

  // TODO
  //  stack-2
  //  stack-longest-1
  //  stack-1

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concatenateCommutingCases(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConcatenated.put(entry.getKey(), concatenateCommutingCases(entry.getValue()));
    }
    return rConcatenated.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> concatenateCommutingCases(
      ImmutableList<SeqCaseClause> pCaseClauses) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseClause> rNewCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<SeqCaseClause> concatenated = new HashSet<>();
    for (SeqCaseClause caseClause : pCaseClauses) {
      // prevent start in already concatenated clauses, otherwise they are duplicated
      if (concatenated.add(caseClause)) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(statement, concatenated, labelValueMap));
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build(), Terminator.CONTINUE);
        rNewCaseClauses.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    return rNewCaseClauses.build();
  }

  private static SeqCaseBlockStatement recursivelyConcatenateStatements(
      SeqCaseBlockStatement pCurrentStatement,
      Set<SeqCaseClause> pConcatenated,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap)
      throws UnrecognizedCodeException {

    Optional<Integer> intTargetPc = SeqCaseClauseUtil.tryExtractIntTargetPc(pCurrentStatement);
    if (validIntTargetPc(intTargetPc)) {
      int targetPc = intTargetPc.orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      if (validConcatenation(pCurrentStatement, newTarget)) {
        pConcatenated.add(newTarget);
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement targetStatement : newTarget.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(targetStatement, pConcatenated, pLabelValueMap));
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

    // TODO think about: must function entries be directly reachable? what if there are two calling
    //  contexts, one can concatenate, the other can't?

    // TODO currently, we do not concatenate if a return_pc is assigned due to the target being
    //  nondet. it would be best to clone functions and rid of all return_pc, including switching
    //  over calling contexts to assign return values. additionally, if we check for parameter vars
    //  we can actually find out if the assigned value is global or local -> less interleavings

    return pStatement.isConcatenable()
        // these are sorted by performance impact in descending order for short circuit evaluation
        && !(pTarget.isGlobal
            || pTarget.isLoopStart
            || !pTarget.alwaysUpdatesPc()
            || pTarget.block.statements.contains(pStatement));
  }

  private static boolean validIntTargetPc(Optional<Integer> pIntTargetPc) {
    if (pIntTargetPc.isPresent()) {
      int targetPc = pIntTargetPc.orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        return true;
      }
    }
    return false;
  }
}
