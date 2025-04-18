// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqThreadLoopGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqCaseClauseUtil {

  public static <T extends SeqCaseBlockStatement> ImmutableSet<T> getAllStatementsByClass(
      ImmutableList<SeqCaseClause> pCaseClauses, Class<T> pStatementClass) {

    ImmutableSet.Builder<T> rAllStatements = ImmutableSet.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rAllStatements.addAll(getAllStatementsByClass(caseClause, pStatementClass));
    }
    return rAllStatements.build();
  }

  public static <T extends SeqCaseBlockStatement> ImmutableSet<T> getAllStatementsByClass(
      SeqCaseClause pCaseClause, Class<T> pStatementClass) {

    ImmutableSet.Builder<T> rAllStatements = ImmutableSet.builder();
    for (SeqCaseBlockStatement statement : pCaseClause.block.statements) {
      if (pStatementClass.isInstance(statement)) {
        rAllStatements.addAll(getAllStatementsByClass(statement, pStatementClass));
      }
    }
    return rAllStatements.build();
  }

  /**
   * Searches all concatenated statements in {@code pStatement} for instances of {@code
   * pStatementClass}, including {@code pStatement} itself.
   */
  private static <T extends SeqCaseBlockStatement> ImmutableSet<T> getAllStatementsByClass(
      SeqCaseBlockStatement pStatement, final Class<T> pStatementClass) {

    ImmutableSet.Builder<T> rAllStatements = ImmutableSet.builder();
    if (pStatementClass.isInstance(pStatement)) {
      rAllStatements.add(pStatementClass.cast(pStatement));
    }
    if (pStatement.isConcatenable()) {
      ImmutableList<SeqCaseBlockStatement> concatStatements =
          pStatement.getConcatenatedStatements();
      for (SeqCaseBlockStatement concatStatement : concatStatements) {
        if (pStatementClass.isInstance(concatStatement)) {
          // recursively search for target pc in concatenated statements
          rAllStatements.addAll(getAllStatementsByClass(concatStatement, pStatementClass));
        }
      }
    }
    return rAllStatements.build();
  }

  // TODO make this return the set
  /** Searches {@code pStatement} and all concatenated statements for their global variables. */
  public static void recursivelyGetGlobalVariables(
      ImmutableSet.Builder<CVariableDeclaration> pFound, SeqCaseBlockStatement pStatement) {

    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      for (CVariableDeclaration variable : substituteEdge.getGlobalVariables()) {
        assert variable.isGlobal();
        pFound.add(variable);
      }
    }
    if (pStatement.isConcatenable()) {
      for (SeqCaseBlockStatement concatStatement : pStatement.getConcatenatedStatements()) {
        recursivelyGetGlobalVariables(pFound, concatStatement);
      }
    }
  }

  /**
   * Searches for all target {@code pc} in {@code pStatement}, including concatenated statements.
   */
  public static ImmutableSet<Integer> collectAllIntegerTargetPc(SeqCaseBlockStatement pStatement) {
    ImmutableSet.Builder<Integer> rAllTargetPc = ImmutableSet.builder();
    if (pStatement.getTargetPc().isPresent()) {
      // add the direct target pc, if present
      rAllTargetPc.add(pStatement.getTargetPc().orElseThrow());
    }
    if (pStatement.isConcatenable()) {
      ImmutableList<SeqCaseBlockStatement> concatStatements =
          pStatement.getConcatenatedStatements();
      for (SeqCaseBlockStatement concatStatement : concatStatements) {
        // recursively search for target pc in concatenated statements
        rAllTargetPc.addAll(collectAllIntegerTargetPc(concatStatement));
      }
    }
    return rAllTargetPc.build();
  }

  /**
   * A helper mapping {@link SeqCaseClause}s to their {@link SeqCaseLabel} values, which are always
   * {@code int} values in the sequentialization.
   */
  public static ImmutableMap<Integer, SeqCaseClause> mapCaseLabelValueToCaseClause(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqCaseClause> rOriginPcs = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.label.value, caseClause);
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
  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> cloneWithConsecutiveLabels(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConsecutiveLabels =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rConsecutiveLabels.put(entry.getKey(), cloneWithConsecutiveLabels(entry.getValue()));
    }
    return rConsecutiveLabels.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> cloneWithConsecutiveLabels(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rConsecutiveLabels = ImmutableList.builder();
    ImmutableMap<Integer, Integer> labelToIndexMap = mapLabelToIndex(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(replaceTargetPc(statement, labelToIndexMap));
      }
      int index = Objects.requireNonNull(labelToIndexMap.get(caseClause.label.value));
      SeqCaseLabel newLabel = new SeqCaseLabel(index);
      SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build());
      rConsecutiveLabels.add(caseClause.cloneWithLabelAndBlock(newLabel, newBlock));
    }
    return rConsecutiveLabels.build();
  }

  private static ImmutableMap<Integer, Integer> mapLabelToIndex(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    for (int i = 0; i < pCaseClauses.size(); i++) {
      rLabelToIndex.put(pCaseClauses.get(i).label.value, i);
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static SeqCaseBlockStatement replaceTargetPc(
      SeqCaseBlockStatement pCurrentStatement,
      final ImmutableMap<Integer, Integer> rLabelToIndexMap) {

    // if there are concatenated statements, replace target pc there too
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqCaseBlockStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement concatenatedStatement : concatenatedStatements) {
          newStatements.add(replaceTargetPc(concatenatedStatement, rLabelToIndexMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        int index = Objects.requireNonNull(rLabelToIndexMap.get(targetPc));
        return pCurrentStatement.cloneWithTargetPc(index);
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  public static SeqCaseBlockStatement replaceTargetPcWithGotoLoopHead(
      SeqCaseBlockStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqLoopHeadLabelStatement> rLabelToLabelMap) {

    // if there are concatenated statements, replace target pc there too
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqCaseBlockStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement concatenatedStatement : concatenatedStatements) {
          newStatements.add(
              replaceTargetPcWithGotoLoopHead(concatenatedStatement, rLabelToLabelMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC && rLabelToLabelMap.containsKey(targetPc)) {
        SeqLoopHeadLabelStatement label = Objects.requireNonNull(rLabelToLabelMap.get(targetPc));
        return pCurrentStatement.cloneWithTargetGoto(label.labelName);
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  public static SeqCaseBlockStatement injectGotoThreadLoop(
      CBinaryExpression pIterationSmallerMax,
      SeqThreadLoopLabelStatement pAssumeLabel,
      SeqThreadLoopLabelStatement pSwitchLabel,
      SeqCaseBlockStatement pCurrentStatement) {

    // if there are concatenated statements, replace target pc there too
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqCaseBlockStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement concatenatedStatement : concatenatedStatements) {
          newStatements.add(
              injectGotoThreadLoop(
                  pIterationSmallerMax, pAssumeLabel, pSwitchLabel, concatenatedStatement));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
        // add previous injections BEFORE (otherwise undefined behavior in seq!)
        newInjections.addAll(pCurrentStatement.getInjectedStatements());
        newInjections.add(
            new SeqThreadLoopGotoStatement(
                pIterationSmallerMax,
                // for statements targeting starts of critical sections, assumes are reevaluated
                priorCriticalSection(pCurrentStatement) ? pAssumeLabel : pSwitchLabel));
        return pCurrentStatement.cloneWithInjectedStatements(newInjections.build());
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }

  public static boolean priorCriticalSection(SeqCaseBlockStatement pStatement) {
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
}
