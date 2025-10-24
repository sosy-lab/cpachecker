// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqIgnoreSleepReductionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.nondet_num_statements.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.nondet_num_statements.SeqRoundGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.thread_sync.SeqSyncUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementUtil {

  // boolean helpers ===============================================================================

  /**
   * Returns {@code true} if {@code pCurrentStatement} starts inside an atomic block, but does not
   * actually start it.
   */
  public static boolean startsInAtomicBlock(ASeqThreadStatement pStatement) {
    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      CFAEdgeForThread threadEdge = substituteEdge.getThreadEdge();
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  public static boolean anySynchronizesThreads(ImmutableList<ASeqThreadStatement> pStatements) {
    for (ASeqThreadStatement statement : pStatements) {
      if (statement.synchronizesThreads()) {
        return true;
      }
    }
    return false;
  }

  public static boolean allHaveTargetGoto(ImmutableList<ASeqThreadStatement> pStatements) {
    for (ASeqThreadStatement statement : pStatements) {
      if (statement.getTargetGoto().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static boolean anyContainsEmptyBitVectorEvaluationExpression(
      ImmutableList<ASeqThreadStatement> pStatements) {

    for (ASeqThreadStatement statement : pStatements) {
      if (containsEmptyBitVectorEvaluationExpression(statement.getInjectedStatements())) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsEmptyBitVectorEvaluationExpression(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement evaluationStatement) {
        if (evaluationStatement.getEvaluationExpression().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  // Statement Finder ==============================================================================

  /**
   * Searches {@code pStatement}, all directly linked statements via {@code goto} and all target
   * {@code pc} statements and stores them in {@code pFound}.
   */
  public static void recursivelyFindTargetStatements(
      Set<ASeqThreadStatement> pFound,
      ASeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<ASeqThreadStatement> targetStatements =
        ImmutableList.<ASeqThreadStatement>builder()
            .addAll(getTargetPcStatements(pStatement, pLabelClauseMap))
            .addAll(getTargetGotoStatements(pStatement, pLabelBlockMap))
            .build();
    for (ASeqThreadStatement targetStatement : targetStatements) {
      // prevent infinite loops when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetStatements(pFound, targetStatement, pLabelClauseMap, pLabelBlockMap);
      }
    }
  }

  /**
   * Searches {@code pStatement} and all directly linked via {@code goto} statements and stores them
   * in {@code pFound}.
   */
  public static void recursivelyFindTargetGotoStatements(
      Set<ASeqThreadStatement> pFound,
      ASeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<ASeqThreadStatement> targetGotoStatements =
        getTargetGotoStatements(pStatement, pLabelBlockMap);
    for (ASeqThreadStatement targetStatement : targetGotoStatements) {
      // prevent infinite recursion when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetGotoStatements(pFound, targetStatement, pLabelBlockMap);
      }
    }
  }

  private static ImmutableList<ASeqThreadStatement> getTargetPcStatements(
      ASeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetNumber = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause targetClause =
          Objects.requireNonNull(pLabelClauseMap.get(targetNumber));
      return targetClause.getFirstBlock().getStatements();
    }
    return ImmutableList.of();
  }

  /**
   * Searches all statements targeted by {@code pStatement} via {@code goto}. This excludes target
   * {@code pc} because they represent a cut i.e. context switch in the sequentialization.
   */
  private static ImmutableList<ASeqThreadStatement> getTargetGotoStatements(
      ASeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pStatement.getTargetGoto().isPresent()) {
      int targetNumber = pStatement.getTargetGoto().orElseThrow().getNumber();
      SeqThreadStatementBlock targetBlock =
          Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
      return targetBlock.getStatements();
    }
    return ImmutableList.of();
  }

  // Injected Statements ===========================================================================

  /**
   * This returns the {@link String} representation for all injected statements, depending on
   * whether {@code pTargetPc} or {@code pTargetGoto} is present.
   */
  static String buildInjectedStatementsString(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    if (pTargetPc.isPresent()) {
      return buildInjectedStatementsStringByTargetPc(
          pOptions, pPcLeftHandSide, pTargetPc.orElseThrow(), pInjectedStatements);

    } else if (pTargetGoto.isPresent()) {
      return buildInjectedStatementsStringByTargetGoto(
          pTargetGoto.orElseThrow(), pInjectedStatements);
    }
    throw new IllegalStateException("either pTargetPc or pTargetGoto must be present");
  }

  private static String buildInjectedStatementsStringByTargetPc(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    StringJoiner statements = new StringJoiner(SeqSyntax.SPACE);
    // first create pruned statements
    ImmutableList<SeqInjectedStatement> pruned = pruneInjectedStatements(pInjectedStatements);
    // create the pc write
    CExpressionAssignmentStatement pcWrite =
        SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPc);
    boolean emptyBitVectorEvaluation =
        SeqThreadStatementUtil.containsEmptyBitVectorEvaluationExpression(pruned);
    // with empty bit vector evaluations, place pc write before injections, otherwise info is lost
    if (emptyBitVectorEvaluation) {
      statements.add(pcWrite.toASTString());
    }
    // add all injected statements in the correct order
    ImmutableList<SeqInjectedStatement> ordered = orderInjectedStatements(pOptions, pruned);
    assert ordered.size() == pruned.size() : "ordering of statements resulted in lost statements";
    for (SeqInjectedStatement injectedStatement : ordered) {
      statements.add(injectedStatement.toASTString());
    }
    // for non-empty bit vector evaluations, place pc write after injections for optimization
    if (!emptyBitVectorEvaluation) {
      statements.add(pcWrite.toASTString());
    }
    return statements.toString();
  }

  private static String buildInjectedStatementsStringByTargetGoto(
      SeqBlockLabelStatement pTargetGoto, ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    StringJoiner statements = new StringJoiner(SeqSyntax.SPACE);
    SeqGotoStatement gotoStatement = new SeqGotoStatement(pTargetGoto);
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqCountUpdateStatement) {
        // count updates are included, even with target gotos
        statements.add(injectedStatement.toASTString());
      }
    }
    return statements.add(gotoStatement.toASTString()).toString();
  }

  private static ImmutableList<SeqInjectedStatement> pruneInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    Set<SeqInjectedStatement> pruned = new HashSet<>();
    if (containsEmptyBitVectorEvaluationExpression(pInjectedStatements)) {
      pruned.addAll(
          pInjectedStatements.stream()
              // prune all bit vector assignments and round goto statements if evaluation is empty
              .filter(
                  s ->
                      s instanceof SeqBitVectorAssignmentStatement
                          || s instanceof SeqRoundGotoStatement)
              .collect(ImmutableSet.toImmutableSet()));
    }
    return pInjectedStatements.stream()
        .filter(i -> !pruned.contains(i))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedStatements(
      MPOROptions pOptions, ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> rOrdered = ImmutableList.builder();
    List<SeqInjectedStatement> leftOver = new ArrayList<>();
    // first order the reduction statements based on pOptions
    leftOver.addAll(orderInjectedReductionStatements(pOptions, pInjectedStatements));
    // bit vector updates are placed at the end, i.e. where pc etc. updates are
    leftOver.addAll(
        getInjectedStatementsByClass(pInjectedStatements, SeqSyncUpdateStatement.class));
    leftOver.addAll(
        getInjectedStatementsByClass(pInjectedStatements, SeqBitVectorAssignmentStatement.class));
    leftOver.addAll(
        getInjectedStatementsByClass(pInjectedStatements, SeqLastBitVectorUpdateStatement.class));
    rOrdered.addAll(
        pInjectedStatements.stream()
            .filter(statement -> !leftOver.contains(statement))
            // since we clone reduceIgnoreSleep, .contains does not work
            .filter(statement -> !(statement instanceof SeqIgnoreSleepReductionStatement))
            .collect(ImmutableList.toImmutableList()));
    rOrdered.addAll(leftOver);
    return rOrdered.build();
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedReductionStatements(
      MPOROptions pOptions, ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return switch (pOptions.reductionOrder) {
      // if NONE is specified, default to BITVECTOR_THEN_CONFLICT
      case NONE, CONFLICT_THEN_LAST_THREAD ->
          orderInjectedReductionStatements(
              pInjectedStatements,
              SeqBitVectorEvaluationStatement.class,
              SeqConflictOrderStatement.class);
      case LAST_THREAD_THEN_CONFLICT ->
          orderInjectedReductionStatements(
              pInjectedStatements,
              SeqConflictOrderStatement.class,
              SeqBitVectorEvaluationStatement.class);
    };
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedReductionStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      Class<? extends SeqInjectedStatement> pFirstClass,
      Class<? extends SeqInjectedStatement> pSecondClass) {

    ImmutableList<SeqInjectedStatement> ignoreSleepStatements =
        getInjectedStatementsByClass(pInjectedStatements, SeqIgnoreSleepReductionStatement.class);
    if (!ignoreSleepStatements.isEmpty()) {
      // order the reduction assumptions inside ignoreSleepStatements separately
      assert ignoreSleepStatements.size() == 1 : "there can only be a single ignoreSleepStatement";
      SeqIgnoreSleepReductionStatement ignoreSleepStatement =
          (SeqIgnoreSleepReductionStatement) ignoreSleepStatements.getFirst();
      ImmutableList<SeqInjectedStatement> reductionAssumptions =
          ignoreSleepStatement.getReductionAssumptions();
      return ImmutableList.of(
          ignoreSleepStatement.cloneWithReductionAssumptions(
              ImmutableList.<SeqInjectedStatement>builder()
                  .addAll(getInjectedStatementsByClass(reductionAssumptions, pFirstClass))
                  .addAll(getInjectedStatementsByClass(reductionAssumptions, pSecondClass))
                  .build()));
    }
    return ImmutableList.<SeqInjectedStatement>builder()
        .addAll(getInjectedStatementsByClass(pInjectedStatements, pFirstClass))
        .addAll(getInjectedStatementsByClass(pInjectedStatements, pSecondClass))
        .build();
  }

  private static ImmutableList<SeqInjectedStatement> getInjectedStatementsByClass(
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      Class<? extends SeqInjectedStatement> pClass) {

    return pInjectedStatements.stream()
        .filter(statement -> pClass.isInstance(statement))
        .collect(ImmutableList.toImmutableList());
  }

  // Helper ========================================================================================

  public static ASeqThreadStatement appendedInjectedStatementsToStatement(
      ASeqThreadStatement pStatement, ImmutableList<SeqInjectedStatement> pAppended) {

    return pStatement.withInjectedStatements(
        appendInjectedStatements(pStatement.getInjectedStatements(), pAppended));
  }

  public static ASeqThreadStatement appendedInjectedStatementsToStatement(
      ASeqThreadStatement pStatement, SeqInjectedStatement... pAppended) {

    return pStatement.withInjectedStatements(
        appendInjectedStatements(
            pStatement.getInjectedStatements(), ImmutableList.copyOf(pAppended)));
  }

  private static ImmutableList<SeqInjectedStatement> appendInjectedStatements(
      ImmutableList<SeqInjectedStatement> pExistingStatements,
      ImmutableList<SeqInjectedStatement> pAppendedStatements) {

    return FluentIterable.concat(pExistingStatements, pAppendedStatements).toList();
  }

  public static Optional<Integer> tryGetTargetPcOrGotoNumber(ASeqThreadStatement pStatement) {
    if (pStatement.getTargetPc().isPresent()) {
      return pStatement.getTargetPc();

    } else if (pStatement.getTargetGoto().isPresent()) {
      return Optional.of(pStatement.getTargetGoto().orElseThrow().getNumber());
    }
    return Optional.empty();
  }
}
