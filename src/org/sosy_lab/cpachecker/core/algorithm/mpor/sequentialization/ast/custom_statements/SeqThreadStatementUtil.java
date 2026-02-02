// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;

public final class SeqThreadStatementUtil {

  // boolean helpers ===============================================================================

  /**
   * Returns {@code true} if {@code pCurrentStatement} starts inside an atomic block, but does not
   * actually start it.
   */
  public static boolean startsInAtomicBlock(CSeqThreadStatement pStatement) {
    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      CFAEdgeForThread threadEdge = substituteEdge.getThreadEdge();
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  public static boolean anySynchronizesThreads(ImmutableList<CSeqThreadStatement> pStatements) {
    for (CSeqThreadStatement statement : pStatements) {
      if (statement.synchronizesThreads()) {
        return true;
      }
    }
    return false;
  }

  public static boolean allHaveTargetGoto(ImmutableList<CSeqThreadStatement> pStatements) {
    for (CSeqThreadStatement statement : pStatements) {
      if (statement.getTargetGoto().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static boolean anyContainsEmptyBitVectorEvaluationExpression(
      ImmutableList<CSeqThreadStatement> pStatements) {

    for (CSeqThreadStatement statement : pStatements) {
      if (isAnyBitVectorEvaluationExpressionEmpty(statement.getInjectedStatements())) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAnyBitVectorEvaluationExpressionEmpty(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement evaluationStatement) {
        if (evaluationStatement.evaluationExpression().isEmpty()) {
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
      Set<CSeqThreadStatement> pFound,
      CSeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<CSeqThreadStatement> targetStatements =
        ImmutableList.<CSeqThreadStatement>builder()
            .addAll(getTargetPcStatements(pStatement, pLabelClauseMap))
            .addAll(getTargetGotoStatements(pStatement, pLabelBlockMap))
            .build();
    for (CSeqThreadStatement targetStatement : targetStatements) {
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
      Set<CSeqThreadStatement> pFound,
      CSeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<CSeqThreadStatement> targetGotoStatements =
        getTargetGotoStatements(pStatement, pLabelBlockMap);
    for (CSeqThreadStatement targetStatement : targetGotoStatements) {
      // prevent infinite recursion when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetGotoStatements(pFound, targetStatement, pLabelBlockMap);
      }
    }
  }

  private static ImmutableList<CSeqThreadStatement> getTargetPcStatements(
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.isTargetPcValid()) {
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
  private static ImmutableList<CSeqThreadStatement> getTargetGotoStatements(
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pStatement.getTargetGoto().isPresent()) {
      int targetNumber = pStatement.getTargetGoto().orElseThrow().labelNumber();
      SeqThreadStatementBlock targetBlock =
          Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
      return targetBlock.getStatements();
    }
    return ImmutableList.of();
  }

  // Injected Statements ===========================================================================

  /** Prepares the given {@link SeqInjectedStatement} by pruning and sorting them. */
  static ImmutableList<SeqInjectedStatement> prepareInjectedStatements(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    if (pTargetPc.isPresent()) {
      return prepareInjectedStatementsByTargetPc(
          pPcLeftHandSide, pTargetPc.orElseThrow(), pInjectedStatements);

    } else if (pTargetGoto.isPresent()) {
      return prepareInjectedStatementsByTargetGoto(pTargetGoto.orElseThrow(), pInjectedStatements);
    }
    throw new IllegalStateException("either pTargetPc or pTargetGoto must be present");
  }

  private static ImmutableList<SeqInjectedStatement> prepareInjectedStatementsByTargetPc(
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> prepared = ImmutableList.builder();

    // first create pruned statements
    ImmutableList<SeqInjectedStatement> pruned = pruneInjectedStatements(pInjectedStatements);

    // create the pc write
    CExpressionAssignmentStatement pcAssignmentStatement =
        ProgramCounterVariables.buildPcAssignmentStatement(pPcLeftHandSide, pTargetPc);
    boolean emptyBitVectorEvaluation =
        SeqThreadStatementUtil.isAnyBitVectorEvaluationExpressionEmpty(pruned);

    // with empty bit vector evaluations, place pc write before injections, otherwise info is lost
    if (emptyBitVectorEvaluation) {
      prepared.add(new SeqProgramCounterUpdateStatement(pcAssignmentStatement));
    }

    // add all injected statements in the correct order
    ImmutableList<SeqInjectedStatement> ordered = orderInjectedStatements(pruned);
    assert ordered.size() == pruned.size() : "ordering of statements resulted in lost statements";
    prepared.addAll(ordered);

    // for non-empty bit vector evaluations, place pc write after injections for optimization
    if (!emptyBitVectorEvaluation) {
      prepared.add(new SeqProgramCounterUpdateStatement(pcAssignmentStatement));
    }

    return prepared.build();
  }

  private static ImmutableList<SeqInjectedStatement> prepareInjectedStatementsByTargetGoto(
      SeqBlockLabelStatement pTargetGoto, ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> prepared = ImmutableList.builder();

    CGotoStatement gotoStatement = new CGotoStatement(pTargetGoto.toCLabelStatement());
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      // add all statements that are not pruned, even when there is a target goto
      if (!injectedStatement.isPrunedWithTargetGoto()) {
        prepared.add(injectedStatement);
      }
    }

    // add the goto last, so that the injected statements appear before it
    return prepared.add(new SeqGotoBlockStatement(gotoStatement)).build();
  }

  private static ImmutableList<SeqInjectedStatement> pruneInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    Set<SeqInjectedStatement> pruned = new HashSet<>();
    if (isAnyBitVectorEvaluationExpressionEmpty(pInjectedStatements)) {
      pruned.addAll(
          pInjectedStatements.stream()
              .filter(s -> s.isPrunedWithEmptyBitVectorEvaluation())
              .collect(ImmutableSet.toImmutableSet()));
    }
    return pInjectedStatements.stream()
        .filter(i -> !pruned.contains(i))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqInjectedStatement> orderInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList.Builder<SeqInjectedStatement> rOrdered = ImmutableList.builder();
    List<SeqInjectedStatement> leftOver = new ArrayList<>();
    // first add partial order reduction statements, since if they abort, the rest is not needed
    leftOver.addAll(orderInjectedReductionStatements(pInjectedStatements));
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
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    ImmutableList<SeqInjectedStatement> ignoreSleepStatements =
        getInjectedStatementsByClass(pInjectedStatements, SeqIgnoreSleepReductionStatement.class);
    if (!ignoreSleepStatements.isEmpty()) {
      // order the reduction assumptions inside ignoreSleepStatements separately
      assert ignoreSleepStatements.size() == 1 : "there can only be a single ignoreSleepStatement";
      SeqIgnoreSleepReductionStatement ignoreSleepStatement =
          (SeqIgnoreSleepReductionStatement) ignoreSleepStatements.getFirst();
      ImmutableList<SeqInjectedStatement> reductionAssumptions =
          ignoreSleepStatement.reductionAssumptions();
      return ImmutableList.of(
          ignoreSleepStatement.withReductionAssumptions(
              ImmutableList.<SeqInjectedStatement>builder()
                  .addAll(
                      getInjectedStatementsByClass(
                          reductionAssumptions, SeqBitVectorEvaluationStatement.class))
                  .build()));
    }
    return ImmutableList.<SeqInjectedStatement>builder()
        .addAll(
            getInjectedStatementsByClass(
                pInjectedStatements, SeqBitVectorEvaluationStatement.class))
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

  public static CSeqThreadStatement appendedInjectedStatementsToStatement(
      CSeqThreadStatement pStatement, ImmutableList<SeqInjectedStatement> pAppended) {

    return pStatement.withInjectedStatements(
        appendInjectedStatements(pStatement.getInjectedStatements(), pAppended));
  }

  public static CSeqThreadStatement appendedInjectedStatementsToStatement(
      CSeqThreadStatement pStatement, SeqInjectedStatement... pAppended) {

    return pStatement.withInjectedStatements(
        appendInjectedStatements(
            pStatement.getInjectedStatements(), ImmutableList.copyOf(pAppended)));
  }

  private static ImmutableList<SeqInjectedStatement> appendInjectedStatements(
      ImmutableList<SeqInjectedStatement> pExistingStatements,
      ImmutableList<SeqInjectedStatement> pAppendedStatements) {

    return FluentIterable.concat(pExistingStatements, pAppendedStatements).toList();
  }
}
