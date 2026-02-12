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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;

public final class SeqThreadStatementUtil {

  // boolean helpers ===============================================================================

  /**
   * Returns {@code true} if {@code pCurrentStatement} starts inside an atomic block, but does not
   * actually start it.
   */
  public static boolean startsInAtomicBlock(SeqThreadStatement pStatement) {
    for (SubstituteEdge substituteEdge : pStatement.data().getSubstituteEdges()) {
      CFAEdgeForThread threadEdge = substituteEdge.getThreadEdge();
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  public static boolean anySynchronizesThreads(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.data().getType().synchronizesThreads) {
        return true;
      }
    }
    return false;
  }

  public static boolean allHaveTargetGoto(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.targetGoto().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static boolean anyContainsEmptyBitVectorEvaluationExpression(
      ImmutableList<SeqThreadStatement> pStatements) {

    for (SeqThreadStatement statement : pStatements) {
      if (isAnyBitVectorEvaluationExpressionEmpty(statement.instrumentation())) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAnyBitVectorEvaluationExpressionEmpty(
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    for (SeqInstrumentation instrumentation : pInstrumentation) {
      if (instrumentation.type().equals(SeqInstrumentationType.UNTIL_CONFLICT_REDUCTION)) {
        if (instrumentation.statement() instanceof CGotoStatement) {
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
      Set<SeqThreadStatement> pFound,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<SeqThreadStatement> targetStatements =
        ImmutableList.<SeqThreadStatement>builder()
            .addAll(getTargetPcStatements(pStatement, pLabelClauseMap))
            .addAll(getTargetGotoStatements(pStatement, pLabelBlockMap))
            .build();
    for (SeqThreadStatement targetStatement : targetStatements) {
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
      Set<SeqThreadStatement> pFound,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<SeqThreadStatement> targetGotoStatements =
        getTargetGotoStatements(pStatement, pLabelBlockMap);
    for (SeqThreadStatement targetStatement : targetGotoStatements) {
      // prevent infinite recursion when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetGotoStatements(pFound, targetStatement, pLabelBlockMap);
      }
    }
  }

  private static ImmutableList<SeqThreadStatement> getTargetPcStatements(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.isTargetPcValid()) {
      int targetNumber = pStatement.targetPc().orElseThrow();
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
  private static ImmutableList<SeqThreadStatement> getTargetGotoStatements(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pStatement.targetGoto().isPresent()) {
      int targetNumber = pStatement.targetGoto().orElseThrow();
      SeqThreadStatementBlock targetBlock =
          Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
      return targetBlock.getStatements();
    }
    return ImmutableList.of();
  }

  // Injected Statements ===========================================================================

  static ImmutableList<SeqInstrumentation> prepareInstrumentationByTargetPc(
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc,
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    ImmutableList.Builder<SeqInstrumentation> prepared = ImmutableList.builder();

    // first create pruned statements
    ImmutableList<SeqInstrumentation> pruned = pruneInjectedStatements(pInstrumentation);

    // create the pc write
    CExpressionAssignmentStatement pcAssignmentStatement =
        ProgramCounterVariables.buildPcAssignmentStatement(pPcLeftHandSide, pTargetPc);
    boolean emptyBitVectorEvaluation =
        SeqThreadStatementUtil.isAnyBitVectorEvaluationExpressionEmpty(pruned);

    // with empty bit vector evaluations, place pc write before injections, otherwise info is lost
    if (emptyBitVectorEvaluation) {
      prepared.add(SeqInstrumentationBuilder.buildProgramCounterUpdate(pcAssignmentStatement));
    }

    // add all injected statements in the correct order
    ImmutableList<SeqInstrumentation> ordered = orderInstrumentation(pruned);
    assert ordered.size() == pruned.size() : "ordering of statements resulted in lost statements";
    prepared.addAll(ordered);

    // for non-empty bit vector evaluations, place pc write after injections for optimization
    if (!emptyBitVectorEvaluation) {
      prepared.add(SeqInstrumentationBuilder.buildProgramCounterUpdate(pcAssignmentStatement));
    }

    return prepared.build();
  }

  static ImmutableList<SeqInstrumentation> prepareInstrumentationByTargetGoto(
      int pThreadId, int pTargetGotoNumber, ImmutableList<SeqInstrumentation> pInstrumentation) {

    ImmutableList.Builder<SeqInstrumentation> prepared = ImmutableList.builder();
    for (SeqInstrumentation instrumentation : pInstrumentation) {
      // add all statements that are not pruned, even when there is a target goto
      if (!instrumentation.type().isPrunedWithTargetGoto) {
        prepared.add(instrumentation);
      }
    }
    // add the goto last, so that the injected statements appear before it
    String labelName = SeqNameUtil.buildThreadStatementBlockLabelName(pThreadId, pTargetGotoNumber);
    SeqInstrumentation gotoBlockLabel =
        SeqInstrumentationBuilder.buildGotoBlockLabelStatement(new CLabelStatement(labelName));
    return prepared.add(gotoBlockLabel).build();
  }

  private static ImmutableList<SeqInstrumentation> pruneInjectedStatements(
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    Set<SeqInstrumentation> pruned = new HashSet<>();
    if (isAnyBitVectorEvaluationExpressionEmpty(pInstrumentation)) {
      pruned.addAll(
          pInstrumentation.stream()
              .filter(s -> s.type().isPrunedWithEmptyBitVectorEvaluation)
              .collect(ImmutableSet.toImmutableSet()));
    }
    return pInstrumentation.stream()
        .filter(i -> !pruned.contains(i))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqInstrumentation> orderInstrumentation(
      ImmutableList<SeqInstrumentation> pInstrumentation) {

    ImmutableList.Builder<SeqInstrumentation> rOrdered = ImmutableList.builder();
    List<SeqInstrumentation> leftOver = new ArrayList<>();
    // first add partial order reduction statements, since if they abort, the rest is not needed
    leftOver.addAll(
        getInstrumentationByType(
            pInstrumentation, SeqInstrumentationType.UNTIL_CONFLICT_REDUCTION));
    // bit vector updates are placed at the end, i.e. where pc etc. updates are
    leftOver.addAll(
        getInstrumentationByType(pInstrumentation, SeqInstrumentationType.THREAD_SYNC_UPDATE));
    leftOver.addAll(
        getInstrumentationByType(pInstrumentation, SeqInstrumentationType.BIT_VECTOR_UPDATE));
    leftOver.addAll(
        getInstrumentationByType(pInstrumentation, SeqInstrumentationType.LAST_BIT_VECTOR_UPDATE));
    rOrdered.addAll(
        pInstrumentation.stream()
            .filter(i -> !leftOver.contains(i))
            .collect(ImmutableList.toImmutableList()));
    rOrdered.addAll(leftOver);
    return rOrdered.build();
  }

  private static ImmutableList<SeqInstrumentation> getInstrumentationByType(
      ImmutableList<SeqInstrumentation> pInstrumentation, SeqInstrumentationType pType) {

    return pInstrumentation.stream()
        .filter(i -> i.type().equals(pType))
        .collect(ImmutableList.toImmutableList());
  }

  // Helper ========================================================================================

  public static SeqThreadStatement appendedInstrumentationStatement(
      SeqThreadStatement pStatement, ImmutableList<SeqInstrumentation> pAppended) {

    return pStatement.withInstrumentation(
        FluentIterable.concat(pStatement.instrumentation(), ImmutableList.copyOf(pAppended))
            .toList());
  }

  public static SeqThreadStatement appendedInstrumentationStatement(
      SeqThreadStatement pStatement, SeqInstrumentation... pAppended) {

    return pStatement.withInstrumentation(
        FluentIterable.concat(pStatement.instrumentation(), ImmutableList.copyOf(pAppended))
            .toList());
  }
}
