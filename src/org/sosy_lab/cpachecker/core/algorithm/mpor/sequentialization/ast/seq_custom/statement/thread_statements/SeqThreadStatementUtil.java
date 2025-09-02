// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqInjectedBitVectorStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementUtil {

  /**
   * Returns {@code true} if {@code pCurrentStatement} starts inside an atomic block, but does not
   * actually start it.
   */
  public static boolean startsInAtomicBlock(SeqThreadStatement pStatement) {
    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      ThreadEdge threadEdge = substituteEdge.getThreadEdge();
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  public static boolean anySynchronizesThreads(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.synchronizesThreads()) {
        return true;
      }
    }
    return false;
  }

  public static boolean allHaveTargetGoto(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.getTargetGoto().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static boolean anyContainsEmptyBitVectorEvaluationExpression(
      ImmutableList<SeqThreadStatement> pStatements) {

    for (SeqThreadStatement statement : pStatements) {
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
  private static ImmutableList<SeqThreadStatement> getTargetGotoStatements(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pStatement.getTargetGoto().isPresent()) {
      int targetNumber = pStatement.getTargetGoto().orElseThrow().labelNumber;
      SeqThreadStatementBlock targetBlock =
          Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
      return targetBlock.getStatements();
    }
    return ImmutableList.of();
  }

  // Injected Statements ===========================================================================

  /**
   * This returns either a {@code pc} write of the form {@code pc[i] = n;} including injected
   * statements, if present.
   */
  static String buildInjectedStatements(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    StringBuilder statements = new StringBuilder();
    if (pTargetPc.isPresent()) {
      // first create pruned statements
      ImmutableList<SeqInjectedStatement> pruned = pruneInjectedStatements(pInjectedStatements);
      // create the pc write
      CExpressionAssignmentStatement pcWrite =
          SeqStatementBuilder.buildPcWrite(pPcLeftHandSide, pTargetPc.orElseThrow());
      boolean emptyBitVectorEvaluation =
          SeqThreadStatementUtil.containsEmptyBitVectorEvaluationExpression(pruned);
      // with empty bit vector evaluations, place pc write before injections, otherwise info is lost
      if (emptyBitVectorEvaluation) {
        statements.append(pcWrite.toASTString()).append(SeqSyntax.SPACE);
      }
      // add all injected statements in the correct order
      ImmutableList<SeqInjectedStatement> ordered = orderInjectedStatements(pruned);
      for (int i = 0; i < ordered.size(); i++) {
        SeqInjectedStatement injectedStatement = ordered.get(i);
        statements.append(injectedStatement.toASTString());
        if (i != ordered.size() - 1) {
          // append space to all statements except last
          statements.append(SeqSyntax.SPACE);
        }
      }
      // for non-empty bit vector evaluations, place pc write after injections for optimization
      if (!emptyBitVectorEvaluation) {
        if (!ordered.isEmpty()) {
          statements.append(SeqSyntax.SPACE);
        }
        statements.append(pcWrite.toASTString());
      }

    } else if (pTargetGoto.isPresent()) {
      SeqGotoStatement gotoStatement = new SeqGotoStatement(pTargetGoto.orElseThrow());
      for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
        if (injectedStatement instanceof SeqCountUpdateStatement) {
          // count updates are included, even with target gotos
          statements.append(injectedStatement.toASTString());
        }
      }
      statements.append(gotoStatement.toASTString());
    }
    return statements.toString();
  }

  private static ImmutableList<SeqInjectedStatement> pruneInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    Set<SeqInjectedStatement> pruned = new HashSet<>();
    if (SeqThreadStatementUtil.containsEmptyBitVectorEvaluationExpression(pInjectedStatements)) {
      // prune all bit vector assignments if the evaluation expression is empty
      pruned.addAll(
          pInjectedStatements.stream()
              .filter(s -> s instanceof SeqBitVectorAssignmentStatement)
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
    // TODO add an option that lets user decide if conflict, or bit vector reduction is first
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqConflictOrderStatement conflictOrderStatement) {
        // place conflict order after r < K, otherwise output is unsound
        leftOver.add(conflictOrderStatement);
      }
    }
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqInjectedBitVectorStatement bitVectorStatement) {
        // place conflict order after r < K, otherwise output is unsound
        leftOver.add(bitVectorStatement);
      }
    }
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqLastBitVectorUpdateStatement lastUpdateStatement) {
        // last updates are always last, i.e. placed where pc and bv updates are
        leftOver.add(lastUpdateStatement);
      }
    }
    rOrdered.addAll(
        pInjectedStatements.stream()
            .filter(stmt -> !leftOver.contains(stmt))
            .collect(ImmutableList.toImmutableList()));
    rOrdered.addAll(leftOver);
    return rOrdered.build();
  }

  // Helper ========================================================================================

  static ImmutableList<SeqInjectedStatement> appendInjectedStatements(
      SeqThreadStatement pStatement,
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return ImmutableList.<SeqInjectedStatement>builder()
        .addAll(pStatement.getInjectedStatements())
        .addAll(pAppendedInjectedStatements)
        .build();
  }

  public static Optional<Integer> tryGetTargetPcOrGotoNumber(SeqThreadStatement pStatement) {
    if (pStatement.getTargetPc().isPresent()) {
      return pStatement.getTargetPc();

    } else if (pStatement.getTargetGoto().isPresent()) {
      return Optional.of(pStatement.getTargetGoto().orElseThrow().labelNumber);
    }
    return Optional.empty();
  }
}
