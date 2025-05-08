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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqThreadLoopGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAccessEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqThreadStatementClauseUtil {

  public static ImmutableList<CVariableDeclaration> findGlobalVariablesInCaseClauseByReductionType(
      SeqThreadStatementClause pCaseClause, BitVectorReduction pReductionType) {

    return switch (pReductionType) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          findGlobalVariablesInCaseClauseByAccessType(pCaseClause, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CVariableDeclaration>builder()
              .addAll(
                  findGlobalVariablesInCaseClauseByAccessType(
                      pCaseClause, BitVectorAccessType.READ))
              .addAll(
                  findGlobalVariablesInCaseClauseByAccessType(
                      pCaseClause, BitVectorAccessType.WRITE))
              .build();
    };
  }

  public static ImmutableList<CVariableDeclaration> findGlobalVariablesInCaseClauseByAccessType(
      SeqThreadStatementClause pCaseClause, BitVectorAccessType pAccessType) {

    ImmutableList.Builder<CVariableDeclaration> rGlobalVariables = ImmutableList.builder();
    for (SeqThreadStatement statement : pCaseClause.block.getStatements()) {
      rGlobalVariables.addAll(
          recursivelyFindGlobalVariablesByAccessType(
              ImmutableList.builder(), statement, pAccessType));
    }
    return rGlobalVariables.build();
  }

  /**
   * Searches {@code pStatement} and all concatenated statements for their global variables based on
   * {@code pAccessType}.
   */
  private static ImmutableList<CVariableDeclaration> recursivelyFindGlobalVariablesByAccessType(
      ImmutableList.Builder<CVariableDeclaration> pFound,
      SeqThreadStatement pStatement,
      BitVectorAccessType pAccessType) {

    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      for (CVariableDeclaration variable :
          substituteEdge.getGlobalVariablesByAccessType(pAccessType)) {
        assert variable.isGlobal();
        pFound.add(variable);
      }
    }
    return pFound.build();
  }

  /**
   * Searches for all target {@code pc} in {@code pStatement}, including concatenated statements.
   */
  public static ImmutableSet<Integer> collectAllIntegerTargetPc(SeqThreadStatement pStatement) {
    ImmutableSet.Builder<Integer> rAllTargetPc = ImmutableSet.builder();
    if (pStatement.getTargetPc().isPresent()) {
      // add the direct target pc, if present
      rAllTargetPc.add(pStatement.getTargetPc().orElseThrow());
    }
    return rAllTargetPc.build();
  }

  /**
   * A helper mapping {@link SeqThreadStatementClause}s to their label values which are always
   * {@code int} values in the sequentialization.
   */
  public static ImmutableMap<Integer, SeqThreadStatementClause> mapLabelNumberToClause(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.labelNumber, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /**
   * Ensures that all {@code int} labels in {@code pCaseClauses} are numbered consecutively, i.e.
   * the numbers {@code 0} to {@code pCaseClauses.size() - 1} are present (no gaps).
   *
   * <p>This function also recursively searches for all target {@code pc} and adjusts them
   * accordingly.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>>
      cloneWithConsecutiveLabels(
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rConsecutiveLabels =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConsecutiveLabels.put(entry.getKey(), cloneWithConsecutiveLabels(entry.getValue()));
    }
    return rConsecutiveLabels.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> cloneWithConsecutiveLabels(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rConsecutiveLabels = ImmutableList.builder();
    ImmutableMap<Integer, Integer> labelToIndexMap = mapLabelNumberToIndex(pCaseClauses);
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : caseClause.block.getStatements()) {
        newStatements.add(replaceTargetPc(statement, labelToIndexMap));
      }
      ImmutableList.Builder<SeqStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqStatementBlock mergedBlock : caseClause.mergedBlocks) {
        ImmutableList.Builder<SeqThreadStatement> newMergedStatements = ImmutableList.builder();
        for (SeqThreadStatement mergedStatement : mergedBlock.getStatements()) {
          newMergedStatements.add(replaceTargetPc(mergedStatement, labelToIndexMap));
        }
        int mergeBlockIndex =
            Objects.requireNonNull(labelToIndexMap.get(mergedBlock.getGotoLabel().labelNumber));
        newMergedBlocks.add(
            mergedBlock.cloneWithLabelAndStatements(mergeBlockIndex, newMergedStatements.build()));
      }
      int index = Objects.requireNonNull(labelToIndexMap.get(caseClause.labelNumber));
      rConsecutiveLabels.add(
          caseClause.cloneWithLabelAndBlockStatementsAndMergedBlocks(
              index, newStatements.build(), newMergedBlocks.build()));
    }
    return rConsecutiveLabels.build();
  }

  private static ImmutableMap<Integer, Integer> mapLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = 0;
    for (SeqThreadStatementClause clause : pCaseClauses) {
      rLabelToIndex.put(clause.labelNumber, index++);
      for (SeqStatementBlock mergedBlock : clause.mergedBlocks) {
        rLabelToIndex.put(mergedBlock.getGotoLabel().labelNumber, index++);
      }
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static SeqThreadStatement replaceTargetPc(
      SeqThreadStatement pCurrentStatement, final ImmutableMap<Integer, Integer> pLabelToIndexMap) {

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        int index = Objects.requireNonNull(pLabelToIndexMap.get(targetPc));
        return pCurrentStatement.cloneWithTargetPc(index);
      }
    } else if (pCurrentStatement.getTargetGoto().isPresent()) {
      SeqBlockGotoLabelStatement label = pCurrentStatement.getTargetGoto().orElseThrow();
      if (!pLabelToIndexMap.containsKey(label.labelNumber)) {
        System.out.println(pLabelToIndexMap);
        System.out.println("trying to find " + label.labelNumber);
      }
      int index = Objects.requireNonNull(pLabelToIndexMap.get(label.labelNumber));
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
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
        // add previous injections BEFORE (otherwise undefined behavior in seq!)
        newInjections.addAll(pCurrentStatement.getInjectedStatements());
        SeqThreadStatementClause target = pLabelValueMap.get(targetPc);
        newInjections.add(
            new SeqThreadLoopGotoStatement(
                pIterationSmallerMax,
                // for statements targeting starts of critical sections, assumes are reevaluated
                priorCriticalSection(pCurrentStatement)
                    ? pAssumeLabel
                    : Objects.requireNonNull(target).block.getGotoLabel()));
        return pCurrentStatement.cloneWithInjectedStatements(newInjections.build());
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  public static boolean priorCriticalSection(SeqThreadStatement pStatement) {
    return pStatement.getInjectedStatements().stream().anyMatch(i -> i.priorCriticalSection());
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
