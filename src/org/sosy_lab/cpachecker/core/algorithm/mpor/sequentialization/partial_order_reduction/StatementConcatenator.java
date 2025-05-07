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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

class StatementConcatenator {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> concat(
      MPOROptions pOptions,
      ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rConcatenated =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      // map label pc to loop head labels created at their start, used later for injecting gotos
      Map<Integer, SeqLoopHeadLabelStatement> loopHeads = new HashMap<>();
      ImmutableList<SeqThreadStatementClause> withLoopHeadLabels =
          injectLoopHeadLabels(pOptions, thread, entry.getValue(), loopHeads);
      ImmutableList<SeqThreadStatementClause> removedInjections =
          removeUnnecessaryInjections(pUpdatedVariables, withLoopHeadLabels);
      ImmutableList<SeqThreadStatementClause> concatenatedCases =
          concatenateCommutingCases(removedInjections, ImmutableMap.copyOf(loopHeads));
      ImmutableList<SeqThreadStatementClause> withGoto =
          replaceLoopHeadTargetPcWithGoto(ImmutableMap.copyOf(loopHeads), concatenatedCases);
      rConcatenated.put(thread, withGoto);
    }
    return rConcatenated.buildOrThrow();
  }

  // Concatenation =================================================================================

  private static ImmutableList<SeqThreadStatementClause> concatenateCommutingCases(
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels) {

    ImmutableList.Builder<SeqThreadStatementClause> newCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<Integer> concatenated = new HashSet<>();
    Set<Integer> duplicated = new HashSet<>();

    boolean firstConcat = true;
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      // prevent start in already concatenated clauses, otherwise they are duplicated
      if (concatenated.add(caseClause.id)) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : caseClause.block.statements) {
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
        SeqThreadStatementBlock newBlock = new SeqThreadStatementBlock(newStatements.build());
        SeqThreadStatementClause clone = caseClause.cloneWithBlock(newBlock);
        newCaseClauses.add(clone);
      }
    }
    ImmutableList<SeqThreadStatementClause> rNewCaseClauses = newCaseClauses.build();
    // we filter out case clauses that were visited twice during concatenation
    return rNewCaseClauses.stream()
        .filter(
            caseClause ->
                concatenated.contains(caseClause.id) && !duplicated.contains(caseClause.id))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Recursively concatenates the target statements of {@code pCurrentStatement}, if applicable.
   *
   * @param pIsFirstConcat for the very first concat, we can concat global statements
   */
  private static SeqThreadStatement recursivelyConcatenateStatements(
      final boolean pIsFirstConcat,
      final boolean pIsGlobal,
      SeqThreadStatement pCurrentStatement,
      Set<Integer> pConcatenated,
      Set<Integer> pDuplicated,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeads,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      // TODO prevent concatenation of statements that are in atomic blocks
      if (validConcatenation(pIsFirstConcat, pIsGlobal, pCurrentStatement, newTarget, pLoopHeads)) {
        // if the target id was seen before, add it to duplicate, except loop heads
        if (!pConcatenated.add(newTarget.id) && !newTarget.isLoopStart) {
          pDuplicated.add(newTarget.id);
        }
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : newTarget.block.statements) {
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
      SeqThreadStatement pStatement,
      SeqThreadStatementClause pTarget,
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    return pStatement.isConcatenable()
        // label injected before -> return to loop head, no concat to prevent infinite loop
        && !pLoopHeads.containsKey(pTarget.labelNumber)
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
      SeqThreadStatementClause pCaseClause, boolean pIsFirstConcat, boolean pIsGlobal) {

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

  /**
   * Adds {@code goto}s when the target statement is a loop head which is not directly reachable
   * after concatenation.
   */
  private static ImmutableList<SeqThreadStatementClause> replaceLoopHeadTargetPcWithGoto(
      ImmutableMap<Integer, SeqLoopHeadLabelStatement> pLoopHeadLabels,
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithGoto = ImmutableList.builder();
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : caseClause.block.statements) {
        newStatements.add(
            SeqThreadStatementClauseUtil.recursivelyReplaceTargetPcWithGotoLoopHead(
                statement, pLoopHeadLabels));
      }
      SeqThreadStatementBlock newBlock = new SeqThreadStatementBlock(newStatements.build());
      rWithGoto.add(caseClause.cloneWithBlock(newBlock));
    }
    return rWithGoto.build();
  }

  private static ImmutableList<SeqThreadStatementClause> injectLoopHeadLabels(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithLabels = ImmutableList.builder();
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList<SeqThreadStatement> newStatements =
          tryInjectLoopHeadLabel(pOptions, pThread, caseClause, pLoopHeads);
      rWithLabels.add(caseClause.cloneWithBlock(new SeqThreadStatementBlock(newStatements)));
    }
    return rWithLabels.build();
  }

  /**
   * When concatenating a {@link SeqThreadStatementClause} that is a loop head, we inject {@code
   * label}s so that these statements remain directly reachable via {@code goto}.
   */
  private static ImmutableList<SeqThreadStatement> tryInjectLoopHeadLabel(
      MPOROptions pOptions,
      MPORThread pThread,
      SeqThreadStatementClause pCaseClause,
      Map<Integer, SeqLoopHeadLabelStatement> pLoopHeads) {

    // for non-loop head and global statements, return statements as they are.
    if (!pCaseClause.isLoopStart || pCaseClause.isGlobal) {
      return pCaseClause.block.statements;
    }

    // create loop head label for the first statement
    SeqThreadStatement firstStatement = pCaseClause.block.getFirstStatement();
    String labelName = SeqNameUtil.buildLoopHeadLabelName(pOptions, pThread.id);
    SeqLoopHeadLabelStatement loopHeadLabel = new SeqLoopHeadLabelStatement(labelName);
    pLoopHeads.put(pCaseClause.labelNumber, loopHeadLabel);
    SeqThreadStatement cloneWithLabel = firstStatement.cloneWithLoopHeadLabel(loopHeadLabel);

    // inject cloned statement with loop head label
    ImmutableList.Builder<SeqThreadStatement> rWithLabel = ImmutableList.builder();
    rWithLabel.add(cloneWithLabel);
    for (SeqThreadStatement statement : pCaseClause.block.statements) {
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
  private static ImmutableList<SeqThreadStatementClause> removeUnnecessaryInjections(
      final ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rPrunedInjections = ImmutableList.builder();
    SeqThreadStatementClause firstCase = pCaseClauses.get(0);
    if (firstCase.block.statements.size() == 1) {
      SeqThreadStatement firstStatement = firstCase.block.getFirstStatement();
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
      SeqThreadStatementClause pFirstCase, SeqThreadStatement pFirstStatement) {

    return pFirstStatement instanceof SeqBlankStatement
        && pFirstStatement.getInjectedStatements().size() == 1
        && !pFirstCase.isLoopStart
        && pFirstStatement.getLoopHeadLabel().isEmpty();
  }
}
