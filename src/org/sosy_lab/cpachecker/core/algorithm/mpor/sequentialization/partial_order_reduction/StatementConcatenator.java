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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

class StatementConcatenator {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concat(
      MPOROptions pOptions,
      ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      // map label pc to loop head labels created at their start, used later for injecting gotos
      Map<Integer, SeqLoopHeadLabelStatement> loopHeads = new HashMap<>();
      ImmutableList<SeqCaseClause> withLoopHeadLabels =
          injectLoopHeadLabels(pOptions, thread, entry.getValue(), loopHeads);
      ImmutableList<SeqCaseClause> removedInjections =
          removeUnnecessaryInjections(pUpdatedVariables, withLoopHeadLabels);
      ImmutableList<SeqCaseClause> concatenatedCases =
          concatenateCommutingCases(removedInjections, ImmutableMap.copyOf(loopHeads));
      ImmutableList<SeqCaseClause> withGoto =
          replaceLoopHeadTargetPcWithGoto(ImmutableMap.copyOf(loopHeads), concatenatedCases);
      rConcatenated.put(thread, withGoto);
    }
    return rConcatenated.buildOrThrow();
  }

  // Concatenation =================================================================================

  private static ImmutableList<SeqCaseClause> concatenateCommutingCases(
      ImmutableList<SeqCaseClause> pCaseClauses,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels) {

    ImmutableList.Builder<SeqCaseClause> newCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<Integer> concatenated = new HashSet<>();
    Set<Integer> duplicated = new HashSet<>();

    boolean firstConcat = true;
    for (SeqCaseClause caseClause : pCaseClauses) {
      // prevent start in already concatenated clauses, otherwise they are duplicated
      if (concatenated.add(caseClause.id)) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(
                  firstConcat,
                  caseClause.isGlobal,
                  statement,
                  concatenated,
                  duplicated,
                  pLoopHeadLabels,
                  labelValueMap));
          firstConcat = false;
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
      final boolean pIsFirstConcat,
      final boolean pIsGlobal,
      SeqCaseBlockStatement pCurrentStatement,
      Set<Integer> pConcatenated,
      Set<Integer> pDuplicated,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeads,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (SeqCaseClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      if (validConcatenation(pIsFirstConcat, pIsGlobal, pCurrentStatement, newTarget, pLoopHeads)) {
        // if the target id was seen before, add it to duplicate, except loop heads
        if (!pConcatenated.add(newTarget.id) && !newTarget.isLoopStart) {
          pDuplicated.add(newTarget.id);
        }
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : newTarget.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(
                  // encounter global in first concat -> set first concat to false
                  pIsFirstConcat && !newTarget.isGlobal,
                  pIsGlobal,
                  statement,
                  pConcatenated,
                  pDuplicated,
                  pLoopHeads,
                  pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    return pCurrentStatement;
  }

  /** Checks if {@code pStatement} and {@code pTarget} can be concatenated. */
  private static boolean validConcatenation(
      final boolean pIsFirstConcat,
      final boolean pIsGlobal,
      SeqCaseBlockStatement pStatement,
      SeqCaseClause pTarget,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    return pStatement.isConcatenable()
        // label injected before -> return to loop head, no concat to prevent infinite loop
        && !pLoopHeads.containsKey(pTarget.label.value)
        // only consider global if not ignored
        && !((!canIgnoreGlobal(pTarget, pIsFirstConcat, pIsGlobal) && pTarget.isGlobal)
            || !pTarget.isCriticalSectionStart()
            || pTarget.block.statements.contains(pStatement));
  }

  /**
   * Whether we can ignore the local/global property of {@code pCaseClause}, e.g. unlocks of global
   * mutexes.
   */
  private static boolean canIgnoreGlobal(
      SeqCaseClause pCaseClause, boolean pIsFirstConcat, boolean pIsGlobal) {

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

  // Loop Heads ====================================================================================

  private static ImmutableList<SeqCaseClause> replaceLoopHeadTargetPcWithGoto(
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels,
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rWithGoto = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(
            SeqCaseClauseUtil.replaceTargetPcWithGotoLoopHead(statement, pLoopHeadLabels));
      }
      SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build());
      rWithGoto.add(caseClause.cloneWithBlock(newBlock));
    }
    return rWithGoto.build();
  }

  private static ImmutableList<SeqCaseClause> injectLoopHeadLabels(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<SeqCaseClause> pCaseClauses,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    ImmutableList.Builder<SeqCaseClause> rWithLabels = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList<SeqCaseBlockStatement> newStatements =
          tryInjectLoopHeadLabel(pOptions, pThread, caseClause, pLoopHeads);
      rWithLabels.add(caseClause.cloneWithBlock(new SeqCaseBlock(newStatements)));
    }
    return rWithLabels.build();
  }

  /**
   * When concatenating a {@link SeqCaseClause} that is a loop head, we inject {@code label}s so
   * that these statements remain directly reachable via {@code goto}.
   */
  private static ImmutableList<SeqCaseBlockStatement> tryInjectLoopHeadLabel(
      MPOROptions pOptions,
      MPORThread pThread,
      SeqCaseClause pCaseClause,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    // for non-loop head, return statements as they are.
    if (!pCaseClause.isLoopStart) {
      return pCaseClause.block.statements;
    }

    // create loop head label for the first statement
    SeqCaseBlockStatement firstStatement = pCaseClause.block.getFirstStatement();
    String labelName = SeqNameUtil.buildLoopHeadLabelName(pOptions, pThread.id);
    SeqLoopHeadLabelStatement loopHeadLabel = new SeqLoopHeadLabelStatement(labelName);
    pLoopHeads.put(pCaseClause.label.value, loopHeadLabel);
    SeqCaseBlockStatement cloneWithLabel = firstStatement.cloneWithLoopHeadLabel(loopHeadLabel);

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

  // Injections ====================================================================================

  /**
   * If a thread starts with just an injection, e.g. to simulate a mutex request, then this function
   * prunes that case clause and adds the variables from the injection to {@code pUpdatedVariables}
   * so that it is initialized with the value that is set in the injection. This reduces the amount
   * of interleavings.
   */
  private static ImmutableList<SeqCaseClause> removeUnnecessaryInjections(
      final ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rPrunedInjections = ImmutableList.builder();
    SeqCaseClause firstCase = pCaseClauses.get(0);
    if (firstCase.block.statements.size() == 1) {
      SeqCaseBlockStatement firstStatement = firstCase.block.getFirstStatement();
      if (isUnnecessaryInjection(firstCase, firstStatement)) {
        SeqInjectedStatement injected = firstStatement.getInjectedStatements().get(0);
        pUpdatedVariables.add(injected.getIdExpression().orElseThrow());
        return rPrunedInjections
            .addAll(
                pCaseClauses.stream()
                    .filter(c -> !c.equals(firstCase))
                    .collect(ImmutableList.toImmutableList()))
            .build();
      }
    }
    return pCaseClauses;
  }

  private static boolean isUnnecessaryInjection(
      SeqCaseClause pFirstCase, SeqCaseBlockStatement pFirstStatement) {

    return pFirstStatement instanceof SeqBlankStatement
        && pFirstStatement.getInjectedStatements().size() == 1
        && !pFirstCase.isLoopStart
        && pFirstStatement.getLoopHeadLabel().isEmpty();
  }
}
