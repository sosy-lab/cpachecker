// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementClauseUtil {

  public static SeqThreadStatementBlock getFirstBlock(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    return pClauses.get(0).getFirstBlock();
  }

  /** Searches for all target {@code pc} in {@code pStatement}. */
  public static ImmutableSet<Integer> collectAllIntegerTargetPc(SeqThreadStatement pStatement) {
    ImmutableSet.Builder<Integer> rAllTargetPc = ImmutableSet.builder();
    if (pStatement.getTargetPc().isPresent()) {
      // add the direct target pc, if present
      rAllTargetPc.add(pStatement.getTargetPc().orElseThrow());
    }
    return rAllTargetPc.build();
  }

  public static ImmutableSet<SubstituteEdge> collectAllSubstituteEdges(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableSet.Builder<SubstituteEdge> rEdges = ImmutableSet.builder();
    for (MPORThread thread : pClauses.keySet()) {
      for (SeqThreadStatementClause clause : pClauses.get(thread)) {
        for (SeqThreadStatementBlock block : clause.getBlocks()) {
          for (SeqThreadStatement statement : block.getStatements()) {
            rEdges.addAll(statement.getSubstituteEdges());
          }
        }
      }
    }
    return rEdges.build();
  }

  public static CExpression getStatementExpressionByEncoding(
      MultiControlStatementEncoding pEncoding,
      CExpression pExpression,
      int pStatementNumber,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build label expression for control encoding " + pEncoding);
      case BINARY_IF_TREE, IF_ELSE_CHAIN ->
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber),
              BinaryOperator.EQUALS);
      case SWITCH_CASE -> SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber);
    };
  }

  public static ImmutableMap<CExpression, SeqThreadStatementClause> mapExpressionToClause(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      CExpression labelExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingStatement,
              pPcLeftHandSide,
              clause.labelNumber,
              pBinaryExpressionBuilder);
      rOriginPcs.put(labelExpression, clause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /**
   * Maps the first {@link SeqThreadStatementBlock} in each {@link SeqThreadStatementClause}s to
   * their label number.
   */
  public static ImmutableMap<Integer, SeqThreadStatementClause> mapLabelNumberToClause(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      rOriginPcs.put(clause.labelNumber, clause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /** Maps {@link SeqThreadStatementBlock}s to their label numbers. */
  public static ImmutableMap<Integer, SeqThreadStatementBlock> mapLabelNumberToBlock(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementBlock> rMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rMap.put(block.getLabel().labelNumber, block);
      }
    }
    return rMap.buildOrThrow();
  }

  /**
   * Ensures that all {@code int} labels in {@code pClauses} are numbered consecutively, i.e. the
   * numbers {@code 0} to {@code pClauses.size() - 1} are present (no gaps).
   *
   * <p>This function also recursively searches for all target {@code pc} and adjusts them
   * accordingly.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause>
      cloneWithConsecutiveLabelNumbers(
          ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rConsecutiveLabels =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      rConsecutiveLabels.putAll(thread, cloneWithConsecutiveLabelNumbers(pClauses.get(thread)));
    }
    return rConsecutiveLabels.build();
  }

  // Including Blocks ==============================================================================

  private static ImmutableList<SeqThreadStatementClause> cloneWithConsecutiveLabelNumbers(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewClauses = ImmutableList.builder();
    ImmutableMap<Integer, Integer> labelBlockMap = mapBlockLabelNumberToIndex(pClauses);
    ImmutableMap<Integer, Integer> labelClauseMap = mapClauseLabelNumberToIndex(pClauses);
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement mergedStatement : block.getStatements()) {
          newStatements.add(replaceTargetPc(mergedStatement, labelBlockMap, labelClauseMap));
        }
        int blockIndex = Objects.requireNonNull(labelBlockMap.get(block.getLabel().labelNumber));
        newBlocks.add(
            block.cloneWithLabelNumber(blockIndex).cloneWithStatements(newStatements.build()));
      }
      int clauseIndex = Objects.requireNonNull(labelClauseMap.get(clause.labelNumber));
      rNewClauses.add(clause.cloneWithLabelNumber(clauseIndex).cloneWithBlocks(newBlocks.build()));
    }
    return rNewClauses.build();
  }

  private static ImmutableMap<Integer, Integer> mapBlockLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = Sequentialization.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rLabelToIndex.put(block.getLabel().labelNumber, index++);
      }
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static ImmutableMap<Integer, Integer> mapClauseLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = Sequentialization.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
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
      int clauseIndex = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      // for injected statements (e.g. bitvector gotos), use the block label
      int blockIndex = Objects.requireNonNull(pLabelBlockMap.get(targetPc));
      ImmutableList<SeqInjectedStatement> replacingInjectedStatements =
          SeqThreadStatementClauseUtil.replaceTargetGotoLabel(
              pCurrentStatement.getInjectedStatements(), blockIndex);
      return pCurrentStatement
          .cloneWithTargetPc(clauseIndex)
          .cloneReplacingInjectedStatements(replacingInjectedStatements);

    } else if (pCurrentStatement.getTargetGoto().isPresent()) {
      SeqBlockLabelStatement label = pCurrentStatement.getTargetGoto().orElseThrow();
      // for gotos, use block labels
      int index = Objects.requireNonNull(pLabelBlockMap.get(label.labelNumber));
      return pCurrentStatement.cloneWithTargetGoto(label.cloneWithLabelNumber(index));
    }
    // no target pc or target goto -> no replacement
    return pCurrentStatement;
  }

  /**
   * Searches {@code pInjectedStatements} for {@link SeqBitVectorEvaluationStatement}s and replaces
   * their {@code goto} labels with the updated {@code pc}.
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
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pCurrent.equals(pTarget)) {
      return true;
    } else {
      SeqThreadStatement firstStatement = pCurrent.getFirstBlock().getFirstStatement();
      SeqThreadStatementClause next =
          pLabelClauseMap.get(firstStatement.getTargetPc().orElseThrow());
      assert next != null : "could not find target case clause";
      if (pCurrent.labelNumber + 1 == next.labelNumber) {
        return isConsecutiveLabelPath(next, pTarget, pLabelClauseMap);
      } else {
        return false;
      }
    }
  }

  // No Upward Goto ================================================================================

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> ensureNoUpwardGoto(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rNoUpwardGoto =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap = mapLabelNumberToBlock(clauses);
      // create set to track which blocks were placed already
      Set<SeqThreadStatementBlock> visited = new HashSet<>();
      ImmutableList.Builder<SeqThreadStatementClause> newClauses = ImmutableList.builder();
      for (SeqThreadStatementClause clause : clauses) {
        Optional<SeqThreadStatementClause> reorderedClause =
            reorderBlocksForClause(clause, labelBlockMap, visited);
        if (reorderedClause.isPresent()) {
          newClauses.add(reorderedClause.orElseThrow());
        }
      }
      assert SeqValidator.validateEqualBlocks(
              ImmutableSet.copyOf(visited), ImmutableSet.copyOf(labelBlockMap.values()))
          : "block sets must be equal before and after reordering";
      rNoUpwardGoto.putAll(thread, newClauses.build());
    }
    return rNoUpwardGoto.build();
  }

  private static Optional<SeqThreadStatementClause> reorderBlocksForClause(
      SeqThreadStatementClause pClause,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      Set<SeqThreadStatementBlock> pVisited) {

    // if there are no target goto in first block, clone clause with only that block
    if (noTargetGoto(pClause.getFirstBlock())) {
      pVisited.add(pClause.getFirstBlock());
      return Optional.of(pClause.cloneWithBlocks(ImmutableList.of(pClause.getFirstBlock())));
    }
    // create list to keep track of new, reordered blocks
    List<SeqThreadStatementBlock> foundOrder = new ArrayList<>();
    // create graph used for dependency checking
    ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> blockGraph =
        ArrayListMultimap.create();
    recursivelyCreateBlockGraph(
        pClause.getFirstBlock(), blockGraph, pLabelBlockMap, new HashSet<>());
    recursivelyReorderBlocks(blockGraph, foundOrder, pVisited);
    if (foundOrder.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(pClause.cloneWithBlocks(ImmutableList.copyOf(foundOrder)));
    }
  }

  private static void recursivelyCreateBlockGraph(
      SeqThreadStatementBlock pCurrentBlock,
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pGraph,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      Set<SeqThreadStatementBlock> pVisited) {

    for (SeqThreadStatement statement : pCurrentBlock.getStatements()) {
      if (statement.getTargetGoto().isPresent()) {
        int targetNumber = statement.getTargetGoto().orElseThrow().labelNumber;
        SeqThreadStatementBlock target = pLabelBlockMap.get(targetNumber);
        assert target != null : "target could not be found in map";
        // add targets only once to prevent infinite recursion (e.g. with loops)
        if (pVisited.add(target)) {
          pGraph.get(pCurrentBlock).add(target);
          recursivelyCreateBlockGraph(target, pGraph, pLabelBlockMap, pVisited);
        }
      }
    }
  }

  private static void recursivelyReorderBlocks(
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pBlockGraph,
      List<SeqThreadStatementBlock> pFoundOrder,
      Set<SeqThreadStatementBlock> pVisited) {

    // first collect and sort blocks with no targeting blocks (= zero in degree)
    ImmutableList<SeqThreadStatementBlock> zeroInDegreeBlocks = getZeroInDegreeBlocks(pBlockGraph);
    ImmutableList<SeqThreadStatementBlock> sortedDescending =
        sortByLabelNumberDescending(zeroInDegreeBlocks);
    for (SeqThreadStatementBlock block : sortedDescending) {
      tryAddToFoundOrder(block, pFoundOrder, pVisited);
    }
    for (SeqThreadStatementBlock block : sortedDescending) {
      for (SeqThreadStatementBlock target : pBlockGraph.get(block)) {
        if (!pBlockGraph.keySet().contains(target)) {
          // if any target is not a key i.e. does not target another block -> add
          tryAddToFoundOrder(target, pFoundOrder, pVisited);
        }
      }
      // remove "used" block
      pBlockGraph.removeAll(block);
    }
    // if there are still blocks in the graph, continue recursive reordering
    if (!pBlockGraph.keySet().isEmpty()) {
      recursivelyReorderBlocks(pBlockGraph, pFoundOrder, pVisited);
    }
  }

  /** Returns the set of zero in-degree blocks i.e. blocks without any incoming edge. */
  private static ImmutableList<SeqThreadStatementBlock> getZeroInDegreeBlocks(
      final ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pBlockGraph) {

    List<SeqThreadStatementBlock> allBlocks = new ArrayList<>(pBlockGraph.keySet());
    for (SeqThreadStatementBlock block : pBlockGraph.keySet()) {
      // remove all blocks that are targeted by any block
      pBlockGraph.get(block).forEach(allBlocks::remove);
    }
    return ImmutableList.copyOf(allBlocks);
  }

  private static ImmutableList<SeqThreadStatementBlock> sortByLabelNumberDescending(
      ImmutableList<SeqThreadStatementBlock> pBlocks) {

    return ImmutableList.sortedCopyOf(
        Comparator.comparingInt((SeqThreadStatementBlock block) -> block.getLabel().labelNumber)
            .reversed(),
        pBlocks);
  }

  private static boolean noTargetGoto(SeqThreadStatementBlock pBlock) {
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      if (statement.getTargetGoto().isPresent()) {
        return false;
      }
    }
    return true;
  }

  private static void tryAddToFoundOrder(
      SeqThreadStatementBlock pBlock,
      List<SeqThreadStatementBlock> pFoundOrder,
      Set<SeqThreadStatementBlock> pVisited) {

    if (!pFoundOrder.contains(pBlock)) {
      // only add blocks once to orders to prevent duplication
      if (pVisited.add(pBlock)) {
        pFoundOrder.add(pBlock);
      }
    }
  }
}
