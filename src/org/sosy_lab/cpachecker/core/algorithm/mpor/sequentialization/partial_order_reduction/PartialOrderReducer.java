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

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concatenateCommutingClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConcatenated.put(entry.getKey(), concatenateCommutingClauses(entry.getValue()));
    }
    return rConcatenated.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> concatenateCommutingClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseClause> rNewCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<SeqCaseClause> concatenatedCases = new HashSet<>();
    for (SeqCaseClause caseClause : pCaseClauses) {
      // do not search for already concatenated clauses
      if (concatenatedCases.add(caseClause)) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          Optional<Integer> intTargetPc = SeqCaseClauseUtil.tryExtractIntTargetPc(statement);
          if (intTargetPc.isPresent()) {
            int targetPc = intTargetPc.orElseThrow();
            if (targetPc != Sequentialization.EXIT_PC) {
              SeqCaseClause caseTarget =
                  Objects.requireNonNull(labelValueMap.get(intTargetPc.orElseThrow()));
              newStatements.add(
                  recursivelyConcatenateStatements(
                      statement, caseTarget, concatenatedCases, labelValueMap));
            }
          }
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build(), Terminator.CONTINUE);
        rNewCaseClauses.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    return rNewCaseClauses.build();
  }

  private static SeqCaseBlockStatement recursivelyConcatenateStatements(
      SeqCaseBlockStatement pCurrentStatement,
      SeqCaseClause pCurrentTarget,
      Set<SeqCaseClause> pConcatenated,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap)
      throws UnrecognizedCodeException {

    // ensure we only process each case clause once
    if (pConcatenated.add(pCurrentTarget)) {
      ImmutableList.Builder<SeqCaseBlockStatement> collectedStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : pCurrentTarget.block.statements) {
        Optional<Integer> intTargetPc = SeqCaseClauseUtil.tryExtractIntTargetPc(statement);
        if (intTargetPc.isPresent()) {
          int targetPc = intTargetPc.get();
          if (targetPc != Sequentialization.EXIT_PC) {
            SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
            if (validConcatenation(statement, newTarget)) {
              // recursively collect statements, replacing original to avoid duplication
              SeqCaseBlockStatement recursiveResult =
                  recursivelyConcatenateStatements(
                      statement, newTarget, pConcatenated, pLabelValueMap);
              collectedStatements.add(recursiveResult);
              continue; // avoid adding statement again below
            }
          }
        }
        collectedStatements.add(statement); // only add if not replaced in recursion
      }
      try {
        // clone with concatenated statements, ensuring no duplicates
        return pCurrentStatement.cloneWithConcatenatedStatements(collectedStatements.build());
      } catch (UnsupportedOperationException ignored) {
        // some statements cannot be cloned with concatenated statements (e.g. return pc write)
      }
    }
    return pCurrentStatement;
  }

  /**
   * Checks if {@code pStatement} and {@code pTarget} can be concatenated. No concatenation when:
   *
   * <ul>
   *   <li>{@code pTarget} contains {@code pStatement} as a statement in its {@link SeqCaseBlock}
   *       (to prevent infinite loops when recursively concatenating)
   *   <li>{@code pTarget} is global (its commutativity is not guaranteed)
   *   <li>{@code pTarget} is a loop head (it must be directly reachable)
   *   <li>{@code pTarget} is not guaranteed to update a {@code pc}, e.g. {@code pthread_mutex_lock}
   *       (must be directly reachable to continue simulation when thread halts)
   * </ul>
   */
  private static boolean validConcatenation(
      SeqCaseBlockStatement pStatement, SeqCaseClause pTarget) {

    // TODO optimize by adding traces and checking if there is at least one global access
    return !(pTarget.block.statements.contains(pStatement)
        || pTarget.isGlobal
        || pTarget.isLoopStart
        || !pTarget.alwaysUpdatesPc());
  }
}
