// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqThreadLoopGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAccessEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqThreadStatementClauseUtil {

  /** Searches for all target {@code pc} in {@code pStatement}. */
  public static ImmutableSet<Integer> collectAllIntegerTargetPc(SeqThreadStatement pStatement) {
    ImmutableSet.Builder<Integer> rAllTargetPc = ImmutableSet.builder();
    if (pStatement.getTargetPc().isPresent()) {
      // add the direct target pc, if present
      rAllTargetPc.add(pStatement.getTargetPc().orElseThrow());
    }
    return rAllTargetPc.build();
  }

  /**
   * Maps the first {@link SeqThreadStatementBlock} in each {@link SeqThreadStatementClause}s to
   * their label number.
   */
  public static ImmutableMap<Integer, SeqThreadStatementClause> mapLabelNumberToClause(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause caseClause : pClauses) {
      rOriginPcs.put(caseClause.labelNumber, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /** Maps {@link SeqThreadStatementBlock}s to their label numbers. */
  public static ImmutableMap<Integer, SeqThreadStatementBlock> mapLabelNumberToBlock(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementBlock> rMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getAllBlocks()) {
        rMap.put(block.getGotoLabel().labelNumber, block);
      }
    }
    return rMap.buildOrThrow();
  }

  /**
   * Ensures that all {@code int} labels in {@code pCaseClauses} are numbered consecutively, i.e.
   * the numbers {@code 0} to {@code pCaseClauses.size() - 1} are present (no gaps).
   *
   * <p>This function also recursively searches for all target {@code pc} and adjusts them
   * accordingly.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>>
      cloneWithConsecutiveLabelNumbers(
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rConsecutiveLabels =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConsecutiveLabels.put(entry.getKey(), cloneWithConsecutiveLabelNumbers(entry.getValue()));
    }
    return rConsecutiveLabels.buildOrThrow();
  }

  // Including Blocks ==============================================================================

  private static ImmutableList<SeqThreadStatementClause> cloneWithConsecutiveLabelNumbers(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewClauses = ImmutableList.builder();
    ImmutableMap<Integer, Integer> labelBlockMap = mapBlockLabelNumberToIndex(pClauses);
    ImmutableMap<Integer, Integer> labelClauseMap = mapClauseLabelNumberToIndex(pClauses);
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(replaceTargetPc(statement, labelBlockMap, labelClauseMap));
      }
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        ImmutableList.Builder<SeqThreadStatement> newMergedStatements = ImmutableList.builder();
        for (SeqThreadStatement mergedStatement : mergedBlock.getStatements()) {
          newMergedStatements.add(replaceTargetPc(mergedStatement, labelBlockMap, labelClauseMap));
        }
        int mergeBlockIndex =
            Objects.requireNonNull(labelBlockMap.get(mergedBlock.getGotoLabel().labelNumber));
        newMergedBlocks.add(
            mergedBlock
                .cloneWithLabelNumber(mergeBlockIndex)
                .cloneWithStatements(newMergedStatements.build()));
      }
      int blockIndex = Objects.requireNonNull(labelBlockMap.get(clause.labelNumber));
      SeqThreadStatementBlock newBlock =
          clause.block.cloneWithLabelNumber(blockIndex).cloneWithStatements(newStatements.build());
      int clauseIndex = Objects.requireNonNull(labelClauseMap.get(clause.labelNumber));
      rNewClauses.add(
          clause
              .cloneWithLabelNumber(clauseIndex)
              .cloneWithBlock(newBlock)
              .cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return rNewClauses.build();
  }

  private static ImmutableMap<Integer, Integer> mapBlockLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = 0;
    for (SeqThreadStatementClause clause : pCaseClauses) {
      rLabelToIndex.put(clause.block.getGotoLabel().labelNumber, index++);
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        rLabelToIndex.put(mergedBlock.getGotoLabel().labelNumber, index++);
      }
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static ImmutableMap<Integer, Integer> mapClauseLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = 0;
    for (SeqThreadStatementClause clause : pCaseClauses) {
      rLabelToIndex.put(clause.labelNumber, index++);
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static SeqThreadStatement replaceTargetPc(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, Integer> pLabelBlockMap,
      final ImmutableMap<Integer, Integer> pLabelClauseMap) {

    if (isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // for pc writes, use clause labels
      int index = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      return pCurrentStatement.cloneWithTargetPc(index);

    } else if (pCurrentStatement.getTargetGoto().isPresent()) {
      SeqBlockGotoLabelStatement label = pCurrentStatement.getTargetGoto().orElseThrow();
      // for gotos, use block labels
      int index = Objects.requireNonNull(pLabelBlockMap.get(label.labelNumber));
      return pCurrentStatement.cloneWithTargetGoto(label.cloneWithLabelNumber(index));
    }
    // no target pc or target goto -> no replacement
    return pCurrentStatement;
  }

  /**
   * Searches {@code pInjectedStatements} for {@link SeqBitVectorAccessEvaluationStatement}s and
   * replaces their {@code goto} labels with the updated {@code pc}.
   */
  public static ImmutableList<SeqInjectedStatement> replaceTargetGotoLabel(
      ImmutableList<SeqInjectedStatement> pInjectedStatements, int pNewTargetPc) {

    ImmutableList.Builder<SeqInjectedStatement> rNewInjected = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorEvaluation) {
        rNewInjected.add(bitVectorEvaluation.cloneWithGotoLabelNumber(pNewTargetPc));
      } else {
        rNewInjected.add(injectedStatement);
      }
    }
    return rNewInjected.build();
  }

  public static SeqThreadStatement recursivelyInjectGotoThreadLoopLabels(
      CBinaryExpression pIterationSmallerMax,
      SeqThreadLoopLabelStatement pAssumeLabel,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap) {

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
        // add previous injections BEFORE (otherwise undefined behavior in seq!)
        newInjections.addAll(pCurrentStatement.getInjectedStatements());
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelValueMap.get(targetPc));
        newInjections.add(
            new SeqThreadLoopGotoStatement(
                pIterationSmallerMax,
                // for statements targeting starts of critical sections, assumes are reevaluated
                target.requiresAssumeEvaluation()
                    ? pAssumeLabel
                    : Objects.requireNonNull(target).block.getGotoLabel()));
        return pCurrentStatement.cloneWithInjectedStatements(newInjections.build());
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  public static boolean isValidTargetPc(Optional<Integer> pTargetPc) {
    if (pTargetPc.isPresent()) {
      int targetPc = pTargetPc.orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        return true;
      }
    }
    return false;
  }

  // Path ==========================================================================================

  /** Returns {@code true} if the path from A to B has consecutive labels. */
  public static boolean isConsecutiveLabelPath(
      SeqThreadStatementClause pCurrent,
      final SeqThreadStatementClause pTarget,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelCaseMap) {

    if (pCurrent.equals(pTarget)) {
      return true;
    } else {
      SeqThreadStatement firstStatement = pCurrent.block.getFirstStatement();
      SeqThreadStatementClause next = pLabelCaseMap.get(firstStatement.getTargetPc().orElseThrow());
      assert next != null : "could not find target case clause";
      if (pCurrent.labelNumber + 1 == next.labelNumber) {
        return isConsecutiveLabelPath(next, pTarget, pLabelCaseMap);
      } else {
        return false;
      }
    }
  }
}
