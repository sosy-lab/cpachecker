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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class PartialOrderReducer {

  // TODO add bit vectors for each thread store which global variables (up to 128 supported) are
  //  accessed in the next case

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concatenateCommutingCases(
      MPOROptions pOptions, ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      // map label pc to loop head labels created at their start, used later for injecting gotos
      Map<Integer, SeqLoopHeadLabelStatement> loopHeadLabels = new HashMap<>();
      ImmutableList<SeqCaseClause> concatenatedCases =
          concatenateCommutingCases(pOptions, entry.getKey(), entry.getValue(), loopHeadLabels);
      ImmutableList<SeqCaseClause> withGoto =
          replaceLoopHeadTargetPcWithGoto(ImmutableMap.copyOf(loopHeadLabels), concatenatedCases);
      rConcatenated.put(entry.getKey(), withGoto);
    }
    return rConcatenated.buildOrThrow();
  }

  // Concatenation =================================================================================

  private static ImmutableList<SeqCaseClause> concatenateCommutingCases(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<SeqCaseClause> pCaseClauses,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels) {

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
              recursivelyConcatenateStatements(
                  pOptions,
                  pThread,
                  statement,
                  concatenated,
                  duplicated,
                  pLoopHeadLabels,
                  labelValueMap));
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build());
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
      final MPOROptions pOptions,
      final MPORThread pThread,
      SeqCaseBlockStatement pCurrentStatement,
      Set<Integer> pConcatenated,
      Set<Integer> pDuplicated,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (validIntTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      if (validConcatenation(pCurrentStatement, newTarget, pLoopHeadLabels)) {
        // if the target id was seen before, add it to duplicate
        if (!pConcatenated.add(newTarget.id)) {
          pDuplicated.add(newTarget.id);
        }
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement :
            injectLoopHeadLabel(pOptions, pThread, newTarget, pLoopHeadLabels)) {
          newStatements.add(
              recursivelyConcatenateStatements(
                  pOptions,
                  pThread,
                  statement,
                  pConcatenated,
                  pDuplicated,
                  pLoopHeadLabels,
                  pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    return pCurrentStatement;
  }

  /** Checks if {@code pStatement} and {@code pTarget} can be concatenated. */
  private static boolean validConcatenation(
      SeqCaseBlockStatement pStatement,
      SeqCaseClause pTarget,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels) {

    // TODO optimize by adding traces and checking if there is at least one global access.
    //  do this by still concatenating globals if it is the first global found.
    //  however, first globals that are loop starts can not be concat, they must be reachable as pcs
    //  so that we can interleave the loops.

    return pStatement.isConcatenable()
        // label injected before -> return to loop head, no concat to prevent infinite loop
        && !pLoopHeadLabels.containsKey(pTarget.label.value)
        // these are sorted by performance impact in descending order for short circuit evaluation
        && !((!canIgnoreGlobal(pTarget) && pTarget.isGlobal) // only consider global if not ignored
            || !pTarget.isCriticalSectionStart()
            || pTarget.block.statements.contains(pStatement));
  }

  // TODO only holds without pthread_mutex_trylock (i.e. unlock does not guarantee commute anymore)
  //  -> remove if adding pthread_mutex_trylock support
  /**
   * Whether we can ignore the local/global property of {@code pCaseClause}, e.g. unlocks of global
   * mutexes.
   */
  private static boolean canIgnoreGlobal(SeqCaseClause pCaseClause) {
    return pCaseClause.block.getFirstStatement() instanceof SeqMutexUnlockStatement;
  }

  /**
   * When concatenating a {@link SeqCaseClause} that is a loop head, we inject {@code label}s so
   * that these statements remain directly reachable via {@code goto}.
   */
  private static ImmutableList<SeqCaseBlockStatement> injectLoopHeadLabel(
      MPOROptions pOptions,
      MPORThread pThread,
      SeqCaseClause pCaseClause,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    // for non-loop head, return statements as they are.
    if (!pCaseClause.isLoopStart) {
      return pCaseClause.block.statements;
    }

    // create loop head label for the first assume statement (= if)
    SeqCaseBlockStatement firstStatement = pCaseClause.block.getFirstStatement();
    assert firstStatement instanceof SeqAssumeStatement
        : "first statement of loop head must be assume";
    String labelName = SeqNameUtil.buildLoopHeadLabelName(pOptions, pThread.id);
    SeqLoopHeadLabelStatement loopHeadLabel = new SeqLoopHeadLabelStatement(labelName);
    pLoopHeads.put(pCaseClause.label.value, loopHeadLabel);
    SeqAssumeStatement cloneWithLabel =
        ((SeqAssumeStatement) firstStatement).cloneWithLoopHeadLabel(loopHeadLabel);

    // inject cloned statement with loop head label
    ImmutableList.Builder<SeqCaseBlockStatement> rWithLabel = ImmutableList.builder();
    rWithLabel.add(cloneWithLabel);
    for (SeqCaseBlockStatement statement : pCaseClause.block.statements) {
      // add other statements as they were
      if (!statement.equals(firstStatement)) {
        rWithLabel.add(statement);
      }
    }
    return rWithLabel.build();
  }

  // Loop Heads ====================================================================================

  private static ImmutableList<SeqCaseClause> replaceLoopHeadTargetPcWithGoto(
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels,
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rWithGoto = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(SeqCaseClauseUtil.replaceTargetPcWithGoto(statement, pLoopHeadLabels));
      }
      SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build());
      rWithGoto.add(caseClause.cloneWithBlock(newBlock));
    }
    return rWithGoto.build();
  }

  // Helpers =======================================================================================

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
